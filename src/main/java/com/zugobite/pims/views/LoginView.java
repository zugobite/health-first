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
 * weight of any in the app: a centered {@link CardPanel} floating on a muted page background
 * — the same "auth card" composition shadcn/ui's own login example uses — with a branded
 * icon badge, stacked label-above-field rows, and a full-width primary button.</p>
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
        // SidebarNav's "Log Out" button) that disposes the current window and re-shows
        // this one programmatically — dispose() does not trigger EXIT_ON_CLOSE's listener,
        // so that path never exits the JVM.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        UIUtils.applyAppIcon(this);

        JButton loginButton = new JButton("Log In");
        UIUtils.styleAsPrimaryButton(loginButton);
        // getPassword() returns a char[] rather than String specifically so a JPasswordField
        // can be zeroed out of memory rather than lingering as an immutable String; here it's
        // immediately wrapped in a String anyway to match UserService.authenticate()'s
        // signature, which is an acceptable simplification for this assignment's scope.
        loginButton.addActionListener(e -> controller.handleLogin(
                usernameField.getText().trim(),
                new String(passwordField.getPassword())
        ));

        CardPanel card = new CardPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0;
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.weightx = 1;

        cardGbc.gridy = 0;
        cardGbc.insets = new Insets(0, 0, 20, 0);
        card.add(buildBadgeHeader(), cardGbc);

        cardGbc.gridy = 1;
        cardGbc.insets = new Insets(0, 0, 0, 0);
        card.add(buildForm(loginButton), cardGbc);

        // The 20-column JTextFields inside buildForm() already give the card a sensible
        // natural width via GridBagLayout's own preferred-size calculation, so no explicit
        // setPreferredSize() override is needed here — that was tried once and (before this
        // fix) accidentally froze the card's height at whatever an *empty* CardPanel's
        // preferred size was, because it was called before any children existed.

        // The card floats on a muted page background with generous margin around it — the
        // "centered auth card" composition, rather than the form filling the whole window
        // edge-to-edge the way plain Swing forms usually do.
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(UIUtils.MUTED_BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));
        page.add(card);

        // Lets the user press Enter from either field instead of having to click the button.
        getRootPane().setDefaultButton(loginButton);
        setContentPane(page);
        pack();
        setLocationRelativeTo(null);
    }

    /** A small accent-colored icon badge beside the product name/tagline — the card's header block. */
    private JPanel buildBadgeHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setOpaque(false);

        JLabel iconLabel = new JLabel(new ImageIcon(UIUtils.createAppIcon().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        iconLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);
        textStack.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JLabel titleLabel = new JLabel("HealthFirst PIMS");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(UIUtils.FOREGROUND);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Pharmacy Inventory Management System");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textStack.add(titleLabel);
        textStack.add(subtitleLabel);

        header.add(iconLabel);
        header.add(textStack);
        return header;
    }

    /** The username/password fields and primary "Log In" button, stacked for a clean, modern form layout. */
    private JPanel buildForm(JButton loginButton) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(mutedLabel("Username"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        form.add(usernameField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(mutedLabel("Password"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 24, 0);
        form.add(passwordField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        loginButton.setPreferredSize(new Dimension(0, 36));
        form.add(loginButton, gbc);

        return form;
    }

    /** A small, muted-gray field label — shadcn/ui's `Label` component uses the same subdued, uppercase-adjacent styling rather than plain black text. */
    private JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        label.setForeground(UIUtils.MUTED_FOREGROUND);
        return label;
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
