package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single income or expense record.
 *
 * Amounts are always persisted in the user's base currency ({@code convertedAmount}),
 * while the original amount/currency entered by the user is retained for auditing.
 */
public class Transaction {

    private Long id;
    private TransactionType type;
    private BigDecimal originalAmount;
    private String originalCurrency;
    private BigDecimal convertedAmount;
    private String baseCurrency;
    private String category;
    private String description;
    private LocalDate date;

    public Transaction() {
    }

    public Transaction(TransactionType type, BigDecimal originalAmount, String originalCurrency,
                        BigDecimal convertedAmount, String baseCurrency, String category,
                        String description, LocalDate date) {
        this.type = type;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
        this.convertedAmount = convertedAmount;
        this.baseCurrency = baseCurrency;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Signed amount in base currency: positive for income, negative for expense.
     * Useful for balance/summary calculations.
     */
    public BigDecimal signedAmount() {
        if (convertedAmount == null) {
            return BigDecimal.ZERO;
        }
        return type == TransactionType.EXPENSE ? convertedAmount.negate() : convertedAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("#%-4s %-7s %10s %s  [%s]  %-15s %s",
                id == null ? "-" : id,
                type,
                convertedAmount + " " + baseCurrency,
                date,
                originalCurrency.equals(baseCurrency)
                        ? "no conversion"
                        : originalAmount + " " + originalCurrency + " -> converted",
                category,
                description == null ? "" : description);
    }
}
