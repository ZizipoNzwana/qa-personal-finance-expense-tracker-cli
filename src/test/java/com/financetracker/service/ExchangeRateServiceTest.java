package com.financetracker.service;

import com.financetracker.exception.ExchangeRateException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API-level tests for {@link ExchangeRateService}, exercising both the happy path
 * and the edge cases called out in the requirements: API downtime and unknown
 * currency codes. The external Frankfurter API is mocked with WireMock so tests
 * are fast, deterministic, and don't depend on network access.
 *
 * Stubs use Frankfurter's current v1 query shape (base/symbols, raw per-unit
 * rate returned) rather than the retired amount/from/to shape.
 */
@DisplayName("ExchangeRateService: live currency conversion via mocked exchange rate API")
class ExchangeRateServiceTest {

    private WireMockServer wireMockServer;
    private String baseUrl;
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        baseUrl = "http://localhost:" + wireMockServer.port();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = wireMockServer.port();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        exchangeRateService = new ExchangeRateService(baseUrl, httpClient);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        RestAssured.reset();
    }

    @Test
    @DisplayName("converts using the rate returned by the API")
    void convert_happyPath() {
        wireMockServer.stubFor(get(urlEqualTo("/latest?base=EUR&symbols=USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"amount\":1.0,\"base\":\"EUR\",\"date\":\"2026-01-01\",\"rates\":{\"USD\":1.0850}}")));

        BigDecimal result = exchangeRateService.convert(new BigDecimal("100"), "EUR", "USD");

        // 100 * 1.0850 = 108.50, multiplied locally since the API only returns the raw rate
        assertEquals(new BigDecimal("108.50"), result);
    }

    @Test
    @DisplayName("REST Assured confirms the WireMock stub itself returns the expected rate payload")
    void wireMockStub_respondsAsExpected_verifiedWithRestAssured() {
        // Registered directly on wireMockServer (the same instance RestAssured.port
        // points at above) rather than the static WireMock.stubFor(...) DSL, which
        // targets a separate, globally-configured "default" admin client and would
        // silently register the stub against the wrong server here.
        wireMockServer.stubFor(get(urlEqualTo("/latest?base=GBP&symbols=USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"amount\":1.0,\"base\":\"GBP\",\"date\":\"2026-01-01\",\"rates\":{\"USD\":1.2650}}")));

        given()
                .when()
                .get("/latest?base=GBP&symbols=USD")
                .then()
                .statusCode(200)
                .body("base", equalTo("GBP"))
                .body("rates.USD", equalTo(1.265f));
    }

    @Test
    @DisplayName("returns the same amount without a network call when currencies match")
    void convert_sameCurrency_noNetworkCall() {
        BigDecimal result = exchangeRateService.convert(new BigDecimal("42.5"), "usd", "USD");
        assertEquals(new BigDecimal("42.50"), result);
    }

    @Test
    @DisplayName("edge case: unknown currency code surfaces as ExchangeRateException")
    void convert_unknownCurrency_throwsExchangeRateException() {
        wireMockServer.stubFor(get(urlEqualTo("/latest?base=USD&symbols=ZZZ"))
                .willReturn(aResponse().withStatus(404).withBody("{\"error\":\"Unknown currency\"}")));

        ExchangeRateException ex = assertThrows(ExchangeRateException.class, () ->
                exchangeRateService.convert(new BigDecimal("10"), "USD", "ZZZ"));

        assertTrue(ex.getMessage().toLowerCase().contains("unknown"));
    }

    @Test
    @DisplayName("edge case: API returns a 5xx error")
    void convert_serverError_throwsExchangeRateException() {
        wireMockServer.stubFor(get(urlEqualTo("/latest?base=USD&symbols=EUR"))
                .willReturn(aResponse().withStatus(503)));

        ExchangeRateException ex = assertThrows(ExchangeRateException.class, () ->
                exchangeRateService.convert(new BigDecimal("10"), "USD", "EUR"));

        assertTrue(ex.getMessage().contains("503"));
    }

    @Test
    @DisplayName("edge case: API downtime (connection refused) surfaces as ExchangeRateException")
    void convert_apiDown_throwsExchangeRateException() {
        wireMockServer.stop();

        assertThrows(ExchangeRateException.class, () ->
                exchangeRateService.convert(new BigDecimal("10"), "USD", "EUR"));
    }

    @Test
    @DisplayName("edge case: malformed JSON response surfaces as ExchangeRateException")
    void convert_malformedResponse_throwsExchangeRateException() {
        wireMockServer.stubFor(get(urlEqualTo("/latest?base=USD&symbols=EUR"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json")));

        assertThrows(ExchangeRateException.class, () ->
                exchangeRateService.convert(new BigDecimal("10"), "USD", "EUR"));
    }
}