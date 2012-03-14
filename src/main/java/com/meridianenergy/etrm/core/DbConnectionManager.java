package com.meridianenergy.etrm.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbConnectionManager {

    private DbConnectionManager() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty(
                "etrm.db.url", "jdbc:h2:./data/etrm;AUTO_SERVER=FALSE");
        return DriverManager.getConnection(url, "sa", "");
    }
}
