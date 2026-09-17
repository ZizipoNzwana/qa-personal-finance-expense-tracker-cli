package com.financetracker.cli;

import com.financetracker.exception.ExchangeRateException;
import com.financetracker.exception.ValidationException;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;

@Command(
        name = "add-expense",
        description = "Record a new expense, converting the amount into your base currency.",
        mixinStandardHelpOptions = true
)
public class AddExpenseCommand implements Callable<Integer> {

    private final AppContext context;

    @Option(names = {"-a", "--amount"}, required = true, description = "Expense amount (must be > 0)")
    private BigDecimal amount;

    @Option(names = {"-c", "--category"}, required = true, description = "Expense category, e.g. food, rent, transport")
    private String category;

    @Option(names = {"--currency"}, description = "Currency of the amount entered (default: your base currency)")
    private String currency;

    @Option(names = {"-d", "--description"}, description = "Optional free-text note")
    private String description;

    @Option(names = {"--date"}, description = "Transaction date (yyyy-MM-dd), default: today")
    private String date;

    public AddExpenseCommand(AppContext context) {
        this.context = context;
    }

    @Override
    public Integer call() {
        try {
            LocalDate txnDate;
            try {
                txnDate = date == null ? null : LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                System.err.println("Error: Invalid date '" + date + "', expected yyyy-MM-dd.");
                return 1;
            }
            Transaction saved = context.transactionService().recordTransaction(
                    TransactionType.EXPENSE, amount, currency, category, description, txnDate);
            System.out.println("Expense recorded: " + saved);
            return 0;
        } catch (ValidationException | ExchangeRateException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return 1;
        }
    }
}