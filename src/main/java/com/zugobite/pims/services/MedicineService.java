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

/**
 * Data-access layer for the `medicines` table — CRUD backing Admin's "Manage Medicines" tab,
 * plus the read-only lookups used by Cashier's POS search and Stock Check screens
 * ({@link #searchByName}).
 *
 * <p>Note this class does not decrement stock on a sale — that happens inside
 * {@link SalesService#checkout} as part of the same database transaction as the sale
 * itself, so a checkout and its stock decrement can never partially succeed.</p>
 */
public class MedicineService {

    /** Every medicine, alphabetical by name — used to populate the full "Manage Medicines" table. */
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

    /**
     * Case-insensitive partial-match search by name (SQL {@code LIKE '%query%'}), used by
     * both {@link com.zugobite.pims.views.PosPanel} (find items to add to the cart) and
     * {@link com.zugobite.pims.views.StockCheckPanel} (look up price/availability without
     * starting a sale, per PLAN.md §5.2).
     */
    public List<Medicine> searchByName(String query) throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM medicines WHERE name LIKE ? ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // The '%' wildcards are baked into the bound parameter value, not the SQL
            // string itself, so a search term containing SQL metacharacters still can't
            // break out of the LIKE clause.
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapRow(rs));
                }
            }
        }
        return medicines;
    }

    /** Inserts a new medicine. The {@code medicine.getMedicineId()} value is ignored — MySQL
     *  assigns the real ID via AUTO_INCREMENT. */
    public void addMedicine(Medicine medicine) throws SQLException {
        String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindMedicine(stmt, medicine);
            stmt.executeUpdate();
        }
    }

    /** Overwrites every column for the row matching {@code medicine.getMedicineId()}. */
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

    /**
     * Deletes a medicine by ID.
     *
     * @throws SQLException if the `sale_items.medicine_id` foreign key blocks the delete
     *                       because this medicine appears in past sales — historical sale
     *                       line items must be preserved
     */
    public void deleteMedicine(int medicineId) throws SQLException {
        String sql = "DELETE FROM medicines WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, medicineId);
            stmt.executeUpdate();
        }
    }

    /**
     * Binds all eight editable {@link Medicine} fields to parameters 1-8 of {@code stmt}, in
     * the exact column order both {@link #addMedicine} and {@link #updateMedicine} use.
     */
    private void bindMedicine(PreparedStatement stmt, Medicine medicine) throws SQLException {
        stmt.setString(1, medicine.getName());
        stmt.setString(2, medicine.getCompany());
        stmt.setString(3, medicine.getMedicineType());
        stmt.setBigDecimal(4, medicine.getPrice());
        stmt.setInt(5, medicine.getQuantityInStock());
        stmt.setInt(6, medicine.getReorderLevel());
        // java.sql.Date.valueOf() converts the model's java.time.LocalDate into the legacy
        // java.sql.Date type PreparedStatement.setDate() expects.
        stmt.setDate(7, Date.valueOf(medicine.getExpiryDate()));
        stmt.setInt(8, medicine.getSupplierId());
    }

    /** Converts one row of a `medicines` {@link ResultSet} into a {@link Medicine}. */
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
                // expiry_date is nullable in the schema, so guard against a NULL column
                // before calling .toLocalDate() on it.
                expiry != null ? expiry.toLocalDate() : null,
                rs.getInt("supplier_id")
        );
    }
}
