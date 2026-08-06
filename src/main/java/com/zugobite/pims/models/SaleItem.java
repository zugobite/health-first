package com.zugobite.pims.models;

import java.math.BigDecimal;

/**
 * One line item within a {@link Sale} - maps to a row in `sale_items`, e.g. "3x Paracetamol
 * @ R25.99 each".
 *
 * <p>{@code priceAtSale} is deliberately captured at the moment the item is added to the
 * cart rather than looked up again at report time. This is what makes historical sales and
 * the Item-Wise report accurate even if {@link Medicine#getPrice()} changes later - a price
 * increase next month must never retroactively change what last month's receipts say a
 * customer paid.</p>
 *
 * <p>Immutable: once a line item has been added to a cart/sale, nothing in the UI needs to
 * edit its quantity or price in place - {@link com.zugobite.pims.views.PosPanel} instead
 * rebuilds the whole cart if the user wants to change something (see "Clear Cart").</p>
 */
public class SaleItem {

    private final int medicineId;
    private final String medicineName;
    private final int quantitySold;
    private final BigDecimal priceAtSale;

    public SaleItem(int medicineId, String medicineName, int quantitySold, BigDecimal priceAtSale) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantitySold = quantitySold;
        this.priceAtSale = priceAtSale;
    }

    /** quantitySold x priceAtSale - this line's contribution to the sale's grand total. */
    public BigDecimal getLineTotal() {
        return priceAtSale.multiply(BigDecimal.valueOf(quantitySold));
    }

    public int getMedicineId() {
        return medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }
}
