package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.utils.AppIcons;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

/**
 * Shared top chrome for the Admin and Cashier dashboards: product mark and Home on the left,
 * the signed-in user and their session actions on the right.
 *
 * <p>The emphasis here is deliberately <em>low</em>. Application chrome frames the work; it
 * should never out-shout it. An earlier revision rendered "Log Out" as a solid red button,
 * which made the single least-used control the loudest thing on the screen - the visual
 * equivalent of shouting the exit sign. Both session actions are now ghost-styled, with
 * "Log Out" picking up its red only on hover, so the destructive intent is still unmistakable
 * at the moment it matters without dominating the interface for the rest of the shift.</p>
 */
final class DashboardToolbar extends JPanel {

    private static final int ICON = 16;
    private static final int BRAND_ICON = 22;
    private static final int AVATAR_SIZE = 32;

    DashboardToolbar(String contextTitle, User user, Runnable onHome, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        add(buildLeft(contextTitle, onHome), BorderLayout.WEST);
        add(buildRight(user, onLogout), BorderLayout.EAST);
    }

    /** Product mark, a divider, then the Home action and the current module's name. */
    private JPanel buildLeft(String contextTitle, Runnable onHome) {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setOpaque(false);

        JLabel brandIcon = new JLabel(new ImageIcon(
                UIUtils.createAppIcon().getScaledInstance(BRAND_ICON, BRAND_ICON, Image.SCALE_SMOOTH)));

        JLabel brandText = new JLabel("HealthFirst PIMS");
        brandText.setFont(brandText.getFont().deriveFont(Font.BOLD, 13f));
        brandText.setForeground(UIUtils.FOREGROUND);
        brandText.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JButton homeButton = new JButton("Home", AppIcons.lucide(AppIcons.HOUSE, ICON, UIUtils.MUTED_FOREGROUND));
        UIUtils.styleAsGhostButton(homeButton);
        homeButton.setForeground(UIUtils.MUTED_FOREGROUND);
        homeButton.addActionListener(e -> onHome.run());

        // Names the screen currently open, so the user always knows where they are - the
        // job a breadcrumb does in a web app.
        JLabel titleLabel = new JLabel(contextTitle);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 13f));
        titleLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        left.add(brandIcon);
        left.add(brandText);
        left.add(Box.createRigidArea(new Dimension(16, 0)));
        left.add(verticalDivider());
        left.add(Box.createRigidArea(new Dimension(12, 0)));
        left.add(homeButton);
        left.add(titleLabel);
        return left;
    }

    /** Identity chip (avatar + name + role) followed by the quiet session actions. */
    private JPanel buildRight(User user, Runnable onLogout) {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);

        right.add(buildIdentityChip(user));
        right.add(Box.createRigidArea(new Dimension(12, 0)));
        right.add(verticalDivider());
        right.add(Box.createRigidArea(new Dimension(8, 0)));

        JButton profileButton = new JButton("Profile", AppIcons.lucide(AppIcons.USER, ICON, UIUtils.MUTED_FOREGROUND));
        UIUtils.styleAsGhostButton(profileButton);
        profileButton.setForeground(UIUtils.MUTED_FOREGROUND);
        profileButton.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ProfileDialog(owner, user).setVisible(true);
        });

        JButton logoutButton = new JButton("Log Out", AppIcons.lucide(AppIcons.LOG_OUT, ICON, UIUtils.DESTRUCTIVE));
        UIUtils.styleAsQuietDestructiveButton(logoutButton);
        logoutButton.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            owner.dispose();
            onLogout.run();
        });

        right.add(profileButton);
        right.add(Box.createRigidArea(new Dimension(4, 0)));
        right.add(logoutButton);
        return right;
    }

    /**
     * The signed-in user's avatar, name, and role. Showing the role explicitly matters in a
     * shared-till environment: a cashier and a manager may use the same physical machine
     * minutes apart, and "which account am I currently operating as" should never require
     * opening a dialog to answer.
     */
    private JPanel buildIdentityChip(User user) {
        JPanel chip = new JPanel();
        chip.setLayout(new BoxLayout(chip, BoxLayout.X_AXIS));
        chip.setOpaque(false);

        chip.add(new AvatarBadge(user.getFullName()));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JLabel nameLabel = new JLabel(user.getFullName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        nameLabel.setForeground(UIUtils.FOREGROUND);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel(user.getRole().toString());
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        roleLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);

        text.add(nameLabel);
        text.add(roleLabel);
        chip.add(text);
        return chip;
    }

    /** A hairline vertical rule used to group the toolbar into visually distinct zones. */
    private JComponent verticalDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(UIUtils.BORDER_COLOR);
        divider.setPreferredSize(new Dimension(1, 24));
        divider.setMaximumSize(new Dimension(1, 24));
        return divider;
    }

    /**
     * A circular initials badge, drawn procedurally rather than loaded from an image - the
     * standard stand-in for a user photo in software that has no photos to show. Derives up
     * to two initials from the user's full name.
     */
    private static final class AvatarBadge extends JLabel {

        private final String initials;

        AvatarBadge(String fullName) {
            this.initials = deriveInitials(fullName);
            setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
            setMaximumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        /** "HealthFirst Administrator" -> "HA"; single-word names fall back to one letter. */
        private static String deriveInitials(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                return "?";
            }
            String[] parts = fullName.trim().split("\\s+");
            String first = parts[0].substring(0, 1).toUpperCase();
            if (parts.length == 1) {
                return first;
            }
            return first + parts[parts.length - 1].substring(0, 1).toUpperCase();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(UIUtils.PRIMARY);
            g2.fillOval(0, 0, AVATAR_SIZE - 1, AVATAR_SIZE - 1);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            java.awt.FontMetrics metrics = g2.getFontMetrics();
            int textX = (AVATAR_SIZE - metrics.stringWidth(initials)) / 2;
            int textY = (AVATAR_SIZE - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(initials, textX, textY);

            g2.dispose();
        }
    }
}
