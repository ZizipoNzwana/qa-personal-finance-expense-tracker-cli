package com.financetracker.service;

import com.financetracker.db.TransactionRepository;
import com.financetracker.exception.ValidationException;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validates and records a new income/expense entry: converts the entered amount
 * into the base currency (via {@link ExchangeRateService}) before saving it
 * through the {@link TransactionRepository}.
 */
public class TransactionService {

    private final TransactionRepository repository;
    private final ExchangeRateService exchangeRateService;
    private final String baseCurrency;

    public TransactionService(TransactionRepository repository,
                               ExchangeRateService exchangeRateService,
                               String baseCurrency) {
        this.repository = repository;
        this.exchangeRateService = exchangeRateService;
        this.baseCurrency = baseCurrency.toUpperCase();
    }

    public Transaction recordTransaction(TransactionType type, BigDecimal amount, String currency,
                                          String category, String description, LocalDate date) {
        validate(amount, category);

        String txnCurrency = (currency == null || currency.isBlank()) ? baseCurrency : currency.toUpperCase();
        LocalDate txnDate = date == null ? LocalDate.now() : date;

        BigDecimal convertedAmount = exchangeRateService.convert(amount, txnCurrency, baseCurrency);

        Transaction txn = new Transaction(
                type, amount, txnCurrency, convertedAmount, baseCurrency,
                category.trim(), description, txnDate);

        return repository.save(txn);
    }
    private void validate(BigDecimal amount, String category) {
        if (amount == null) {
            throw new ValidationException("Amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (category == null || category.isBlank()) {
            throw new ValidationException("Category is required.");
        }
    }
}
