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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.wire.blender.Blender;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends MessageHandlerBase {
    private static final String PREVIEW_PIC_URL = "https://www.elmbrookschools.org/uploaded/images/Google_Suite.png";
    private final CallScheduler callScheduler;
    private final ClientRepo repo;
    private final StorageFactory storageFactory;
    private final ConcurrentHashMap<String, Blender> blenders = new ConcurrentHashMap<>();
    private final AlertManager alertManager;

    MessageHandler(ClientRepo repo, StorageFactory storageFactory) {
        this.repo = repo;
        this.storageFactory = storageFactory;
        this.callScheduler = new CallScheduler(Service.CONFIG.postgres, repo);
        this.alertManager = new AlertManager(Service.CONFIG.postgres, repo);

        try {
            callScheduler.loadSchedules();
        } catch (Exception e) {
            Logger.error("CallScheduler: %s", e);
        }
    }

    @Override
    public String getName(NewBot newBot) {
        return String.format("%s's Google Calendar", newBot.origin.name);
    }

    @Override
    public boolean onNewBot(NewBot newBot) {
        try {
            boolean insertNewSubscriber = alertManager.insertNewSubscriber(newBot.id);
            Logger.info("onNewBot: bot: %s, user: %s %s", newBot.id, newBot.origin.id, insertNewSubscriber);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("onNewBot: bot: %s, error: %s", newBot.id, e);
        }
        return true;
    }

    @Override
    public void onNewConversation(final WireClient client) {
        try {
            //client.sendText("Hey, I just met you and this is crazy\nBut here's my number, so call me maybe");
            showAuthLink(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBotRemoved(String botId) {
        try {
            boolean remove = alertManager.removeSubscriber(botId);
            Logger.info("onBotRemoved. Bot: %s %s", botId, remove);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onCalling(WireClient client, String userId, String clientId, String content) {
        String botId = client.getId();
        Blender blender = getBlender(botId);
        blender.recvMessage(botId, userId, clientId, content);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String text = msg.getText().toLowerCase();

            if (text.equalsIgnoreCase("/auth")) {
                showAuthLink(client);
            } else if (text.equalsIgnoreCase("/list")) {
                listEvents(client);
            } else if (text.startsWith("/cali")) {
                scheduleNewEvent(client, text.replace("/cali", "").trim());
            } else if (text.startsWith("/polly")) {
                scheduleCall(client, text);
            }
        } catch (Exception e) {
            Logger.warning("onText: %s", e.getMessage());
        }
    }

    private void scheduleCall(WireClient client, String text) throws Exception {
        String botId = client.getId();
        Date date = CallScheduler.parse(text);
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm', 'EEEE, MMMMM d, yyyy");
            format.setTimeZone(TimeZone.getTimeZone("CET"));

            boolean scheduled = callScheduler.schedule(botId, date);
            if (scheduled) {
                String schedule = date.toString();
                callScheduler.saveSchedule(botId, schedule);
                client.sendText("OK, I will start the call here at: " + format.format(date));
                Logger.info("Scheduled call for: `%s`, bot: %s", schedule, botId);
            } else {
                client.sendText("I am sorry, but I could not schedule the call for: " + format.format(date));
            }
        } else {
            client.sendText("I am sorry, I could not parse that.");
        }
    }

    private void showAuthLink(WireClient client) throws Exception {
        try {
            String origin = getOwner(client).id;

            String authUrl = CalendarAPI.getAuthUrl(client.getId());
            Picture preview = uploadPreview(client);
            client.sendDirectLinkPreview(authUrl, "Sign in - Google Accounts", preview, origin);
        } catch (Exception e) {
            Logger.error("showAuthLink: bot: %s error: %s", client.getId(), e);
            client.sendText("Something went wrong :(. Try: /auth");
        }
    }

    private void scheduleNewEvent(WireClient client, String text) throws Exception {
        try {
            Event event = CalendarAPI.addEvent(client.getId(), text);
            if (event == null) {
                client.sendText("Sorry, I did not get that.");
                return;
            }

            DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
            DateTime dateTime = event.getStart().getDateTime();
            long value = dateTime.getValue() + TimeUnit.MINUTES.toMillis(dateTime.getTimeZoneShift());
            String s = String.format("I've created new event for you:\n" +
                            "**%s** on %s\n%s",
                    event.getSummary(),
                    format.format(new Date(value)),
                    event.getHtmlLink());
            client.sendText(s);
        } catch (Exception e) {
            Logger.warning("scheduleNewEvent: %s", e.getMessage());
            client.sendText("Something went wrong :(. Try: /auth");
        }
    }

    private void listEvents(WireClient client) throws Exception {
        try {
            final DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
            final StringBuilder sb = new StringBuilder("Upcoming events:\n");

            final Events events = CalendarAPI.listEvents(client.getId(), 5);

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
            Logger.warning("listEvents: %s", e.getMessage());
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

    private Blender getBlender(String botId) {
        return blenders.computeIfAbsent(botId, k -> {
            try {
                String module = Service.CONFIG.getModule();
                String ingress = Service.CONFIG.getIngress();
                int portMin = Service.CONFIG.getPortMin();
                int portMax = Service.CONFIG.getPortMax();

                State storage = storageFactory.create(botId);
                NewBot state = storage.getState();
                Blender blender = new Blender();
                blender.init(module, botId, state.client, ingress, portMin, portMax);
                blender.registerListener(new CallListener(repo));
                return blender;
            } catch (Exception e) {
                Logger.error(e.toString());
                return null;
            }
        });
    }

    private User getOwner(WireClient client) throws Exception {
        String botId = client.getId();
        NewBot state = storageFactory.create(botId).getState();
        Collection<User> users = client.getUsers(Collections.singletonList(state.origin.id));
        return users.stream().findFirst().get();
    }
}
