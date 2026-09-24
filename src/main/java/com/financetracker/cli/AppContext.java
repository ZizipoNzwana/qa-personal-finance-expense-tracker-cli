package com.financetracker.cli;

import com.financetracker.db.DatabaseManager;
import com.financetracker.db.TransactionRepository;
import com.financetracker.service.BudgetService;
import com.financetracker.service.ExchangeRateService;
import com.financetracker.service.TransactionService;

/**
 * Wires together the shared services (DB, exchange rate, budget, transaction)
 * that every CLI command needs, and holds the user's configured base currency.
 * A single instance is created at startup and handed to each command.
 */
public class AppContext {

    private final DatabaseManager databaseManager;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;
    private final BudgetService budgetService;
    private final TransactionService transactionService;
    private String baseCurrency;

    public AppContext(String baseCurrency) {this(new DatabaseManager(), new ExchangeRateService(), baseCurrency);
    }

    public AppContext(DatabaseManager databaseManager, ExchangeRateService exchangeRateService, String baseCurrency) {
        this.databaseManager = databaseManager;
        this.databaseManager.initSchema();
        this.transactionRepository = new TransactionRepository(databaseManager);
        this.exchangeRateService = exchangeRateService;
        this.budgetService = new BudgetService(transactionRepository);
        this.baseCurrency = baseCurrency.toUpperCase();
        this.transactionService = new TransactionService(transactionRepository, exchangeRateService, this.baseCurrency);
    }

    public TransactionRepository transactionRepository() {
        return transactionRepository;
    }

    public ExchangeRateService exchangeRateService() {
        return exchangeRateService;
    }

    public BudgetService budgetService() {
        return budgetService;
    }

    public TransactionService transactionService() {
        return transactionService;
    }

    public String baseCurrency() {
        return baseCurrency;
    }

    public void close() {
        databaseManager.close();
    }
}
