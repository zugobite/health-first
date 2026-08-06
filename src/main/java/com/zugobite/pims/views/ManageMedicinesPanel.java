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
    private final JTable table = new StripedTable(tableModel);

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
        UIUtils.styleAsDestructiveButton(deleteButton);

        refreshButton.addActionListener(e -> loadMedicines());
        addButton.addActionListener(e -> openAddDialog());
        editButton.addActionListener(e -> openEditDialog());
        deleteButton.addActionListener(e -> deleteSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(refreshButton);
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);

        // The table lives inside a bordered, rounded CardPanel rather than sitting directly
        // on the page background - the same "content in a card" composition used on every
        // data screen in the app (see CardPanel's class comment).
        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(UIUtils.createRoundedScrollPane(table), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        loadMedicines();
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
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to load medicines: " + e.getMessage());
        }
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
        int row = table.getSelectedRow();
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
        int row = table.getSelectedRow();
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
