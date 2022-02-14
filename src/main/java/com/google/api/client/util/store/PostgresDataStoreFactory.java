/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.util.store;

import com.DAO.SubscribersDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

public class PostgresDataStoreFactory extends AbstractDataStoreFactory {
    SubscribersDAO subscribersDAO;

    public PostgresDataStoreFactory(Jdbi jdbi) {
        subscribersDAO = jdbi.onDemand(SubscribersDAO.class);
    }

    @Override
    protected <V extends Serializable> DataStore<V> createDataStore(String id) throws IOException {
        return new PostgresDataStore<>(this, subscribersDAO, id);
    }

    /**
     * File data store that inherits from the abstract memory data store because the key-value pairs
     * are stored in a memory cache, and saved in the file (see {@link #save()} when changing values.
     *
     * @param <V> serializable type of the mapped value
     */
    private static class PostgresDataStore<V extends Serializable> extends AbstractMemoryDataStore<V> {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final SubscribersDAO subscribersDAO;

        PostgresDataStore(PostgresDataStoreFactory dataStore, SubscribersDAO subscribersDAO, String id) throws IOException {
            super(dataStore, id);
            this.subscribersDAO = subscribersDAO;

            UUID botId = UUID.fromString(getId());

            String value = subscribersDAO.getCredentials(botId);
            // create new file (if necessary)
            if (value == null) {
                keyValueMap = Maps.newHashMap();
                // save the credentials
                Logger.info("Saving credentials for %s", id);
                save();
            } else {
                // load credentials
                Logger.info("Loading credentials for %s", id);
                keyValueMap = objectMapper.readValue(value, new TypeReference<>() {
                });
            }
        }

        @Override
        public void save() throws IOException {
            String credentials = objectMapper.writeValueAsString(keyValueMap);
            UUID botId = UUID.fromString(getId());
            subscribersDAO.setCredentials(botId, credentials);
        }
    }
}
