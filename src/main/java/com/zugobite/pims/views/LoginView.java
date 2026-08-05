package com.zugobite.pims.views;

import com.zugobite.pims.controllers.LoginController;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class LoginView extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginView(LoginController controller) {
        super("HealthFirst PIMS — Login");
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
        loginButton.addActionListener(e -> controller.handleLogin(
                usernameField.getText().trim(),
                new String(passwordField.getPassword())
        ));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(loginButton, gbc);

        getRootPane().setDefaultButton(loginButton);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    public void showError(String message) {
        UIUtils.showError(this, message);
    }
}
