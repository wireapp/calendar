package com.DAO;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.ArrayList;
import java.util.UUID;

public interface SubscribersDAO {
    @SqlUpdate("INSERT INTO Cali (botId) VALUES (:botId) ON CONFLICT (botId) DO NOTHING")
    int insertSubscriber(@Bind("botId") UUID botId);

    @SqlQuery("SELECT botId FROM Cali")
    ArrayList<UUID> getSubscribers();

    @SqlUpdate("DELETE FROM Cali WHERE botId = :botId")
    int unsubscribe(@Bind("botId") UUID botId);

    @SqlQuery("SELECT muted FROM Cali WHERE botId = :botId")
    boolean isMuted(@Bind("botId") UUID botId);

    @SqlUpdate("UPDATE Cali set muted = :muted WHERE botId = :botId")
    int setMuted(@Bind("botId") UUID botId, @Bind("muted") boolean value);

    @SqlUpdate("UPDATE Cali set schedule = :schedule WHERE botId = :botId")
    int setSchedule(@Bind("botId") UUID botId, @Bind("schedule") String schedule);

    @SqlQuery("SELECT schedule FROM Cali WHERE botId = :botId")
    String getSchedule(@Bind("botId") UUID botId);
}
