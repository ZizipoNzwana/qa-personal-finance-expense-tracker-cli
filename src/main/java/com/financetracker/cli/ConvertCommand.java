package com.financetracker.cli;

import com.financetracker.exception.ExchangeRateException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

@Command(
        name = "convert",
        description = "Convert an amount between two currencies using live exchange rates.",
        mixinStandardHelpOptions = true
)
public class ConvertCommand implements Callable<Integer> {

    private final AppContext context;

    @Option(names = {"-a", "--amount"}, required = true, description = "Amount to convert")
    private BigDecimal amount;

    @Option(names = {"--from"}, required = true, description = "Source currency code, e.g.ZAR")
    private String from;

    @Option(names = {"--to"}, required = true, description = "Target currency code, e.g. EUR")
    private String to;

    public ConvertCommand(AppContext context) {
        this.context = context;
    }

    @Override
    public Integer call() {
        try {
            BigDecimal result = context.exchangeRateService().convert(amount, from, to);
            System.out.printf("%s %s = %s %s%n", amount, from.toUpperCase(), result, to.toUpperCase());
            return 0;
        } catch (ExchangeRateException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
