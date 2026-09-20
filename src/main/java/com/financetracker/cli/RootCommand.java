package com.financetracker.cli;

import picocli.CommandLine.Command;

/**
 * Top-level command. Running with no subcommand (in batch mode) just prints help;
 * the interactive REPL in {@code Main} is the primary way this app is used.
 */
// Subcommands are registered manually in Main (as instances) because each one
// needs an AppContext injected via its constructor rather than picocli's
// default no-arg reflection-based instantiation.
@Command(
        name = "finance-tracker",
        mixinStandardHelpOptions = true,
        version = "finance-tracker-cli 1.0.0",
        description = "Personal Finance & Expense Tracker CLI"
)
public class RootCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use --help to see available commands, or run with no arguments for interactive mode.");
    }
}
