package dev.gaphunter.redoscatastrophicbacktrackingcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.model.RegexCallHit
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex.BacktrackingAnalyzer
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex.RegexParser

/**
 * Finds `java.util.regex.Pattern.compile("...")` and
 * `"literal".matches("...")` call sites in Java source whose regex
 * argument is a static string literal, parses that literal with
 * [RegexParser] (a hand-rolled grammar, never `java.util.regex`
 * itself), and flags it when [BacktrackingAnalyzer] finds a
 * catastrophic-backtracking structural pattern.
 *
 * **v0.1 scope, stated honestly:** only a direct string literal
 * argument -- never a regex built dynamically at runtime (string
 * concatenation, a constant resolved through a field/variable). A
 * `Pattern.compile(someVariable)` or `Pattern.compile("a" + suffix)`
 * call is silently skipped, not flagged as safe.
 */
object JavaRegexCallFinder {

    fun findAll(file: PsiFile): List<RegexCallHit> {
        val hits = mutableListOf<RegexCallHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                hitForPatternCompile(expression)?.let { hits += it }
                hitForStringMatches(expression)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitForPatternCompile(call: PsiMethodCallExpression): RegexCallHit? {
        val methodExpr = call.methodExpression
        if (methodExpr.referenceName != "compile") return null
        val qualifier = methodExpr.qualifierExpression ?: return null
        if (qualifier.text != "Pattern" && qualifier.text != "java.util.regex.Pattern") return null
        val patternArg = call.argumentList.expressions.getOrNull(0) ?: return null
        return hitFor(patternArg, methodExpr)
    }

    private fun hitForStringMatches(call: PsiMethodCallExpression): RegexCallHit? {
        val methodExpr = call.methodExpression
        if (methodExpr.referenceName != "matches") return null
        // Only the instance form `"...".matches("...")`/`someString.matches("...")` --
        // qualifier must be present (excludes an unrelated static `matches(...)` overload).
        methodExpr.qualifierExpression ?: return null
        val patternArg = call.argumentList.expressions.getOrNull(0) ?: return null
        return hitFor(patternArg, methodExpr)
    }

    private fun hitFor(patternArg: PsiExpression, methodExpr: PsiElement): RegexCallHit? {
        val literal = patternArg as? PsiLiteralExpression ?: return null
        val patternText = literal.value as? String ?: return null
        val ast = RegexParser.parse(patternText) ?: return null
        val findings = BacktrackingAnalyzer.findAll(ast)
        if (findings.isEmpty()) return null
        // Anchor on the method NAME identifier (e.g. "compile"/"matches"), not
        // the whole call expression or methodExpression -- descending blindly
        // via firstChild can land on an empty PsiReferenceParameterList node
        // (present even with no explicit generics), which the platform
        // rejects with "Empty PSI elements must not be passed to
        // createDescriptor". referenceNameElement is always a real,
        // non-empty identifier leaf.
        val anchor = (methodExpr as? PsiReferenceExpression)?.referenceNameElement ?: methodExpr
        return RegexCallHit(anchor, patternText, findings.map { it.pattern }.distinct())
    }
}
