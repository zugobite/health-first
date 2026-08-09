package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.models.Supplier;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Admin tab for full CRUD on {@link Medicine} records, including the expiry date and
 * supplier link (PLAN.md §5.3 "Manage Medicines tab").
 *
 * <p>Needs {@link SupplierService} in addition to {@link MedicineService} purely so the
 * "Add"/"Edit" dialog can offer a dropdown of valid suppliers - this panel never writes to
 * the `suppliers` table itself.</p>
 */
public class ManageMedicinesPanel extends JPanel {

    private final MedicineService medicineService;
    private final SupplierService supplierService;

    // Column order here must match the order values are appended in loadMedicines() below.
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Company", "Type", "Price", "In Stock", "Reorder Level", "Expiry Date", "Supplier ID"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        // Declaring the numeric columns' real type (rather than the Object.class default)
        // is what makes Swing's built-in table renderer right-align them automatically -
        // JTable falls back through a value's superclasses to find a registered renderer,
        // and both Integer and BigDecimal resolve to the same right-aligned Number renderer.
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 5, 6, 8 -> Integer.class;
                case 4 -> BigDecimal.class;
                default -> Object.class;
            };
        }
    };
    private final StripedTable table = new StripedTable(tableModel);

    /** Instant free-text filter across every visible column. */
    private final JTextField filterField = new JTextField();
    /** Drives the filter; also lets the user sort by clicking a column header. */
    private final javax.swing.table.TableRowSorter<DefaultTableModel> sorter =
            new javax.swing.table.TableRowSorter<>(tableModel);
    /** Row count summary, e.g. "7 medicines - 2 low stock, 1 expiring soon". */
    private final JLabel summaryLabel = new JLabel();

    /**
     * Admin medicines CRUD UI: Add/Edit/Delete/Refresh toolbar, name filter,
     * {@link StripedTable} with low-stock/expiry row accents, and a summary label.
     * {@code supplierService} only populates the supplier dropdown in the form dialog.
     * Calls {@link #loadMedicines()} so the table is not empty on first show.
     */
    public ManageMedicinesPanel(MedicineService medicineService, SupplierService supplierService) {
        this.medicineService = medicineService;
        this.supplierService = supplierService;
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JButton refreshButton = new JButton("Refresh");
        JButton addButton = new JButton("Add Medicine");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        UIUtils.styleAsOutlineButton(refreshButton);
        UIUtils.styleAsPrimaryButton(addButton);
        UIUtils.styleAsOutlineButton(editButton);
        UIUtils.styleAsSolidDestructiveButton(deleteButton);

        refreshButton.addActionListener(e -> loadMedicines());
        addButton.addActionListener(e -> openAddDialog());
        editButton.addActionListener(e -> openEditDialog());
        deleteButton.addActionListener(e -> deleteSelected());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(refreshButton);

        // Sorting and filtering both come from the same TableRowSorter. Clicking a header
        // now sorts - which, combined with the numeric column classes declared above, means
        // Price sorts by value rather than as text.
        table.setRowSorter(sorter);
        // Rows in a state that needs attention are tinted, so an admin scanning a long
        // catalogue sees problems without reading a single number. Low stock (which blocks
        // sales) outranks an approaching expiry, so it is checked first.
        table.setRowAccent(modelRow -> {
            if (isLowStock(modelRow)) {
                return LOW_STOCK_TINT;
            }
            return isExpiringSoon(modelRow) ? EXPIRING_TINT : null;
        });

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.add(actions, BorderLayout.WEST);
        toolbar.add(buildFilterField(), BorderLayout.EAST);

        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.PLAIN, 12f));
        summaryLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        // The table lives inside a bordered CardPanel rather than sitting directly on the
        // page background - the same "content in a card" composition used on every data
        // screen in the app (see CardPanel's class comment).
        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(UIUtils.createRoundedScrollPane(table), BorderLayout.CENTER);
        card.add(summaryLabel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);

        loadMedicines();
    }

    /** Tint for rows at or below their reorder level. */
    private static final Color LOW_STOCK_TINT = new Color(0xFE, 0xF2, 0xF2);
    /** Tint for rows expiring within the next month. */
    private static final Color EXPIRING_TINT = new Color(0xFF, 0xFB, 0xEB);

    /**
     * The instant filter box. Filters on every keystroke via a case-insensitive regex applied
     * across all columns, so an admin can narrow a catalogue of thousands by name, company,
     * or type without choosing which field to search first.
     */
    private JTextField buildFilterField() {
        UIUtils.setPlaceholder(filterField, "Filter medicines...");
        filterField.setColumns(22);
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }
        });
        return filterField;
    }

    /** Applies the current filter text to the sorter, then refreshes the row summary. */
    private void applyFilter() {
        String query = filterField.getText().trim();
        // Quote the input so a user typing regex metacharacters (a bracket in a product
        // name, say) filters literally instead of throwing a PatternSyntaxException.
        sorter.setRowFilter(query.isEmpty()
                ? null
                : javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        updateSummary();
    }

    /** True when the medicine on this model row is at or below its reorder level. */
    private boolean isLowStock(int modelRow) {
        Object stock = tableModel.getValueAt(modelRow, 5);
        Object reorder = tableModel.getValueAt(modelRow, 6);
        return stock instanceof Integer stockValue
                && reorder instanceof Integer reorderValue
                && stockValue <= reorderValue;
    }

    /** True when the medicine on this model row expires within the next month. */
    private boolean isExpiringSoon(int modelRow) {
        Object expiry = tableModel.getValueAt(modelRow, 7);
        if (!(expiry instanceof LocalDate expiryDate)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !expiryDate.isBefore(today) && !expiryDate.isAfter(today.plusMonths(1));
    }

    /** Updates the "N medicines - X low stock, Y expiring soon" line beneath the table. */
    private void updateSummary() {
        int total = tableModel.getRowCount();
        int shown = table.getRowCount();
        int lowStock = 0;
        int expiring = 0;
        for (int row = 0; row < total; row++) {
            if (isLowStock(row)) {
                lowStock++;
            } else if (isExpiringSoon(row)) {
                expiring++;
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append(shown == total
                ? total + (total == 1 ? " medicine" : " medicines")
                : shown + " of " + total + " medicines");
        if (lowStock > 0) {
            summary.append("  -  ").append(lowStock).append(" low stock");
        }
        if (expiring > 0) {
            summary.append("  -  ").append(expiring).append(" expiring soon");
        }
        summaryLabel.setText(summary.toString());
    }

    /** Re-queries every medicine from the database and repopulates the table from scratch. */
    public void loadMedicines() {
        try {
            List<Medicine> medicines = medicineService.getAllMedicines();
            tableModel.setRowCount(0);
            for (Medicine m : medicines) {
                tableModel.addRow(new Object[]{
                        m.getMedicineId(), m.getName(), m.getCompany(), m.getMedicineType(),
                        m.getPrice(), m.getQuantityInStock(), m.getReorderLevel(), m.getExpiryDate(), m.getSupplierId()
                });
            }
            updateSummary();
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to load medicines: " + e.getMessage());
        }
    }

    /**
     * The model index of the selected row, or {@code -1} when nothing is selected.
     *
     * <p>Going through {@link JTable#convertRowIndexToModel} is <b>required</b>, not
     * defensive: once a {@link javax.swing.table.TableRowSorter} is attached (for the header
     * sorting and the filter box above), {@link JTable#getSelectedRow()} returns a position
     * in the <em>visible</em> ordering, which no longer matches the model's row order. Using
     * the raw view index to look up a record would silently read - and, in the delete path,
     * destroy - a different medicine than the one highlighted on screen.</p>
     */
    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    /**
     * Loads the current supplier list and opens a blank {@link MedicineFormDialog}. Requires
     * at least one supplier to exist, since a medicine can't be saved without a supplier_id.
     */
    private void openAddDialog() {
        List<Supplier> suppliers = fetchSuppliersOrShowError();
        if (suppliers == null) {
            return;
        }
        if (suppliers.isEmpty()) {
            UIUtils.showError(this, "Add at least one supplier before adding medicines.");
            return;
        }

        MedicineFormDialog dialog = new MedicineFormDialog(ownerFrame(), null, suppliers);
        dialog.setVisible(true);

        Optional<Medicine> newMedicine = dialog.getResult();
        if (newMedicine.isEmpty()) {
            return;
        }

        try {
            medicineService.addMedicine(newMedicine.get());
            loadMedicines();
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to add medicine: " + e.getMessage());
        }
    }

    /** Opens {@link MedicineFormDialog} pre-filled with the selected row; on confirmation, updates it. */
    private void openEditDialog() {
        int row = selectedModelRow();
        if (row < 0) {
            UIUtils.showError(this, "Select a medicine to edit.");
            return;
        }

        List<Supplier> suppliers = fetchSuppliersOrShowError();
        if (suppliers == null) {
            return;
        }

        Medicine existing = medicineFromRow(row);
        MedicineFormDialog dialog = new MedicineFormDialog(ownerFrame(), existing, suppliers);
        dialog.setVisible(true);

        Optional<Medicine> updated = dialog.getResult();
        if (updated.isEmpty()) {
            return;
        }

        try {
            medicineService.updateMedicine(updated.get());
            loadMedicines();
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to update medicine: " + e.getMessage());
        }
    }

    /** Deletes the selected medicine after an explicit confirmation prompt (destructive action). */
    private void deleteSelected() {
        int row = selectedModelRow();
        if (row < 0) {
            UIUtils.showError(this, "Select a medicine to delete.");
            return;
        }

        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete medicine \"" + name + "\"? This cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int medicineId = (int) tableModel.getValueAt(row, 0);
        try {
            medicineService.deleteMedicine(medicineId);
            loadMedicines();
        } catch (SQLException e) {
            // Likely cause: existing sale_items rows reference this medicine_id and the
            // foreign key is blocking the delete - surfaced as-is rather than masked.
            UIUtils.showError(this, "Failed to delete medicine: " + e.getMessage());
        }
    }

    /** Fetches suppliers for the form dialog's dropdown, or shows an error and returns null on failure. */
    private List<Supplier> fetchSuppliersOrShowError() {
        try {
            return supplierService.getAllSuppliers();
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to load suppliers: " + e.getMessage());
            return null;
        }
    }

    /** Reconstructs a {@link Medicine} from a table row so it can be handed to the edit dialog. */
    private Medicine medicineFromRow(int row) {
        return new Medicine(
                (int) tableModel.getValueAt(row, 0),
                (String) tableModel.getValueAt(row, 1),
                (String) tableModel.getValueAt(row, 2),
                (String) tableModel.getValueAt(row, 3),
                (BigDecimal) tableModel.getValueAt(row, 4),
                (int) tableModel.getValueAt(row, 5),
                (int) tableModel.getValueAt(row, 6),
                (LocalDate) tableModel.getValueAt(row, 7),
                (int) tableModel.getValueAt(row, 8)
        );
    }

    /** Finds the enclosing {@link Frame} so dialogs can center themselves over the app window. */
    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
