package com.financetracker.cli;

import com.financetracker.model.Transaction;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "list",
        description = "List all recorded transactions, most recent first.",
        mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    private final AppContext context;

    public ListCommand(AppContext context) {
        this.context = context;
    }

    @Override
    public Integer call() {
        List<Transaction> transactions = context.transactionRepository().findAll();
        if (transactions.isEmpty()) {
            System.out.println("No transactions recorded yet.");
            return 0;
        }
        transactions.forEach(t -> System.out.println(t));
        System.out.printf("%nTotal: %d transaction(s)%n", transactions.size());
        return 0;
    }
}
