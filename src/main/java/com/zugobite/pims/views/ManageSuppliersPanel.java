package com.zugobite.pims.views;

import com.zugobite.pims.models.Supplier;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Admin tab for full CRUD on {@link Supplier} records (PLAN.md §5.3 "Manage Suppliers tab").
 *
 * <p>The panel is a thin UI layer: it never talks to JDBC directly. Every button click
 * delegates to {@link SupplierService}, and the panel's only job is to translate between
 * the {@link JTable} the user sees and the {@link Supplier} objects the service layer
 * understands.</p>
 */
public class ManageSuppliersPanel extends JPanel {

    private final SupplierService supplierService;

    // Column order here must match the order values are appended in loadSuppliers() below.
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Contact Person", "Phone", "Email", "Address"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // The table is read-only display; edits go through SupplierFormDialog instead
            // of in-place cell editing, so every value here stays consistent with the DB
            // until the user explicitly submits an edit.
            return false;
        }

        // ID is the only numeric column here - declaring its real type right-aligns it via
        // Swing's built-in Number renderer (see ManageMedicinesPanel for the full explanation).
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Integer.class : Object.class;
        }
    };
    private final JTable table = new StripedTable(tableModel);

    /**
     * Admin suppliers CRUD UI (Refresh/Add/Edit/Delete + table). Calls
     * {@link #loadSuppliers()} on construct so the table is populated immediately.
     */
    public ManageSuppliersPanel(SupplierService supplierService) {
        this.supplierService = supplierService;
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JButton refreshButton = new JButton("Refresh");
        JButton addButton = new JButton("Add Supplier");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        UIUtils.styleAsOutlineButton(refreshButton);
        UIUtils.styleAsPrimaryButton(addButton);
        UIUtils.styleAsOutlineButton(editButton);
        UIUtils.styleAsSolidDestructiveButton(deleteButton);

        refreshButton.addActionListener(e -> loadSuppliers());
        addButton.addActionListener(e -> openAddDialog());
        editButton.addActionListener(e -> openEditDialog());
        deleteButton.addActionListener(e -> deleteSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(refreshButton);
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);

        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(UIUtils.createRoundedScrollPane(table), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        // Populate the table immediately on construction so the tab isn't blank until the
        // user manually clicks "Refresh".
        loadSuppliers();
    }

    /** Re-queries every supplier from the database and repopulates the table from scratch. */
    public void loadSuppliers() {
        try {
            List<Supplier> suppliers = supplierService.getAllSuppliers();
            tableModel.setRowCount(0);
            for (Supplier s : suppliers) {
                tableModel.addRow(new Object[]{
                        s.getSupplierId(), s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress()
                });
            }
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to load suppliers: " + e.getMessage());
        }
    }

    /** Opens a blank {@link SupplierFormDialog}; on confirmation, inserts and refreshes the table. */
    private void openAddDialog() {
        SupplierFormDialog dialog = new SupplierFormDialog(ownerFrame(), null);
        // setVisible(true) blocks here because the dialog is modal - by the time it
        // returns, the user has already clicked OK/Cancel or closed the window.
        dialog.setVisible(true);

        Optional<Supplier> newSupplier = dialog.getResult();
        if (newSupplier.isEmpty()) {
            // User cancelled - nothing to persist.
            return;
        }

        try {
            supplierService.addSupplier(newSupplier.get());
            loadSuppliers();
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to add supplier: " + e.getMessage());
        }
    }

    /** Opens {@link SupplierFormDialog} pre-filled with the selected row; on confirmation, updates it. */
    private void openEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showError(this, "Select a supplier to edit.");
            return;
        }

        Supplier existing = supplierFromRow(row);
        SupplierFormDialog dialog = new SupplierFormDialog(ownerFrame(), existing);
        dialog.setVisible(true);

        Optional<Supplier> updated = dialog.getResult();
        if (updated.isEmpty()) {
            return;
        }

        try {
            supplierService.updateSupplier(updated.get());
            loadSuppliers();
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to update supplier: " + e.getMessage());
        }
    }

    /** Deletes the selected supplier after an explicit confirmation prompt (destructive action). */
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showError(this, "Select a supplier to delete.");
            return;
        }

        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete supplier \"" + name + "\"? This cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int supplierId = (int) tableModel.getValueAt(row, 0);
        try {
            supplierService.deleteSupplier(supplierId);
            loadSuppliers();
        } catch (SQLException e) {
            // Most likely cause: medicines still reference this supplier_id, and the
            // foreign key in the `medicines` table is rejecting the delete. Surfacing the
            // raw JDBC message tells the admin exactly that, without us needing a bespoke
            // "supplier is in use" check that would just duplicate what the DB already knows.
            UIUtils.showError(this, "Failed to delete supplier: " + e.getMessage());
        }
    }

    /** Reconstructs a {@link Supplier} from a table row so it can be handed to the edit dialog. */
    private Supplier supplierFromRow(int row) {
        return new Supplier(
                (int) tableModel.getValueAt(row, 0),
                (String) tableModel.getValueAt(row, 1),
                (String) tableModel.getValueAt(row, 2),
                (String) tableModel.getValueAt(row, 3),
                (String) tableModel.getValueAt(row, 4),
                (String) tableModel.getValueAt(row, 5)
        );
    }

    /** Finds the enclosing {@link Frame} so dialogs can center themselves over the app window. */
    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
