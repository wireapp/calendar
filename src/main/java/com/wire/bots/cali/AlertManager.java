package com.wire.bots.cali;

import com.DAO.SubscribersDAO;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;
import com.wire.lithium.ClientRepo;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.MessageText;
import com.wire.xenon.assets.Ping;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

class AlertManager {
    private static final int PERIOD = 1;
    private final DateFormat dateFormat = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
    private final Timer timer = new Timer();
    private final HashMap<String, Event> remindersMap = new HashMap<>();
    private final SubscribersDAO subscribersDAO;

    AlertManager(Jdbi jdbi) {
        subscribersDAO = jdbi.onDemand(SubscribersDAO.class);
    }

    boolean insertNewSubscriber(UUID botId) {
        final int i = subscribersDAO.insertSubscriber(botId);
        return i != 0;
    }

    void crone(final ClientRepo repo) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    ArrayList<UUID> subscribers = subscribersDAO.getSubscribers();
                    for (UUID botId : subscribers) {
                        try (WireClient wireClient = repo.getClient(botId)) {
                            if (wireClient == null) {
                                subscribersDAO.unsubscribe(botId);
                                continue;
                            }
                            fetchEvents(wireClient);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.warning("crone: error: %s", e);
                }
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(PERIOD));
    }

    private void fetchEvents(final WireClient wireClient) {
        try {
            final UUID botId = wireClient.getId();
            Events events = CalendarAPI.listEvents(botId.toString(), 1);

            for (final Event event : events.getItems()) {
                try {
                    int i = 0;
                    List<EventReminder> overrides = event.getReminders().getOverrides();
                    if (overrides != null) {
                        for (EventReminder reminder : overrides) {
                            scheduleReminder(wireClient, event, reminder, i++);
                        }
                    } else {
                        for (EventReminder reminder : events.getDefaultReminders()) {
                            scheduleReminder(wireClient, event, reminder, i++);
                        }
                    }
                } catch (Exception e) {
                    Logger.warning("AlertManager.fetchEvents: %s %s %s", botId, event.getId(), e);
                }
            }
        } catch (IOException e) {
            Logger.warning("AlertManager.fetchEvents: %s", e);
        }
    }

    private void scheduleReminder(WireClient wireClient, Event event, EventReminder reminder, int i) {
        String id = String.format("%s-%s-%d", wireClient.getId(), event.getId(), i);
        if (remindersMap.put(id, event) != null)
            return;

        final DateTime start = event.getStart().getDateTime();
        if (start != null) {
            Date at = new Date(start.getValue() - TimeUnit.MINUTES.toMillis(reminder.getMinutes()));
            if (at.getTime() > System.currentTimeMillis()) {
                scheduleReminder(wireClient, at, event.getId());
            }
        }
    }

    private void scheduleReminder(final WireClient wireClient, final Date at, final String eventId) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                UUID botId = wireClient.getId();
                try {
                    Event event = CalendarAPI.getEvent(botId.toString(), eventId);
                    if (event != null) {
                        boolean muted = subscribersDAO.isMuted(botId);
                        if (muted) {
                            Logger.info("scheduleReminder: %s Event: %s Muted", botId, event.getId());
                            return;
                        }

                        if (Objects.equals("cancelled", event.getStatus())) {
                            Logger.info("scheduleReminder: %s Event: %s Cancelled: %s", botId, event.getId(), event.getStatus());
                            return;
                        }

                        int timeZoneShift = event.getStart().getDateTime().getTimeZoneShift();
                        long start = event.getStart().getDateTime().getValue();
                        int minutes = Math.round((start - System.currentTimeMillis()) / 60000f);

                        String msg = String.format("Starting in %d minutes\n[%s](%s)\n%s",
                                minutes,
                                event.getSummary(),
                                event.getHtmlLink(),
                                dateFormat.format(new Date(start + TimeUnit.MINUTES.toMillis(timeZoneShift))));

                        wireClient.send(new Ping());
                        wireClient.send(new MessageText(msg));
                    }
                } catch (Exception e) {
                    Logger.warning("scheduleReminder: %s error: %s", botId, e);
                }
            }
        }, at);
    }

    boolean removeSubscriber(UUID botId) {
        return 0 != subscribersDAO.unsubscribe(botId);
    }
}
