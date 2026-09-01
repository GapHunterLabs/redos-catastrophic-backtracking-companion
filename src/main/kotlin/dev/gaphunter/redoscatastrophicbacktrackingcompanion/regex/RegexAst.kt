package dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex

/**
 * A hand-rolled AST for a small, structural subset of Java regex syntax
 * -- built without using `java.util.regex` internally (the platform
 * exposes no PSI for the interior of a regex literal, so this is a
 * second, independent grammar just for reasoning about backtracking
 * shape, not for actually matching anything).
 *
 * Deliberately NOT a full regex engine: no semantic knowledge of
 * character classes' actual matched characters, no execution, no
 * capture-group numbering beyond what's needed to describe structure.
 * Only what [BacktrackingAnalyzer] needs to name the two structural
 * patterns that cause catastrophic backtracking.
 */
sealed class RegexNode {
    /** A single literal character, escape, character class (`[abc]`), or `.` -- an atomic, non-recursive unit. */
    data class Literal(val text: String) : RegexNode()

    /** Sequence of nodes with no operator between them: `ab` is `Concat([Literal(a), Literal(b)])`. */
    data class Concat(val parts: List<RegexNode>) : RegexNode()

    /** `a|b|c` -- alternatives, each itself a [RegexNode] (usually a [Concat]). */
    data class Alternation(val branches: List<RegexNode>) : RegexNode()

    /** `(...)`, `(?:...)`, `(?<name>...)` -- capturing or not doesn't matter for backtracking shape, so not tracked. */
    data class Group(val body: RegexNode) : RegexNode()

    /** [body] followed by a quantifier (`*`, `+`, `?`, `{n,m}`, `{n,}`). [minRepeat] is 0 for `*`/`?`, else >= 1. */
    data class Quantified(val body: RegexNode, val minRepeat: Int, val unbounded: Boolean) : RegexNode()
}
