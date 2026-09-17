package com.wtc.realestate.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost3306/realestate");
                String user = System.getenv().getOrDefault("DB_USER". "root");
                String password = System.getenv().getOrDefault("DB_PASSWORD", "");

                connection = DriverManager.getConnection(url, user,password);
                System.out.println("Connected to database: " + url);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to database", e))
            }
        }
        return connection;
    }
}