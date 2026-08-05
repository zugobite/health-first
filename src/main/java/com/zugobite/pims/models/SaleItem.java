package com.zugobite.pims.models;

import java.math.BigDecimal;

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
