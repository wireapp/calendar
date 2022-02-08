package com.wire.bots.cali;

import com.DAO.SubscribersDAO;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.jdbi.v3.core.Jdbi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;

public class DatabaseTest {
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
    public void testSubscribersDAO() {
        final SubscribersDAO subscribersDAO = jdbi.onDemand(SubscribersDAO.class);

        final UUID botId = UUID.randomUUID();
        final int insertSubscriber = subscribersDAO.insertSubscriber(botId);
        final ArrayList<UUID> subscribers = subscribersDAO.getSubscribers();

        subscribersDAO.setSchedule(botId, "schedule");
        final String schedule = subscribersDAO.getSchedule(botId);

        subscribersDAO.setMuted(botId, true);
        final boolean muted = subscribersDAO.isMuted(botId);

        final int unsubscribe = subscribersDAO.unsubscribe(botId);

    }
}
