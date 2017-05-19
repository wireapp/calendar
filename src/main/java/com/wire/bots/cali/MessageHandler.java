//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.cali;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.TextMessage;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends MessageHandlerBase {
    private static final int REMIND_IN = 15;
    private static final int PERIOD = 15;

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);
    private final Timer timer = new Timer();
    private final HashMap<String, Event> remindersMap = new HashMap<>();

    private final ClientRepo repo;

    MessageHandler(ClientRepo repo) {
        this.repo = repo;

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
            Logger.warning(e.getLocalizedMessage());
        }
    }

    private void scheduleReminder(final WireClient wireClient, final Event event) {
        DateTime start = event.getStart().getDateTime();
        long startUTC = start.getValue() - start.getTimeZoneShift();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String msg = String.format("**%s** in **%d** minutes", event.getSummary(), REMIND_IN);
                    wireClient.sendText(msg);
                } catch (Exception e) {
                    Logger.warning(e.getLocalizedMessage());
                }
            }
        }, new Date(startUTC - TimeUnit.MINUTES.toMillis(REMIND_IN)));
    }

    @Override
    public String getName() {
        return "Cali";
    }

    @Override
    public void onNewConversation(final WireClient client) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String authUrl = CalendarAPI.getAuthUrl(client.getId());

                    Picture preview = uploadPreview(client, "https://www.elmbrookschools.org/uploaded/images/Google_Suite.png");
                    client.sendLinkPreview(authUrl, "Sign in - Google Accounts", preview);
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.error(e.getMessage());
                }
            }
        });
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            if (msg.getText().equalsIgnoreCase("/auth")) {
                String authUrl = CalendarAPI.getAuthUrl(client.getId());
                client.sendText(authUrl);
            } else if (msg.getText().equalsIgnoreCase("/list")) {
                try {
                    Calendar service = CalendarAPI.getCalendarService(client.getId());

                    String text = listEvents(service);
                    client.sendText(text);
                } catch (GoogleJsonResponseException ex) {
                    Logger.warning(ex.getLocalizedMessage());
                    client.sendText("Failed to connect to Google Calendar. Have you signed in? Type: `/auth` and sign in " +
                            "to your Google account");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String listEvents(Calendar service) throws java.io.IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary")
                .setMaxResults(5)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        StringBuilder sb = new StringBuilder("Upcoming events:\n");
        for (Event event : events.getItems()) {
            EventDateTime eventStart = event.getStart();
            DateTime start = eventStart.getDateTime();
            sb.append(String.format("**%s** at %s\n", event.getSummary(), start));
        }
        return sb.toString();
    }

    private Picture uploadPreview(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }

    private ArrayList<WireClient> getClients() {
        final ArrayList<WireClient> ret = new ArrayList<>();
        File dir = new File(repo.getPath());
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String botId = file.getName();
                WireClient wireClient = repo.getWireClient(botId);
                if (wireClient != null)
                    ret.add(wireClient);
                return wireClient != null;
            }
        });
        return ret;
    }
}
