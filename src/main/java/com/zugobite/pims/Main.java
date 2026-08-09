package com.zugobite.pims;

import com.zugobite.pims.controllers.LoginController;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.SwingUtilities;
import java.awt.Taskbar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Application entry point for PIMS (Pharmacy Inventory Management System).
 *
 * <p>Run with:
 * <pre>{@code
 * java -cp out:mysql-connector-j.jar com.zugobite.pims.Main
 * }</pre>
 * after loading {@code database.sql} into a local MySQL instance (see the credentials in
 * {@link com.zugobite.pims.data.DBConnection}). On macOS, {@code Main} re-launches the JVM
 * with {@code -Xdock:name} so the Dock shows {@link #APP_NAME} instead of {@code java}.</p>
 */
public class Main {

    /** Display name used for the macOS Dock label / application menu. */
    public static final String APP_NAME = "HealthFirst PIMS";

    /** Set on the child JVM after a macOS Dock-name re-launch so we do not loop. */
    private static final String DOCK_CONFIGURED_PROP = "healthfirst.dock.configured";

    /**
     * Boots the desktop app. On macOS may re-exec the JVM once with {@code -Xdock:name}
     * (forwarding {@code args}) and return early; otherwise sets the Apple application
     * name and, on the EDT, installs FlatLaf, applies the Dock icon, and opens
     * {@link LoginController}.
     */
    public static void main(String[] args) {
        // -Xdock:name can only be set at JVM start. If the user launched without it (the
        // usual `java -cp … Main` flow), re-exec once on macOS so the Dock label is correct.
        if (relaunchOnMacForDockName(args)) {
            return;
        }

        // Must be set before AWT/Swing initializes so macOS picks up the real app name
        // instead of the JVM process name ("java") in the application menu.
        System.setProperty("apple.awt.application.name", APP_NAME);

        // All Swing UI construction and mutation must happen on the Event Dispatch Thread
        // (EDT), not the main() thread that starts the JVM - invokeLater() schedules the
        // first window's creation to run there. Every subsequent screen in this app is only
        // ever opened from inside an existing Swing event handler (e.g. a button's
        // ActionListener), which is already running on the EDT, so this is the one and only
        // place that needs an explicit hop onto it.
        SwingUtilities.invokeLater(() -> {
            // Must run before any Swing component is constructed - see
            // UIUtils.installLookAndFeel()'s Javadoc for why.
            UIUtils.installLookAndFeel();
            applyDockIcon();
            new LoginController().start();
        });
    }

    /**
     * On macOS, starts a replacement JVM with {@code -Xdock:name} and exits this process.
     * Returns {@code true} when this process should stop (re-launch started, or we are
     * already the configured child). Returns {@code false} on non-macOS platforms so
     * startup continues normally.
     */
    private static boolean relaunchOnMacForDockName(String[] args) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("mac")) {
            return false;
        }
        if ("true".equals(System.getProperty(DOCK_CONFIGURED_PROP))) {
            return false;
        }

        String javaBin = System.getProperty("java.home")
                + File.separator + "bin" + File.separator + "java";
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-Xdock:name=" + APP_NAME);
        cmd.add("-D" + DOCK_CONFIGURED_PROP + "=true");
        cmd.add("-Dapple.awt.application.name=" + APP_NAME);
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(Main.class.getName());
        for (String arg : args) {
            cmd.add(arg);
        }

        try {
            Process child = new ProcessBuilder(cmd)
                    .directory(new File(System.getProperty("user.dir")))
                    .inheritIO()
                    .start();
            // Wait so the user's terminal stays attached until they quit the app; then
            // propagate the child's exit code.
            System.exit(child.waitFor());
        } catch (IOException e) {
            // Fall through and run in this JVM - Dock may still say "java", but the app works.
            System.err.println("Could not re-launch with Dock name; continuing without it: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true; // unreachable - System.exit above - keeps the compiler happy
    }

    /**
     * Sets the app's icon in the OS taskbar/Dock (on platforms that support it - notably
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
                // support setting the icon image (e.g. certain Linux window managers) -
                // the app should still run fine without a Dock icon in that case.
            }
        }
    }
}
