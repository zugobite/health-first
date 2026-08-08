package com.zugobite.pims.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single point-of-sale transaction: a header row in `sales` plus its associated
 * `sale_items` rows. This class doubles as both the in-progress shopping cart in
 * {@link com.zugobite.pims.views.PosPanel} (built up and edited as the cashier serves a
 * customer) and the object handed to
 * {@link com.zugobite.pims.services.SalesService#checkout} to be persisted.
 *
 * <p>{@code saleId} and {@code saleDate} start unset (0 / {@code null}) and only become
 * meaningful *after* {@code checkout()} has run — the database assigns the `sale_id` via
 * AUTO_INCREMENT and the `sale_date` via {@code DEFAULT CURRENT_TIMESTAMP}, so this object
 * can't know either value up front. {@code SalesService} sets both once it reads them back.</p>
 *
 * <p><b>Mutability model:</b> {@link SaleItem} is immutable, but a cart is not — a cashier
 * routinely corrects a quantity or voids a line mid-sale. So quantity changes are expressed
 * as "replace the item at this index with an identical one at a new quantity"
 * ({@link #setItemQuantity}) rather than by mutating a {@code SaleItem} in place. Every
 * mutator funnels through {@link #recalculateTotal()}, which is what guarantees
 * {@link #getTotalAmount()} can never drift out of sync with {@link #getItems()} — the bug
 * that an add-only API quietly avoided but that removal/editing would otherwise introduce.</p>
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
     * Appends a line item to the cart. Callers that want "add one more of something already
     * in the cart" should prefer {@link #indexOfMedicine} + {@link #setItemQuantity} so the
     * same medicine never appears on two separate receipt lines.
     */
    public void addItem(SaleItem item) {
        items.add(item);
        recalculateTotal();
    }

    /**
     * Replaces the line at {@code index} with an otherwise-identical line at
     * {@code newQuantity} — how an inline quantity edit in the cart table is applied.
     * A {@code newQuantity} of zero or less removes the line outright, matching the
     * behaviour cashiers expect when they clear a quantity cell to void an item.
     */
    public void setItemQuantity(int index, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(index);
            return;
        }
        SaleItem existing = items.get(index);
        items.set(index, new SaleItem(
                existing.getMedicineId(), existing.getMedicineName(), newQuantity, existing.getPriceAtSale()));
        recalculateTotal();
    }

    /** Voids a single line (the cart's "Remove" action), leaving the rest of the sale intact. */
    public void removeItem(int index) {
        items.remove(index);
        recalculateTotal();
    }

    /**
     * @return the index of the line selling {@code medicineId}, or {@code -1} if this
     *         medicine isn't in the cart yet. Used to merge a repeat scan/add into the
     *         existing line instead of creating a duplicate one.
     */
    public int indexOfMedicine(int medicineId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getMedicineId() == medicineId) {
                return i;
            }
        }
        return -1;
    }

    /** Total number of physical units in the cart (summed across lines) — shown as the cart's item count. */
    public int getTotalQuantity() {
        int total = 0;
        for (SaleItem item : items) {
            total += item.getQuantitySold();
        }
        return total;
    }

    /**
     * Recomputes {@link #totalAmount} from scratch off the current line items. Deliberately a
     * full recomputation rather than an incremental add/subtract: incremental updates are
     * where floating rounding and "forgot to subtract on remove" bugs come from, and a cart
     * is never long enough for the cost to matter.
     */
    private void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (SaleItem item : items) {
            sum = sum.add(item.getLineTotal());
        }
        totalAmount = sum;
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

    /**
     * @return an unmodifiable view of the cart's lines. Unmodifiable on purpose: every
     *         mutation must go through this class's own methods so {@link #recalculateTotal()}
     *         always runs — a caller editing the returned list directly would silently
     *         desynchronise the total from the items.
     */
    public List<SaleItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
