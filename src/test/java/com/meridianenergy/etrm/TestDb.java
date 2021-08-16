package com.meridianenergy.etrm;

import com.meridianenergy.etrm.core.SchemaBootstrap;

public final class TestDb {

    private static boolean initialized;

    private TestDb() {
    }

    public static synchronized void initialize() {
        System.setProperty("etrm.db.url",
                "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        if (!initialized) {
            SchemaBootstrap.initialize();
            initialized = true;
        }
    }
}
