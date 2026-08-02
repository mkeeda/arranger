package dev.mkeeda.arranger.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpotlessCustomRulesTest {

    @Test
    fun `allows standard package and import statements`() {
        val content = """
            package dev.mkeeda.arranger.richtext
            
            import dev.mkeeda.arranger.richtext.LinkKey
            import androidx.compose.ui.Modifier
        """.trimIndent()

        val result = SpotlessCustomRules.noFullyQualifiedNames(content)
        assertEquals(content, result)
    }

    @Test
    fun `allows import alias for fully qualified names`() {
        val content = """
            import dev.mkeeda.arranger.richtext.LinkKey as ArrangerLinkKey
        """.trimIndent()

        val result = SpotlessCustomRules.noFullyQualifiedNames(content)
        assertEquals(content, result)
    }

    @Test
    fun `fails on fully qualified name in body`() {
        val content = """
            import androidx.compose.ui.Modifier
            
            val key = dev.mkeeda.arranger.richtext.LinkKey
        """.trimIndent()

        assertFailsWith<AssertionError> {
            SpotlessCustomRules.noFullyQualifiedNames(content)
        }
    }

    @Test
    fun `fails on androidx compose fully qualified name in body`() {
        val content = """
            fun applyModifier(modifier: androidx.compose.ui.Modifier) {
                // ...
            }
        """.trimIndent()

        assertFailsWith<AssertionError> {
            SpotlessCustomRules.noFullyQualifiedNames(content)
        }
    }
}
