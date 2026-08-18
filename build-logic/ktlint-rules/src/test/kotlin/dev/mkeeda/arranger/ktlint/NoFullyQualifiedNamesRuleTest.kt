package dev.mkeeda.arranger.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import kotlin.test.Test

class NoFullyQualifiedNamesRuleTest {
    private val ruleAssertThat = assertThatRule { NoFullyQualifiedNamesRule() }

    @Test
    fun `allows standard imports and packages`() {
        val code = """
            package dev.mkeeda.arranger.richtext

            import androidx.compose.ui.Modifier
            import dev.mkeeda.arranger.richtext.LinkKey
        """.trimIndent()

        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `allows import aliases`() {
        val code = """
            import dev.mkeeda.arranger.richtext.LinkKey as ArrangerLinkKey
        """.trimIndent()

        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `allows comments and kdoc containing fully qualified names`() {
        val code = """
            // See androidx.compose.ui.Modifier
            /**
             * Refer to [dev.mkeeda.arranger.richtext.LinkKey]
             */
            fun foo() {}
        """.trimIndent()

        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `disallows fully qualified expression in code body`() {
        val code = """
            fun foo() {
                val modifier = androidx.compose.ui.Modifier
            }
        """.trimIndent()

        ruleAssertThat(code)
            .hasLintViolations(
                LintViolation(
                    line = 2,
                    col = 20,
                    detail = "Fully qualified name 'androidx.compose.ui.Modifier' is not allowed in code body. Use imports or import aliases (e.g. `import package.ClassName as AliasName`) instead.",
                    canBeAutoCorrected = false,
                )
            )
    }

    @Test
    fun `disallows fully qualified type in function parameter`() {
        val code = """
            fun foo(key: dev.mkeeda.arranger.richtext.LinkKey) {}
        """.trimIndent()

        ruleAssertThat(code)
            .hasLintViolations(
                LintViolation(
                    line = 1,
                    col = 14,
                    detail = "Fully qualified name 'dev.mkeeda.arranger.richtext.LinkKey' is not allowed in code body. Use imports or import aliases (e.g. `import package.ClassName as AliasName`) instead.",
                    canBeAutoCorrected = false,
                )
            )
    }

    @Test
    fun `allows nested classes and member access chains`() {
        val code = """
            fun foo(entry: Map.Entry<String, String>, user: User) {
                val name = user.name.length
                val color = Color.Red
            }
        """.trimIndent()

        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `disallows java util fully qualified expression in code body`() {
        val code = """
            fun foo() {
                val uuid = java.util.UUID.randomUUID()
            }
        """.trimIndent()

        ruleAssertThat(code)
            .hasLintViolations(
                LintViolation(
                    line = 2,
                    col = 16,
                    detail = "Fully qualified name 'java.util.UUID.randomUUID()' is not allowed in code body. Use imports or import aliases (e.g. `import package.ClassName as AliasName`) instead.",
                    canBeAutoCorrected = false,
                )
            )
    }

    @Test
    fun `disallows custom package fully qualified type in function parameter`() {
        val code = """
            fun foo(key: com.example.custom.MyClass) {}
        """.trimIndent()

        ruleAssertThat(code)
            .hasLintViolations(
                LintViolation(
                    line = 1,
                    col = 14,
                    detail = "Fully qualified name 'com.example.custom.MyClass' is not allowed in code body. Use imports or import aliases (e.g. `import package.ClassName as AliasName`) instead.",
                    canBeAutoCorrected = false,
                )
            )
    }
}
