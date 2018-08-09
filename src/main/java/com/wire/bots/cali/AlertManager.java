package com.wire.bots.cali;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

class AlertManager {
    private static final int REMIND_IN = 16;
    private static final int PERIOD = 5;

    private final Timer timer = new Timer();
    private final HashMap<String, Event> remindersMap = new HashMap<>();
    private final ClientRepo repo;
    private final Database database;

    AlertManager(Config.DB postgres, ClientRepo repo) {
        this.repo = repo;
        this.database = new Database(postgres);

        crone();
    }

    boolean insertNewSubscriber(String botId) throws Exception {
        return database.insertSubscriber(botId);
    }

    private void crone() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    ArrayList<String> subscribers = database.getSubscribers();
                    for (String botId : subscribers) {
                        WireClient wireClient = repo.getWireClient(botId);
                        if (wireClient == null) {
                            database.unsubscribe(botId);
                            continue;
                        }
                        fetchEvents(wireClient);
                    }
                } catch (Exception e) {
                    Logger.warning("crone: error: %s", e);
                }
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(PERIOD));
    }

    private void fetchEvents(final WireClient wireClient) {
        try {
            String botId = wireClient.getId();
            Events events = CalendarAPI.listEvents(botId, 3);

            for (final Event event : events.getItems()) {
                try {
                    String id = String.format("%s-%s", botId, event.getId());
                    if (remindersMap.put(id, event) == null) {
                        Event.Reminders reminders = event.getReminders();
                        if (reminders.getOverrides() == null) {
                            for (EventReminder reminder : events.getDefaultReminders()) {
                                scheduleReminder(wireClient, event, reminder.getMinutes());
                            }
                        } else {
                            for (EventReminder reminder : reminders.getOverrides()) {
                                scheduleReminder(wireClient, event, reminder.getMinutes());
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.warning("AlertManager.fetchEvents: %s %s %s", botId, event.getId(), e);
                }
            }
        } catch (IOException e) {
            // Logger.warning("AlertManager.fetchEvents: %s", e);
        }
    }

    private boolean scheduleReminder(WireClient wireClient, Event event, int remindIn) {
        final DateTime start = event.getStart().getDateTime();
        if (start != null) {
            Date at = new Date(start.getValue() - TimeUnit.MINUTES.toMillis(remindIn));
            if (at.getTime() > System.currentTimeMillis()) {
                scheduleReminder(wireClient, at, event.getId());
                return true;
            }
        }
        return false;
    }

    private void scheduleReminder(final WireClient wireClient, final Date at, final String eventId) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String botId = wireClient.getId();
                try {
                    Event e = CalendarAPI.getEvent(botId, eventId);
                    if (e != null) {
                        DateTime eventStart = e.getStart().getDateTime();

                        long l = eventStart.getValue() - System.currentTimeMillis();
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(l);

                        wireClient.ping();
                        String msg = String.format("**%s** in **%d** minutes", e.getSummary(), minutes);
                        wireClient.sendText(msg);
                    }
                } catch (Exception e) {
                    Logger.warning("scheduleReminder: %s error: %s", botId, e);
                }
            }
        }, at);
    }

    boolean removeSubscriber(String botId) throws SQLException {
        return database.unsubscribe(botId);
    }
}
