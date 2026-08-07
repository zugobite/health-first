package com.zugobite.pims.views;

import com.zugobite.pims.utils.AppIcons;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Desktop-native home / launcher screen: a short role title and a grid of large module
 * tiles (Lucide icon + name). Replaces the web-style sidebar as the primary way to open
 * Admin and Cashier modules.
 */
public class ModuleHomePanel extends JPanel {

    /** Lucide icon key for each module tile — see {@link AppIcons}. */
    public enum IconKind {
        MEDICINES(AppIcons.PILL),
        SUPPLIERS(AppIcons.PACKAGE),
        USERS(AppIcons.USERS),
        REPORTS(AppIcons.CHART),
        POS(AppIcons.CART),
        STOCK(AppIcons.CLIPBOARD);

        private final String lucideName;

        IconKind(String lucideName) {
            this.lucideName = lucideName;
        }

        String lucideName() {
            return lucideName;
        }
    }

    /** One launcher entry: visible label, icon kind, and CardLayout index to open. */
    public record ModuleTile(String label, IconKind iconKind, int index) {
    }

    private static final int TILE_ARC = UIUtils.CARD_RADIUS;
    private static final int ICON_SIZE = 40;

    /**
     * @param roleTitle short heading, e.g. {@code "Admin"} or {@code "Cashier"}
     * @param tiles     modules to show, in display order
     * @param onOpen    called with a tile's {@link ModuleTile#index()} when clicked
     */
    public ModuleHomePanel(String roleTitle, List<ModuleTile> tiles, IntConsumer onOpen) {
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(32, 40, 40, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        JLabel title = new JLabel("Home");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(UIUtils.FOREGROUND);

        JLabel subtitle = new JLabel(roleTitle + " — choose a module to open");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
        subtitle.setForeground(UIUtils.MUTED_FOREGROUND);
        subtitle.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        int columns = Math.min(2, Math.max(1, tiles.size()));
        int rows = (int) Math.ceil(tiles.size() / (double) columns);
        JPanel grid = new JPanel(new GridLayout(rows, columns, 16, 16));
        grid.setOpaque(false);

        for (ModuleTile tile : tiles) {
            grid.add(buildTile(tile, onOpen));
        }

        JPanel gridWrap = new JPanel(new GridBagLayout());
        gridWrap.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gridWrap.add(grid, gbc);
        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gridWrap.add(new JPanel() {{
            setOpaque(false);
        }}, gbc);

        add(gridWrap, BorderLayout.CENTER);
    }

    private JPanel buildTile(ModuleTile tile, IntConsumer onOpen) {
        JPanel tilePanel = new JPanel(new BorderLayout(0, 12)) {
            private boolean hovered;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        onOpen.accept(tile.index());
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Float(
                        0.5f, 0.5f,
                        Math.max(0, getWidth() - 1.5f), Math.max(0, getHeight() - 1.5f),
                        TILE_ARC, TILE_ARC);
                g2.setColor(hovered ? UIUtils.HOVER_BACKGROUND : UIUtils.BACKGROUND);
                g2.fill(shape);
                g2.setStroke(new java.awt.BasicStroke(1.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.setColor(UIUtils.BORDER_COLOR);
                g2.draw(shape);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tilePanel.setOpaque(false);
        tilePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tilePanel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        tilePanel.setPreferredSize(new Dimension(220, 160));

        JLabel iconLabel = new JLabel(
                AppIcons.lucide(tile.iconKind().lucideName(), ICON_SIZE, UIUtils.PRIMARY),
                SwingConstants.CENTER);
        iconLabel.setOpaque(false);

        JLabel nameLabel = new JLabel(tile.label(), SwingConstants.CENTER);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 15f));
        nameLabel.setForeground(UIUtils.FOREGROUND);
        nameLabel.setOpaque(false);

        tilePanel.add(iconLabel, BorderLayout.CENTER);
        tilePanel.add(nameLabel, BorderLayout.SOUTH);
        return tilePanel;
    }
}
