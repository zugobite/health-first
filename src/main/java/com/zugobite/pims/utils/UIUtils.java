package com.zugobite.pims.utils;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JButton;
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
 * Small, stateless Swing helpers shared across every screen in the {@code views} package —
 * the app's design system in one file: look-and-feel setup, a neutral color palette, button
 * variants, and layout/dialog conventions, so every screen draws from the same values
 * instead of picking its own.
 *
 * <p>The palette and component styling here deliberately follow the same visual language as
 * shadcn/ui (neutral zinc grays, a near-black primary action color, subtle borders instead
 * of heavy chrome, consistently rounded corners) — that's a web/React/Tailwind component
 * library with no Java equivalent, so it can't be "installed" here, but its <em>look</em> is
 * fully reproducible in Swing via FlatLaf's per-component styling API
 * ({@link FlatClientProperties#STYLE}), which is what {@link #styleAsPrimaryButton} and
 * friends below use. See {@link com.zugobite.pims.views.CardPanel} for the other half of
 * that visual language — bordered, rounded "card" containers instead of bare panels.</p>
 */
public final class UIUtils {

    // ---------------------------------------------------------------------
    // Design tokens — a neutral "zinc" palette (the shadcn/ui default theme's palette) plus
    // one brand accent. Every color used anywhere in the UI should trace back to one of
    // these constants rather than being a one-off literal, so the whole app reads as one
    // consistent, considered design instead of a pile of individually-styled screens.
    // ---------------------------------------------------------------------

    /** Page/card background — pure white. */
    public static final Color BACKGROUND = Color.WHITE;
    /** Sidebar and subtle-section background — zinc-50. */
    public static final Color MUTED_BACKGROUND = new Color(0xFA, 0xFA, 0xFA);
    /** Hover background for outline/ghost controls — zinc-100. */
    public static final Color HOVER_BACKGROUND = new Color(0xF4, 0xF4, 0xF5);
    /** Card/divider/input borders — zinc-200. */
    public static final Color BORDER_COLOR = new Color(0xE4, 0xE4, 0xE7);
    /** Primary text color — zinc-900. */
    public static final Color FOREGROUND = new Color(0x18, 0x18, 0x1B);
    /** Secondary/caption text color — zinc-500. */
    public static final Color MUTED_FOREGROUND = new Color(0x71, 0x71, 0x7A);

    /** Primary action background (buttons like "Log In", "Save", "Checkout") — near-black, the shadcn/ui default-theme primary color. */
    public static final Color PRIMARY = new Color(0x18, 0x18, 0x1B);
    /** Hover shade for {@link #PRIMARY} buttons — zinc-800. */
    public static final Color PRIMARY_HOVER = new Color(0x27, 0x27, 0x2A);
    /** Text color on top of {@link #PRIMARY}. */
    public static final Color PRIMARY_FOREGROUND = Color.WHITE;

    /** Destructive action color (Delete buttons) — red-600. */
    public static final Color DESTRUCTIVE = new Color(0xDC, 0x26, 0x26);
    /** Very light red hover background for outline-style destructive buttons — red-50. */
    public static final Color DESTRUCTIVE_HOVER_BACKGROUND = new Color(0xFE, 0xF2, 0xF2);
    /** Border for outline-style destructive buttons — red-200. */
    public static final Color DESTRUCTIVE_BORDER = new Color(0xFE, 0xCA, 0xCA);

    /**
     * HealthFirst's brand color — a medical/pharmacy-appropriate teal, used as FlatLaf's
     * global accent color (see {@link #installLookAndFeel()}) purely for focus rings and
     * text-selection highlighting, and for the app icon (see {@link #createAppIcon()}) and
     * a few small brand touches (the login header band, the POS total). Primary buttons
     * intentionally use {@link #PRIMARY} (near-black) instead of this accent — that
     * neutral-primary-button + colored-focus-ring combination is exactly how shadcn/ui's own
     * default theme is composed.
     */
    public static final Color ACCENT_COLOR = new Color(0x0F, 0x76, 0x6E);

    private static final int RADIUS = 8;

    // Utility class: never instantiated.
    private UIUtils() {
    }

    /**
     * Installs FlatLaf's light theme with {@link #ACCENT_COLOR} as the global accent, plus a
     * set of global overrides (corner radius, focus ring width, scrollbar style) that push
     * FlatLaf's default look toward the flatter, more rounded, lower-chrome aesthetic of
     * shadcn/ui — in place of Swing's default "Metal" look. This is the single highest-impact
     * call in the whole UI layer: it changes every button, field, table, and scrollbar in the
     * app at once, before any other styling in this class or {@link com.zugobite.pims.views.CardPanel}
     * layers on top.
     *
     * <p>Must be called before any Swing component is constructed — see
     * {@link com.zugobite.pims.Main#main}, which calls this first thing on the Event
     * Dispatch Thread, before {@code LoginController} builds the login window.</p>
     *
     * <p>The accent-color override has to be set via {@code UIManager.put(...)} <em>before</em>
     * {@link FlatLightLaf#setup()} runs — that's the FlatLaf-documented hook for a custom
     * accent color: {@code setup()} reads this value while building its defaults table, so
     * every accent-derived color (focus rings, selection backgrounds) is computed from it
     * automatically. The corner-radius/scrollbar overrides below are ordinary UIManager keys
     * and are applied after {@code setup()}, which is the correct order for those.</p>
     */
    public static void installLookAndFeel() {
        UIManager.put("Component.accentColor", ACCENT_COLOR);
        FlatLightLaf.setup();

        // Rounded corners everywhere — the single biggest visual signal of a modern,
        // "designed" UI versus Swing's boxy default. 8px on buttons/fields/menus matches
        // Tailwind's "rounded-lg" scale that shadcn/ui components use.
        UIManager.put("Button.arc", RADIUS);
        UIManager.put("Component.arc", RADIUS);
        UIManager.put("TextComponent.arc", RADIUS);
        UIManager.put("CheckBox.arc", 4);
        UIManager.put("ProgressBar.arc", RADIUS);
        UIManager.put("PopupMenu.borderInsets", new java.awt.Insets(4, 4, 4, 4));

        // A thin, subtle focus ring rather than FlatLaf's default thicker glow — closer to
        // Tailwind's 1-2px ring utility that shadcn/ui inputs use.
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);

        // Thin, fully-rounded "pill" scrollbar thumbs with no visible track — the minimal
        // overlay-scrollbar look most modern web and native apps use now, instead of
        // Swing's default chunky, square scrollbar.
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.showButtons", false);

        // Borders/backgrounds pulled from this class's own palette, so FlatLaf's neutrals
        // match the exact zinc scale used everywhere else (cards, sidebar, table stripes)
        // rather than FlatLaf's own slightly different default grays.
        UIManager.put("Component.borderColor", BORDER_COLOR);
        UIManager.put("Table.gridColor", BORDER_COLOR);
        UIManager.put("Table.selectionBackground", new Color(0xE4, 0xE4, 0xE7));
        UIManager.put("Table.selectionForeground", FOREGROUND);
        UIManager.put("TableHeader.background", MUTED_BACKGROUND);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER_COLOR);
    }

    /**
     * Applies the app-wide standard 12px padding around a panel's contents. Called once by
     * (almost) every panel/dialog constructor in {@code views} right after the layout
     * manager is set, so screens don't feel cramped against the window edge.
     */
    public static void applyStandardPadding(JComponent component) {
        component.setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    // ---------------------------------------------------------------------
    // Button variants — the shadcn/ui "Button" component's variant set (primary / outline /
    // destructive), each implemented as a FlatLaf per-component style string rather than a
    // custom JButton subclass. This is FlatLaf's documented styling mechanism
    // (FlatClientProperties.STYLE): a semicolon-separated list of "key: value" pairs that
    // override that one button's UI defaults (background, hover/pressed shades, border,
    // corner radius) without touching any other button in the app.
    // ---------------------------------------------------------------------

    /**
     * The main call-to-action style: solid near-black background, white text — used for
     * exactly one button per screen (the single most important action: "Log In", "Save",
     * "Checkout", a dialog's "OK"), the same restraint shadcn/ui's own guidance recommends
     * for its {@code default} button variant.
     */
    public static void styleAsPrimaryButton(JButton button) {
        button.putClientProperty(FlatClientProperties.STYLE, String.format(
                "arc: %d; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0;" +
                        "background: #%s; foreground: #%s; hoverBackground: #%s; pressedBackground: #000000;",
                RADIUS, hex(PRIMARY), hex(PRIMARY_FOREGROUND), hex(PRIMARY_HOVER)
        ));
    }

    /**
     * The default, lower-emphasis style: white background, a thin neutral border, dark text.
     * Used for every secondary action — "Refresh", "Cancel", "Edit" — so the primary action
     * on a screen is the only one competing for attention.
     */
    public static void styleAsOutlineButton(JButton button) {
        button.putClientProperty(FlatClientProperties.STYLE, String.format(
                "arc: %d; borderWidth: 1; focusWidth: 1;" +
                        "background: #%s; foreground: #%s; borderColor: #%s; hoverBorderColor: #%s; hoverBackground: #%s;",
                RADIUS, hex(BACKGROUND), hex(FOREGROUND), hex(BORDER_COLOR), hex(new Color(0xD4, 0xD4, 0xD8)), hex(HOVER_BACKGROUND)
        ));
    }

    /**
     * The destructive-action style: white background, red text/border, light-red hover —
     * used for every "Delete" button. Intentionally an <em>outline</em> style rather than a
     * solid red fill, so a row of toolbar buttons doesn't read as "something's already gone
     * wrong" at rest — the red only asserts itself on hover, right before the click that
     * actually matters.
     */
    public static void styleAsDestructiveButton(JButton button) {
        button.putClientProperty(FlatClientProperties.STYLE, String.format(
                "arc: %d; borderWidth: 1; focusWidth: 1;" +
                        "background: #%s; foreground: #%s; borderColor: #%s; hoverBackground: #%s;",
                RADIUS, hex(BACKGROUND), hex(DESTRUCTIVE), hex(DESTRUCTIVE_BORDER), hex(DESTRUCTIVE_HOVER_BACKGROUND)
        ));
    }

    /**
     * The lowest-emphasis style: no border, no background until hovered — used for
     * navigation items (see {@link com.zugobite.pims.views.SidebarNav}) and other controls
     * that need to feel like part of the page's structure rather than a distinct button.
     */
    public static void styleAsGhostButton(JButton button) {
        button.putClientProperty(FlatClientProperties.STYLE, String.format(
                "arc: %d; borderWidth: 0; focusWidth: 0; background: #%s; foreground: #%s; hoverBackground: #%s;",
                RADIUS, hex(BACKGROUND), hex(FOREGROUND), hex(HOVER_BACKGROUND)
        ));
    }

    private static String hex(Color color) {
        return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
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
     * Procedurally draws a "pharmacy cross" mark in the style Apple's own app icons moved to
     * with macOS Tahoe's "Liquid Glass" redesign — a squircle (superellipse, not a plain
     * rounded rectangle) with a layered glass surface: a top-lit gradient base, a soft glossy
     * highlight sweeping across the upper half, a darkened lower edge for depth, and a subtle
     * drop shadow under the glyph — rather than the flat, single-color rounded-square icon
     * this app used before. Drawn procedurally (no bitmap asset) so the whole app's visual
     * identity stays self-contained in source code, with no binary to track in git and no
     * licensing question about where an icon image came from.
     *
     * <p>Rendered at a high internal resolution (256px) and scaled down by every caller as
     * needed (title bars, the sidebar header, the Dock icon), since the gradient/highlight
     * layers need real pixels to read as "glass" rather than "flat" once shrunk.</p>
     */
    public static Image createAppIcon() {
        if (appIcon != null) {
            return appIcon;
        }
        int size = 256;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        java.awt.Shape squircle = createSquirclePath(size);

        // Base surface: a top-lit vertical gradient rather than a flat fill, so the icon
        // reads as a lit, three-dimensional surface instead of a solid color swatch.
        g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x17, 0x8F, 0x84), 0, size, new Color(0x0A, 0x4F, 0x49)));
        g.fill(squircle);

        // Every subsequent "glass" layer is clipped to the squircle so highlights/shadows
        // never spill past the icon's own silhouette.
        Graphics2D clipped = (Graphics2D) g.create();
        clipped.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clipped.clip(squircle);

        // The glossy highlight — a large, soft, translucent white oval biased toward the top
        // — is the single most recognisable trait of the "Liquid Glass" treatment: it's what
        // makes a flat-colored shape read as a curved, reflective surface instead of paint.
        clipped.setPaint(new java.awt.GradientPaint(0, -size * 0.15f, new Color(255, 255, 255, 130), 0, size * 0.65f, new Color(255, 255, 255, 0)));
        clipped.fill(new java.awt.geom.Ellipse2D.Float(-size * 0.15f, -size * 0.4f, size * 1.3f, size * 1.05f));

        // A darkened lower edge grounds the glass — without it, the highlight above would
        // leave the icon looking washed-out rather than lit from one direction.
        clipped.setPaint(new java.awt.GradientPaint(0, size * 0.55f, new Color(0, 0, 0, 0), 0, size, new Color(0, 0, 0, 80)));
        clipped.fillRect(0, 0, size, size);
        clipped.dispose();

        // A thin, near-white rim stroke along the whole silhouette — the "edge catching the
        // light" detail that sells the glass illusion at a glance, even at small sizes.
        g.setStroke(new java.awt.BasicStroke(size * 0.012f));
        g.setColor(new Color(255, 255, 255, 70));
        g.draw(squircle);

        drawGlassCross(g, size);

        g.dispose();
        appIcon = image;
        return appIcon;
    }

    /**
     * Builds a squircle (superellipse) path rather than a plain rounded rectangle — the
     * exact shape Apple's own app icons use, with a flatter mid-edge and a more continuous
     * corner curve than {@link Graphics2D#fillRoundRect} can produce. Generated by sampling
     * the parametric superellipse equation {@code |x/a|^n + |y/b|^n = 1} (with {@code n = 5},
     * matching Apple's own icon-grid corner curvature) rather than trying to express it as a
     * fixed number of arcs and lines.
     */
    private static java.awt.Shape createSquirclePath(int size) {
        double n = 5.0;
        double a = size / 2.0;
        java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
        int samples = 200;
        for (int i = 0; i <= samples; i++) {
            double t = 2 * Math.PI * i / samples;
            double cosT = Math.cos(t);
            double sinT = Math.sin(t);
            double x = a * Math.signum(cosT) * Math.pow(Math.abs(cosT), 2.0 / n);
            double y = a * Math.signum(sinT) * Math.pow(Math.abs(sinT), 2.0 / n);
            if (i == 0) {
                path.moveTo(a + x, a + y);
            } else {
                path.lineTo(a + x, a + y);
            }
        }
        path.closePath();
        return path;
    }

    /**
     * The pharmacy-cross glyph itself: a soft dark shadow offset slightly below/right of the
     * white bars, so the cross reads as sitting slightly proud of the glass surface behind
     * it rather than printed flat onto it — the same "layered, not flat" treatment applied
     * to the icon's background above.
     */
    private static void drawGlassCross(Graphics2D g, int size) {
        int barThickness = (int) (size * 0.16);
        int inset = (int) (size * 0.24);
        int arc = (int) (barThickness * 0.4);
        int shadowOffset = Math.max(1, size / 85);

        g.setColor(new Color(0, 0, 0, 55));
        g.fillRoundRect(inset, size / 2 - barThickness / 2 + shadowOffset, size - 2 * inset, barThickness, arc, arc);
        g.fillRoundRect(size / 2 - barThickness / 2 + shadowOffset, inset, barThickness, size - 2 * inset, arc, arc);

        g.setPaint(new java.awt.GradientPaint(0, inset, Color.WHITE, 0, size - inset, new Color(232, 245, 243)));
        g.fillRoundRect(inset, size / 2 - barThickness / 2, size - 2 * inset, barThickness, arc, arc);
        g.fillRoundRect(size / 2 - barThickness / 2, inset, barThickness, size - 2 * inset, arc, arc);
    }
}
