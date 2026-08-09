package com.zugobite.pims.models;

import com.zugobite.pims.enums.Role;

/**
 * A logged-in account: either the pharmacy's Admin or one of its Cashiers. Maps 1:1 to a
 * row in the `users` table (see {@code database.sql}), minus the password - a {@code User}
 * only ever exists in memory *after* successful authentication, so there is never a reason
 * to carry the password hash/plaintext around once the login check has already happened.
 *
 * <p>Immutable by design: nothing in the UI ever needs to change a logged-in user's own
 * identity mid-session, so all fields are {@code final} with no setters.</p>
 */
public class User {

    private final int userId;
    private final String username;
    private final Role role;
    private final String fullName;

    /**
     * Builds the post-auth identity used across the session (no password).
     * Typically produced by {@link com.zugobite.pims.services.UserService#authenticate}
     * or {@link com.zugobite.pims.services.UserService#getCashiers}.
     */
    public User(int userId, String username, Role role, String fullName) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }
}
