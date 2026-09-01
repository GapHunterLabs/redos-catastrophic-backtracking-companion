package dev.gaphunter.redoscatastrophicbacktrackingcompanion.model

import com.intellij.psi.PsiElement
import dev.gaphunter.redoscatastrophicbacktrackingcompanion.regex.BacktrackingPattern

/** One call site (`Pattern.compile(...)`/`String.matches(...)`) whose literal regex argument has a catastrophic-backtracking structural pattern. */
data class RegexCallHit(
    val anchor: PsiElement,
    val patternText: String,
    val findings: List<BacktrackingPattern>,
)
