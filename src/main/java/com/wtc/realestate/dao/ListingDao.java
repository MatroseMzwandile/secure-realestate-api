package com.wtc.realestate.dao;

import com.wtc.realestate.model.Listing;
import com.wtc.realestate.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ListingDao {

    public List<Listing> findAll() {
        String query = "SELECT * FROM listings ORDER BY created_at DESC";
        List<Listing> listings = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()){
                listings.add(mapRow(rs));
            }
            return listings;
        } catch (SQLException e) {
            throw new RuntimeException("Fetching listings failed", e);
        }
    }


    public Listing findById(int id) {
        String query = "SELECT * FROM listings WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1,id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Fetching listing failed", e);
        }
    }


    public void create(Listing listing){
        String query = "INSERT INTO listings (title, description, price, realtor_id) VALUES (? , ? ,? , ?)";

        try(Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, listing.getTitle());
            stmt.setString(2, listing.getDescription());
            stmt.setInt(3,listing.getPrice().intValue());
            stmt.setInt(4,listing.getRealtorId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Creating listing failed", e);
        }
    }


    public boolean update(Listing listing){
        String query = "UPDATE listings SET title = ?, description = ?, price = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, listing.getTitle());
            stmt.setString(2, listing.getDescription());
            stmt.setInt(3, listing.getPrice().intValue());
            stmt.setInt(4, listing.getId());

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Updating listing failed", e);
        }
    }


    public boolean delete(int id) {
        String query = "DELETE FROM listings WHERE id= ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1,id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Deleting listing failed", e);
        }
    }


    private Listing mapRow(ResultSet rs) throws SQLException {
        return new Listing(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getInt("realtor_id")
        );
    }
}
