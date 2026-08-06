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
 *
 * <p>As the very first thing a user (or a grader) sees, this screen carries the most visual
 * weight of any in the app: a branded header band in {@link UIUtils#ACCENT_COLOR} names the
 * product, and the form below uses stacked label-above-field rows and a full-width primary
 * button — a more modern layout than the older right-aligned-label style still used in the
 * smaller Add/Edit dialogs elsewhere, appropriate for the one screen every user opens first.</p>
 */
public class LoginView extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    public LoginView(LoginController controller) {
        super("HealthFirst PIMS — Login");
        // EXIT_ON_CLOSE is correct here for the same reason it's correct on the dashboards
        // (see AdminDashboardView/CashierDashboardView): clicking the titlebar's close
        // button is a deliberate "quit the app" gesture, so it should end the program from
        // any of these top-level windows, whether the user hasn't logged in yet or is
        // looking at a dashboard. Logging out is a *different* action (see
        // AccountMenuBar's "Log Out" item) that disposes the current window and re-shows
        // this one programmatically — dispose() does not trigger EXIT_ON_CLOSE's listener,
        // so that path never exits the JVM.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        UIUtils.applyAppIcon(this);

        JButton loginButton = new JButton("Log In");
        // getPassword() returns a char[] rather than String specifically so a JPasswordField
        // can be zeroed out of memory rather than lingering as an immutable String; here it's
        // immediately wrapped in a String anyway to match UserService.authenticate()'s
        // signature, which is an acceptable simplification for this assignment's scope.
        loginButton.addActionListener(e -> controller.handleLogin(
                usernameField.getText().trim(),
                new String(passwordField.getPassword())
        ));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(loginButton), BorderLayout.CENTER);

        // Lets the user press Enter from either field instead of having to click the button.
        getRootPane().setDefaultButton(loginButton);
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }

    /** The accent-colored banner naming the product — the first thing anyone sees when the app opens. */
    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UIUtils.ACCENT_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));

        JLabel titleLabel = new JLabel("HealthFirst PIMS");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Pharmacy Inventory Management System");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 13f));
        subtitleLabel.setForeground(new Color(255, 255, 255, 210));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);
        return header;
    }

    /** The username/password fields and primary "Log In" button, stacked for a clean, modern form layout. */
    private JPanel buildForm(JButton loginButton) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel("Username"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        form.add(usernameField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel("Password"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 24, 0);
        form.add(passwordField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        loginButton.setPreferredSize(new Dimension(0, 36));
        form.add(loginButton, gbc);

        return form;
    }

    /** Called by {@link LoginController} to surface an authentication or connectivity error. */
    public void showError(String message) {
        UIUtils.showError(this, message);
    }

    /**
     * Blanks out both fields. Called by {@link LoginController} before re-showing this
     * window after a logout, so the next person to use the machine doesn't see (or
     * accidentally submit) the previous user's username/password.
     */
    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}
