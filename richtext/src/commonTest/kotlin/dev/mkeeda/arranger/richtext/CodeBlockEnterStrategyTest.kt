package dev.mkeeda.arranger.richtext

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CodeBlockEnterStrategyTest {
    @Test
    fun `CodeBlockEnterStrategy with text returns InheritAttributes`() {
        val currentAttributes = attributeContainerOf(CodeBlockKey to "kotlin")
        val context =
            EnterKeyContext(
                text = "val x = 1\n",
                cursorPosition = 9,
                paragraphRange = 0..9,
                currentAttributes = currentAttributes,
            )

        val result = CodeBlockEnterStrategy.execute(context)
        result shouldBe EnterKeyResult.InheritAttributes(attributes = currentAttributes)
    }

    @Test
    fun `CodeBlockEnterStrategy with empty line returns Outdent removing CodeBlockKey`() {
        val currentAttributes = attributeContainerOf(CodeBlockKey to "kotlin")
        val context =
            EnterKeyContext(
                text = "\n",
                cursorPosition = 0,
                paragraphRange = 0..0,
                currentAttributes = currentAttributes,
            )

        val result = CodeBlockEnterStrategy.execute(context)
        result shouldBe EnterKeyResult.Outdent(attributes = AttributeContainer.empty())
    }

    @Test
    fun `CodeBlockEnterStrategy with whitespace-only line returns Outdent removing CodeBlockKey`() {
        val currentAttributes = attributeContainerOf(CodeBlockKey to null)
        val context =
            EnterKeyContext(
                text = "    \n",
                cursorPosition = 4,
                paragraphRange = 0..4,
                currentAttributes = currentAttributes,
            )

        val result = CodeBlockEnterStrategy.execute(context)
        result shouldBe EnterKeyResult.Outdent(attributes = AttributeContainer.empty())
    }

    @Test
    fun `CodeBlockEnterStrategy with empty line preserves non-block attributes on Outdent`() {
        val currentAttributes =
            attributeContainerOf(
                CodeBlockKey to "kotlin",
                TextAlignmentKey to TextAlignment.Center,
            )
        val context =
            EnterKeyContext(
                text = "\n",
                cursorPosition = 0,
                paragraphRange = 0..0,
                currentAttributes = currentAttributes,
            )

        val result = CodeBlockEnterStrategy.execute(context)
        val expectedAttributes = attributeContainerOf(TextAlignmentKey to TextAlignment.Center)
        result shouldBe EnterKeyResult.Outdent(attributes = expectedAttributes)
    }
}
