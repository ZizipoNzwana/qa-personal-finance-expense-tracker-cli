package com.financetracker.cli;

import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "filter",
        description = "Filter transactions by type, category, and/or date range.",
        mixinStandardHelpOptions = true
)
public class FilterCommand implements Callable<Integer> {

    private final AppContext context;

    @Option(names = {"-t", "--type"}, description = "INCOME or EXPENSE")
    private String type;

    @Option(names = {"-c", "--category"}, description = "Category name")
    private String category;

    @Option(names = {"--from"}, description = "Start date (yyyy-MM-dd), inclusive")
    private String from;

    @Option(names = {"--to"}, description = "End date (yyyy-MM-dd), inclusive")
    private String to;

    public FilterCommand(AppContext context) {
        this.context = context;
    }

    @Override
    public Integer call() {
        try {
            TransactionType txnType = type == null ? null : TransactionType.fromString(type);
            LocalDate fromDate = parseDateOrThrow(from, "--from");
            LocalDate toDate = parseDateOrThrow(to, "--to");

            List<Transaction> results = context.transactionRepository()
                    .findByFilters(txnType, category, fromDate, toDate);

            if (results.isEmpty()) {
                System.out.println("No transactions match those filters.");
                return 0;
            }
            results.forEach(System.out::println);
            System.out.printf("%nMatched: %d transaction(s)%n", results.size());
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    /** Parses a yyyy-MM-dd date, turning a malformed value into a clear IllegalArgumentException
     *  instead of letting DateTimeParseException escape uncaught. */
    private static LocalDate parseDateOrThrow(String value, String optionName) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date '" + value + "' for " + optionName + ", expected yyyy-MM-dd.");
        }
    }
}