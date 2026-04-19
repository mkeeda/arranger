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
        runs[0] shouldBe RichRun(text = "Line2\n", range = 6..11, value = Unit)
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
        runs[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = Unit)
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

    @Test
    fun `paragraph DSL methods correctly snap and apply attributes`() {
        val richString = RichString(text = paragraphText)

        val actual = richString.edit {
            editAttributes(range = 0..0) {
                headingLevel(HeadingLevel.H1)
            }
            editAttributes(range = 8..8) {
                blockquote()
            }
            editAttributes(range = 14..14) {
                textAlignment(TextAlignment.Center)
            }
        }

        val headingRuns = actual.runs(HeadingKey)
        headingRuns shouldHaveSize 1
        headingRuns[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = HeadingLevel.H1)

        val blockquoteRuns = actual.runs(BlockquoteKey)
        blockquoteRuns shouldHaveSize 1
        blockquoteRuns[0] shouldBe RichRun(text = "Line2\n", range = 6..11, value = Unit)

        val alignRuns = actual.runs(TextAlignmentKey)
        alignRuns shouldHaveSize 1
        alignRuns[0] shouldBe RichRun(text = "Line3", range = 12..16, value = TextAlignment.Center)
    }

    @Test
    fun `paragraph DSL clear methods correctly remove attributes`() {
        val richString = RichString(text = paragraphText).edit {
            // Apply attributes to the entire text
            editAttributes(range = paragraphText.indices) {
                headingLevel(HeadingLevel.H1)
                blockquote()
                textAlignment(TextAlignment.Center)
            }
        }

        val actual = richString.edit {
            // Clear attributes only from the middle paragraph ("Line2")
            val line2Start = paragraphText.indexOf("Line2")
            editAttributes(range = line2Start..line2Start) {
                clearHeadingLevel()
                clearBlockquote()
                clearTextAlignment()
            }
        }

        // The first paragraph ("Line1\n") and the third paragraph ("Line3") 
        // should retain the attributes. The middle paragraph ("Line2\n") is cleared.
        val headingRuns = actual.runs(HeadingKey)
        headingRuns shouldHaveSize 2
        headingRuns[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = HeadingLevel.H1)
        headingRuns[1] shouldBe RichRun(text = "Line3", range = 12..16, value = HeadingLevel.H1)
    }
}
