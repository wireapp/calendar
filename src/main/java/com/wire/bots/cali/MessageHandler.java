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

public class MessageHandler extends MessageHandlerBase {
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

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            if (msg.getText().equalsIgnoreCase("/auth")) {
                String authUrl = CalendarAPI.getAuthUrl(client.getId());
                client.sendText(authUrl);
            } else if (msg.getText().equalsIgnoreCase("/list")) {
                Calendar service = CalendarAPI.getCalendarService(client.getId());
                String text = listEvents(service);
                client.sendText(text);
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
            DateTime start = event.getStart().getDateTime();
            sb.append(String.format("%s at %s\n", event.getSummary(), start));
        }
        return sb.toString();
    }
}
