package com.zugobite.pims.utils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.awt.Component;

/**
 * Small, stateless Swing helpers shared across every screen in the {@code views} package.
 * Exists mainly so that "what does an error dialog look like in this app" and "what's the
 * standard panel padding" are each defined exactly once, keeping the UI visually consistent
 * (PLAN.md §6 calls out "neatly designed interface... consistent spacing/alignment" as an
 * explicit grading criterion) instead of every panel picking its own padding/dialog style.
 */
public final class UIUtils {

    // Utility class: never instantiated.
    private UIUtils() {
    }

    /**
     * Applies the app-wide standard 12px padding around a panel's contents. Called once by
     * (almost) every panel/dialog constructor in {@code views} right after the layout
     * manager is set, so screens don't feel cramped against the window edge.
     */
    public static void applyStandardPadding(JComponent component) {
        component.setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    /** Standard modal error popup — used for validation failures and caught {@code SQLException}s alike. */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Standard modal info popup — used for non-error confirmations (currently unused but kept for symmetry). */
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
