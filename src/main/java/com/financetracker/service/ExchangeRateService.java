package com.financetracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.exception.ExchangeRateException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletionException;

/**
 * Talks to the free Frankfurter exchange-rate API (https://frankfurter.dev)
 * to convert a foreign-currency amount into the user's base currency.
 *
 * As of the v1/v2 migration, Frankfurter no longer accepts amount/from/to
 * together and does not multiply server-side — it returns a raw per-unit
 * rate via base/symbols, and the caller is expected to multiply locally.
 *
 * The base URL is injectable so tests can point this at a WireMock stub instead
 * of the real service.
 */
public class ExchangeRateService {

    private static final String DEFAULT_BASE_URL = "https://api.frankfurter.dev/v1";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeRateService() {
        this(DEFAULT_BASE_URL, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public ExchangeRateService(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
    }

    /**
     * Converts {@code amount} from {@code fromCurrency} into {@code toCurrency}.
     * If the currencies are identical, returns the amount unchanged without a network call.
     *
     * @throws ExchangeRateException if the API is unreachable, times out, returns an
     *                                unexpected status, or the currency code is unknown.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);

        // Same currency shortcut: enforce 2 decimals
        if (from.equals(to)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        String url = String.format("%s/latest?base=%s&symbols=%s", baseUrl, from, to);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ExchangeRateException(
                    "Exchange rate service is unreachable (network error). Please try again later.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateException("Exchange rate request was interrupted.", e);
        } catch (CompletionException e) {
            throw new ExchangeRateException("Exchange rate request failed unexpectedly.", e);
        }

        if (response.statusCode() == 404) {
            throw new ExchangeRateException(
                    "Unknown or unsupported currency code: '" + from + "' or '" + to + "'.");
        }
        if (response.statusCode() != 200) {
            throw new ExchangeRateException(
                    "Exchange rate service returned an error (HTTP " + response.statusCode() + ").");
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rates = root.get("rates");
            if (rates == null || !rates.has(to)) {
                throw new ExchangeRateException("Exchange rate response did not include currency: " + to);
            }
            // The API now returns a raw per-unit rate (not a pre-multiplied amount),
            // so the conversion itself happens here, not server-side.
            BigDecimal rate = rates.get(to).decimalValue();
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        } catch (IOException e) {
            throw new ExchangeRateException("Could not parse exchange rate response.", e);
        }
    }

    private String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be blank");
        }
        return currency.trim().toUpperCase();
    }
}