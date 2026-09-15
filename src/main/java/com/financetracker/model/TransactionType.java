package com.financetracker.model;

/**
 * Whether a transaction represents money coming in or going out.
 */
public enum TransactionType {
    INCOME,
    EXPENSE;

    public static TransactionType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        try {
            return TransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid transaction type '" + value + "'. Expected INCOME or EXPENSE.");
        }
    }
}
