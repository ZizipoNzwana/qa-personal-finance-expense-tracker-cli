#QA-Personal Finance and Expense tracker
WTC-E4L3VNTR

A professional QA Automation portfolio documenting my journey from QA fundamentals to building a complete automation framework using industry practices and open-source software.

ABOUT THE PROJECT: 

This is a command-line tool for keeping track of everyday income and expenses.This solves a simple problem: most budgeting apps are more than what a single person needs day to day, and a lot of people are perfectly happy logging a transaction from a terminal if it's fast and doesn't get in the way. So this project focuses on exactly that — logging what comes in and what goes out, and seeing where you actually stand between the two, without much else in the way.

It tracks both sides on purpose, income and expenses, because the point isn't just seeing where your money went, it's seeing whether you're ahead or behind once both are counted. In practice, expenses still end up logged far more often than income — a paycheck is one entry a month, coffee and groceries are dozens — but the balance the app shows you only means anything because both sides are there.

The feature that goes beyond basic transaction logging is currency conversion. If you spend money in a currency that isn't your own — a hotel booked in euros, a subscription billed in GBP — the tool looks up the live exchange rate and stores the converted amount in your base currency, so your totals and balance stay meaningful even if half your transactions came in different currencies. It talks to Frankfurter, a free public exchange rate API, for that.

Everything is stored locally in an embedded H2 database, so there's no server to run and no account to sign up for. You just build the jar and go.
