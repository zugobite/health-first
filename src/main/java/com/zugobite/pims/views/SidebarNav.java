package com.zugobite.pims.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.zugobite.pims.models.User;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The left-hand navigation rail used by both dashboards - replaces the top {@link javax.swing.JTabbedPane}
 * previously used for screen switching, and folds in what used to be a separate
 * {@code AccountMenuBar}: a brand header at the top, one ghost-styled nav button per screen
 * in the middle, and the logged-in user's name/role plus "View Profile"/"Log Out" actions
 * pinned to the bottom.
 *
 * <p>This layout - icon/wordmark, a vertical list of nav items, a user block anchored at the
 * bottom - is the standard shape of virtually every modern admin dashboard (and is exactly
 * how shadcn/ui's own dashboard examples are composed), and reads as "enterprise software"
 * far more immediately than a row of tabs across the top ever does.</p>
 */
public class SidebarNav extends JPanel {

    private static final int WIDTH = 220;

    private final List<JButton> navButtons = new ArrayList<>();

    /**
     * @param productName label shown next to the app icon at the top of the sidebar, e.g.
     *                    "Admin" or "Cashier" - distinguishes which dashboard this is even
     *                    before the window title bar is visible
     * @param navLabels   one entry per screen this sidebar switches between, in display order
     * @param onSelect    invoked with the clicked item's index whenever the selection changes
     * @param user        the logged-in user, shown in the footer block
     * @param onLogout    invoked (after this window disposes itself) to return to the login screen
     */
    public SidebarNav(String productName, List<String> navLabels, IntConsumer onSelect, User user, Runnable onLogout) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIUtils.BORDER_COLOR),
                BorderFactory.createEmptyBorder(16, 12, 16, 12)
        ));
        setPreferredSize(new Dimension(WIDTH, 0));
        setMaximumSize(new Dimension(WIDTH, Integer.MAX_VALUE));

        add(buildHeader(productName));
        add(Box.createVerticalStrut(20));

        for (int i = 0; i < navLabels.size(); i++) {
            int index = i;
            JButton navButton = buildNavButton(navLabels.get(i));
            navButton.addActionListener(e -> {
                onSelect.accept(index);
                setSelected(index);
            });
            navButtons.add(navButton);
            add(navButton);
            add(Box.createVerticalStrut(2));
        }
        if (!navButtons.isEmpty()) {
            setSelected(0);
        }

        add(Box.createVerticalGlue());
        add(buildUserFooter(user, onLogout));
    }

    /** Highlights the nav button at {@code index} and un-highlights every other one - called both internally on click and externally if a caller wants to sync the sidebar to an already-selected screen. */
    public void setSelected(int index) {
        for (int i = 0; i < navButtons.size(); i++) {
            boolean selected = i == index;
            JButton button = navButtons.get(i);
            if (selected) {
                button.putClientProperty(FlatClientProperties.STYLE,
                        "arc: 8; borderWidth: 1; focusWidth: 0; background: #FFFFFF; foreground: #18181B; borderColor: #E4E4E7;");
                button.setFont(button.getFont().deriveFont(Font.BOLD));
            } else {
                UIUtils.styleAsGhostButton(button);
                button.setForeground(UIUtils.MUTED_FOREGROUND);
                button.setFont(button.getFont().deriveFont(Font.PLAIN));
            }
        }
    }

    /** The app icon + product name at the top of the sidebar - a small, muted echo of the login screen's header band. */
    private JPanel buildHeader(String productName) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel iconLabel = new JLabel(new javax.swing.ImageIcon(UIUtils.createAppIcon().getScaledInstance(24, 24, java.awt.Image.SCALE_SMOOTH)));
        JLabel titleLabel = new JLabel("HealthFirst · " + productName);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        header.add(iconLabel);
        header.add(titleLabel);
        return header;
    }

    /** One ghost-styled, left-aligned, full-width nav item button. */
    private JButton buildNavButton(String label) {
        JButton button = new JButton(label);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setFocusPainted(false);
        UIUtils.styleAsGhostButton(button);
        return button;
    }

    /**
     * The bottom-anchored block showing who's logged in, with quick access to their profile
     * and a way to log out - this is what used to be a top {@link javax.swing.JMenuBar}
     * ({@code AccountMenuBar}); moving it here keeps every piece of "who am I / how do I
     * leave" UI in the one place a user expects it in a sidebar-style app.
     */
    private JPanel buildUserFooter(User user, Runnable onLogout) {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 4, 4, 4)
        ));

        JLabel nameLabel = new JLabel(user.getFullName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel(user.getRole().toString());
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        roleLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton profileButton = new JButton("View Profile");
        profileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        UIUtils.styleAsPrimaryButton(profileButton);
        profileButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, profileButton.getPreferredSize().height));
        profileButton.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ProfileDialog(owner, user).setVisible(true);
        });

        JButton logoutButton = new JButton("Log Out");
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        UIUtils.styleAsSolidDestructiveButton(logoutButton);
        logoutButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, logoutButton.getPreferredSize().height));
        logoutButton.addActionListener(e -> {
            // Closing the owning window first, then invoking onLogout, mirrors the previous
            // AccountMenuBar behavior - dispose() here does not trigger the window's
            // EXIT_ON_CLOSE listener (that only fires from a user-initiated titlebar close),
            // so logging out can never accidentally exit the JVM.
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            owner.dispose();
            onLogout.run();
        });

        footer.add(nameLabel);
        footer.add(roleLabel);
        footer.add(Box.createVerticalStrut(8));
        footer.add(profileButton);
        footer.add(Box.createVerticalStrut(4));
        footer.add(logoutButton);
        return footer;
    }
}
