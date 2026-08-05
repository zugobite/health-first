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

/**
 * Admin's "Reports" tab: a single screen that can render any of the four required reports
 * (PLAN.md §5.4) by re-purposing one generic table for whichever report is currently
 * selected, rather than building four separate hardcoded tab panels. This works because
 * {@link ReportService}'s report methods all return the same generic
 * {@code List<Map<String, Object>>} shape — see {@link #populateTable} for how that's turned
 * into a table's columns/rows on the fly.
 */
public class ReportsPanel extends JPanel {

    private final ReportService reportService;
    // Starts with zero columns; populateTable() reconfigures both columns and rows every
    // time a different report is run, since each report has a different column set.
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

    /**
     * Calls whichever {@link ReportService} method matches the selected dropdown entry, then
     * hands the result to {@link #populateTable}. The Sales Report is hardcoded to the last
     * month as a reasonable default range — PLAN.md doesn't specify a UI for picking a custom
     * date range, so "the last month" keeps the report immediately useful without extra
     * date-picker fields.
     */
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

    /**
     * Rebuilds the table's columns and rows from a generic report result. Column headers are
     * taken from the first row's map keys (every row within a single report has the same
     * keys, since they all come from the same SQL query), and each row's values are appended
     * in that same key order. If the report is empty, the table is simply cleared with no
     * columns — an empty Low Stock or Expiry report genuinely has nothing to show.
     */
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
