package com.zugobite.pims.services;

import com.zugobite.pims.data.DBConnection;
import com.zugobite.pims.models.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access layer for the `suppliers` table - straightforward CRUD backing Admin's
 * "Manage Suppliers" tab ({@link com.zugobite.pims.views.ManageSuppliersPanel}).
 *
 * <p>Also consumed read-only by {@link com.zugobite.pims.views.MedicineFormDialog} (via
 * {@link com.zugobite.pims.views.ManageMedicinesPanel}) to populate its supplier dropdown,
 * since every {@link com.zugobite.pims.models.Medicine} must link to a valid supplier.</p>
 */
public class SupplierService {

    /** All suppliers, alphabetical by name - the order the "Manage Suppliers" table and the
     *  medicine form's supplier dropdown both display them in. */
    public List<Supplier> getAllSuppliers() throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                suppliers.add(mapRow(rs));
            }
        }
        return suppliers;
    }

    /** Inserts a new supplier. The {@code supplier.getSupplierId()} value is ignored - MySQL
     *  assigns the real ID via AUTO_INCREMENT. */
    public void addSupplier(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindSupplier(stmt, supplier);
            stmt.executeUpdate();
        }
    }

    /** Overwrites every column for the row matching {@code supplier.getSupplierId()}. */
    public void updateSupplier(Supplier supplier) throws SQLException {
        String sql = "UPDATE suppliers SET name = ?, contact_person = ?, phone = ?, email = ?, address = ? WHERE supplier_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindSupplier(stmt, supplier);
            stmt.setInt(6, supplier.getSupplierId());
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a supplier by ID.
     *
     * @throws SQLException if the `medicines.supplier_id` foreign key blocks the delete
     *                       because one or more medicines still reference this supplier -
     *                       those medicines must be reassigned or removed first
     */
    public void deleteSupplier(int supplierId) throws SQLException {
        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supplierId);
            stmt.executeUpdate();
        }
    }

    /**
     * Binds the five editable {@link Supplier} fields to parameters 1-5 of {@code stmt}, in
     * the exact column order both {@link #addSupplier} and {@link #updateSupplier} use.
     * Shared here so the two INSERT/UPDATE statements can't drift out of sync with each
     * other over time.
     */
    private void bindSupplier(PreparedStatement stmt, Supplier supplier) throws SQLException {
        stmt.setString(1, supplier.getName());
        stmt.setString(2, supplier.getContactPerson());
        stmt.setString(3, supplier.getPhone());
        stmt.setString(4, supplier.getEmail());
        stmt.setString(5, supplier.getAddress());
    }

    /** Converts one row of a `suppliers` {@link ResultSet} into a {@link Supplier}. */
    private Supplier mapRow(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getInt("supplier_id"),
                rs.getString("name"),
                rs.getString("contact_person"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address")
        );
    }
}
