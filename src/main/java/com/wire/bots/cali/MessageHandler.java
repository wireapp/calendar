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

import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.MessageText;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.backend.models.SystemMessage;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.factories.StorageFactory;
import com.wire.xenon.models.TextMessage;
import com.wire.xenon.tools.Logger;

import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private final StorageFactory storageF;
    private final AlertManager alertManager;
    private final CommandManager commandManager;

    MessageHandler(AlertManager alertManager, CommandManager commandManager, StorageFactory storageF) {
        this.storageF = storageF;
        this.alertManager = alertManager;
        this.commandManager = commandManager;
    }

    @Override
    public String getName(NewBot newBot) {
        return String.format("%s's Calendar", newBot.origin.name);
    }

    @Override
    public boolean onNewBot(NewBot newBot, String accessToken) {
        try {
            final boolean b = alertManager.insertNewSubscriber(newBot.id);
            Logger.info("onNewBot: bot: %s, user: %s %s", newBot.id, newBot.origin.id, b);
        } catch (Exception e) {
            Logger.error("onNewBot: bot: %s, error: %s", newBot.id, e);
        }
        return true;
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage message) {
        try {
            client.send(new MessageText("Hello!\n" +
                    "Thank you for adding me here. Follow this link to connect me to one of your calendars."));
            commandManager.showAuthLink(client, getOwner(client));
        } catch (Exception e) {
            Logger.warning("onNewConversation: %s %s", client.getId(), e);
        }
    }

    @Override
    public void onBotRemoved(UUID botId, SystemMessage msg) {
        try {
            boolean remove = alertManager.removeSubscriber(botId);
            Logger.info("onBotRemoved. Bot: %s %s", botId, remove);
        } catch (Exception e) {
            Logger.error("onBotRemoved: %s %s", botId, e);
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            commandManager.processCommand(client, msg.getUserId(), msg.getText());
        } catch (Exception e) {
            Logger.warning("onText: %s %s", client.getId(), e);
        }
    }

    private User getOwner(WireClient client) throws Exception {
        final UUID botId = client.getId();
        NewBot state = storageF.create(botId).getState();
        return client.getUser(state.origin.id);
    }
}
