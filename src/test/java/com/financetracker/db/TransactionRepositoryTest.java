package com.financetracker.db;

import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests that exercise {@link TransactionRepository} against a real
 * (in-memory) H2 database rather than a mock, verifying schema creation, SQL,
 * and JDBC mapping actually work end-to-end.
 */
@DisplayName("TransactionRepository: H2 integration tests")
class TransactionRepositoryTest {

    private DatabaseManager databaseManager;
    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        // Unique in-memory DB per test run so tests don't interfere with each other.
        String dbName = "mem:testdb-" + UUID.randomUUID();
        databaseManager = new DatabaseManager(dbName);
        databaseManager.initSchema();
        repository = new TransactionRepository(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    private Transaction sampleExpense(String category, LocalDate date) {
        return new Transaction(TransactionType.EXPENSE, new BigDecimal("25.00"), "USD",
                new BigDecimal("25.00"), "USD", category, "test expense", date);
    }

    private Transaction sampleIncome(LocalDate date) {
        return new Transaction(TransactionType.INCOME, new BigDecimal("1000.00"), "USD",
                new BigDecimal("1000.00"), "USD", "salary", "monthly pay", date);
    }

    @Test
    @DisplayName("save assigns a generated id and persists the row")
    void save_assignsIdAndPersists() {
        Transaction saved = repository.save(sampleExpense("food", LocalDate.of(2026, 1, 15)));

        assertTrue(saved.getId() > 0);

        Optional<Transaction> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("food", found.get().getCategory());
        assertEquals(new BigDecimal("25.00"), found.get().getConvertedAmount());
    }

    @Test
    @DisplayName("findAll returns saved transactions ordered by date descending")
    void findAll_returnsAllInDateDescendingOrder() {
        repository.save(sampleExpense("food", LocalDate.of(2026, 1, 1)));
        repository.save(sampleExpense("rent", LocalDate.of(2026, 3, 1)));
        repository.save(sampleExpense("travel", LocalDate.of(2026, 2, 1)));

        List<Transaction> all = repository.findAll();

        assertEquals(3, all.size());
        assertEquals("rent", all.get(0).getCategory());
        assertEquals("travel", all.get(1).getCategory());
        assertEquals("food", all.get(2).getCategory());
    }

    @Test
    @DisplayName("findByFilters filters by type, category, and date range")
    void findByFilters_appliesAllFilters() {
        repository.save(sampleExpense("food", LocalDate.of(2026, 1, 10)));
        repository.save(sampleExpense("food", LocalDate.of(2026, 5, 10)));
        repository.save(sampleExpense("rent", LocalDate.of(2026, 1, 20)));
        repository.save(sampleIncome(LocalDate.of(2026, 1, 15)));

        List<Transaction> results = repository.findByFilters(
                TransactionType.EXPENSE, "food", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(1, results.size());
        assertEquals("food", results.get(0).getCategory());
        assertEquals(LocalDate.of(2026, 1, 10), results.get(0).getDate());
    }

    @Test
    @DisplayName("findByFilters with no filters returns everything")
    void findByFilters_noFilters_returnsAll() {
        repository.save(sampleExpense("food", LocalDate.now()));
        repository.save(sampleIncome(LocalDate.now()));

        assertEquals(2, repository.findByFilters(null, null, null, null).size());
    }

    @Test
    @DisplayName("deleteById removes the row and returns true; false when not found")
    void deleteById_removesRow() {
        Transaction saved = repository.save(sampleExpense("food", LocalDate.now()));

        assertTrue(repository.deleteById(saved.getId()));
        assertTrue(repository.findById(saved.getId()).isEmpty());
        assertFalse(repository.deleteById(saved.getId()));
    }

    @Test
    @DisplayName("deleteAll clears every transaction")
    void deleteAll_clearsTable() {
        repository.save(sampleExpense("food", LocalDate.now()));
        repository.save(sampleIncome(LocalDate.now()));

        repository.deleteAll();

        assertTrue(repository.findAll().isEmpty());
    }
}
