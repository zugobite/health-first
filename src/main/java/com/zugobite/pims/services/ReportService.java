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

public class ReportService {

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

    public List<Map<String, Object>> getLowStockReport() throws SQLException {
        String sql = "SELECT name, quantity_in_stock, reorder_level FROM medicines WHERE quantity_in_stock <= reorder_level ORDER BY name";
        return runSimpleQuery(sql);
    }

    public List<Map<String, Object>> getExpiryReport() throws SQLException {
        String sql = "SELECT name, expiry_date FROM medicines "
                + "WHERE expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 MONTH) ORDER BY expiry_date";
        return runSimpleQuery(sql);
    }

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
