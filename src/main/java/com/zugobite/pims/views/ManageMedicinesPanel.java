package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ManageMedicinesPanel extends JPanel {

    private final MedicineService medicineService;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Company", "Type", "Price", "Stock", "Reorder", "Expiry", "Supplier ID"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ManageMedicinesPanel(MedicineService medicineService) {
        this.medicineService = medicineService;
        setLayout(new BorderLayout(8, 8));
        UIUtils.applyStandardPadding(this);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        refreshButton.addActionListener(e -> loadMedicines());
        buttons.add(refreshButton);
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        add(buttons, BorderLayout.SOUTH);

        loadMedicines();
    }

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
}
