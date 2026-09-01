package dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex

enum class BacktrackingPattern {
    /** A quantifier applied to a group that itself contains an unbounded-quantified sub-expression: `(a+)+`, `(a*)+`, `(a+)*`. */
    NESTED_QUANTIFIER,

    /** An unbounded quantifier applied to a group whose alternation branches overlap (can both match the same text): `(a|a)*`, `(a|ab)*`. */
    OVERLAPPING_ALTERNATION,
}

data class BacktrackingFinding(val pattern: BacktrackingPattern)

/**
 * Walks a [RegexNode] tree looking for the two structural shapes that
 * cause exponential/polynomial backtracking in a backtracking regex
 * engine (java.util.regex included): a nested unbounded quantifier, and
 * an unbounded-quantified alternation whose branches overlap.
 *
 * Both patterns require an UNBOUNDED outer quantifier (`*`/`+`/`{n,}`)
 * to actually cause catastrophic behavior -- `(a+){3}` (a bounded outer
 * repeat) can't blow up the same way, so a bounded outer quantifier is
 * never flagged even if its body has the same shape.
 */
object BacktrackingAnalyzer {

    fun findAll(root: RegexNode): List<BacktrackingFinding> {
        val findings = mutableListOf<BacktrackingFinding>()
        walk(root, findings)
        return findings
    }

    private fun walk(node: RegexNode, findings: MutableList<BacktrackingFinding>) {
        when (node) {
            is RegexNode.Literal -> {}
            is RegexNode.Concat -> node.parts.forEach { walk(it, findings) }
            is RegexNode.Alternation -> node.branches.forEach { walk(it, findings) }
            is RegexNode.Group -> walk(node.body, findings)
            is RegexNode.Quantified -> {
                if (node.unbounded) {
                    if (containsUnboundedRepeat(node.body)) {
                        findings += BacktrackingFinding(BacktrackingPattern.NESTED_QUANTIFIER)
                    }
                    val alternation = unwrapToAlternation(node.body)
                    if (alternation != null && branchesOverlap(alternation.branches)) {
                        findings += BacktrackingFinding(BacktrackingPattern.OVERLAPPING_ALTERNATION)
                    }
                }
                // Still recurse into the body for findings nested deeper inside it.
                walk(node.body, findings)
            }
        }
    }

    /** True if [node] contains, anywhere within it (through groups/concat/alternation), an unbounded-quantified sub-expression. */
    private fun containsUnboundedRepeat(node: RegexNode): Boolean = when (node) {
        is RegexNode.Literal -> false
        is RegexNode.Concat -> node.parts.any { containsUnboundedRepeat(it) }
        is RegexNode.Alternation -> node.branches.any { containsUnboundedRepeat(it) }
        is RegexNode.Group -> containsUnboundedRepeat(node.body)
        is RegexNode.Quantified -> node.unbounded || containsUnboundedRepeat(node.body)
    }

    /** Peels through a single wrapping Group/Concat-of-one to find an Alternation directly inside a quantified body, e.g. `(a|b)*` -> the Alternation. */
    private fun unwrapToAlternation(node: RegexNode): RegexNode.Alternation? = when (node) {
        is RegexNode.Alternation -> node
        is RegexNode.Group -> unwrapToAlternation(node.body)
        is RegexNode.Concat -> if (node.parts.size == 1) unwrapToAlternation(node.parts[0]) else null
        else -> null
    }

    /**
     * True if any two branches of an alternation can match a common
     * non-empty string -- the real signature of catastrophic
     * backtracking in `(x|y)*`. v0.1 uses a structural approximation
     * (never a real string-matching engine, consistent with "no
     * java.util.regex dependency"): two branches overlap when their
     * literal texts are identical, or when one branch's literal text is
     * a prefix of the other's -- covers the canonical textbook cases
     * (`(a|a)*`, `(a|ab)*`) without claiming to detect every possible
     * overlap (e.g. two different character classes that happen to
     * intersect are NOT detected -- documented v0.1 limit).
     */
    private fun branchesOverlap(branches: List<RegexNode>): Boolean {
        val texts = branches.mapNotNull { flattenToLiteralText(it) }
        if (texts.size < branches.size) return false // a branch wasn't a plain literal sequence -- can't safely compare, skip
        for (i in texts.indices) {
            for (j in i + 1 until texts.size) {
                val a = texts[i]
                val b = texts[j]
                if (a.isEmpty() || b.isEmpty()) continue
                if (a == b || a.startsWith(b) || b.startsWith(a)) return true
            }
        }
        return false
    }

    /** Flattens a branch made only of literal characters/concat into its plain text, or null if it contains anything else (group/alternation/quantifier) that can't be safely compared as text. */
    private fun flattenToLiteralText(node: RegexNode): String? = when (node) {
        // Only a bare single character counts as safely comparable text --
        // an escape (`\d`), a character class (`[abc]`), or `.` carries
        // semantic meaning this structural check doesn't interpret, so
        // treating its raw text as a literal would risk a wrong overlap
        // verdict; excluding it here just means that branch (and so the
        // whole alternation, via the `texts.size < branches.size` check in
        // branchesOverlap) is skipped rather than analyzed.
        is RegexNode.Literal -> node.text.takeIf { it.length == 1 }
        is RegexNode.Concat -> {
            val parts = node.parts.map { flattenToLiteralText(it) ?: return null }
            parts.joinToString("")
        }
        else -> null
    }
}
