package com.financetracker.exception;

/**
 * Thrown when the exchange rate API cannot supply a rate: the service is down,
 * the network call fails, or an unknown/invalid currency code is requested.
 */
public class ExchangeRateException extends RuntimeException {

    public ExchangeRateException(String message) {
        super(message);
    }

    public ExchangeRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
