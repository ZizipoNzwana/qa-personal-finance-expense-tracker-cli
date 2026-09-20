package com.financetracker.cli;

import picocli.CommandLine;

/**
 * Builds a {@link CommandLine} with every subcommand wired to the shared
 * {@link AppContext}. Kept separate from {@code Main} so tests (and the REPL)
 * can build the same command tree without duplicating wiring logic.
 */
public final class CliFactory {

    private CliFactory() {
    }

    public static CommandLine build(AppContext context) {
        CommandLine cli = new CommandLine(new RootCommand());
        cli.addSubcommand("add-expense", new AddExpenseCommand(context));
        cli.addSubcommand("add-income", new AddIncomeCommand(context));
        cli.addSubcommand("list", new ListCommand(context));
        cli.addSubcommand("filter", new FilterCommand(context));
        cli.addSubcommand("convert", new ConvertCommand(context));
        cli.addSubcommand("summary", new SummaryCommand(context));
        return cli;
    }
}
