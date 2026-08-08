package com.zugobite.pims.services;

import com.zugobite.pims.data.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Supplies the "what's happening in my pharmacy right now" figures shown across the top of
 * the Admin and Cashier home screens (see {@link com.zugobite.pims.views.ModuleHomePanel}).
 *
 * <p>Distinct from {@link ReportService}, which answers "give me every row matching X" for
 * an on-screen table the user then reads, exports, or prints. This class answers "give me
 * one number" — the aggregates are computed by MySQL ({@code SUM}/{@code COUNT}) and come
 * back as scalars, so opening the home screen costs a handful of cheap aggregate queries
 * rather than pulling whole result sets across the wire just to count them in Java.</p>
 *
 * <p>All "today" and "expiring soon" windows are evaluated with the database's own
 * {@code CURDATE()} rather than a date passed in from Java, so the figures stay correct
 * against the DB server's clock even if a till's local clock has drifted.</p>
 */
public class DashboardService {

    /**
     * One atomic read of every headline figure. A record (rather than separate getters that
     * each run their own query) so a single {@link #loadSnapshot()} call gives the home
     * screen an internally-consistent set of numbers to render in one pass.
     *
     * @param todayRevenue      total value of sales rung up today
     * @param todayTransactions how many separate sales were completed today
     * @param lowStockCount     medicines at or below their reorder level
     * @param expiringCount     medicines expiring within the next month
     * @param medicineCount     total distinct medicines on the catalogue
     * @param inventoryValue    retail value of everything currently on the shelf
     */
    public record DashboardSnapshot(
            BigDecimal todayRevenue,
            int todayTransactions,
            int lowStockCount,
            int expiringCount,
            int medicineCount,
            BigDecimal inventoryValue) {
    }

    /**
     * Runs every headline aggregate over a single shared {@link Connection}, rather than one
     * connection per figure — six sequential {@code DriverManager.getConnection()} round
     * trips would visibly delay the home screen on a slow network for no benefit.
     *
     * @throws SQLException if the database is unreachable; the caller
     *         ({@link com.zugobite.pims.views.ModuleHomePanel}) degrades gracefully by hiding
     *         the stat row rather than blocking access to the modules themselves
     */
    public DashboardSnapshot loadSnapshot() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new DashboardSnapshot(
                    queryDecimal(conn, "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE DATE(sale_date) = CURDATE()"),
                    queryInt(conn, "SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = CURDATE()"),
                    queryInt(conn, "SELECT COUNT(*) FROM medicines WHERE quantity_in_stock <= reorder_level"),
                    queryInt(conn, "SELECT COUNT(*) FROM medicines "
                            + "WHERE expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 MONTH)"),
                    queryInt(conn, "SELECT COUNT(*) FROM medicines"),
                    queryDecimal(conn, "SELECT COALESCE(SUM(price * quantity_in_stock), 0) FROM medicines")
            );
        }
    }

    /** Runs a single-row, single-column {@code COUNT(...)} query and returns the scalar. */
    private int queryInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Runs a single-row, single-column {@code SUM(...)} query and returns the scalar. Every
     * such query in this class wraps its SUM in {@code COALESCE(..., 0)}, so an empty table
     * (a pharmacy's very first day, before any sale) yields zero rather than SQL NULL — which
     * is why this can safely return a non-null {@link BigDecimal} without a null guard here.
     */
    private BigDecimal queryDecimal(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }
}
