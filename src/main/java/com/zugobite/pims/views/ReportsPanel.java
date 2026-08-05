package com.zugobite.pims.views;

import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportsPanel extends JPanel {

    private final ReportService reportService;
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JComboBox<String> reportSelector = new JComboBox<>(
            new String[]{"Sales Report", "Item-Wise Report", "Low Stock Report", "Expiry Report"}
    );

    public ReportsPanel(ReportService reportService) {
        this.reportService = reportService;
        setLayout(new BorderLayout(8, 8));
        UIUtils.applyStandardPadding(this);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton runButton = new JButton("Run Report");
        runButton.addActionListener(e -> runSelectedReport());
        top.add(new JLabel("Report:"));
        top.add(reportSelector);
        top.add(runButton);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void runSelectedReport() {
        try {
            List<Map<String, Object>> rows = switch (reportSelector.getSelectedIndex()) {
                case 0 -> reportService.getSalesReport(LocalDate.now().minusMonths(1), LocalDate.now());
                case 1 -> reportService.getItemWiseReport();
                case 2 -> reportService.getLowStockReport();
                default -> reportService.getExpiryReport();
            };
            populateTable(rows);
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to run report: " + e.getMessage());
        }
    }

    private void populateTable(List<Map<String, Object>> rows) {
        tableModel.setDataVector(new Object[][]{}, new Object[]{});
        if (rows.isEmpty()) {
            return;
        }
        Object[] columns = rows.get(0).keySet().toArray();
        tableModel.setColumnIdentifiers(columns);
        for (Map<String, Object> row : rows) {
            tableModel.addRow(row.values().toArray());
        }
    }
}
