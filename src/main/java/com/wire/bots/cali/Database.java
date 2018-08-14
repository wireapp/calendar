package com.wire.bots.cali;

import com.wire.bots.sdk.Configuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

class Database {
    private final Configuration.DB conf;

    Database(Configuration.DB postgres) {
        this.conf = postgres;
    }

    boolean insertSubscriber(String botId) throws Exception {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("INSERT INTO Cali (botId) VALUES (?) ON CONFLICT (botId) DO NOTHING");
            stmt.setObject(1, UUID.fromString(botId));
            return stmt.executeUpdate() == 1;
        }
    }

    ArrayList<String> getSubscribers() throws Exception {
        ArrayList<String> ret = new ArrayList<>();
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT botId FROM Cali");
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                ret.add(resultSet.getString("botId"));
            }
        }
        return ret;
    }

    boolean setSchedule(String botId, String schedule) throws Exception {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("UPDATE Cali set schedule = ? WHERE botId = ?");
            stmt.setString(1, schedule);
            stmt.setObject(2, UUID.fromString(botId));
            return stmt.executeUpdate() == 1;
        }
    }

    String getSchedule(String botId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT schedule FROM Cali WHERE botId = ?");
            stmt.setObject(1, UUID.fromString(botId));
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("schedule");
            }
        }
        return null;
    }

    boolean deleteSchedule(String botId) throws Exception {
        return setSchedule(botId, null);
    }

    boolean unsubscribe(String botId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("DELETE FROM Cali WHERE botId = ?");
            stmt.setObject(1, UUID.fromString(botId));
            return stmt.executeUpdate() == 1;
        }
    }

    private Connection newConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s", conf.host, conf.port, conf.database);
        return DriverManager.getConnection(url, conf.user, conf.password);
    }
}
