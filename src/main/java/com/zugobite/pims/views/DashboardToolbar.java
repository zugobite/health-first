package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.utils.AppIcons;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;

/**
 * Shared top chrome for Admin and Cashier dashboards: Home, context title, View Profile,
 * and Log Out. Lucide icons match the home-module tiles for one cohesive icon set.
 */
final class DashboardToolbar extends JPanel {

    private static final int ICON = 16;

    DashboardToolbar(String contextTitle, User user, Runnable onHome, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setOpaque(false);

        JButton homeButton = new JButton("Home", AppIcons.lucide(AppIcons.HOUSE, ICON, UIUtils.FOREGROUND));
        UIUtils.styleAsOutlineButton(homeButton);
        homeButton.addActionListener(e -> onHome.run());

        JLabel titleLabel = new JLabel(contextTitle);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));

        left.add(homeButton);
        left.add(titleLabel);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);

        JLabel userLabel = new JLabel(user.getFullName());
        userLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        userLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JButton profileButton = new JButton("View Profile", AppIcons.lucide(AppIcons.USER, ICON, Color.WHITE));
        UIUtils.styleAsPrimaryButton(profileButton);
        profileButton.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ProfileDialog(owner, user).setVisible(true);
        });

        JButton logoutButton = new JButton("Log Out", AppIcons.lucide(AppIcons.LOG_OUT, ICON, Color.WHITE));
        UIUtils.styleAsSolidDestructiveButton(logoutButton);
        logoutButton.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            owner.dispose();
            onLogout.run();
        });

        right.add(userLabel);
        right.add(profileButton);
        right.add(Box.createRigidArea(new Dimension(8, 0)));
        right.add(logoutButton);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }
}
