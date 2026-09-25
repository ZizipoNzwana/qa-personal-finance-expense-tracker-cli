package com.financetracker;

import com.financetracker.cli.AppContext;
import com.financetracker.cli.CliFactory;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry point.
 *
 * Two modes of operation:
 *  - Batch mode: {@code java -jar finance-tracker-cli.jar add-expense --amount 50 --category food}
 *    executes one command and exits (handy for scripting/CI).
 *  - Interactive mode: run with no arguments to enter a REPL where commands can
 *    be typed one after another, e.g.:
 *      finance-tracker> add-expense --amount 50 --category food
 *      finance-tracker> list
 *      finance-tracker> exit
 */
public class Main {

    private static final String BASE_CURRENCY_ENV = "FINANCE_TRACKER_BASE_CURRENCY";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");

    public static void main(String[] args) {
        String baseCurrency = System.getenv().getOrDefault(BASE_CURRENCY_ENV, "ZAR");
        AppContext context = new AppContext(baseCurrency);

        try {
            if (args.length > 0) {
                int exitCode = CliFactory.build(context).execute(args);
                System.exit(exitCode);
            } else {
                runInteractive(context, baseCurrency);
            }
        } finally {
            context.close();
        }
    }

    private static void runInteractive(AppContext context, String baseCurrency) {
        System.out.println("Personal Finance & Expense Tracker CLI (interactive mode)");
        System.out.println("Base currency: " + baseCurrency
                + "  (set " + BASE_CURRENCY_ENV + " env var to change)");
        System.out.println("Type a command (add-expense, add-income, list, filter, convert, summary),");
        System.out.println("'help' for details on a command, or 'exit'/'quit' to leave.\n");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        CommandLine cli = CliFactory.build(context);

        while (true) {
            System.out.print("finance-tracker> ");
            System.out.flush();
            String line;
            try {
                line = reader.readLine();
            } catch (Exception e) {
                break;
            }
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }
            if (line.equalsIgnoreCase("help")) {
                cli.usage(System.out);
                continue;
            }

            String[] tokens = tokenize(line);
            try {
                cli.execute(tokens);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    static String[] tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(line);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                tokens.add(matcher.group(2));
            } else {
                tokens.add(matcher.group(3));
            }
        }
        return tokens.toArray(new String[0]);
    }
}