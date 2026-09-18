package com.wtc.realestate.dao;

import com.wtc.realestate.model.Realtor;
import com.wtc.realestate.util.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RealtorDao {

    public Realtor login(String username, String password) {
        String query = "SELECT * FROM realtors WHERE username = '" + username
                + "' AND password = '" + password + "'";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return new Realtor(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Login query failed", e);
        }
    }

    public Realtor findById(int id) {
        String query = "SELECT * FROM realtors WHERE id = " + id; // also vulnerable — fixed in Phase 4

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return new Realtor(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Find realtor query failed", e);
        }
    }
}
