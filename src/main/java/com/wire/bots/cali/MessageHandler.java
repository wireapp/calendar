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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MessageHandler extends MessageHandlerBase {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

    MessageHandler(ClientRepo repo) {
        new AlertManager(repo);
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
            String text = msg.getText();
            if (text.equalsIgnoreCase("/auth")) {
                showAuthLink(client);
            } else if (text.equalsIgnoreCase("/list")) {
                showCalendar(client);
            } else {
                scheduleNewEvent(client, text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAuthLink(WireClient client) throws Exception {
        try {
            String authUrl = CalendarAPI.getAuthUrl(client.getId());
            client.sendText(authUrl);
        } catch (Exception e) {
            e.printStackTrace();
            client.sendText("Something went wrong :(");
        }
    }
    
    private void scheduleNewEvent(WireClient client, String text) throws Exception {
        try {
            Event event = CalendarAPI.addEvent(client.getId(), text.replace("/new", ""));
            if (event == null)
                client.sendText("Sorry, I did not get that.");
            else {
                String s = String.format("I've created new event for you:\n" +
                                "**%s** on %s\n%s",
                        event.getSummary(),
                        event.getStart().getDateTime().toString(),
                        event.getHtmlLink());
                client.sendText(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
            client.sendText("Something went wrong :(");
        }
    }

    private void showCalendar(WireClient client) throws Exception {
        try {
            Calendar service = CalendarAPI.getCalendarService(client.getId());
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
            client.sendText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            client.sendText("Failed to connect to Google Calendar. Have you signed in? Type: `/auth` and sign in " +
                    "to your Google account");
        }
    }

    private Picture uploadPreview(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }
}
