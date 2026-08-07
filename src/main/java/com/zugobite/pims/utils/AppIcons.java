package com.zugobite.pims.utils;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.awt.Color;
import java.io.File;
import java.net.URL;

/**
 * Loads Lucide SVG icons from {@code src/main/resources/icons} so every UI glyph comes from
 * one icon set (Lucide) rather than a mix of hand-drawn shapes.
 */
public final class AppIcons {

    private AppIcons() {
    }

    /** Module / chrome icon names matching files under {@code icons/*.svg}. */
    public static final String PILL = "pill";
    public static final String PACKAGE = "package";
    public static final String USERS = "users";
    public static final String CHART = "chart-column";
    public static final String CART = "shopping-cart";
    public static final String CLIPBOARD = "clipboard-list";
    public static final String HOUSE = "house";
    public static final String USER = "user";
    public static final String LOG_OUT = "log-out";

    /**
     * Returns a Lucide icon tinted to {@code color} at {@code size}×{@code size} px.
     * Prefers the classpath ({@code icons/name.svg}); falls back to the project resources
     * folder when running via {@code javac -d out} without copying resources into {@code out/}.
     */
    public static Icon lucide(String name, int size, Color color) {
        FlatSVGIcon icon = load(name);
        FlatSVGIcon sized = icon.derive(size, size);
        sized.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return sized;
    }

    public static Icon lucide(String name, int size) {
        return lucide(name, size, UIUtils.PRIMARY);
    }

    private static FlatSVGIcon load(String name) {
        String resourcePath = "icons/" + name + ".svg";
        URL url = AppIcons.class.getClassLoader().getResource(resourcePath);
        if (url != null) {
            return new FlatSVGIcon(url);
        }
        // Dev classpath often is only `out/` - resolve from the repo resources tree.
        File file = new File("src/main/resources/" + resourcePath);
        if (file.isFile()) {
            return new FlatSVGIcon(file);
        }
        throw new IllegalStateException("Missing Lucide icon resource: " + resourcePath);
    }
}
