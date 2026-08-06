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

/**
 * Handles the one operation with real correctness stakes in this app: turning a Cashier's
 * cart into a persisted sale. PLAN.md §5.2 requires checkout to write the `sales` row plus
 * one `sale_items` row per line item "in a single transaction", and to decrement
 * `quantity_in_stock` — this class is where all three of those happen atomically.
 */
public class SalesService {

    /**
     * Persists a completed {@link Sale}: inserts the `sales` header, inserts one
     * `sale_items` row per cart line, and decrements `medicines.quantity_in_stock` for each
     * medicine sold — all inside a single JDBC transaction.
     *
     * <p>Why a transaction matters here: without one, a crash or error partway through (say,
     * after the sale header and 2 of 3 line items have been written) would leave the
     * database in an inconsistent state — a sale that doesn't match its own total, and stock
     * counts that were never decremented for the missing item. Wrapping everything in
     * {@code conn.setAutoCommit(false)} / {@code commit()} / {@code rollback()} means either
     * the *entire* checkout succeeds, or none of it is saved at all.</p>
     *
     * <p>On success, mutates the passed-in {@code sale} by calling {@link Sale#setSaleId(int)}
     * and {@link Sale#setSaleDate}, populated from what the database actually assigned (the
     * AUTO_INCREMENT id and the {@code DEFAULT CURRENT_TIMESTAMP} sale_date respectively), so
     * the caller (see {@link com.zugobite.pims.views.PosPanel#checkout()}) can immediately
     * print or PDF-export a bill referencing the real, persisted sale number and timestamp.</p>
     *
     * @throws SQLException propagated after a rollback, if any step of the transaction fails
     *                       (e.g. a medicine_id that no longer exists)
     */
    public void checkout(Sale sale) throws SQLException {
        String insertSale = "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)";
        String insertItem = "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES (?, ?, ?, ?)";
        String decrementStock = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? WHERE medicine_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            // Turn off auto-commit so none of the statements below take effect until we
            // explicitly commit() at the very end.
            conn.setAutoCommit(false);
            try {
                int saleId;
                // Statement.RETURN_GENERATED_KEYS lets us read back the AUTO_INCREMENT
                // sale_id MySQL assigns, so the sale_items rows below can reference it.
                try (PreparedStatement stmt = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setBigDecimal(1, sale.getTotalAmount());
                    stmt.setInt(2, sale.getUserId());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        keys.next();
                        saleId = keys.getInt(1);
                    }
                }

                // sale_date has no value to bind above — it's populated server-side by the
                // column's DEFAULT CURRENT_TIMESTAMP — so it has to be read back with its own
                // query rather than via getGeneratedKeys(), which only ever returns the
                // AUTO_INCREMENT column. Still inside the same transaction/connection, so
                // this reads back exactly the row that was just inserted.
                try (PreparedStatement stmt = conn.prepareStatement("SELECT sale_date FROM sales WHERE sale_id = ?")) {
                    stmt.setInt(1, saleId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        Timestamp saleDate = rs.getTimestamp("sale_date");
                        sale.setSaleDate(saleDate.toLocalDateTime());
                    }
                }

                // Batch every line item's INSERT and every stock decrement together so the
                // whole cart is written in two round-trips to the DB rather than one per
                // item — a minor efficiency win, but more importantly it keeps the "insert
                // items" and "decrement stock" steps clearly grouped and easy to read.
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
                // Everything above succeeded — make it permanent.
                conn.commit();
            } catch (SQLException e) {
                // Something failed partway through: undo every statement issued since
                // setAutoCommit(false), so the sale header, line items, and stock counts
                // are all left exactly as they were before checkout() was called.
                conn.rollback();
                throw e;
            } finally {
                // Restore the connection's default auto-commit behavior before it's closed
                // (harmless here since the connection isn't reused, but it's the correct
                // hygiene if this method's connection-handling ever changes in the future).
                conn.setAutoCommit(true);
            }
        }
    }
}
