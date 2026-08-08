package com.zugobite.pims.views;

import com.zugobite.pims.services.DashboardService;
import com.zugobite.pims.services.DashboardService.DashboardSnapshot;
import com.zugobite.pims.utils.AppIcons;
import com.zugobite.pims.utils.UIUtils;
import com.zugobite.pims.utils.UIUtils.ModuleAccent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The home screen for both dashboards: a live operations summary across the top, then a grid
 * of module tiles below it.
 *
 * <p>Two deliberate layout decisions carry most of the "this is real software" impression:</p>
 * <ul>
 *   <li><b>Live figures come first.</b> A launcher that only offers navigation makes the user
 *       do all the thinking; leading with today's takings, transaction count, and stock
 *       alerts answers the question they actually walked up to the till with.</li>
 *   <li><b>Content is capped and centred, not stretched.</b> Everything sits inside a
 *       {@value #MAX_CONTENT_WIDTH}px column (the same "max-width container" convention every
 *       professional web dashboard uses). Without this, four tiles on a 2000px-wide monitor
 *       each become ~950px of mostly-empty tinted rectangle, which is what makes a Swing app
 *       read as amateurish no matter how well the individual components are styled.</li>
 * </ul>
 */
public class ModuleHomePanel extends JPanel {

    /** Lucide icon key and one-line description for each module tile - see {@link AppIcons}. */
    public enum IconKind {
        MEDICINES(AppIcons.PILL, "Add, edit and remove catalogue items"),
        SUPPLIERS(AppIcons.PACKAGE, "Manage supplier contacts and details"),
        USERS(AppIcons.USERS, "Create and manage cashier accounts"),
        REPORTS(AppIcons.CHART, "Sales, item-wise, low stock and expiry"),
        POS(AppIcons.CART, "Ring up a sale and issue a receipt"),
        STOCK(AppIcons.CLIPBOARD, "Look up price and availability");

        private final String lucideName;
        private final String description;

        IconKind(String lucideName, String description) {
            this.lucideName = lucideName;
            this.description = description;
        }

        String lucideName() {
            return lucideName;
        }

        String description() {
            return description;
        }
    }

    /** One launcher entry: visible label, icon kind, and CardLayout index to open. */
    public record ModuleTile(String label, IconKind iconKind, int index) {
    }

    private static final int GRID_COLUMNS = 3;
    private static final int GRID_GAP = 16;
    private static final int ICON_SIZE = 22;
    private static final int ICON_TILE_SIZE = 44;
    private static final int TILE_HEIGHT = 96;

    /** Caps how wide the content column grows on large monitors, in pixels. */
    private static final int MAX_CONTENT_WIDTH = 1180;

    /** Holds the stat cards; repopulated once {@link DashboardService} returns. */
    private final JPanel statsRow = new JPanel(new GridLayout(1, 4, GRID_GAP, 0));

    /**
     * Returns the on-brand tinted accent for a home-module tile. Colors live in
     * {@link UIUtils} so the design system stays in one place; the switch stays here to
     * avoid a circular dependency between {@code UIUtils} and this view.
     */
    public static ModuleAccent moduleAccent(IconKind kind) {
        return switch (kind) {
            case MEDICINES -> UIUtils.MODULE_MEDICINES;
            case SUPPLIERS -> UIUtils.MODULE_SUPPLIERS;
            case USERS -> UIUtils.MODULE_USERS;
            case REPORTS -> UIUtils.MODULE_REPORTS;
            case POS -> UIUtils.MODULE_POS;
            case STOCK -> UIUtils.MODULE_STOCK;
        };
    }

    /**
     * @param roleTitle        short heading, e.g. {@code "Admin"} or {@code "Cashier"}
     * @param tiles            modules to show, in display order
     * @param onOpen           called with a tile's {@link ModuleTile#index()} when clicked
     * @param dashboardService supplies the headline figures; may be {@code null} to render a
     *                         plain launcher with no stat row
     */
    public ModuleHomePanel(String roleTitle, List<ModuleTile> tiles, IntConsumer onOpen,
                            DashboardService dashboardService) {
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        column.add(buildHeader(roleTitle));
        column.add(Box.createVerticalStrut(20));

        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setVisible(false); // stays hidden until real figures arrive
        column.add(statsRow);

        column.add(Box.createVerticalStrut(24));
        column.add(sectionLabel("Modules"));
        column.add(Box.createVerticalStrut(12));
        column.add(buildModuleGrid(tiles, onOpen));

        // Absorbs leftover vertical space so the content stays top-aligned instead of
        // floating in the middle of a tall window.
        column.add(Box.createVerticalGlue());

        add(centeredMaxWidth(column), BorderLayout.CENTER);

        if (dashboardService != null) {
            loadStats(dashboardService);
        }
    }

    /** Page title and role subtitle. */
    private JPanel buildHeader(String roleTitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Overview");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(UIUtils.FOREGROUND);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(roleTitle + " workspace - today at a glance");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
        subtitle.setForeground(UIUtils.MUTED_FOREGROUND);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        return header;
    }

    /** A small uppercase divider label introducing a group of content. */
    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(UIUtils.MUTED_FOREGROUND);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * The module tile grid. Uses {@code GridLayout(0, GRID_COLUMNS, ...)} - a <b>zero</b> row
     * count with a fixed column count. This matters: when {@link GridLayout} is given a
     * non-zero row count it honours the rows and <em>derives</em> the columns, so passing
     * {@code (2, 3)} for four tiles silently produces a 2x2 grid rather than the intended
     * 3-across layout. Zero rows makes the column count authoritative.
     */
    private JPanel buildModuleGrid(List<ModuleTile> tiles, IntConsumer onOpen) {
        JPanel grid = new JPanel(new GridLayout(0, GRID_COLUMNS, GRID_GAP, GRID_GAP));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (ModuleTile tile : tiles) {
            grid.add(buildTile(tile, onOpen));
        }

        int rows = (int) Math.ceil(tiles.size() / (double) GRID_COLUMNS);
        int gridHeight = rows * TILE_HEIGHT + (rows - 1) * GRID_GAP;
        // Pin the height so the surrounding BoxLayout cannot stretch tiles vertically to fill
        // a tall window - the "giant empty billboard" effect this layout is fixing.
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, gridHeight));
        grid.setPreferredSize(new Dimension(grid.getPreferredSize().width, gridHeight));
        return grid;
    }

    /**
     * One module tile: a tinted icon square on the left, with the module name and its
     * one-line description stacked beside it. Horizontal rather than the previous
     * centred icon-above-label layout, which left a large void in the middle of every tile
     * once the tiles were wide.
     */
    private JPanel buildTile(ModuleTile tile, IntConsumer onOpen) {
        ModuleAccent accent = moduleAccent(tile.iconKind());

        JPanel tilePanel = new JPanel(new BorderLayout(14, 0)) {
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
                // White at rest with the accent reserved for the icon square, so a grid of
                // tiles reads as one calm surface; the tint only appears under the cursor.
                g2.setColor(hovered ? accent.background() : UIUtils.BACKGROUND);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.setColor(hovered ? accent.accent() : UIUtils.BORDER_COLOR);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tilePanel.setOpaque(false);
        tilePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tilePanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        tilePanel.setPreferredSize(new Dimension(0, TILE_HEIGHT));

        tilePanel.add(buildIconTile(tile, accent), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel nameLabel = new JLabel(tile.label());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
        nameLabel.setForeground(UIUtils.FOREGROUND);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel(tile.iconKind().description());
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        descLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(Box.createVerticalGlue());
        text.add(nameLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(descLabel);
        text.add(Box.createVerticalGlue());

        tilePanel.add(text, BorderLayout.CENTER);
        return tilePanel;
    }

    /** The tinted square holding a module's icon - the tile's only splash of colour at rest. */
    private JPanel buildIconTile(ModuleTile tile, ModuleAccent accent) {
        JPanel iconTile = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent.background());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconTile.setOpaque(false);
        iconTile.setPreferredSize(new Dimension(ICON_TILE_SIZE, ICON_TILE_SIZE));

        JLabel iconLabel = new JLabel(
                AppIcons.lucide(tile.iconKind().lucideName(), ICON_SIZE, accent.accent()),
                javax.swing.SwingConstants.CENTER);
        iconLabel.setOpaque(false);
        iconTile.add(iconLabel, BorderLayout.CENTER);

        // Wrapped so BorderLayout.WEST cannot stretch the square into a tall rectangle.
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(iconTile, BorderLayout.NORTH);
        return holder;
    }

    /**
     * Centres {@code content} horizontally and stops it growing past
     * {@value #MAX_CONTENT_WIDTH}px. Implemented with {@link BoxLayout} specifically because
     * it is one of the few Swing layout managers that honours a component's
     * {@code maximumSize} - {@link java.awt.GridBagLayout} with a horizontal fill would
     * ignore the cap and stretch the content across the full window.
     */
    private JComponent centeredMaxWidth(JComponent content) {
        content.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, Integer.MAX_VALUE));

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.add(Box.createHorizontalGlue());
        row.add(content);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    /**
     * Fetches the headline figures off the Event Dispatch Thread and populates the stat row
     * when they arrive. A {@link SwingWorker} rather than a direct call because these are six
     * database round trips: doing them inline would freeze the window for the duration, and
     * on an unreachable database would hang the whole app rather than just this one row.
     *
     * <p>On failure the stat row simply stays hidden - a cashier must still be able to reach
     * the POS module even if the reporting queries are unavailable, so this deliberately does
     * not surface a blocking error dialog.</p>
     */
    private void loadStats(DashboardService dashboardService) {
        new SwingWorker<DashboardSnapshot, Void>() {
            @Override
            protected DashboardSnapshot doInBackground() throws Exception {
                return dashboardService.loadSnapshot();
            }

            @Override
            protected void done() {
                try {
                    populateStats(get());
                } catch (Exception e) {
                    // Intentionally silent - see method Javadoc.
                    statsRow.setVisible(false);
                }
            }
        }.execute();
    }

    /** Builds the four stat cards from a loaded snapshot and reveals the row. */
    private void populateStats(DashboardSnapshot snapshot) {
        statsRow.removeAll();

        statsRow.add(new StatCard(
                "Today's Sales",
                UIUtils.formatCurrency(snapshot.todayRevenue()),
                snapshot.todayTransactions() == 1 ? "1 transaction" : snapshot.todayTransactions() + " transactions",
                AppIcons.CART,
                StatCard.Tone.NEUTRAL));

        statsRow.add(new StatCard(
                "Stock On Hand",
                UIUtils.formatCurrency(snapshot.inventoryValue()),
                snapshot.medicineCount() + " medicines listed",
                AppIcons.PILL,
                StatCard.Tone.NEUTRAL));

        statsRow.add(new StatCard(
                "Low Stock",
                String.valueOf(snapshot.lowStockCount()),
                snapshot.lowStockCount() == 0 ? "All items above reorder level" : "At or below reorder level",
                AppIcons.CLIPBOARD,
                snapshot.lowStockCount() == 0 ? StatCard.Tone.NEUTRAL : StatCard.Tone.DANGER));

        statsRow.add(new StatCard(
                "Expiring Soon",
                String.valueOf(snapshot.expiringCount()),
                snapshot.expiringCount() == 0 ? "Nothing expiring this month" : "Within the next month",
                AppIcons.PACKAGE,
                snapshot.expiringCount() == 0 ? StatCard.Tone.NEUTRAL : StatCard.Tone.WARNING));

        // Height is pinned for the same reason the module grid's is - so the surrounding
        // vertical BoxLayout cannot stretch the cards to fill a tall window.
        int height = statsRow.getPreferredSize().height;
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

        statsRow.setVisible(true);
        statsRow.revalidate();
        statsRow.repaint();
    }
}
