package dev.gaphunter.redoscatastrophicbacktrackingcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CatastrophicBacktrackingInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(CatastrophicBacktrackingInspection::class.java)
    }

    fun `test Pattern compile with nested quantifier produces a warning`() {
        myFixture.configureByText(
            "Validator.java",
            """
            import java.util.regex.Pattern;

            class Validator {
                private static final Pattern BAD = Pattern.compile("(a+)+${'$'}");
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("catastrophic-backtracking") == true })
    }

    fun `test String matches with overlapping alternation produces a warning`() {
        myFixture.configureByText(
            "Validator.java",
            """
            class Validator {
                boolean check(String s) {
                    return s.matches("(a|a)*");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("catastrophic-backtracking") == true })
    }

    fun `test a safe regex produces no warning`() {
        myFixture.configureByText(
            "Validator.java",
            """
            import java.util.regex.Pattern;

            class Validator {
                private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+");
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("catastrophic-backtracking") == true })
    }

    fun `test a dynamically built pattern is never flagged`() {
        myFixture.configureByText(
            "Validator.java",
            """
            import java.util.regex.Pattern;

            class Validator {
                Pattern build(String suffix) {
                    return Pattern.compile("(a+)+" + suffix);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("catastrophic-backtracking") == true })
    }

    fun `test a non-java file is never scanned`() {
        myFixture.configureByText(
            "notes.txt",
            "Pattern.compile(\"(a+)+\")",
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("catastrophic-backtracking") == true })
    }
}
