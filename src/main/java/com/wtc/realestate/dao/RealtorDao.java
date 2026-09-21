package com.wtc.realestate.dao;

import org.mindrot.jbcrypt.BCrypt;
import com.wtc.realestate.model.Realtor;
import com.wtc.realestate.util.Database;

import java.sql.*;

public class RealtorDao {

    public Realtor login(String username, String password) {
        String query = "SELECT * FROM realtors WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");

                    if(BCrypt.checkpw(password,storedHash)){
                        return new Realtor(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("password")
                        );
                    }
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Login query failed", e);
        }
    }

    public Realtor findById(int id) {
        String query = "SELECT * FROM realtors WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Realtor(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password")
                    );
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Find realtor query failed", e);
        }
    }
}