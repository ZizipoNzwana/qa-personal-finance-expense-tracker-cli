package com.financetracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection to the embedded H2 database and creates the schema
 * (accounts, categories, transactions) on first use.
 *
 * H2 runs in embedded "file" mode so data survives between CLI invocations,
 * e.g. jdbc:h2:file:./data/financetracker
 */
public final class DatabaseManager {

    private static final String DEFAULT_DB_PATH = "./data/financetracker";

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private Connection connection;

    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }

    public DatabaseManager(String dbPath) {
        boolean inMemory = dbPath.startsWith("mem:");
        this.jdbcUrl = "jdbc:h2:" + dbPath
                + (inMemory ? "" : ";AUTO_SERVER=TRUE")
                + ";DB_CLOSE_DELAY=-1";
        this.user = "sa";
        this.password = "";
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(jdbcUrl, user, password);
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to H2 database at " + jdbcUrl, e);
        }
    }

    public void initSchema() {
        String accounts = """
                CREATE TABLE IF NOT EXISTS accounts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    base_currency VARCHAR(3) NOT NULL
                )
                """;
        String categories = """
                CREATE TABLE IF NOT EXISTS categories (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    type VARCHAR(10) NOT NULL
                )
                """;
        String transactions = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(10) NOT NULL,
                    original_amount DECIMAL(19,2) NOT NULL,
                    original_currency VARCHAR(3) NOT NULL,
                    converted_amount DECIMAL(19,2) NOT NULL,
                    base_currency VARCHAR(3) NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    description VARCHAR(500),
                    txn_date DATE NOT NULL
                )
                """;

        try (Statement statement = getConnection().createStatement()) {
            statement.execute(accounts);
            statement.execute(categories);
            statement.execute(transactions);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
        }
    }
}