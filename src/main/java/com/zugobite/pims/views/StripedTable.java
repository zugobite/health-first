package com.zugobite.pims.views;

import com.zugobite.pims.utils.UIUtils;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;

/**
 * A {@link JTable} with alternating-row ("zebra") striping and a few sensible defaults
 * (taller rows, no reorderable columns, no grid lines - FlatLaf's own borders already
 * separate cells cleanly). Every data table in the app - Manage Medicines/Suppliers/Users,
 * Reports, POS search/cart, Stock Check - uses this instead of a bare {@code JTable}, so the
 * "looks professional" bar (striped rows are one of the most recognisable marks of a
 * polished data table, the same convention Excel, Finder, and most admin dashboards use)
 * is met consistently everywhere at once instead of being hand-rolled per screen.
 *
 * <p>All cell text (including numbers) and column headers are left-aligned - a deliberate
 * desktop-native choice rather than Swing's default right-alignment for {@link Number}
 * columns.</p>
 *
 * <p>Striping is implemented by overriding {@link #prepareRenderer}, the standard vanilla-Swing
 * technique for this - it runs after the real cell renderer has already set the cell's
 * content and default colors, so it only needs to override the background, and only for
 * unselected rows (selection highlighting must never be fought with by striping).</p>
 *
 * <p>Two extras layer on top of that: {@link BigDecimal} cells render as formatted money
 * ({@code 1,234.56}) instead of a raw {@code toString()}, and screens can supply a
 * {@link RowAccent} to tint whole rows that need attention - which is how Manage Medicines
 * flags low-stock and expiring items without the user having to read every number.</p>
 */
public class StripedTable extends JTable {

    private static final Color EVEN_ROW_COLOR = Color.WHITE;
    private static final Color ODD_ROW_COLOR = new Color(0xF3, 0xF6, 0xF6);

    /**
     * Optional per-row background override, used to draw attention to rows in a notable
     * state (e.g. stock at/below reorder level, or an item expiring within the month).
     * Implementations receive a <em>model</em> row index, so they stay correct even when the
     * user has sorted or filtered the view.
     */
    @FunctionalInterface
    public interface RowAccent {
        /** @return the background to use for this model row, or {@code null} to keep normal striping. */
        Color backgroundFor(int modelRow);
    }

    private RowAccent rowAccent;

    /**
     * Applies project table defaults: row height 28, no grid, fill viewport, no column
     * reorder, left-aligned headers, and a {@link BigDecimal} money renderer.
     */
    public StripedTable(TableModel model) {
        super(model);
        setRowHeight(28);
        setShowGrid(false);
        setIntercellSpacing(new java.awt.Dimension(0, 0));
        setFillsViewportHeight(true);
        getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        // Registered per-class rather than per-column so any table whose model declares a
        // BigDecimal column (via getColumnClass) gets money formatting for free, with no
        // extra wiring on each screen.
        setDefaultRenderer(BigDecimal.class, new MoneyRenderer());
    }

    /** Installs a row-tinting rule; pass {@code null} to go back to plain zebra striping. */
    public void setRowAccent(RowAccent rowAccent) {
        this.rowAccent = rowAccent;
        repaint();
    }

    /**
     * Left-aligns label cells; for unselected rows applies {@link RowAccent} (model index)
     * or zebra striping. Selection highlight is never overridden.
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        if (component instanceof JLabel label) {
            label.setHorizontalAlignment(SwingConstants.LEFT);
        }
        if (!isRowSelected(row)) {
            Color accent = rowAccent == null ? null : rowAccent.backgroundFor(convertRowIndexToModel(row));
            component.setBackground(accent != null ? accent : (row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR));
        }
        return component;
    }

    /**
     * Renders {@link BigDecimal} cells as grouped 2-decimal money ({@code 1,234.56}) while
     * leaving the model's value a real {@code BigDecimal} - so sorting a price column still
     * sorts numerically rather than lexicographically ("9.00" before "10.00", not after).
     */
    private static final class MoneyRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            setText(value instanceof BigDecimal amount ? UIUtils.formatAmount(amount) : "");
        }
    }
}
