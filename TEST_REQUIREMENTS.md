**Test Case Specification — Finance Tracker CLI**
**Introduction**

This document defines the Finance Tracker CLI’s test suite as executable test cases. Each requirement (TR‑01 to TR‑37)
is mapped to a test case specification with clear steps and expected outcomes.
Scope

In scope: 
    -six CLI commands (add-expense, add-income, list, filter, convert, summary), validation logic, currency 
conversion, H2 persistence, exchange rate API.

Out of scope:
    -performance/load testing, security/auth, REPL line-editing behavior.

**Test Strategy**

-Unit tests for validation/calculation.

-Integration tests for DB persistence.

-API tests for exchange rate scenarios.

-End-to-end tests for CLI behavior.

**Environment**

-Local JDK 26, CI JDK 21.

-WireMock for API, H2 in-memory DB.

**Criteria**

-Entry: feature compiles, dependencies tested.

-Exit: all requirements covered, mvn test passes, coverage threshold met.

**Deliverables**

-Automated test suite.

-Coverage report.

-Bug report log.

**Risks**

-API contract changes.

-JDK mismatch risk.

**Test Cases**
**Add-expense / Add-income**

**_TR-01 Valid transaction recorded**_

Preconditions: CLI installed, DB initialized

Steps: Run add-expense --amount 100 --category Food  

Expected: Transaction saved with correct details

Actual Result: Transaction saved successfully, verified in DB


**_-TR-02 Reject zero/negative amount**_

Steps: Run add-expense --amount -50 --category Food  

Expected: Error message; Amount must be greater than zero.

Actual Result: CLI prints “Amount must be greater than zero.”; DB unchanged


**_-TR-03 Reject missing amount/category**_

Steps: Run add-expense --amount 100  

Expected: Error message; Missing required option "--category=<category>"

Actual Result: CLI prints "Missing required option: '--category=<category>'"; DB unchanged


**_-TR-04 Reject blank category**_

Steps: Run add-expense --amount 100 --category ""  

Expected: Error message; required parameter for option --category

Actual Result: CLI prints “Missing required parameter for option '--category' (<category>)”; DB unchanged


**_-TR-05 Convert foreign currency before saving**_

Steps: Run add-expense --amount 100 --category Food --currency USD  

Expected: Expense recorded

Actual Result: CLI converts USD to ZAR using API stub; DB entry saved in ZAR


**_-TR-06 Default currency if omitted**_

Steps: Run add-expense --amount 100 --category Food  

Expected: Currency defaults to base with no conversion

Actual Result: Transaction saved in ZAR (default currency)


**_-TR-07 Default date if omitted**_

Steps: Run add-expense --amount 100 --category Food  

Expected: Date defaults to today

Actual Result: Transaction saved with today’s date


**_-TR-08 Reject invalid date**_

Steps: Run add-expense --amount 100 --category Food --date 2025-99-99  

Expected: Error: Invalid date 

Actual Result: CLI prints “Error: Invalid date '2025-99-99', expected yyyy-MM-dd.”; DB unchanged


**_-TR-09 add-income enforces same validation**_

Steps: Run add-income --amount -50 --category Salary  

Expected: Error message: Amount must be greater than zero.

Actual Result: CLI prints “Error: Amount must be greater than zero.”; DB unchanged


**_-TR-10 API failure during add**_

Steps: Run add-income --amount 500 --category Salary --currency USD with API down

Expected: Error message; transaction not saved

Actual Result: CLI prints “Exchange rate service unavailable”; DB unchanged


**List**

**_-TR-11 List all transactions, most recent first**_

Steps: Run list  

Expected: Transactions displayed in descending order

Actual Result: CLI lists transactions newest first, listed in descending order


**_-TR-12 Empty DB shows output message**_

Steps: Run list on empty DB

Expected: “No transactions found” message

Actual Result: CLI prints “No transactions found”


**Filter**

_**-TR-13 Filter by type**_

Steps: Run filter --type expense  

Expected: Only expenses listed

Actual Result: CLI lists only expense transactions, Matched: <number of expense transactions> transaction(s)


****__**-TR-14 Filter by category_****_**

Steps: Run filter --category Food  

Expected: Only Food transactions listed

Actual Result: CLI lists only Food category transactions,Matched: <number of food expense transactions> transaction(s)


****__**-TR-15 Filter by date range_****_**

Steps: Run filter --from 2025-01-01 --to 2025-01-31  

Expected: Transactions in range listed

Actual Result: CLI lists transactions between given dates


**_-TR-16 Combine multiple filters**_

Steps: Run filter --type income --category Salary

Expected: Only Salary incomes listed

Actual Result: CLI lists only Salary income transactions, Matched: <number of income transactions> transaction(s)


**_-TR-17 No filters returns everything**_

Steps: Run filter

Expected: All transactions listed

Actual Result: CLI lists all transactions and the number of matched transactions


**_-TR-18 Reject invalid type**_

Steps: Run filter --type invalidType  

Expected: Invalid transaction type

Actual Result: CLI prints “Error: Invalid transaction type 'Luxury'. Expected INCOME or EXPENSE.”; DB unchanged


**_-TR-19 Reject invalid date filters**_

Steps: Run filter --from 2025-13-01  

Expected: Invalid date, expected yyyy-MM-dd

Actual Result: CLI prints “Error: Invalid date '2025-13-01' for --from, expected yyyy-MM-dd.”; exits 


**_-TR-20 No matches prints message**_

Steps: Run filter --category Nonexistent  

Expected: “No transactions found” message

Actual Result: CLI prints “No transactions match those filters”


**Convert**

_**-TR-21 Convert currencies using live rate**_

Steps: Run convert --amount 100 --from USD --to EUR  

Expected: Correct converted amount shown

Actual Result: CLI prints “100 USD = 86.64 EUR” (stubbed rate)


**_-TR-22 Skip same-currency conversion**_

Steps: Run convert --amount 100 --from USD --to USD  

Expected: Output equals input; no API call

Actual Result: CLI prints “100 USD = 100 USD”; no API call made


**_-TR-23 Unknown currency error**_
Steps: Run convert --amount 100 --from USD --to POUND 

Expected: “Unknown or unsupported currency code” error

Actual Result: CLI prints “Error: Unknown or unsupported currency code”; exits 

****__**-TR-24 API downtime error_****_**

Steps: Run convert --amount 100 --from USD --to EUR with API down

Expected: “Exchange rate service unavailable” error

Actual Result: CLI prints “Exchange rate service unavailable”; exits 


**_-TR-25 Output formatted clearly**_

Steps: Run convert --amount 100 --from USD --to EUR  

Expected: Output readable and clear

Actual Result: CLI prints “100 USD = 86.64 EUR” in clean format


**Summary**

**_-TR-26 Correct income/expenses/balance**_

Steps: Run summary  

Expected: Totals calculated correctly

Actual Result: CLI prints correct totals for income, expenses, balance


**_-TR-27 Correct per-category breakdown**_

Steps: Run summary  

Expected: Breakdown matches transactions

Actual Result: CLI prints per-category totals correctly


**_-TR-28 Empty DB doesn’t error**_

Steps: Run summary on empty DB

Expected: Friendly message; no crash 

Actual Result: CLI prints “No transactions found”


**_-TR-29 Monthly over-budget check**_

Steps: Run summary with expenses exceeding budget

Expected: Over-budget warning displayed

Actual Result: CLI prints “Budget exceeded for September”


**Cross-cutting**

**_-TR-30 Data persists across CLI runs**_

Steps: Add transaction, close CLI, reopen, run list  

Expected: Transaction still present

Actual Result: Transaction persisted and listed after restart


**_-TR-31 REPL tokenizer handles quoted input**_

Steps: Run add-expense --category "Food and Drink"  

Expected: Category parsed correctly

Actual Result: CLI saves transaction with category “Food and Drink”


**_-TR-32 REPL loop handles exit/help/blank**_

Steps: Run exit, help, blank input

Expected: CLI responds correctly

Actual Result: CLI exits on exit, prints help on help, ignores blank input


**_-TR-33 Two processes access same DB**_

Steps: Run CLI in two terminals on same DB

Expected: No corruption; errors handled gracefully

Actual Result: Second process prints “Database locked”; no corruption observed


**Exchange rate API resilience**

**_-TR-34 API downtime surfaced clearly**_

Steps: Run conversion with API down

Expected: Clear error message

Actual Result: CLI prints “Exchange rate service

