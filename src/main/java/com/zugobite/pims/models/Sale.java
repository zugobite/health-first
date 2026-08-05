package com.zugobite.pims.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public void addItem(SaleItem item) {
        items.add(item);
        totalAmount = totalAmount.add(item.getLineTotal());
    }

    public int getSaleId() {
        return saleId;
    }

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
