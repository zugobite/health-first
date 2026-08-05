package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class StockCheckPanel extends JPanel {

    private final MedicineService medicineService;
    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Name", "Type", "Price", "In Stock"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public StockCheckPanel(MedicineService medicineService) {
        this.medicineService = medicineService;
        setLayout(new BorderLayout(8, 8));
        UIUtils.applyStandardPadding(this);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> search());
        top.add(new JLabel("Medicine name:"));
        top.add(searchField);
        top.add(searchButton);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void search() {
        try {
            List<Medicine> results = medicineService.searchByName(searchField.getText().trim());
            tableModel.setRowCount(0);
            for (Medicine m : results) {
                tableModel.addRow(new Object[]{m.getName(), m.getMedicineType(), m.getPrice(), m.getQuantityInStock()});
            }
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to search medicines: " + e.getMessage());
        }
    }
}
