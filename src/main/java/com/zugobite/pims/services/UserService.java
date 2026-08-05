package com.zugobite.pims.services;

import com.zugobite.pims.data.DBConnection;
import com.zugobite.pims.enums.Role;
import com.zugobite.pims.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {

    public Optional<User> authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, role, full_name FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    return Optional.of(new User(rs.getInt("user_id"), rs.getString("username"), role, rs.getString("full_name")));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> getCashiers() throws SQLException {
        List<User> cashiers = new ArrayList<>();
        String sql = "SELECT user_id, username, role, full_name FROM users WHERE role = 'Cashier'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                cashiers.add(new User(rs.getInt("user_id"), rs.getString("username"), Role.CASHIER, rs.getString("full_name")));
            }
        }
        return cashiers;
    }

    public void createCashier(String username, String password, String fullName) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'Cashier', ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullName);
            stmt.executeUpdate();
        }
    }

    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
}
