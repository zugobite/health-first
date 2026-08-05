package com.zugobite.pims.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single point-of-sale transaction: a header row in `sales` plus its associated
 * `sale_items` rows. This class doubles as both the in-progress shopping cart in
 * {@link com.zugobite.pims.views.PosPanel} (built up one {@link #addItem} call at a time as
 * the cashier scans/searches medicines) and the object handed to
 * {@link com.zugobite.pims.services.SalesService#checkout} to be persisted.
 *
 * <p>{@code saleId} and {@code saleDate} start unset (0 / {@code null}) and only become
 * meaningful *after* {@code checkout()} has run — the database assigns the `sale_id` via
 * AUTO_INCREMENT and the `sale_date` via {@code DEFAULT CURRENT_TIMESTAMP}, so this object
 * can't know either value up front. {@code SalesService} calls {@link #setSaleId} once it
 * reads back the generated key.</p>
 */
public class Sale {

    private int saleId;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private final int userId;
    private final List<SaleItem> items = new ArrayList<>();

    public Sale(int userId) {
        this.userId = userId;
        this.totalAmount = BigDecimal.ZERO;
    }

    /**
     * Adds a line item to the cart and rolls its line total into the running
     * {@code totalAmount}, so the cart total is always kept in sync incrementally rather
     * than recomputed by summing {@code items} on every read.
     */
    public void addItem(SaleItem item) {
        items.add(item);
        totalAmount = totalAmount.add(item.getLineTotal());
    }

    public int getSaleId() {
        return saleId;
    }

    /** Set by {@link com.zugobite.pims.services.SalesService#checkout} once the DB assigns an ID. */
    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public int getUserId() {
        return userId;
    }

    public List<SaleItem> getItems() {
        return items;
    }
}
