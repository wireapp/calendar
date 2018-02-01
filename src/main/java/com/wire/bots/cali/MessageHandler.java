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
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends MessageHandlerBase {
    private static final String PREVIEW_PIC_URL = "https://www.elmbrookschools.org/uploaded/images/Google_Suite.png";
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

    MessageHandler(ClientRepo repo) {
        new AlertManager(repo);
    }

    @Override
    public boolean onNewBot(NewBot newBot) {
        Logger.info("onNewBot: bot: %s, user: %s, token: %s",
                newBot.id,
                newBot.origin.id,
                newBot.token
        );

        for (Member member : newBot.conversation.members) {
            if (member.service != null) {
                Logger.warning("Rejecting NewBot. user: %s service: %s",
                        newBot.origin.id,
                        member.service.id);
                return false; // we don't want to be in a conv if other bots are there.
            }
        }
        return true;
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
                    //showAuthLink(client);
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
            } else if (text.equalsIgnoreCase("/cali")) {
                scheduleNewEvent(client, text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAuthLink(WireClient client) throws Exception {
        try {
            String authUrl = CalendarAPI.getAuthUrl(client.getId());
            Picture preview = uploadPreview(client);
            client.sendLinkPreview(authUrl, "Sign in - Google Accounts", preview);
        } catch (Exception e) {
            e.printStackTrace();
            client.sendText("Something went wrong :(");
        }
    }

    private void scheduleNewEvent(WireClient client, String text) throws Exception {
        try {
            Event event = CalendarAPI.addEvent(client.getId(), text);
            if (event == null)
                client.sendText("Sorry, I did not get that.");
            else {
                DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
                DateTime dateTime = event.getStart().getDateTime();
                long value = dateTime.getValue() + TimeUnit.MINUTES.toMillis(dateTime.getTimeZoneShift());
                String s = String.format("I've created new event for you:\n" +
                                "**%s** on %s\n%s",
                        event.getSummary(),
                        format.format(new Date(value)),
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
            DateTime now = new DateTime(System.currentTimeMillis());
            DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");

            Calendar service = CalendarAPI.getCalendarService(client.getId());
            StringBuilder sb = new StringBuilder("Upcoming events:\n");
            Events events = service.events().list("primary")
                    .setMaxResults(5)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            for (Event event : events.getItems()) {
                EventDateTime eventStart = event.getStart();
                DateTime start = eventStart.getDateTime();
                long value = start != null
                        ? start.getValue() + TimeUnit.MINUTES.toMillis(start.getTimeZoneShift())
                        : eventStart.getDate().getValue();
                sb.append(String.format("**%s** on %s\n", event.getSummary(), format.format(new Date(value))));
            }
            client.sendText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            client.sendText("Failed to connect to Google Calendar. Have you signed in? Type: `/auth` and sign in " +
                    "to your Google account");
        }
    }

    private Picture uploadPreview(WireClient client) throws Exception {
        Picture preview = new Picture(PREVIEW_PIC_URL);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }
}
