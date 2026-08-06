package com.zugobite.pims.services;

import com.zugobite.pims.data.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Backs the four reports PLAN.md §5.4 requires on Admin's "Reports" tab: Sales,
 * Item-Wise, Low Stock, and Expiry.
 *
 * <p>Each report method returns {@code List<Map<String, Object>>} rather than a dedicated
 * model class per report. This is a deliberate shortcut: report rows are display-only (they
 * flow straight into a {@link javax.swing.table.DefaultTableModel} in
 * {@link com.zugobite.pims.views.ReportsPanel} and are never edited or passed back into a
 * service), so a generic row shape avoids four near-identical throwaway classes. The
 * {@link LinkedHashMap} specifically (not a plain {@code HashMap}) preserves column order,
 * which is what lets {@code ReportsPanel} build its table columns directly from
 * {@code rows.get(0).keySet()}.</p>
 */
public class ReportService {

    /**
     * Every sale between {@code from} and {@code to} (inclusive), joined with the cashier's
     * name. Powers the "Sales report - transactions over a date range" requirement.
     */
    public List<Map<String, Object>> getSalesReport(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT s.sale_id, s.sale_date, s.total_amount, u.full_name AS cashier "
                + "FROM sales s JOIN users u ON s.user_id = u.user_id "
                + "WHERE DATE(s.sale_date) BETWEEN ? AND ? ORDER BY s.sale_date";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("saleId", rs.getInt("sale_id"));
                    row.put("saleDate", rs.getTimestamp("sale_date"));
                    row.put("totalAmount", rs.getBigDecimal("total_amount"));
                    row.put("cashier", rs.getString("cashier"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /**
     * Sales aggregated per medicine - total quantity sold and total revenue, richest first.
     * Powers the "Item-Wise report" requirement. Deliberately reads from `sale_items`
     * (historical, immutable per-sale prices) rather than joining live `medicines.price`, so
     * this report reflects what customers were actually charged at the time, not today's
     * price.
     */
    public List<Map<String, Object>> getItemWiseReport() throws SQLException {
        String sql = "SELECT m.name, SUM(si.quantity_sold) AS quantity_sold, SUM(si.quantity_sold * si.price_at_sale) AS revenue "
                + "FROM sale_items si JOIN medicines m ON si.medicine_id = m.medicine_id "
                + "GROUP BY m.medicine_id, m.name ORDER BY revenue DESC";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", rs.getString("name"));
                row.put("quantitySold", rs.getInt("quantity_sold"));
                row.put("revenue", rs.getBigDecimal("revenue"));
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Every medicine at or below its reorder threshold. Powers the "Low Stock report -
     * medicines where quantity_in_stock <= reorder_level" requirement - the same condition
     * as {@link com.zugobite.pims.models.Medicine#isLowStock()}, just run as an aggregate
     * query across the whole table instead of checked on one object at a time.
     */
    public List<Map<String, Object>> getLowStockReport() throws SQLException {
        String sql = "SELECT name, quantity_in_stock, reorder_level FROM medicines WHERE quantity_in_stock <= reorder_level ORDER BY name";
        return runSimpleQuery(sql);
    }

    /**
     * Every medicine expiring within the next month from today. Powers the "Expiry report"
     * requirement - uses MySQL's own {@code CURDATE()}/{@code DATE_ADD()} (evaluated on the
     * database server) rather than passing today's date in from Java, so the report is
     * always correct relative to the DB server's clock regardless of the client machine's.
     */
    public List<Map<String, Object>> getExpiryReport() throws SQLException {
        String sql = "SELECT name, expiry_date FROM medicines "
                + "WHERE expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 MONTH) ORDER BY expiry_date";
        return runSimpleQuery(sql);
    }

    /**
     * Runs a report query that takes no parameters and returns whatever columns it selects,
     * mapping each row generically via {@link java.sql.ResultSetMetaData} instead of naming
     * columns by hand - shared by {@link #getLowStockReport()} and {@link #getExpiryReport()}
     * since both are simple "SELECT ... WHERE ..." queries with no joins or bound parameters.
     */
    private List<Map<String, Object>> runSimpleQuery(String sql) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
