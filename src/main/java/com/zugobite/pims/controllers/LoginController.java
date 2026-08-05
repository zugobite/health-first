package com.zugobite.pims.controllers;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.views.AdminDashboardView;
import com.zugobite.pims.views.CashierDashboardView;
import com.zugobite.pims.views.LoginView;

import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    private final UserService userService = new UserService();
    private final LoginView loginView = new LoginView(this);

    public void start() {
        loginView.setVisible(true);
    }

    public void handleLogin(String username, String password) {
        try {
            Optional<User> user = userService.authenticate(username, password);
            if (user.isEmpty()) {
                loginView.showError("Invalid username or password.");
                return;
            }
            loginView.dispose();
            route(user.get());
        } catch (SQLException e) {
            loginView.showError("Could not reach the database: " + e.getMessage());
        }
    }

    private void route(User user) {
        switch (user.getRole()) {
            case ADMIN -> new AdminController(user).start();
            case CASHIER -> new CashierController(user).start();
        }
    }
}
