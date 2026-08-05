package com.zugobite.pims;

import com.zugobite.pims.controllers.LoginController;

import javax.swing.SwingUtilities;

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
        SwingUtilities.invokeLater(() -> new LoginController().start());
    }
}
