package org.example.chat;

import org.example.chat.util.Logger;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {

    public static DataSource createDataSource() {

        PGSimpleDataSource ds = new PGSimpleDataSource();

        ds.setURL(System.getenv("DB_URL"));
        ds.setUser(System.getenv("DB_USER"));
        ds.setPassword(System.getenv("DB_PASSWORD"));

        Logger.info("Postgres DataSource created.");

        return ds;
    }
}