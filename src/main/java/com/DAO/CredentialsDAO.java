package com.DAO;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.ArrayList;
import java.util.UUID;

public interface CredentialsDAO {
    @SqlUpdate("UPDATE Credentials set credentials = :credentials WHERE id = :id")
    int setCredentials(@Bind("id") String id, @Bind("credentials") String credentials);

    @SqlQuery("SELECT credentials FROM Credentials WHERE id = :id")
    String getCredentials(@Bind("id") String id);
}
