package dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktrackingAnalyzerTest {

    private fun findingsFor(pattern: String): List<BacktrackingFinding> {
        val ast = RegexParser.parse(pattern) ?: error("test pattern failed to parse: $pattern")
        return BacktrackingAnalyzer.findAll(ast)
    }

    @Test
    fun `flags the canonical nested quantifier pattern (a+)+`() {
        val findings = findingsFor("(a+)+")
        assertTrue(findings.any { it.pattern == BacktrackingPattern.NESTED_QUANTIFIER })
    }

    @Test
    fun `flags (a*)+ as nested quantifier`() {
        val findings = findingsFor("(a*)+")
        assertTrue(findings.any { it.pattern == BacktrackingPattern.NESTED_QUANTIFIER })
    }

    @Test
    fun `flags (a+)star as nested quantifier`() {
        val findings = findingsFor("(a+)*")
        assertTrue(findings.any { it.pattern == BacktrackingPattern.NESTED_QUANTIFIER })
    }

    @Test
    fun `does not flag a bounded outer repeat even with an unbounded inner quantifier`() {
        val findings = findingsFor("(a+){3}")
        assertTrue(findings.none { it.pattern == BacktrackingPattern.NESTED_QUANTIFIER })
    }

    @Test
    fun `does not flag a single unbounded quantifier with no nesting`() {
        val findings = findingsFor("a+")
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not flag a quantified group whose body has only a bounded inner repeat`() {
        val findings = findingsFor("(a{2,4})+")
        assertTrue(findings.none { it.pattern == BacktrackingPattern.NESTED_QUANTIFIER })
    }

    @Test
    fun `flags the canonical overlapping alternation pattern (a|a) star`() {
        val findings = findingsFor("(a|a)*")
        assertTrue(findings.any { it.pattern == BacktrackingPattern.OVERLAPPING_ALTERNATION })
    }

    @Test
    fun `flags overlapping alternation where one branch is a prefix of the other`() {
        // Real single-char-branch case: (a|a)* is the only shape flattenToLiteralText
        // can safely compare (multi-char literal concat also works, e.g. (ab|a)*).
        val findings = findingsFor("(ab|a)*")
        assertTrue(findings.any { it.pattern == BacktrackingPattern.OVERLAPPING_ALTERNATION })
    }

    @Test
    fun `does not flag a non-overlapping alternation under a quantifier`() {
        val findings = findingsFor("(a|b)*")
        assertTrue(findings.none { it.pattern == BacktrackingPattern.OVERLAPPING_ALTERNATION })
    }

    @Test
    fun `does not flag an overlapping alternation under a bounded quantifier`() {
        val findings = findingsFor("(a|a){2,4}")
        assertTrue(findings.none { it.pattern == BacktrackingPattern.OVERLAPPING_ALTERNATION })
    }

    @Test
    fun `does not flag a safe, common pattern like email-ish regex`() {
        val findings = findingsFor("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not flag a plain literal-only pattern`() {
        val findings = findingsFor("hello world")
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `real-world example from RegexStaticAnalysis style catches nested group with alternation and quantifier`() {
        val findings = findingsFor("(a|a)+")
        assertEquals(1, findings.count { it.pattern == BacktrackingPattern.OVERLAPPING_ALTERNATION })
    }
}
