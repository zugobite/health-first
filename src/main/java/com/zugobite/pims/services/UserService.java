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

/**
 * Data-access layer for the `users` table: login authentication plus Admin's Cashier
 * account management (create/list/delete). Every method opens and closes its own JDBC
 * {@link Connection} via {@link DBConnection} — there is no persistent connection or
 * connection pool, which is fine for this single-user desktop app.
 *
 * <p>All queries use {@link PreparedStatement} with bound parameters rather than string
 * concatenation, per PLAN.md §6's explicit requirement ("especially for login") — this is
 * what prevents SQL injection via a crafted username/password at the login screen.</p>
 */
public class UserService {

    /**
     * Checks a username/password pair against the `users` table and, on a match, returns
     * the corresponding {@link User} (role included, so the caller can route to the right
     * dashboard). Returns {@link Optional#empty()} — not an exception — when the credentials
     * simply don't match, since a failed login is expected user behavior, not an error
     * condition; {@link SQLException} is reserved for actual database/connectivity failures.
     *
     * <p><b>Maintainer note:</b> passwords are compared as plaintext here because PLAN.md's
     * sample seed credentials (`admin123`, `cash123`) are plaintext and the assignment
     * explicitly allows this baseline. Upgrading to hashed passwords (e.g. BCrypt) would
     * only require changing this one query to fetch the stored hash and verify it, plus
     * updating {@code database.sql}'s seed INSERTs — no other class depends on the storage
     * format.</p>
     */
    public Optional<User> authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, role, full_name FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // The DB stores 'Admin'/'Cashier' (matching the ENUM in database.sql);
                    // Role's constants are ADMIN/CASHIER, hence the uppercase before valueOf().
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    return Optional.of(new User(rs.getInt("user_id"), rs.getString("username"), role, rs.getString("full_name")));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lists every Cashier account for {@link com.zugobite.pims.views.ManageUsersPanel}.
     * Deliberately filtered to {@code role = 'Cashier'} so the Admin account itself never
     * shows up in — and therefore can never be accidentally deleted from — this screen.
     */
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

    /**
     * Inserts a new Cashier account. The role is hardcoded to {@code 'Cashier'} in the SQL
     * itself (not a parameter) — this method has no way to create an Admin account, which is
     * intentional: PLAN.md scopes Admin's user management to Cashier accounts only.
     *
     * @throws SQLException if, among other causes, {@code username} collides with the
     *                       `UNIQUE` constraint on that column
     */
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

    /**
     * Deletes a user account by ID.
     *
     * @throws SQLException if the `sales.user_id` foreign key blocks the delete because this
     *                       user has processed sales in the past — those historical sales
     *                       records must be preserved, so the schema won't let the cashier
     *                       who created them be removed while they're still referenced
     */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
}
