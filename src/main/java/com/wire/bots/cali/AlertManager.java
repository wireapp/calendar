package com.wire.bots.cali;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

class AlertManager {
    private static final int REMIND_IN = 16;
    private static final int PERIOD = 5;

    private final Timer timer = new Timer();
    private final HashMap<String, Event> remindersMap = new HashMap<>();
    private final ClientRepo repo;

    AlertManager(ClientRepo repo) {
        this.repo = repo;

        crone();
    }

    private void crone() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    ArrayList<String> subscribers = getSubscribers();
                    for (String botId : subscribers) {
                        WireClient wireClient = repo.getWireClient(botId);
                        fetchEvents(wireClient);
                    }
                } catch (Exception e) {
                    Logger.warning("crone: error: %s", e);
                }
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(PERIOD));
    }

    private ArrayList<String> getSubscribers() {
        return new ArrayList<>(); //todo add DB call here
    }

    private void fetchEvents(final WireClient wireClient) {
        try {
            Calendar service = CalendarAPI.getCalendarService(wireClient.getId());
            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                    .setMaxResults(3)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            for (final Event event : events.getItems()) {
                String id = String.format("%s-%s", wireClient.getId(), event.getId());
                if (remindersMap.put(id, event) == null) {
                    final DateTime start = event.getStart().getDateTime();
                    if (start == null)
                        continue;

                    Date at = new Date(start.getValue() - TimeUnit.MINUTES.toMillis(REMIND_IN));
                    if (at.getTime() > System.currentTimeMillis()) {
                        scheduleReminder(wireClient, at, event.getId());
                    }
                }
            }
        } catch (IOException e) {
            //Logger.warning(e.getLocalizedMessage());
        }
    }

    private void scheduleReminder(final WireClient wireClient, final Date at, final String eventId) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String botId = wireClient.getId();
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
                    Logger.warning("scheduleReminder: %s error: %s", wireClient.getId(), e);
                }
            }
        }, at);
    }
}
