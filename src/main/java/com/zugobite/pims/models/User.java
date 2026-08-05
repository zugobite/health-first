package com.zugobite.pims.models;

import com.zugobite.pims.enums.Role;

public class User {

    private final int userId;
    private final String username;
    private final Role role;
    private final String fullName;

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
