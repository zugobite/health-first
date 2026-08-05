package com.zugobite.pims.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single, centralised place that knows how to open a JDBC connection to the `pims_db`
 * MySQL database. Every service class (see {@code services} package) calls
 * {@link #getConnection()} rather than embedding its own connection URL/credentials, so
 * there is exactly one place to update if the DB moves, or if credentials need to change
 * for a different environment.
 *
 * <p><b>Maintainer note:</b> {@code URL}/{@code USER}/{@code PASSWORD} are hardcoded here for
 * assignment simplicity (PLAN.md targets a local MySQL instance for grading). In a real
 * deployment these would come from an external config file or environment variables instead
 * of being committed to source — see the {@code db.properties} / {@code *.env} entries
 * already reserved in {@code .gitignore} for that future step.</p>
 *
 * <p>No connection pooling is used: each call opens a fresh {@link Connection}, and every
 * caller is expected to use it in a try-with-resources block so it's closed immediately
 * after use. That's appropriate for a single-user desktop app talking to a local DB, but
 * would need revisiting (e.g. HikariCP) under real concurrent load.</p>
 */
public final class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/pims_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Utility class: never instantiated, only used via the static getConnection() method.
    private DBConnection() {
    }

    /**
     * Opens a brand-new connection to {@code pims_db}. Callers are responsible for closing
     * it — always via try-with-resources, e.g.:
     * <pre>{@code
     * try (Connection conn = DBConnection.getConnection();
     *      PreparedStatement stmt = conn.prepareStatement(sql)) {
     *     ...
     * }
     * }</pre>
     *
     * @throws SQLException if the JDBC driver can't reach MySQL (wrong host/port, DB not
     *                       running, wrong credentials, database not yet created via
     *                       {@code database.sql}, etc.) — deliberately not caught here so
     *                       callers can surface a meaningful error to the user instead of
     *                       this class swallowing it.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
