package com.zugobite.pims.services;

import com.zugobite.pims.data.DBConnection;
import com.zugobite.pims.models.Medicine;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MedicineService {

    public List<Medicine> getAllMedicines() throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM medicines ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                medicines.add(mapRow(rs));
            }
        }
        return medicines;
    }

    public List<Medicine> searchByName(String query) throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM medicines WHERE name LIKE ? ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapRow(rs));
                }
            }
        }
        return medicines;
    }

    public void addMedicine(Medicine medicine) throws SQLException {
        String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindMedicine(stmt, medicine);
            stmt.executeUpdate();
        }
    }

    public void updateMedicine(Medicine medicine) throws SQLException {
        String sql = "UPDATE medicines SET name = ?, company = ?, medicine_type = ?, price = ?, quantity_in_stock = ?, "
                + "reorder_level = ?, expiry_date = ?, supplier_id = ? WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindMedicine(stmt, medicine);
            stmt.setInt(9, medicine.getMedicineId());
            stmt.executeUpdate();
        }
    }

    public void deleteMedicine(int medicineId) throws SQLException {
        String sql = "DELETE FROM medicines WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, medicineId);
            stmt.executeUpdate();
        }
    }

    private void bindMedicine(PreparedStatement stmt, Medicine medicine) throws SQLException {
        stmt.setString(1, medicine.getName());
        stmt.setString(2, medicine.getCompany());
        stmt.setString(3, medicine.getMedicineType());
        stmt.setBigDecimal(4, medicine.getPrice());
        stmt.setInt(5, medicine.getQuantityInStock());
        stmt.setInt(6, medicine.getReorderLevel());
        stmt.setDate(7, Date.valueOf(medicine.getExpiryDate()));
        stmt.setInt(8, medicine.getSupplierId());
    }

    private Medicine mapRow(ResultSet rs) throws SQLException {
        Date expiry = rs.getDate("expiry_date");
        return new Medicine(
                rs.getInt("medicine_id"),
                rs.getString("name"),
                rs.getString("company"),
                rs.getString("medicine_type"),
                rs.getBigDecimal("price"),
                rs.getInt("quantity_in_stock"),
                rs.getInt("reorder_level"),
                expiry != null ? expiry.toLocalDate() : null,
                rs.getInt("supplier_id")
        );
    }
}
