package com.wtc.realestate.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection getConnection() {
        try {
            String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/realestate");
            String user = System.getenv().getOrDefault("DB_USER", "root");
            String password = System.getenv().getOrDefault("DB_PASSWORD", "");

            Connection connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database: " + url);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
}