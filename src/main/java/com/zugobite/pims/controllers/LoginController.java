package com.zugobite.pims.controllers;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.views.AdminDashboardView;
import com.zugobite.pims.views.CashierDashboardView;
import com.zugobite.pims.views.LoginView;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Entry point of the application's login flow: owns the {@link LoginView}, authenticates
 * the credentials the user submits, and routes to the correct role-specific dashboard on
 * success (PLAN.md §5.1 "On success: redirect by role → AdminDashboard or
 * CashierDashboard").
 *
 * <p>This is the only controller {@link com.zugobite.pims.Main} instantiates directly —
 * {@link AdminController} and {@link CashierController} are created here, one level down,
 * once we know which role just logged in.</p>
 */
public class LoginController {

    private final UserService userService = new UserService();
    private final LoginView loginView = new LoginView(this);

    /** Shows the login window. Called once from {@code Main.main()} at application startup. */
    public void start() {
        loginView.setVisible(true);
    }

    /**
     * Invoked by {@link LoginView} when the user clicks "Log In". Authenticates against the
     * `users` table and either routes to the appropriate dashboard, or shows an error and
     * leaves the login window open so the user can retry (PLAN.md §5.1 "On failure: show a
     * clear error message, no crash, no silent fail").
     */
    public void handleLogin(String username, String password) {
        try {
            Optional<User> user = userService.authenticate(username, password);
            if (user.isEmpty()) {
                loginView.showError("Invalid username or password.");
                return;
            }
            // Only close the login window once we know login actually succeeded — if we
            // disposed it unconditionally, a failed login would leave the user stranded
            // with no window at all.
            loginView.dispose();
            route(user.get());
        } catch (SQLException e) {
            // Distinct message from "invalid credentials" above: this branch means the DB
            // itself couldn't be reached (wrong host, MySQL not running, schema not loaded),
            // which the user needs to know is a different problem than a typo'd password.
            loginView.showError("Could not reach the database: " + e.getMessage());
        }
    }

    /** Dispatches to {@link AdminController} or {@link CashierController} based on the authenticated user's role. */
    private void route(User user) {
        switch (user.getRole()) {
            case ADMIN -> new AdminController(user).start();
            case CASHIER -> new CashierController(user).start();
        }
    }
}
