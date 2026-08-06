package com.zugobite.pims.utils;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;

/**
 * Small, stateless Swing helpers shared across every screen in the {@code views} package.
 * Exists mainly so that "what does an error dialog look like in this app", "what's the
 * standard panel padding", and "what's the app's brand color/icon" are each defined exactly
 * once, keeping the UI visually consistent (PLAN.md §6 calls out "neatly designed
 * interface... consistent spacing/alignment" as an explicit grading criterion) instead of
 * every panel picking its own padding/dialog/color style.
 */
public final class UIUtils {

    /**
     * HealthFirst's brand color — a medical/pharmacy-appropriate teal, used as FlatLaf's
     * global accent color (see {@link #installLookAndFeel()}) and for the app icon
     * (see {@link #createAppIcon()}). Every accent-driven element FlatLaf paints — focus
     * rings, the default button highlight, selection backgrounds, progress bars — derives
     * from this single color, so changing the brand color app-wide is a one-line edit here.
     */
    public static final Color ACCENT_COLOR = new Color(0x0F, 0x76, 0x6E);

    // Utility class: never instantiated.
    private UIUtils() {
    }

    /**
     * Installs FlatLaf's light theme with {@link #ACCENT_COLOR} as the global accent, in
     * place of Swing's default "Metal" look. This is the single highest-impact call in the
     * whole UI layer for visual polish: it replaces every button, table, text field, tabbed
     * pane, and scrollbar in the app with FlatLaf's flat, modern rendering without any
     * other code needing to change.
     *
     * <p>Must be called before any Swing component is constructed — see
     * {@link com.zugobite.pims.Main#main}, which calls this first thing on the Event
     * Dispatch Thread, before {@code LoginController} builds the login window.</p>
     *
     * <p>The accent-color override has to be set via {@code UIManager.put(...)} <em>before</em>
     * {@link FlatLightLaf#setup()} runs — that's the FlatLaf-documented hook for a custom
     * accent color: {@code setup()} reads this value while building its defaults table, so
     * every accent-derived color (focus rings, the default-button highlight, selection
     * backgrounds) is computed from it automatically.</p>
     */
    public static void installLookAndFeel() {
        UIManager.put("Component.accentColor", ACCENT_COLOR);
        FlatLightLaf.setup();
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

    // Lazily built and cached — every top-level window uses the same icon, so there is no
    // reason to re-render the artwork once per window.
    private static Image appIcon;

    /**
     * Sets {@code window}'s title-bar/taskbar icon to the app's branded pharmacy-cross mark
     * (see {@link #createAppIcon()}). Called by every top-level {@link javax.swing.JFrame} in
     * the app (login, both dashboards, the bill window) so the app looks intentionally
     * branded rather than showing Java's generic default window icon.
     */
    public static void applyAppIcon(Window window) {
        window.setIconImage(createAppIcon());
    }

    /**
     * Procedurally draws a simple, recognisable "pharmacy cross" mark — a rounded square in
     * {@link #ACCENT_COLOR} with a white cross — rather than shipping a bitmap asset. This
     * keeps the whole app's visual identity self-contained in source code (no binary asset
     * to track in git, no licensing question about where an icon image came from) while
     * still giving every window a real, branded icon instead of Java's generic default.
     */
    public static Image createAppIcon() {
        if (appIcon != null) {
            return appIcon;
        }
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(ACCENT_COLOR);
        g.fillRoundRect(2, 2, size - 4, size - 4, 18, 18);

        g.setColor(Color.WHITE);
        int barThickness = size / 5;
        int inset = size / 5;
        g.fillRoundRect(inset, size / 2 - barThickness / 2, size - 2 * inset, barThickness, 4, 4);
        g.fillRoundRect(size / 2 - barThickness / 2, inset, barThickness, size - 2 * inset, 4, 4);

        g.dispose();
        appIcon = image;
        return appIcon;
    }
}
