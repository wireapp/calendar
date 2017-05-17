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
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.TextMessage;
import io.dropwizard.setup.Environment;

import java.util.List;

public class MessageHandler extends MessageHandlerBase {
    private final Config config;

    public MessageHandler(Config config, Environment env) {
        this.config = config;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            Calendar service = CalendarAPI.getCalendarService("user");

            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();
            if (items.size() == 0) {
                client.sendText("No upcoming events found.");
            } else {
                StringBuilder sb = new StringBuilder("Upcoming events\n");
                for (Event event : items) {
                    DateTime start = event.getStart().getDateTime();
                    if (start == null) {
                        start = event.getStart().getDate();
                    }
                    sb.append(String.format("%s (%s)\n", event.getSummary(), start));
                }
                client.sendText(sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            String authUrl = CalendarAPI.getAuthUrl(client.getId());
            client.sendText(authUrl);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

}
