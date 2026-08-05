package com.zugobite.pims.views;

import com.zugobite.pims.models.Supplier;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ManageSuppliersPanel extends JPanel {

    private final SupplierService supplierService;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Contact Person", "Phone", "Email", "Address"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ManageSuppliersPanel(SupplierService supplierService) {
        this.supplierService = supplierService;
        setLayout(new BorderLayout(8, 8));
        UIUtils.applyStandardPadding(this);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        refreshButton.addActionListener(e -> loadSuppliers());
        buttons.add(refreshButton);
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        add(buttons, BorderLayout.SOUTH);

        loadSuppliers();
    }

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
}
