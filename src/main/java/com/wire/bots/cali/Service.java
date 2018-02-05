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

import com.wire.bots.cryptonite.CryptoService;
import com.wire.bots.cryptonite.StorageService;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.Server;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import io.dropwizard.setup.Environment;

import java.net.URI;

public class Service extends Server<Config> {
    static Config CONFIG;

    public static void main(String[] args) throws Exception {
        new Service().run(args);
    }

    @Override
    protected MessageHandlerBase createHandler(Config config, Environment env) {
        CONFIG = config;
        return new MessageHandler(repo);
    }

    @Override
    protected void onRun(Config config, Environment env) {
        addResource(new AuthResource(repo), env);
    }

    @Override
    protected StorageFactory getStorageFactory(Config config) {
        return botId -> new StorageService("cali", botId, new URI(config.data));
    }

    @Override
    protected CryptoFactory getCryptoFactory(Config config) {
        return (botId) -> new CryptoService(botId, new URI(config.data));
    }
}
