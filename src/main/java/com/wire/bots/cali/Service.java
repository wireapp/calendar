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

import com.wire.bots.cali.resources.AuthResource;
import com.wire.bots.cali.resources.NotificationResource;
import com.wire.lithium.ClientRepo;
import com.wire.lithium.Server;
import com.wire.xenon.MessageHandlerBase;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;

public class Service extends Server<Config> {
    static Config CONFIG;
    static ClientRepo repo;
    static Service service;
    private AlertManager alertManager;
    private CommandManager commandManager;

    public static void main(String[] args) throws Exception {
        new Service().run(args);
    }

    @Override
    protected MessageHandlerBase createHandler(Config config, Environment env) {
        return new MessageHandler(alertManager, commandManager, getStorageFactory());
    }

    @Override
    protected void initialize(Config config, Environment env) {
        CONFIG = config;
        service = this;

        alertManager = new AlertManager(jdbi);
        commandManager = new CommandManager(jdbi);
    }

    @Override
    protected void onRun(Config config, Environment env) {
        Service.repo = super.repo;
        addResource(new AuthResource(repo));
        addResource(new NotificationResource(repo));
    }
}
