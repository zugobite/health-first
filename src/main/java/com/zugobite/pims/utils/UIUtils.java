package com.zugobite.pims.utils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.awt.Component;

public final class UIUtils {

    private UIUtils() {
    }

    public static void applyStandardPadding(JComponent component) {
        component.setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
