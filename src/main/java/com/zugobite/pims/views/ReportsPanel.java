package com.zugobite.pims.views;

import com.zugobite.pims.services.ReportExportService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin's "Reports" tab: a single screen that can render any of the four required reports
 * (PLAN.md §5.4) by re-purposing one generic table for whichever report is currently
 * selected, rather than building four separate hardcoded tab panels. This works because
 * {@link ReportService}'s report methods all return the same generic
 * {@code List<Map<String, Object>>} shape - see {@link #populateTable} for how that's turned
 * into a table's columns/rows on the fly, and see {@link ReportExportService} for how the
 * same generic shape is reused again to drive the "Export to PDF"/"Export to Excel" buttons.
 */
public class ReportsPanel extends JPanel {

    private final ReportService reportService;
    private final ReportExportService reportExportService = new ReportExportService();

    // Starts with zero columns; populateTable() reconfigures both columns and rows every
    // time a different report is run, since each report has a different column set.
    // columnClasses is kept in step with the current column set so numeric columns (any
    // report might have several, e.g. Sales' totalAmount or Low Stock's two int columns)
    // render right-aligned via Swing's built-in Number renderer, the same as every other
    // table in the app - see ManageMedicinesPanel for the full explanation of the mechanism.
    private Class<?>[] columnClasses = new Class<?>[0];
    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex < columnClasses.length ? columnClasses[columnIndex] : Object.class;
        }
    };
    private final JTable table = new StripedTable(tableModel);
    private final JComboBox<String> reportSelector = new JComboBox<>(
            new String[]{"Sales Report", "Item-Wise Report", "Low Stock Report", "Expiry Report"}
    );

    // The data behind whatever's currently on screen, kept around so the Export buttons can
    // hand the exact same rows to ReportExportService without re-querying the database -
    // what the user sees in the table is guaranteed to be what gets exported. Null until the
    // first successful "Run Report" click.
    private List<Map<String, Object>> lastRows;
    private String lastReportTitle;

    public ReportsPanel(ReportService reportService) {
        this.reportService = reportService;
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JButton runButton = new JButton("Run Report");
        JButton exportPdfButton = new JButton("Export to PDF");
        JButton exportExcelButton = new JButton("Export to Excel");
        UIUtils.styleAsPrimaryButton(runButton);
        UIUtils.styleAsOutlineButton(exportPdfButton);
        UIUtils.styleAsOutlineButton(exportExcelButton);
        runButton.addActionListener(e -> runSelectedReport());
        exportPdfButton.addActionListener(e -> exportReport(ExportFormat.PDF));
        exportExcelButton.addActionListener(e -> exportReport(ExportFormat.EXCEL));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Report"));
        toolbar.add(reportSelector);
        toolbar.add(runButton);
        toolbar.add(exportPdfButton);
        toolbar.add(exportExcelButton);

        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(UIUtils.createRoundedScrollPane(table), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    /**
     * Calls whichever {@link ReportService} method matches the selected dropdown entry, then
     * hands the result to {@link #populateTable}. The Sales Report is hardcoded to the last
     * month as a reasonable default range - PLAN.md doesn't specify a UI for picking a custom
     * date range, so "the last month" keeps the report immediately useful without extra
     * date-picker fields.
     */
    private void runSelectedReport() {
        try {
            String title = (String) reportSelector.getSelectedItem();
            List<Map<String, Object>> rows = switch (reportSelector.getSelectedIndex()) {
                case 0 -> reportService.getSalesReport(LocalDate.now().minusMonths(1), LocalDate.now());
                case 1 -> reportService.getItemWiseReport();
                case 2 -> reportService.getLowStockReport();
                default -> reportService.getExpiryReport();
            };
            populateTable(rows);
            lastRows = rows;
            lastReportTitle = title;
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to run report: " + e.getMessage());
        }
    }

    /**
     * Rebuilds the table's columns and rows from a generic report result. Column headers are
     * human-readable labels derived from the first row's map keys (every row within a single
     * report has the same keys, since they all come from the same SQL query), and each row's
     * values are appended in that same key order. If the report is empty, the table is simply
     * cleared with no columns - an empty Low Stock or Expiry report genuinely has nothing to show.
     */
    private void populateTable(List<Map<String, Object>> rows) {
        tableModel.setDataVector(new Object[][]{}, new Object[]{});
        columnClasses = new Class<?>[0];
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> firstRow = rows.get(0);
        Object[] columns = firstRow.keySet().stream()
                .map(UIUtils::prettifyColumnName)
                .toArray();
        tableModel.setColumnIdentifiers(columns);

        // Each column's class comes from the actual runtime type of that column's first
        // value (Integer/BigDecimal ⇒ right-aligned; String/Timestamp/Date ⇒ left-aligned) -
        // the same type-detection approach ReportExportService uses for its PDF/Excel column
        // alignment, kept in sync here so the on-screen table and the exported files agree.
        columnClasses = firstRow.values().stream()
                .map(value -> value instanceof Number ? value.getClass() : (Class<?>) Object.class)
                .toArray(Class<?>[]::new);

        for (Map<String, Object> row : rows) {
            tableModel.addRow(row.values().toArray());
        }
    }

    /**
     * Prompts for a save location (defaulting to a filename derived from the report title
     * and the chosen format) and delegates the actual rendering to
     * {@link ReportExportService}. Requires a report to have been run first - exporting
     * "whatever's currently on screen" doesn't make sense before anything has been run.
     */
    private void exportReport(ExportFormat format) {
        if (lastRows == null) {
            UIUtils.showError(this, "Run a report first, then export it.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String baseName = lastReportTitle.replace(" ", "-");
        chooser.setSelectedFile(new File(baseName + "." + format.extension));
        chooser.setFileFilter(new FileNameExtensionFilter(format.description, format.extension));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith("." + format.extension)) {
            target = new File(target.getParentFile(), target.getName() + "." + format.extension);
        }

        try {
            if (format == ExportFormat.PDF) {
                reportExportService.exportToPdf(lastReportTitle, lastRows, target);
            } else {
                reportExportService.exportToExcel(lastReportTitle, lastRows, target);
            }
            UIUtils.showInfo(this, lastReportTitle + " saved to " + target.getAbsolutePath());
        } catch (IOException e) {
            UIUtils.showError(this, "Failed to export report: " + e.getMessage());
        }
    }

    /** The two export targets this tab supports, each carrying its own file extension/filter description. */
    private enum ExportFormat {
        PDF("pdf", "PDF Document (*.pdf)"),
        EXCEL("xlsx", "Excel Workbook (*.xlsx)");

        private final String extension;
        private final String description;

        ExportFormat(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }
    }
}
