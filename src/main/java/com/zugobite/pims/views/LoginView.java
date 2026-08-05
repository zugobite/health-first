package com.zugobite.pims.views;

import com.zugobite.pims.controllers.LoginController;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The application's very first screen: a simple username/password form (PLAN.md §5.1).
 * Deliberately "dumb" — it holds no business logic of its own and doesn't talk to the
 * database directly. Every click just forwards the raw field values to
 * {@link LoginController#handleLogin}, which owns the actual authentication decision and
 * subsequent routing.
 */
public class LoginView extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginView(LoginController controller) {
        super("HealthFirst PIMS — Login");
        // EXIT_ON_CLOSE here (rather than DISPOSE_ON_CLOSE) is correct specifically because
        // this is the *first* window: closing it before logging in should end the program.
        // Dashboards opened after login use DISPOSE_ON_CLOSE-style defaults instead, since
        // those are launched by, but not equivalent to, the whole application.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        UIUtils.applyStandardPadding(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordField, gbc);

        JButton loginButton = new JButton("Log In");
        // getPassword() returns a char[] rather than String specifically so a JPasswordField
        // can be zeroed out of memory rather than lingering as an immutable String; here it's
        // immediately wrapped in a String anyway to match UserService.authenticate()'s
        // signature, which is an acceptable simplification for this assignment's scope.
        loginButton.addActionListener(e -> controller.handleLogin(
                usernameField.getText().trim(),
                new String(passwordField.getPassword())
        ));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(loginButton, gbc);

        // Lets the user press Enter from either field instead of having to click the button.
        getRootPane().setDefaultButton(loginButton);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    /** Called by {@link LoginController} to surface an authentication or connectivity error. */
    public void showError(String message) {
        UIUtils.showError(this, message);
    }
}
