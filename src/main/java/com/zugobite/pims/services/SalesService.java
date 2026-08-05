package com.zugobite.pims.services;

import com.zugobite.pims.data.DBConnection;
import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class SalesService {

    public void checkout(Sale sale) throws SQLException {
        String insertSale = "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)";
        String insertItem = "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES (?, ?, ?, ?)";
        String decrementStock = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? WHERE medicine_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int saleId;
                try (PreparedStatement stmt = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setBigDecimal(1, sale.getTotalAmount());
                    stmt.setInt(2, sale.getUserId());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        keys.next();
                        saleId = keys.getInt(1);
                    }
                }

                try (PreparedStatement itemStmt = conn.prepareStatement(insertItem);
                     PreparedStatement stockStmt = conn.prepareStatement(decrementStock)) {
                    for (SaleItem item : sale.getItems()) {
                        itemStmt.setInt(1, saleId);
                        itemStmt.setInt(2, item.getMedicineId());
                        itemStmt.setInt(3, item.getQuantitySold());
                        itemStmt.setBigDecimal(4, item.getPriceAtSale());
                        itemStmt.addBatch();

                        stockStmt.setInt(1, item.getQuantitySold());
                        stockStmt.setInt(2, item.getMedicineId());
                        stockStmt.addBatch();
                    }
                    itemStmt.executeBatch();
                    stockStmt.executeBatch();
                }

                sale.setSaleId(saleId);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
