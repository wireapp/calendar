package com.wire.bots.cali;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AlertManager {
    private static final int REMIND_IN = 15;
    private static final int PERIOD = 15;

    private final Timer timer = new Timer();
    private final HashMap<String, Event> remindersMap = new HashMap<>();
    private final ClientRepo repo;

    AlertManager(ClientRepo repo) {
        this.repo = repo;
        
        schedule();
    }

    private void schedule() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (WireClient client : getClients()) {
                    fetchEvents(client, 3);
                }
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(PERIOD));
    }

    private void fetchEvents(final WireClient wireClient, int count) {
        try {
            Calendar service = CalendarAPI.getCalendarService(wireClient.getId());
            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                    .setMaxResults(count)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            for (final Event event : events.getItems()) {
                String id = String.format("%s-%s", wireClient.getId(), event.getId());
                if (remindersMap.put(id, event) == null) {
                    scheduleReminder(wireClient, event);
                }
            }
        } catch (IOException e) {
            //Logger.warning(e.getLocalizedMessage());
        }
    }

    private void scheduleReminder(final WireClient wireClient, final Event event) {
        DateTime start = event.getStart().getDateTime();
        //long startUTC = start.getValue() - start.getTimeZoneShift();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    wireClient.ping();
                    String msg = String.format("**%s** in **%d** minutes", event.getSummary(), REMIND_IN);
                    wireClient.sendText(msg);
                } catch (Exception e) {
                    Logger.warning(e.getLocalizedMessage());
                }
            }
        }, new Date(start.getValue() - TimeUnit.MINUTES.toMillis(REMIND_IN)));
    }

    private ArrayList<WireClient> getClients() {
        final ArrayList<WireClient> ret = new ArrayList<>();
        File dir = new File(repo.getPath());
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String botId = file.getName();
                WireClient wireClient = repo.getWireClient(botId);
                boolean valid = wireClient != null;
                if (valid)
                    ret.add(wireClient);
                return valid;
            }
        });
        return ret;
    }
}
