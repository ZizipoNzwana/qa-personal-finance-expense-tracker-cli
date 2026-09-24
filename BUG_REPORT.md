** Bug Report — Finance Tracker CLI**

**BUG-001: `filter` crashed with a raw stack trace on an invalid date — Fixed**

**Severity:** High
**Component:** `cli/FilterCommand.java`

**What was wrong:** `FilterCommand.call()` only caught `IllegalArgumentException`,
but `LocalDate.parse()` throws `DateTimeParseException`, which doesn't
extend `IllegalArgumentException`. So running something like
`filter --from 2026-13-40` slipped past the catch block entirely and printed
an unhandled stack trace instead of a clean error.

**Fix:** `FilterCommand` now parses `--from`/`--to` through a small helper
that catches `DateTimeParseException` and rethrows it as a clear
`IllegalArgumentException` with a specific message (e.g. "Invalid date
'2026-13-40' for --from, expected yyyy-MM-dd"), which the existing catch
block already handles correctly.

**BUG-002: Bad `--date` on add-expense/add-income gave a confusing error — Fixed**

**Severity:** Medium
**Component:** `cli/AddExpenseCommand.java`, `cli/AddIncomeCommand.java`

**What was wrong:** A malformed `--date` didn't crash (there's a catch-all
`Exception` handler here, unlike BUG-001), but it surfaced the raw
`DateTimeParseException` message — "Text 'not-a-date' could not be parsed at
index 0" — instead of a clear, consistent validation error.

**Fix:** Both commands now catch `DateTimeParseException` specifically
around the date parse and print a plain "Invalid date, expected yyyy-MM-dd"
message, consistent with how every other bad-input case in this app is
reported.

**BUG-003: Transaction list output misaligns depending on currency note length**

**Severity:** Low (cosmetic)
**Component:** `model/Transaction.java`, `toString()`

**Status:** Still open.

**Steps to reproduce**

1. Add one transaction in the base currency: `add-expense --amount 10 --category food`
2. Add one transaction in a foreign currency: `add-expense --amount 10 --category food --currency EUR`
3. Run `list` and compare the two rows.

**Actual result:** the `[no conversion]` and `[40 EUR -> converted]`
bracket segments are different lengths and aren't padded to a fixed width,
so everything after them (category, description) shifts depending on which
one printed. Doesn't affect the data, just makes the table harder to scan.

**Suggested fix:** give that bracketed segment a fixed width in the format
string (e.g. `%-22s` instead of `%s`), the same way the other columns
already are.

 ****Not filed as bugs, but worth a second look****

- `filter --type` and the currency fields (`--currency`, `--from`/`--to` in
  `convert`) don't validate their format before hitting the database or the
  exchange rate API — they rely on downstream errors to surface a problem.
  Not incorrect, just a case where earlier validation would give a faster,
  clearer error.
- No dedicated regression test currently exercises the invalid-date fixes
  for BUG-001 and BUG-002 — worth adding one for each so a future change
  can't silently reintroduce either bug.