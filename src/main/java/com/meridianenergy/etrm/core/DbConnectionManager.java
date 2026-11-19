package com.meridianenergy.etrm.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class DbConnectionManager {

    // H2 closes an embedded database when its last connection closes, so
    // without a held connection every query reopens the database file.
    private static final Map<String, Connection> KEEP_OPEN =
            new HashMap<String, Connection>();

    private DbConnectionManager() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty(
                "etrm.db.url", "jdbc:h2:./data/etrm;AUTO_SERVER=FALSE");
        keepOpen(url);
        return DriverManager.getConnection(url, "sa", "");
    }

    private static synchronized void keepOpen(String url) throws SQLException {
        Connection held = KEEP_OPEN.get(url);
        if (held == null || held.isClosed()) {
            KEEP_OPEN.put(url, DriverManager.getConnection(url, "sa", ""));
        }
    }

    public static synchronized void closeAll() throws SQLException {
        for (Connection held : KEEP_OPEN.values()) {
            held.close();
        }
        KEEP_OPEN.clear();
    }
}
