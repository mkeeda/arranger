package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class ParagraphSnappingTest {
    @Test
    fun `snapToParagraphs expands range to paragraph boundaries`() {
        val text = "Line1\nLine2\nLine3"

        // Index 8 is 'n' in "Line2". Should snap to "Line2\n" (6..11)
        (8..8).snapToParagraphs(text) shouldBe (6..11)

        // Index 0 is 'L' in "Line1". Should snap to "Line1\n" (0..5)
        (0..0).snapToParagraphs(text) shouldBe (0..5)

        // Last index is in "Line3". Should snap to "Line3" (12..16)
        (text.lastIndex..text.lastIndex).snapToParagraphs(text) shouldBe (12..16)

        // Spanning across paragraphs (from 'n' in Line1 to 'L' in Line3)
        (2..12).snapToParagraphs(text) shouldBe (0..16)
    }

    @Test
    fun `resnapParagraphSpans adjusts ranges of spans with ParagraphAttributeKey`() {
        val text = "Line1\nLine2\nLine3"

        val attributes = attributeContainerOf(BlockquoteKey to Unit)
        // Simulate a shifted span that doesn't align with paragraph boundaries
        // E.g. (8..10) which is inside "Line2"
        val shiftedSpan = RichSpan(range = 8..10, attributes = attributes)

        val resnappedSpans = listOf(shiftedSpan).resnapParagraphSpans(text)

        resnappedSpans shouldHaveSize 1
        // It should expand to the full paragraph "Line2\n"
        resnappedSpans.first().range shouldBe (6..11)
    }

    @Test
    fun `resnapParagraphSpans ignores spans with only SpanAttributeKey`() {
        val text = "Line1\nLine2\nLine3"

        val attributes = attributeContainerOf(BoldKey to Unit)
        // A span applied only to the word "Line2"
        val span = RichSpan(range = 6..10, attributes = attributes)

        val resnappedSpans = listOf(span).resnapParagraphSpans(text)

        resnappedSpans shouldHaveSize 1
        // Range should remain unchanged
        resnappedSpans.first().range shouldBe (6..10)
    }

    @Test
    fun `resnapParagraphSpans removes paragraph spans when text is empty`() {
        val text = ""

        val paragraphSpan = RichSpan(range = 0..5, attributes = attributeContainerOf(BlockquoteKey to Unit))
        val textSpan = RichSpan(range = 0..5, attributes = attributeContainerOf(BoldKey to Unit))

        val resnappedSpans = listOf(paragraphSpan, textSpan).resnapParagraphSpans(text)

        resnappedSpans shouldHaveSize 1
        // Only the SpanAttributeKey span should remain
        resnappedSpans.first().attributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `resnapParagraphSpans clamps out of bounds ranges before snapping`() {
        val text = "Line1"

        val attributes = attributeContainerOf(BlockquoteKey to Unit)
        // A span whose range exceeds the text length
        val outOfBoundsSpan = RichSpan(range = 0..100, attributes = attributes)

        val resnappedSpans = listOf(outOfBoundsSpan).resnapParagraphSpans(text)

        resnappedSpans shouldHaveSize 1
        // Range should be clamped to valid text length
        resnappedSpans.first().range shouldBe (0..4)
    }
}
