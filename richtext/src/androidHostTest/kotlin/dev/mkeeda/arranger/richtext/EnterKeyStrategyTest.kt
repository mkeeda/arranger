package dev.mkeeda.arranger.richtext

import io.kotest.matchers.shouldBe
import org.junit.Test

class EnterKeyStrategyTest {
    @Test
    fun `InheritParagraphStrategy returns InheritAttributes`() {
        val currentAttributes = attributeContainerOf(TextAlignmentKey to TextAlignment.Center)
        val context =
            EnterKeyContext(
                text = "Hello\n",
                cursorPosition = 5,
                paragraphRange = 0..5,
                currentAttributes = currentAttributes,
            )

        val result = InheritParagraphStrategy.execute(context)
        result shouldBe EnterKeyResult.InheritAttributes(currentAttributes)
    }

    @Test
    fun `HeadingEnterStrategy returns ClearAttributes`() {
        val currentAttributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)
        val context =
            EnterKeyContext(
                text = "Heading\n",
                cursorPosition = 7,
                paragraphRange = 0..7,
                currentAttributes = currentAttributes,
            )

        val result = HeadingEnterStrategy.execute(context)
        result shouldBe EnterKeyResult.ClearAttributes
    }

    @Test
    fun `ListEnterStrategy with text returns InheritAttributes`() {
        val currentAttributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
        val context =
            EnterKeyContext(
                text = "List item\n",
                cursorPosition = 9,
                paragraphRange = 0..9,
                currentAttributes = currentAttributes,
            )

        val result = ListEnterStrategy.execute(context)
        result shouldBe EnterKeyResult.InheritAttributes(currentAttributes)
    }

    @Test
    fun `ListEnterStrategy empty Level1 returns Outdent with empty attributes`() {
        val currentAttributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
        val context =
            EnterKeyContext(
                text = "\n",
                cursorPosition = 0,
                paragraphRange = 0..0,
                currentAttributes = currentAttributes,
            )

        val result = ListEnterStrategy.execute(context)
        val expectedAttributes = AttributeContainer.empty()
        result shouldBe EnterKeyResult.Outdent(expectedAttributes)
    }

    @Test
    fun `ListEnterStrategy empty Level2 returns Outdent`() {
        val currentAttributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level2)
        val context =
            EnterKeyContext(
                text = "\n",
                cursorPosition = 0,
                paragraphRange = 0..0,
                currentAttributes = currentAttributes,
            )

        val result = ListEnterStrategy.execute(context)
        val expectedAttributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
        result shouldBe EnterKeyResult.Outdent(expectedAttributes)
    }

    @Test
    fun `ListEnterStrategy empty Level3 returns Outdent`() {
        val currentAttributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level3)
        val context =
            EnterKeyContext(
                text = "Item 1\n\n",
                cursorPosition = 7,
                paragraphRange = 7..7,
                currentAttributes = currentAttributes,
            )

        val result = ListEnterStrategy.execute(context)
        val expectedAttributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)
        result shouldBe EnterKeyResult.Outdent(expectedAttributes)
    }
}
