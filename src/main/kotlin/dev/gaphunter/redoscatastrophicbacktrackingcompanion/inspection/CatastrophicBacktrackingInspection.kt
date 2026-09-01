package dev.gaphunter.redoscatastrophicbacktrackingcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.detect.JavaRegexCallFinder
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.model.RegexCallHit
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex.BacktrackingPattern
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.review.ReviewPrompt

/**
 * Flags a `Pattern.compile("...")`/`"...".matches("...")` call in Java
 * source whose literal regex argument has a structural
 * catastrophic-backtracking shape (CWE-400) -- see
 * [JavaRegexCallFinder] and `BacktrackingAnalyzer` for the two patterns
 * detected: a nested unbounded quantifier (`(a+)+`), and an
 * unbounded-quantified alternation with overlapping branches
 * (`(a|a)*`).
 *
 * Runs via `checkFile` (same shape as every other Gap Hunter Labs
 * inspection in this catalog, including PSI-walk-backed ones like
 * `JavaClientBuildFinder`-style finders) rather than `buildVisitor` --
 * the finder itself does the `JavaRecursiveElementWalkingVisitor` walk.
 */
class CatastrophicBacktrackingInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null
        if (file !is PsiJavaFile) return null

        val hits = JavaRegexCallFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: RegexCallHit): String {
        val shapes = hit.findings.joinToString(", ") {
            when (it) {
                BacktrackingPattern.NESTED_QUANTIFIER -> "nested quantifier (e.g. (a+)+)"
                BacktrackingPattern.OVERLAPPING_ALTERNATION -> "overlapping alternation under a quantifier (e.g. (a|a)*)"
            }
        }
        return "Regex \"${hit.patternText}\" has a catastrophic-backtracking structural pattern: $shapes -- " +
            "an untrusted input crafted against this pattern can hang a thread indefinitely (CWE-400, ReDoS)"
    }
}
