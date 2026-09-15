package com.financetracker.exception;

/**
 * Thrown when user-supplied input fails validation (e.g. negative amount,
 * blank category, malformed date).
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
