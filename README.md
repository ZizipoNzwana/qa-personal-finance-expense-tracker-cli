#QA-Personal Finance and Expense tracker with CLI

WTC-E4L3VNTR

## **1. About The Project:** 

This solves a simple problem: most budgeting apps are more than what a single person needs day to day, and a lot of 
people are perfectly happy logging a transaction from a terminal if it's fast and doesn't get in the way. 
So this project focuses on exactly that — logging what comes in and what goes out, and seeing where you actually stand 
between the two, without much else in the way. It tracks both sides on purpose, income and expenses, because the point 
isn't just seeing where your money went, it's seeing whether you're ahead or behind once both are counted. In practice,
expenses still end up logged far more often than income — a paycheck is one entry a month, coffee and groceries are 
dozens — but the balance the app shows you only means anything because both sides are there. The feature that goes beyond
basic transaction logging is currency conversion. If you spend money in a currency that isn't your own — a hotel booked in
euros, a subscription billed in GBP — the tool looks up the live exchange rate and stores the converted amount in your 
base currency, so your totals and balance stay meaningful even if half your transactions came in different currencies. 
It talks to Frankfurter, a free public exchange rate API, for that. Everything is stored locally in an embedded H2 
database, so there's no server to run and no account to sign up for. You just build the jar and go.

## **2. Tools Used:**

**-Picocli:**  
Instead of manually writing option definitions and parsing, you just annotate fields. It also generates help text 
and validates arguments automatically. Since this project uses multiple subcommands (add-expense, add-income, list, 
filter, convert, summary), Picocli’s built‑in subcommand support saved a lot of repetitive code.

**-H2 Database:**  
Runs in‑process, no installation or server needed, but still uses standard JDBC/SQL. In file mode, data persists 
between runs; in memory mode, it’s perfect for fast integration tests. Switching between the two is just a one‑line change.

**-Java HttpClient:**  
The built‑in client was enough for simple GET requests to the exchange rate API. No need for heavier libraries like OkHttp.

**-Jackson:**  
Used for JSON parsing. It’s the standard in Java, and the API response was simple enough that lightweight JsonNode 
traversal was clearer than generating model classes.

**-JUnit 5 + Mockito:**  
Standard combo for unit testing. Keeps business logic testable without needing a live database or network.

**-WireMock:**  
Provides a fake HTTP server for testing API calls. This avoids hitting the real Frankfurter API, making tests 
faster and more reliable. It also lets you simulate tricky cases like errors or dropped connections.

**-REST Assured:**  
Used alongside WireMock in one test to confirm stubbed responses look correct before relying on them.

**-Jacoco:**  
Measures test coverage so you know how much of the codebase is exercised.

**-GitHub Actions:**  
Automates builds and tests on every pull request, ensuring checks run consistently. 


## ****3. Project Structure/How The Project Is Organised:****

The code is organized into layers, each with a single responsibility. This separation makes the system easier to test,
maintain, and extend.

**-Model layer:**  
Holds simple data classes like Transaction and TransactionType. These classes only store information — they don’t contain
any logic.

**-Database layer:**  
Handles all database work (DatabaseManager, TransactionRepository). This is the only part of the project that writes SQL
or talks directly to JDBC.

**-Service layer:**  
Contains the business logic (ExchangeRateService, TransactionService, BudgetService). It validates input, converts 
currencies, and calculates totals. Services don’t know whether they’re being called from the CLI or from tests — they 
just focus on the rules.

**-CLI layer:**  
Lightweight and focused. Each command reads flags and arguments, passes them to a service, and prints the result. 
It doesn’t contain business logic or database code — only input/output.

**-AppContext:**  
Wires everything together at startup. It builds the real database connection and exchange rate client.

**CliFactory:**  
Connects the application context to each CLI command so they can run with the right dependencies.

**-Main:**  
Decides how commands are run:
With arguments → runs one command and exits (useful for scripting). 
With no arguments → starts an interactive loop, reading commands line by line and passing them to the same parser.


## ****4. Testing:****

**The test suite mirrors the project’s layered design:**
-**BudgetServiceTest and TransactionServiceTest**: pure unit tests with mocked dependencies (Mockito).
-**TransactionRepositoryTes**t: runs against a real in‑memory H2 database to verify SQL and JDBC mapping.
-**ExchangeRateServiceTest:** uses WireMock to simulate normal responses, unknown currency codes, server errors, malformed 
JSON, and unreachable API cases.
-**CliEndToEndTest:** drives the CLI parser like a user, checking exit codes and printed output, including edge cases 
(negative amounts, missing flags, empty lists).

## **5. Building and running it:**

**-mvn clean package**
This compiles everything, runs the test suite, checks the Jacoco coverage threshold, and produces a runnable jar at 
target/finance-tracker-cli.jar

**Running it with no arguments starts the interactive mode:**
java -jar target/finance-tracker-cli.jar

The base currency defaults to ZAR, since that's the currency this was built around, but it can be overridden with an 
environment variable before starting the app if you want to run it in something else:

**one-off commands, pass arguments directly instead:**

java -jar target/finance-tracker-cli.jar add-expense --amount 850 --category food

java -jar target/finance-tracker-cli.jar add-income --amount 32000 --category salary

java -jar target/finance-tracker-cli.jar list

java -jar target/finance-tracker-cli.jar filter --category food --from 2026-01-01 --to 2026-12-31

java -jar target/finance-tracker-cli.jar convert --amount 100 --from USD --to ZAR

java -jar target/finance-tracker-cli.jar summary

add-expense and add-income both take a required --amount and --category, with optional --currency (defaults to your 
base currency), --description, and --date (defaults to today). filter narrows the transaction list by --type, --category,
--from, and --to, any of which can be left out. convert is a standalone currency lookup — it doesn't save anything, 
it just prints the converted amount. summary prints total income, total expenses, current balance, and a breakdown 
of expenses by category.

Data is written to ./data/financetracker.mv.db, relative to wherever the jar is run from, so it carries over between 
sessions as long as you run it from the same directory.

The base currency is set once per run (through an environment variable, or ZAR by default) — there's no command yet to 
change it after the fact, and no concept of multiple accounts, even though the database schema already has tables 
(accounts, categories) that anticipate that. Right now only the transactions table is actually used.

