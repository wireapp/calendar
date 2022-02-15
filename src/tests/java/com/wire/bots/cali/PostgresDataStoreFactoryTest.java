package com.wire.bots.cali;

import com.google.api.client.util.store.DataStore;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.jdbi.v3.core.Jdbi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

public class PostgresDataStoreFactoryTest {
    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
            Service.class, "cali.yaml",
            ConfigOverride.config("token", "TcZA2Kq4GaOcIbQuOvasrw34321cZAfLW4Ga54fsds43hUuOdcdm42"));
    private Jdbi jdbi;

    @Before
    public void beforeClass() throws Exception {
        SUPPORT.before();
        Service application = SUPPORT.getApplication();
        jdbi = application.getJdbi();
    }

    @After
    public void afterClass() {

        SUPPORT.after();
    }

    @Test
    public void credentialsTest() throws IOException {
        String credentials = "These are some very secret credentials";
        PostgresDataStoreFactory factory = new PostgresDataStoreFactory(jdbi);

        String id = "dfdsfsd";
        DataStore<Serializable> dataStore = factory.getDataStore(id);

        DataStore<Serializable> set = dataStore.set(id, credentials);

        Serializable serializable = dataStore.get(id);

    }
}
