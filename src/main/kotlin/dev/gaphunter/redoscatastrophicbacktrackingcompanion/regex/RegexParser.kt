package dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex

/**
 * Recursive-descent parser for the structural subset of Java regex
 * syntax that [RegexNode] models. Standard grammar shape (same
 * precedence as every regex flavor: alternation is lowest, then
 * concatenation, then postfix quantifiers on atoms):
 *
 * ```
 * alternation = concat ('|' concat)*
 * concat      = quantified*
 * quantified  = atom ('*' | '+' | '?' | '{' n (',' m?)? '}')?
 * atom        = group | charClass | escape | literalChar
 * group       = '(' ('?' [:<name>=!]?)? alternation ')'
 * charClass   = '[' ('^')? (']')? (escape | range | char)* ']'
 * ```
 *
 * Returns `null` on anything that doesn't parse cleanly as this subset
 * (an unterminated group/class, a dangling backslash, etc.) -- callers
 * treat a `null` result as "can't reason about this regex", never as
 * "safe". Deliberately lenient about constructs it doesn't specially
 * understand (POSIX classes, backreferences, lookaround contents) --
 * they're consumed as opaque atoms rather than causing a parse failure,
 * since flagging false structural danger inside e.g. a lookahead body
 * would be a worse outcome than just not analyzing that part.
 */
object RegexParser {

    fun parse(pattern: String): RegexNode? =
        try {
            val cursor = Cursor(pattern)
            val node = cursor.parseAlternation()
            if (cursor.atEnd()) node else null
        } catch (_: ParseFailure) {
            null
        }

    private class ParseFailure : Exception()

    private class Cursor(private val text: String) {
        private var pos = 0

        fun atEnd(): Boolean = pos >= text.length
        private fun peek(): Char? = text.getOrNull(pos)
        private fun advance(): Char = text[pos++]

        fun parseAlternation(): RegexNode {
            val branches = mutableListOf(parseConcat())
            while (peek() == '|') {
                advance()
                branches += parseConcat()
            }
            return if (branches.size == 1) branches[0] else RegexNode.Alternation(branches)
        }

        private fun parseConcat(): RegexNode {
            val parts = mutableListOf<RegexNode>()
            while (peek() != null && peek() != '|' && peek() != ')') {
                parts += parseQuantified()
            }
            return when (parts.size) {
                0 -> RegexNode.Concat(emptyList())
                1 -> parts[0]
                else -> RegexNode.Concat(parts)
            }
        }

        private fun parseQuantified(): RegexNode {
            val atom = parseAtom()
            return when (peek()) {
                '*' -> {
                    advance(); skipLazyOrPossessive()
                    RegexNode.Quantified(atom, minRepeat = 0, unbounded = true)
                }
                '+' -> {
                    advance(); skipLazyOrPossessive()
                    RegexNode.Quantified(atom, minRepeat = 1, unbounded = true)
                }
                '?' -> {
                    advance(); skipLazyOrPossessive()
                    RegexNode.Quantified(atom, minRepeat = 0, unbounded = false)
                }
                '{' -> parseBraceQuantifier(atom)
                else -> atom
            }
        }

        /** Consumes a trailing `?` (lazy) or `+` (possessive) right after a quantifier -- doesn't change backtracking shape analysis for v0.1. */
        private fun skipLazyOrPossessive() {
            if (peek() == '?' || peek() == '+') advance()
        }

        private fun parseBraceQuantifier(atom: RegexNode): RegexNode {
            val start = pos
            advance() // '{'
            val minDigits = StringBuilder()
            while (peek()?.isDigit() == true) minDigits.append(advance())
            if (minDigits.isEmpty()) {
                // Not a real quantifier (e.g. a literal '{' in some flavors) -- treat '{' as a literal atom.
                pos = start
                advance()
                return RegexNode.Literal("{")
            }
            // Only "{n,}" (a comma with no max digits) is truly unbounded --
            // "{n}" (exact count) and "{n,m}" both behave like a bounded
            // repeat and can't cause catastrophic backtracking on their own.
            var unbounded = false
            if (peek() == ',') {
                advance()
                val maxDigits = StringBuilder()
                while (peek()?.isDigit() == true) maxDigits.append(advance())
                if (maxDigits.isEmpty()) unbounded = true
            }
            if (peek() != '}') throw ParseFailure()
            advance()
            skipLazyOrPossessive()
            val min = minDigits.toString().toIntOrNull() ?: 0
            return RegexNode.Quantified(atom, minRepeat = min, unbounded = unbounded)
        }

        private fun parseAtom(): RegexNode = when (peek()) {
            '(' -> parseGroup()
            '[' -> parseCharClass()
            '\\' -> parseEscape()
            '.' -> { advance(); RegexNode.Literal(".") }
            '^', '$' -> { RegexNode.Literal(advance().toString()) }
            null -> throw ParseFailure()
            else -> RegexNode.Literal(advance().toString())
        }

        private fun parseGroup(): RegexNode {
            advance() // '('
            if (peek() == '?') {
                advance()
                when (peek()) {
                    ':' -> advance() // non-capturing (?:...)
                    '<' -> {
                        advance()
                        if (peek() == '=' || peek() == '!') {
                            // lookbehind (?<= / (?<! -- consume as opaque, still parse body structurally.
                            advance()
                        } else {
                            // named group (?<name>...
                            while (peek() != null && peek() != '>') advance()
                            if (peek() == '>') advance()
                        }
                    }
                    '=', '!' -> advance() // lookahead (?= / (?!
                    else -> { /* other inline-flag forms, e.g. (?i) -- consume until ':' or ')' handled by body parse */ }
                }
            }
            val body = parseAlternation()
            if (peek() != ')') throw ParseFailure()
            advance()
            return RegexNode.Group(body)
        }

        private fun parseCharClass(): RegexNode {
            val start = pos
            advance() // '['
            if (peek() == '^') advance()
            if (peek() == ']') advance() // a leading ']' right after '[' or '[^' is a literal member
            while (true) {
                val c = peek() ?: throw ParseFailure()
                if (c == ']') { advance(); break }
                if (c == '\\') { advance(); if (peek() != null) advance() } else advance()
            }
            return RegexNode.Literal(text.substring(start, pos))
        }

        private fun parseEscape(): RegexNode {
            val start = pos
            advance() // '\'
            if (peek() == null) throw ParseFailure()
            advance()
            return RegexNode.Literal(text.substring(start, pos))
        }
    }
}
