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

    public Optional<Transaction> findById(long id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch transaction " + id, e);
        }
    }

    public List<Transaction> findByFilters(TransactionType type, String category,
                                            LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(category) = LOWER(?)");
            params.add(category);
        }
        if (from != null) {
            sql.append(" AND txn_date >= ?");
            params.add(java.sql.Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND txn_date <= ?");
            params.add(java.sql.Date.valueOf(to));
        }
        sql.append(" ORDER BY txn_date DESC, id DESC");

        try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to filter transactions", e);
        }
    }

    public boolean deleteById(long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete transaction " + id, e);
        }
    }

    /** Wipes all rows without dropping the table. */
    public void deleteAll() {
        try (Statement statement = databaseManager.getConnection().createStatement()) {
            statement.execute("DELETE FROM transactions");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear transactions", e);
        }
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction txn = new Transaction();
        txn.setId(rs.getLong("id"));
        txn.setType(TransactionType.valueOf(rs.getString("type")));
        txn.setOriginalAmount(rs.getBigDecimal("original_amount"));
        txn.setOriginalCurrency(rs.getString("original_currency"));
        txn.setConvertedAmount(rs.getBigDecimal("converted_amount"));
        txn.setBaseCurrency(rs.getString("base_currency"));
        txn.setCategory(rs.getString("category"));
        txn.setDescription(rs.getString("description"));
        txn.setDate(rs.getDate("txn_date").toLocalDate());
        return txn;
    }
}
