# Demo data for screenshots

`Validator.java` — `GREEDY_LINE` (nested quantifier) and
`REDUNDANT_ALT` (overlapping alternation) flagged; `BOUNDED` and
`EMAIL` not flagged. `isRisky` also flagged (String.matches form).

## How to get the screenshot

1. `./gradlew runIde` from `redos-catastrophic-backtracking-companion`,
   open this `demo/` folder as the project.
2. Full Screen, open `Validator.java` — warnings should appear on
   `GREEDY_LINE`, `REDUNDANT_ALT`, and the `matches(...)` call inside
   `isRisky`, but not on `BOUNDED`/`EMAIL`.
3. Screenshot with all patterns visible, save into
   `redos-catastrophic-backtracking-companion/docs/screenshots/`.
   Close the sandbox.
