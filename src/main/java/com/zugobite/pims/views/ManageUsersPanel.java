package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ManageUsersPanel extends JPanel {

    private final UserService userService;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Username", "Full Name", "Role"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ManageUsersPanel(UserService userService) {
        this.userService = userService;
        setLayout(new BorderLayout(8, 8));
        UIUtils.applyStandardPadding(this);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton addButton = new JButton("Add Cashier");
        JButton deleteButton = new JButton("Delete");
        refreshButton.addActionListener(e -> loadCashiers());
        buttons.add(refreshButton);
        buttons.add(addButton);
        buttons.add(deleteButton);
        add(buttons, BorderLayout.SOUTH);

        loadCashiers();
    }

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
}
