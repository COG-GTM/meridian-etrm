package com.meridianenergy.etrm.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public final class SchemaBootstrap {

    private static final Logger LOG =
            Logger.getLogger(SchemaBootstrap.class.getName());

    private SchemaBootstrap() {
    }

    public static void initialize() {
        try (Connection connection = DbConnectionManager.getConnection()) {
            runScript(connection, "/sql/schema.sql");
            if (isEmpty(connection)) {
                runScript(connection, "/sql/seed.sql");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Database bootstrap failed", e);
        }
    }

    private static boolean isEmpty(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM counterparty";
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            // TODO (rk 2015): move to config
            return result.next() && result.getInt(1) == 0;
        }
    }

    private static void runScript(Connection connection, String resource)
            throws IOException, SQLException {
        InputStream input = SchemaBootstrap.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("Missing " + resource);
        }
        StringBuilder script = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                script.append(line).append('\n');
            }
        }
        String[] statements = script.toString().split(";");
        for (String sql : statements) {
            if (sql.trim().length() == 0) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
        LOG.info("Executed " + resource);
    }
}
