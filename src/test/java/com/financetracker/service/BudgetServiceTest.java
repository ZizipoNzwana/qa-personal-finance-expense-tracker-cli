package com.financetracker.service;

import com.financetracker.db.TransactionRepository;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService: budget calculation logic")
class BudgetServiceTest {

    @Mock
    private TransactionRepository repository;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(repository);
    }

    private Transaction txn(TransactionType type, String amount, String category, LocalDate date) {
        return new Transaction(type, new BigDecimal(amount), "USDZ",
                new BigDecimal(amount), "USD", category, "note", date);
    }

    @Test
    @DisplayName("totalIncome sums only INCOME transactions")
    void totalIncome_sumsOnlyIncome() {
        when(repository.findAll()).thenReturn(List.of(
                txn(TransactionType.INCOME, "1000.00", "salary", LocalDate.now()),
                txn(TransactionType.INCOME, "250.50", "freelance", LocalDate.now()),
                txn(TransactionType.EXPENSE, "60.00", "food", LocalDate.now())
        ));

        assertEquals(new BigDecimal("1250.50"), budgetService.totalIncome());
    }

    @Test
    @DisplayName("totalExpenses sums only EXPENSE transactions")
    void totalExpenses_sumsOnlyExpenses() {
        when(repository.findAll()).thenReturn(List.of(
                txn(TransactionType.EXPENSE, "60.00", "food", LocalDate.now()),
                txn(TransactionType.EXPENSE, "40.25", "transport", LocalDate.now()),
                txn(TransactionType.INCOME, "1000.00", "salary", LocalDate.now())
        ));

        assertEquals(new BigDecimal("100.25"), budgetService.totalExpenses());
    }

    @Test
    @DisplayName("balance is income minus expenses")
    void balance_isIncomeMinusExpenses() {
        when(repository.findAll()).thenReturn(List.of(
                txn(TransactionType.INCOME, "2000.00", "salary", LocalDate.now()),
                txn(TransactionType.EXPENSE, "500.00", "rent", LocalDate.now()),
                txn(TransactionType.EXPENSE, "150.00", "food", LocalDate.now())
        ));

        assertEquals(new BigDecimal("1350.00"), budgetService.balance());
    }

    @Test
    @DisplayName("balance with no transactions is zero")
    void balance_noTransactions_isZero() {
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(new BigDecimal("0.00"), budgetService.balance());
    }

    @Test
    @DisplayName("expensesByCategory groups and sums expenses per category")
    void expensesByCategory_groupsCorrectly() {
        when(repository.findAll()).thenReturn(List.of(
                txn(TransactionType.EXPENSE, "30.00", "food", LocalDate.now()),
                txn(TransactionType.EXPENSE, "20.00", "food", LocalDate.now()),
                txn(TransactionType.EXPENSE, "100.00", "rent", LocalDate.now()),
                txn(TransactionType.INCOME, "1000.00", "salary", LocalDate.now())
        ));

        var breakdown = budgetService.expensesByCategory();

        assertEquals(new BigDecimal("50.00"), breakdown.get("food"));
        assertEquals(new BigDecimal("100.00"), breakdown.get("rent"));
        assertFalse(breakdown.containsKey("salary"));
    }

    @Test
    @DisplayName("isOverBudget returns true when spending in the month exceeds the budget")
    void isOverBudget_true_whenSpendingExceedsBudget() {
        LocalDate month = LocalDate.of(2026, 6, 15);
        when(repository.findByFilters(TransactionType.EXPENSE, null,
                month.withDayOfMonth(1), month.withDayOfMonth(month.lengthOfMonth())))
                .thenReturn(List.of(
                        txn(TransactionType.EXPENSE, "300.00", "food", month),
                        txn(TransactionType.EXPENSE, "250.00", "rent", month)
                ));

        assertTrue(budgetService.isOverBudget(new BigDecimal("500.00"), month));
    }

    @Test
    @DisplayName("isOverBudget returns false when spending is within budget")
    void isOverBudget_false_whenWithinBudget() {
        LocalDate month = LocalDate.of(2026, 6, 15);
        when(repository.findByFilters(TransactionType.EXPENSE, null,
                month.withDayOfMonth(1), month.withDayOfMonth(month.lengthOfMonth())))
                .thenReturn(List.of(
                        txn(TransactionType.EXPENSE, "100.00", "food", month)
                ));

        assertFalse(budgetService.isOverBudget(new BigDecimal("500.00"), month));
    }

    @Test
    @DisplayName("isOverBudget rejects a null budget")
    void isOverBudget_nullBudget_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> budgetService.isOverBudget(null, LocalDate.now()));
    }
}
