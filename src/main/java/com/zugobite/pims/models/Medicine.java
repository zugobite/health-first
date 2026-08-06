package com.zugobite.pims.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single inventory item - maps 1:1 to a row in the `medicines` table. This is the core
 * domain object the whole app revolves around: Admin does CRUD on it directly (Manage
 * Medicines tab), Cashier reads its price/stock during checkout (POS + Stock Check), and
 * every report either aggregates or filters on its fields.
 *
 * <p>{@code price} is a {@link BigDecimal} rather than {@code double} to avoid floating-point
 * rounding errors on money - the same reason the `price` and `price_at_sale` columns in
 * {@code database.sql} are {@code DECIMAL(10,2)} rather than {@code FLOAT}.</p>
 */
public class Medicine {

    private int medicineId;
    private String name;
    private String company;
    private String medicineType;
    private BigDecimal price;
    private int quantityInStock;
    private int reorderLevel;
    private LocalDate expiryDate;
    private int supplierId;

    public Medicine(int medicineId, String name, String company, String medicineType, BigDecimal price,
                     int quantityInStock, int reorderLevel, LocalDate expiryDate, int supplierId) {
        this.medicineId = medicineId;
        this.name = name;
        this.company = company;
        this.medicineType = medicineType;
        this.price = price;
        this.quantityInStock = quantityInStock;
        this.reorderLevel = reorderLevel;
        this.expiryDate = expiryDate;
        this.supplierId = supplierId;
    }

    /**
     * Mirrors the condition used by {@link com.zugobite.pims.services.ReportService#getLowStockReport()}
     * (`quantity_in_stock <= reorder_level`). Kept here too as a convenience for any future
     * UI that wants to flag a single medicine (e.g. highlighting a low-stock row red in a
     * table) without running a separate report query.
     */
    public boolean isLowStock() {
        return quantityInStock <= reorderLevel;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getMedicineType() {
        return medicineType;
    }

    public void setMedicineType(String medicineType) {
        this.medicineType = medicineType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(int quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * Used by Swing components (e.g. any future {@link javax.swing.JComboBox} of medicines)
     * to render this object as just its name instead of the default {@code Object.toString()}.
     */
    @Override
    public String toString() {
        return name;
    }
}
