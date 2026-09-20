package com.financetracker.cli;

import picocli.CommandLine.Command;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
        name = "summary",
        description = "Show total income, total expenses, balance, and spending by category.",
        mixinStandardHelpOptions = true
)
public class SummaryCommand implements Callable<Integer> {

    private final AppContext context;

    public SummaryCommand(AppContext context) {
        this.context = context;
    }

    @Override
    public Integer call() {
        BigDecimal income = context.budgetService().totalIncome();
        BigDecimal expenses = context.budgetService().totalExpenses();
        BigDecimal balance = context.budgetService().balance();

        System.out.println("===== Budget Summary (" + context.baseCurrency() + ") =====");
        System.out.printf("Total income:   %12s%n", income);
        System.out.printf("Total expenses: %12s%n", expenses);
        System.out.printf("Balance:        %12s%n", balance);

        Map<String, BigDecimal> byCategory = context.budgetService().expensesByCategory();
        if (!byCategory.isEmpty()) {
            System.out.println("\nExpenses by category:");
            byCategory.forEach((cat, amt) -> System.out.printf("  %-20s %s%n", cat, amt));
        }
        return 0;
    }
}
