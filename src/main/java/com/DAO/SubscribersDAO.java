package com.DAO;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.ArrayList;
import java.util.UUID;

public interface SubscribersDAO {
    @SqlUpdate("INSERT INTO Subscribers (botId) VALUES (:botId) ON CONFLICT (botId) DO NOTHING")
    int insertSubscriber(@Bind("botId") UUID botId);

    @SqlQuery("SELECT botId FROM Subscribers")
    ArrayList<UUID> getSubscribers();

    @SqlUpdate("DELETE FROM Subscribers WHERE botId = :botId")
    int unsubscribe(@Bind("botId") UUID botId);

    @SqlQuery("SELECT muted FROM Subscribers WHERE botId = :botId")
    boolean isMuted(@Bind("botId") UUID botId);

    @SqlUpdate("UPDATE Subscribers set muted = :muted WHERE botId = :botId")
    int setMuted(@Bind("botId") UUID botId, @Bind("muted") boolean value);

    @SqlUpdate("UPDATE Subscribers set schedule = :schedule WHERE botId = :botId")
    int setSchedule(@Bind("botId") UUID botId, @Bind("schedule") String schedule);

    @SqlQuery("SELECT schedule FROM Subscribers WHERE botId = :botId")
    String getSchedule(@Bind("botId") UUID botId);
}
