package com.zugobite.pims.views;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;

/**
 * A {@link JTable} with alternating-row ("zebra") striping and a few sensible defaults
 * (taller rows, no reorderable columns, no grid lines — FlatLaf's own borders already
 * separate cells cleanly). Every data table in the app — Manage Medicines/Suppliers/Users,
 * Reports, POS search/cart, Stock Check — uses this instead of a bare {@code JTable}, so the
 * "looks professional" bar (striped rows are one of the most recognisable marks of a
 * polished data table, the same convention Excel, Finder, and most admin dashboards use)
 * is met consistently everywhere at once instead of being hand-rolled per screen.
 *
 * <p>Striping is implemented by overriding {@link #prepareRenderer}, the standard vanilla-Swing
 * technique for this — it runs after the real cell renderer has already set the cell's
 * content and default colors, so it only needs to override the background, and only for
 * unselected rows (selection highlighting must never be fought with by striping).</p>
 */
public class StripedTable extends JTable {

    private static final Color EVEN_ROW_COLOR = Color.WHITE;
    private static final Color ODD_ROW_COLOR = new Color(0xF3, 0xF6, 0xF6);

    public StripedTable(TableModel model) {
        super(model);
        setRowHeight(28);
        setShowGrid(false);
        setIntercellSpacing(new java.awt.Dimension(0, 0));
        setFillsViewportHeight(true);
        getTableHeader().setReorderingAllowed(false);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        if (!isRowSelected(row)) {
            component.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
        }
        return component;
    }
}
