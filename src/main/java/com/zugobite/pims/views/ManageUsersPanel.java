package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.awt.*;

/**
 * Admin tab for managing Cashier accounts (PLAN.md §5.3 "Manage Users tab":
 * "create/delete/manage Cashier accounts").
 *
 * <p>Deliberately scoped to Cashier accounts only - {@link UserService#getCashiers()} never
 * returns the logged-in Admin, so there's no risk of an Admin accidentally deleting their
 * own account from this screen, and no "Edit" action since PLAN.md only calls for
 * create/delete.</p>
 */
public class ManageUsersPanel extends JPanel {

    private final UserService userService;

    // Column order here must match the order values are appended in loadCashiers() below.
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Username", "Full Name", "Role"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
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
     * Admin Cashier-account management UI (Refresh/Add/Delete; no Edit). Calls
     * {@link #loadCashiers()} on construct so the table is populated immediately.
     */
    public ManageUsersPanel(UserService userService) {
        this.userService = userService;
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JButton refreshButton = new JButton("Refresh");
        JButton addButton = new JButton("Add Cashier");
        JButton deleteButton = new JButton("Delete");
        UIUtils.styleAsOutlineButton(refreshButton);
        UIUtils.styleAsPrimaryButton(addButton);
        UIUtils.styleAsSolidDestructiveButton(deleteButton);

        refreshButton.addActionListener(e -> loadCashiers());
        addButton.addActionListener(e -> openAddDialog());
        deleteButton.addActionListener(e -> deleteSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(refreshButton);
        toolbar.add(addButton);
        toolbar.add(deleteButton);

        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(UIUtils.createRoundedScrollPane(table), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        loadCashiers();
    }

    /** Re-queries every Cashier account from the database and repopulates the table from scratch. */
    public void loadCashiers() {
        try {
            List<User> cashiers = userService.getCashiers();
            tableModel.setRowCount(0);
            for (User u : cashiers) {
                tableModel.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getFullName(), u.getRole()});
            }
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to load cashier accounts: " + e.getMessage());
        }
    }

    /** Opens {@link CashierFormDialog}; on confirmation, creates the account and refreshes the table. */
    private void openAddDialog() {
        CashierFormDialog dialog = new CashierFormDialog(ownerFrame());
        dialog.setVisible(true);

        Optional<CashierFormDialog.CashierInput> input = dialog.getResult();
        if (input.isEmpty()) {
            return;
        }

        try {
            CashierFormDialog.CashierInput cashier = input.get();
            userService.createCashier(cashier.username(), cashier.password(), cashier.fullName());
            loadCashiers();
        } catch (SQLException e) {
            // Most likely cause: the `username` UNIQUE constraint rejected a duplicate -
            // surfaced as-is so the admin knows to pick a different username.
            UIUtils.showError(this, "Failed to create cashier account: " + e.getMessage());
        }
    }

    /** Deletes the selected Cashier account after an explicit confirmation prompt. */
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showError(this, "Select a cashier account to delete.");
            return;
        }

        String username = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete cashier account \"" + username + "\"? This cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        try {
            userService.deleteUser(userId);
            loadCashiers();
        } catch (SQLException e) {
            // Likely cause: existing sales rows reference this user_id as the cashier who
            // processed them, and the foreign key is blocking the delete.
            UIUtils.showError(this, "Failed to delete cashier account: " + e.getMessage());
        }
    }

    /** Finds the enclosing {@link Frame} so dialogs can center themselves over the app window. */
    private Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
