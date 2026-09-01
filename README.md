# ReDoS Catastrophic-Backtracking Companion

Warning on a `Pattern.compile("...")`/`"...".matches("...")` call in
Java whose literal regex argument has a structural
catastrophic-backtracking pattern: a nested unbounded quantifier
(`(a+)+`), or an unbounded-quantified alternation with overlapping
branches (`(a|a)*`). A single crafted input against either pattern can
hang a server thread indefinitely.

## Why it exists

CWE-400 (Uncontrolled Resource Consumption) via Regular Expression
Denial of Service — a regex compiled from a plain string literal with
one of these two well-documented structural shapes turns a single HTTP
request with a crafted input into an indefinite hang on whatever thread
runs it. Standalone academic tooling (RegexStaticAnalysis) and a
SonarQube rule cover this category; no dedicated plugin was found on
the JetBrains Marketplace across two search passes (short name +
descriptive phrase).

## Why built this way

- **Its own regex grammar, not `java.util.regex`** — the platform
  exposes no PSI for the interior of a regex string literal, so a
  second, independent parser is built by hand ([RegexParser]) just to
  reason about backtracking shape. It never executes the regex or
  depends on the JDK's own regex engine.
- **Structural, not simulated** — `BacktrackingAnalyzer` recognizes the
  two named shapes by walking the parsed AST, not by actually running
  the pattern against sample input or estimating real backtracking
  cost.
- **Real PSI walk for the call site** — `Pattern.compile(...)`/
  `.matches(...)` calls are found via a `JavaRecursiveElementWalkingVisitor`
  over real Java PSI (`com.intellij.java` dependency), same as any
  other call-site detector in this catalog.

## v0.1 scope — stated honestly, not exhaustively

- Java only (`Pattern.compile`, `String.matches`) — Kotlin, Python, and
  other languages are out of scope for this version.
- Only the two structural patterns named above — not a complete
  backtracking-cost engine that estimates worst-case time for an
  arbitrary regex.
- Only a static string literal argument — a regex built dynamically at
  runtime (string concatenation, a value read from a variable/constant)
  is silently skipped, never flagged as safe.
- Overlap detection between alternation branches is a structural
  approximation (identical or prefix-of-each-other literal text) — two
  different character classes that happen to intersect are not
  detected.

## Usage

Open any Java file. A `Pattern.compile("...")` or `"...".matches("...")`
call whose literal regex has one of the two dangerous shapes shows a
warning on that call.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
