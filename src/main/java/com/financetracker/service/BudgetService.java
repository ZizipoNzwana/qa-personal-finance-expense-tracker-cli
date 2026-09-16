package com.financetracker.service;

import com.financetracker.db.TransactionRepository;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregation/reporting logic over stored transactions: totals, balance, and
 * per-category breakdowns. Kept independent of the CLI and DB wiring so it is
 * easy to unit test with a mocked {@link TransactionRepository}.
 */
public class BudgetService {

    private final TransactionRepository repository;

    public BudgetService(TransactionRepository repository) {
        this.repository = repository;
    }

    public BigDecimal totalIncome() {
        return sum(repository.findAll(), TransactionType.INCOME);
    }

    public BigDecimal totalExpenses() {
        return sum(repository.findAll(), TransactionType.EXPENSE);
    }

    public BigDecimal balance() {
        List<Transaction> all = repository.findAll();
        return all.stream()
                .map(Transaction::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> expensesByCategory() {
        return repository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getConvertedAmount, BigDecimal::add)));
    }

    public boolean isOverBudget(BigDecimal monthlyBudget, LocalDate month) {
        if (monthlyBudget == null) {
            throw new IllegalArgumentException("Monthly budget cannot be null");
        }
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = month.withDayOfMonth(month.lengthOfMonth());

        BigDecimal spent = repository.findByFilters(TransactionType.EXPENSE, null, start, end)
                .stream()
                .map(Transaction::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return spent.compareTo(monthlyBudget) > 0;
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
