package com.financetracker.db;

import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed CRUD/query operations for {@link Transaction} records stored in H2.
 */
public class TransactionRepository {

    private final DatabaseManager databaseManager;

    public TransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Transaction save(Transaction txn) {
        String sql = """
                INSERT INTO transactions
                    (type, original_amount, original_currency, converted_amount,
                     base_currency, category, description, txn_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = databaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, txn.getType().name());
            ps.setBigDecimal(2, txn.getOriginalAmount());
            ps.setString(3, txn.getOriginalCurrency());
            ps.setBigDecimal(4, txn.getConvertedAmount());
            ps.setString(5, txn.getBaseCurrency());
            ps.setString(6, txn.getCategory());
            ps.setString(7, txn.getDescription());
            ps.setDate(8, java.sql.Date.valueOf(txn.getDate()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    txn.setId(keys.getLong(1));
                }
            }
            return txn;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save transaction", e);
        }
    }

    public List<Transaction> findAll() {
        String sql = "SELECT * FROM transactions ORDER BY txn_date DESC, id DESC";
        List<Transaction> results = new ArrayList<>();
        try (Statement statement = databaseManager.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                results.add(map(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch transactions", e);
        }
    }
}
