package dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexParserTest {

    @Test
    fun `parses a plain literal sequence as Concat of Literals`() {
        val node = RegexParser.parse("abc")
        assertTrue(node is RegexNode.Concat)
        val parts = (node as RegexNode.Concat).parts
        assertEquals(3, parts.size)
        assertTrue(parts.all { it is RegexNode.Literal })
    }

    @Test
    fun `parses a single character with no operator as a bare Literal`() {
        val node = RegexParser.parse("a")
        assertTrue(node is RegexNode.Literal)
    }

    @Test
    fun `parses a plus-quantified group as unbounded with minRepeat 1`() {
        val node = RegexParser.parse("(a)+")
        assertTrue(node is RegexNode.Quantified)
        val q = node as RegexNode.Quantified
        assertTrue(q.unbounded)
        assertEquals(1, q.minRepeat)
        assertTrue(q.body is RegexNode.Group)
    }

    @Test
    fun `parses a star-quantified group as unbounded with minRepeat 0`() {
        val node = RegexParser.parse("(a)*") as RegexNode.Quantified
        assertTrue(node.unbounded)
        assertEquals(0, node.minRepeat)
    }

    @Test
    fun `parses an alternation inside a group`() {
        val node = RegexParser.parse("(a|b)") as RegexNode.Group
        assertTrue(node.body is RegexNode.Alternation)
        assertEquals(2, (node.body as RegexNode.Alternation).branches.size)
    }

    @Test
    fun `parses exact-count brace quantifier as bounded`() {
        val node = RegexParser.parse("a{3}") as RegexNode.Quantified
        assertEquals(3, node.minRepeat)
        assertTrue(!node.unbounded)
    }

    @Test
    fun `parses bounded range brace quantifier as bounded`() {
        val node = RegexParser.parse("a{2,5}") as RegexNode.Quantified
        assertEquals(2, node.minRepeat)
        assertTrue(!node.unbounded)
    }

    @Test
    fun `parses open-ended brace quantifier as unbounded`() {
        val node = RegexParser.parse("a{2,}") as RegexNode.Quantified
        assertEquals(2, node.minRepeat)
        assertTrue(node.unbounded)
    }

    @Test
    fun `parses a character class as a single Literal`() {
        val node = RegexParser.parse("[a-z]+") as RegexNode.Quantified
        assertTrue(node.body is RegexNode.Literal)
        assertEquals("[a-z]", (node.body as RegexNode.Literal).text)
    }

    @Test
    fun `parses an escape sequence as a single Literal`() {
        val node = RegexParser.parse("\\d+") as RegexNode.Quantified
        assertTrue(node.body is RegexNode.Literal)
        assertEquals("\\d", (node.body as RegexNode.Literal).text)
    }

    @Test
    fun `parses a non-capturing group`() {
        val node = RegexParser.parse("(?:abc)+") as RegexNode.Quantified
        assertTrue(node.body is RegexNode.Group)
    }

    @Test
    fun `parses a named group`() {
        val node = RegexParser.parse("(?<year>\\d{4})")
        assertTrue(node is RegexNode.Group)
    }

    @Test
    fun `parses lazy quantifier same as greedy structurally`() {
        val node = RegexParser.parse("(a)+?") as RegexNode.Quantified
        assertTrue(node.unbounded)
    }

    @Test
    fun `returns null for an unterminated group`() {
        assertNull(RegexParser.parse("(a"))
    }

    @Test
    fun `returns null for an unterminated character class`() {
        assertNull(RegexParser.parse("[a-z"))
    }

    @Test
    fun `returns null for a dangling backslash`() {
        assertNull(RegexParser.parse("abc\\"))
    }

    @Test
    fun `handles a realistic nested-quantifier ReDoS pattern`() {
        val node = RegexParser.parse("(a+)+$")
        assertNotNull(node)
    }
}
