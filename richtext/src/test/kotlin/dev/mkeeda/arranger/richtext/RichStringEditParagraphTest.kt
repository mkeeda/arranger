package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichStringEditParagraphTest {
    private val paragraphText = "Line1\nLine2\nLine3"

    @Test
    fun `setParagraphAttribute snaps given range to paragraph boundaries`() {
        val richString = RichString(text = paragraphText)
        
        val actual = richString.edit {
            // Index 8 is 'n' in "Line2"
            setParagraphAttribute(BoldKey, Unit, 8..8)
        }

        // "Line2" is from index 6 to 10. "Line1\n" is 0..5, "Line2\n" is 6..11.
        val runs = actual.runs(BoldKey)
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = "Line2", range = 6..10, value = Unit)
    }

    @Test
    fun `setParagraphAttribute snaps correctly at the start of text`() {
        val richString = RichString(text = paragraphText)
        
        val actual = richString.edit {
            // Index 0 is 'L' in "Line1"
            setParagraphAttribute(BoldKey, Unit, 0..0)
        }

        val runs = actual.runs(BoldKey)
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = "Line1", range = 0..4, value = Unit)
    }

    @Test
    fun `setParagraphAttribute snaps correctly at the end of text`() {
        val richString = RichString(text = paragraphText)
        
        val actual = richString.edit {
            // Last index is "Line3"
            setParagraphAttribute(BoldKey, Unit, paragraphText.lastIndex..paragraphText.lastIndex)
        }

        val runs = actual.runs(BoldKey)
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = "Line3", range = 12..16, value = Unit)
    }
    
    @Test
    fun `setParagraphAttribute snaps safely across multiple paragraphs`() {
        val richString = RichString(text = paragraphText)
        
        val actual = richString.edit {
            // Include 'n' in Line1 (index 2) to 'L' in Line3 (index 12)
            setParagraphAttribute(BoldKey, Unit, 2..12)
        }

        val runs = actual.runs(BoldKey)
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = paragraphText, range = 0..16, value = Unit)
    }
}
