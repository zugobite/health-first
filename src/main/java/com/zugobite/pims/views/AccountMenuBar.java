package com.zugobite.pims.views;

import com.zugobite.pims.models.User;

import javax.swing.*;

/**
 * The "who's logged in" menu bar shared by {@link AdminDashboardView} and
 * {@link CashierDashboardView}: a single menu, labeled with the current user's name and
 * role, holding "View Profile..." and "Log Out". This is the piece that closes the login
 * loop — before this existed, the only way back to the login screen was quitting and
 * restarting the whole application.
 *
 * <p>Takes a plain {@link Runnable} for the logout action rather than depending on
 * {@link com.zugobite.pims.controllers.LoginController} directly, so the {@code views}
 * package still has no compile-time dependency on {@code controllers} — the actual "go back
 * to the login screen" behavior is supplied by whichever controller constructed this
 * dashboard (see {@link com.zugobite.pims.controllers.AdminController} /
 * {@link com.zugobite.pims.controllers.CashierController}).</p>
 */
public class AccountMenuBar extends JMenuBar {

    /**
     * @param user       the currently logged-in user, used for the menu label and the
     *                   Profile dialog's contents
     * @param owner      the dashboard frame this menu bar is attached to, both as the
     *                   Profile dialog's parent and as the window that gets closed on logout
     * @param onLogout   invoked after {@code owner} has been disposed, so the caller can
     *                   reopen the login screen
     */
    public AccountMenuBar(User user, JFrame owner, Runnable onLogout) {
        JMenu accountMenu = new JMenu(user.getFullName() + " (" + user.getRole() + ")");

        JMenuItem profileItem = new JMenuItem("View Profile...");
        profileItem.addActionListener(e -> new ProfileDialog(owner, user).setVisible(true));

        JMenuItem logoutItem = new JMenuItem("Log Out");
        logoutItem.addActionListener(e -> {
            // Close this dashboard first, then hand control back to the caller-supplied
            // callback to show the login screen again. Calling dispose() directly here
            // (rather than owner.setVisible(false)) fully releases the window instead of
            // just hiding it, and — importantly — does NOT trigger the frame's
            // EXIT_ON_CLOSE behavior, since that only fires from a user-initiated "window
            // closing" event (the titlebar's close button), not from a programmatic
            // dispose() call. So logging out here can never accidentally exit the JVM.
            owner.dispose();
            onLogout.run();
        });

        accountMenu.add(profileItem);
        accountMenu.addSeparator();
        accountMenu.add(logoutItem);
        add(accountMenu);
    }
}
