package com.zugobite.pims;

import com.zugobite.pims.controllers.LoginController;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.SwingUtilities;
import java.awt.Taskbar;

/**
 * Application entry point for PIMS (Pharmacy Inventory Management System).
 *
 * <p>Run with:
 * <pre>{@code
 * java -cp out:mysql-connector-j.jar com.zugobite.pims.Main
 * }</pre>
 * after loading {@code database.sql} into a local MySQL instance (see the credentials in
 * {@link com.zugobite.pims.data.DBConnection}).</p>
 */
public class Main {

    public static void main(String[] args) {
        // All Swing UI construction and mutation must happen on the Event Dispatch Thread
        // (EDT), not the main() thread that starts the JVM — invokeLater() schedules the
        // first window's creation to run there. Every subsequent screen in this app is only
        // ever opened from inside an existing Swing event handler (e.g. a button's
        // ActionListener), which is already running on the EDT, so this is the one and only
        // place that needs an explicit hop onto it.
        SwingUtilities.invokeLater(() -> {
            // Must run before any Swing component is constructed — see
            // UIUtils.installLookAndFeel()'s Javadoc for why.
            UIUtils.installLookAndFeel();
            applyDockIcon();
            new LoginController().start();
        });
    }

    /**
     * Sets the app's icon in the OS taskbar/Dock (on platforms that support it — notably
     * macOS's Dock). This is separate from {@link UIUtils#applyAppIcon}, which sets each
     * individual window's title-bar icon; this one is for the single, OS-level app icon
     * that's visible even before any window exists.
     */
    private static void applyDockIcon() {
        if (Taskbar.isTaskbarSupported()) {
            try {
                Taskbar.getTaskbar().setIconImage(UIUtils.createAppIcon());
            } catch (UnsupportedOperationException ignored) {
                // Some platforms report isTaskbarSupported() = true but don't actually
                // support setting the icon image (e.g. certain Linux window managers) —
                // the app should still run fine without a Dock icon in that case.
            }
        }
    }
}
