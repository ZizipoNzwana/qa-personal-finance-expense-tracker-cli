package com.financetracker.cli;

import com.financetracker.db.DatabaseManager;
import com.financetracker.service.ExchangeRateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional / end-to-end tests that drive the CLI exactly the way a user would
 * (tokenized command strings through the wired {@link CommandLine}), asserting
 * on exit codes and console output. Uses an in-memory H2 database and same-currency
 * amounts so no live network call to the exchange rate API is required.
 */
@DisplayName("CLI end-to-end tests")
class CliEndToEndTest {

    private AppContext context;
    private CommandLine cli;
    private ByteArrayOutputStream stdout;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream stderr;

    @BeforeEach
    void setUp() {
        DatabaseManager databaseManager = new DatabaseManager("mem:clitest-" + UUID.randomUUID());
        context = new AppContext(databaseManager, new ExchangeRateService(), "USD");
        cli = CliFactory.build(context);

        originalOut = System.out;
        originalErr = System.err;
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        System.setErr(new PrintStream(stderr));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        context.close();
    }

    @Test
    @DisplayName("add-expense then list shows the recorded transaction")
    void addExpense_thenList_showsTransaction() {
        int exitCode = cli.execute("add-expense", "--amount", "50", "--category", "food",
                "--description", "groceries");
        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("Expense recorded"));

        stdout.reset();
        exitCode = cli.execute("list");
        assertEquals(0, exitCode);
        String output = stdout.toString();
        assertTrue(output.contains("food"));
        assertTrue(output.contains("EXPENSE"));
        assertTrue(output.contains("Total: 1 transaction(s)"));
    }

    @Test
    @DisplayName("add-income then summary reflects balance")
    void addIncome_thenSummary_reflectsBalance() {
        assertEquals(0, cli.execute("add-income", "--amount", "1000", "--category", "salary"));
        assertEquals(0, cli.execute("add-expense", "--amount", "300", "--category", "rent"));

        stdout.reset();
        assertEquals(0, cli.execute("summary"));

        String output = stdout.toString();
        assertTrue(output.contains("1000.00"));
        assertTrue(output.contains("300.00"));
        assertTrue(output.contains("700.00"));
    }

    @Test
    @DisplayName("filter narrows results by category")
    void filter_narrowsByCategory() {
        cli.execute("add-expense", "--amount", "20", "--category", "food");
        cli.execute("add-expense", "--amount", "40", "--category", "transport");

        stdout.reset();
        int exitCode = cli.execute("filter", "--category", "food");

        assertEquals(0, exitCode);
        String output = stdout.toString();
        assertTrue(output.contains("food"));
        assertTrue(output.contains("Matched: 1 transaction(s)"));
    }

    @Test
    @DisplayName("input validation: negative amount is rejected with a non-zero exit code")
    void addExpense_negativeAmount_rejected() {
        int exitCode = cli.execute("add-expense", "--amount", "-10", "--category", "food");

        assertEquals(1, exitCode);
        assertTrue(stderr.toString().contains("greater than zero"));
    }

    @Test
    @DisplayName("input validation: missing required option is rejected")
    void addExpense_missingCategory_rejected() {
        int exitCode = cli.execute("add-expense", "--amount", "10");

        // picocli returns a non-zero usage-error exit code when a required option is missing
        assertTrue(exitCode != 0);
    }

    @Test
    @DisplayName("usability: list on an empty database says so instead of erroring")
    void list_emptyDatabase_friendlyMessage() {
        int exitCode = cli.execute("list");

        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("No transactions recorded yet."));
    }

    @Test
    @DisplayName("input validation: unknown transaction type in filter is rejected clearly")
    void filter_invalidType_rejected() {
        int exitCode = cli.execute("filter", "--type", "not-a-type");

        assertEquals(1, exitCode);
        assertTrue(stderr.toString().toLowerCase().contains("invalid transaction type"));
    }
}
