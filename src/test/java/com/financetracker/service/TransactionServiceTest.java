package com.financetracker.service;

import com.financetracker.db.TransactionRepository;
import com.financetracker.exception.ExchangeRateException;
import com.financetracker.exception.ValidationException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService: input validation and currency conversion orchestration")
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private ExchangeRateService exchangeRateService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(repository, exchangeRateService, "USD");
    }

    @Test
    @DisplayName("rejects a null amount")
    void rejectsNullAmount() {
        assertThrows(ValidationException.class, () ->
                transactionService.recordTransaction(TransactionType.EXPENSE, null, "USD",
                        "food", "lunch", LocalDate.now()));
    }

    @Test
    @DisplayName("rejects a zero or negative amount")
    void rejectsNonPositiveAmount() {
        assertThrows(ValidationException.class, () ->
                transactionService.recordTransaction(TransactionType.EXPENSE, BigDecimal.ZERO, "USD",
                        "food", "lunch", LocalDate.now()));
        assertThrows(ValidationException.class, () ->
                transactionService.recordTransaction(TransactionType.EXPENSE, new BigDecimal("-5"), "USD",
                        "food", "lunch", LocalDate.now()));
    }

    @Test
    @DisplayName("rejects a blank category")
    void rejectsBlankCategory() {
        assertThrows(ValidationException.class, () ->
                transactionService.recordTransaction(TransactionType.EXPENSE, new BigDecimal("10"), "USD",
                        "   ", "lunch", LocalDate.now()));
    }

    @Test
    @DisplayName("converts foreign-currency amounts before saving")
    void convertsForeignCurrencyBeforeSaving() {
        when(exchangeRateService.convert(new BigDecimal("100"), "EUR", "USD"))
                .thenReturn(new BigDecimal("108.50"));
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.recordTransaction(
                TransactionType.EXPENSE, new BigDecimal("100"), "EUR", "travel", "hotel", LocalDate.of(2026, 3, 1));

        assertEquals(new BigDecimal("108.50"), result.getConvertedAmount());
        assertEquals("USD", result.getBaseCurrency());
        assertEquals("EUR", result.getOriginalCurrency());
        verify(repository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("defaults currency to base currency when none is supplied")
    void defaultsToBaseCurrencyWhenCurrencyOmitted() {
        when(exchangeRateService.convert(new BigDecimal("50"), "USD", "USD"))
                .thenReturn(new BigDecimal("50.0000"));
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.recordTransaction(
                TransactionType.EXPENSE, new BigDecimal("50"), null, "food", null, null);

        assertEquals("USD", result.getOriginalCurrency());
        assertEquals(LocalDate.now(), result.getDate());
    }

    @Test
    @DisplayName("propagates ExchangeRateException when the rate service fails")
    void propagatesExchangeRateException() {
        when(exchangeRateService.convert(any(), any(), any()))
                .thenThrow(new ExchangeRateException("API down"));

        assertThrows(ExchangeRateException.class, () ->
                transactionService.recordTransaction(TransactionType.EXPENSE, new BigDecimal("10"), "XYZ",
                        "food", "lunch", LocalDate.now()));
    }
}
