package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichStringEditParagraphTest {
    private val paragraphText = "Line1\nLine2\nLine3"

    @Test
    fun `setParagraphAttribute snaps given range to paragraph boundaries`() {
        val richString = RichString(text = paragraphText)

        val actual =
            richString.edit {
                // Index 8 is 'n' in "Line2"
                setParagraphAttribute(BlockquoteKey, Unit, 8..8)
            }

        // "Line2" is from index 6 to 10. "Line1\n" is 0..5, "Line2\n" is 6..11.
        val runs = actual.runs(BlockquoteKey).toList()
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = "Line2\n", range = 6..11, value = Unit)
    }

    @Test
    fun `setParagraphAttribute snaps correctly at the start of text`() {
        val richString = RichString(text = paragraphText)

        val actual =
            richString.edit {
                // Index 0 is 'L' in "Line1"
                setParagraphAttribute(BlockquoteKey, Unit, 0..0)
            }

        val runs = actual.runs(BlockquoteKey).toList()
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = Unit)
    }

    @Test
    fun `setParagraphAttribute snaps correctly at the end of text`() {
        val richString = RichString(text = paragraphText)

        val actual =
            richString.edit {
                // Last index is "Line3"
                setParagraphAttribute(BlockquoteKey, Unit, paragraphText.lastIndex..paragraphText.lastIndex)
            }

        val runs = actual.runs(BlockquoteKey).toList()
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = "Line3", range = 12..16, value = Unit)
    }

    @Test
    fun `setParagraphAttribute snaps safely across multiple paragraphs`() {
        val richString = RichString(text = paragraphText)

        val actual =
            richString.edit {
                // Include 'n' in Line1 (index 2) to 'L' in Line3 (index 12)
                setParagraphAttribute(BlockquoteKey, Unit, 2..12)
            }

        val runs = actual.runs(BlockquoteKey).toList()
        runs shouldHaveSize 1
        runs[0] shouldBe RichRun(text = paragraphText, range = 0..16, value = Unit)
    }

    @Test
    fun `paragraph DSL methods correctly snap and apply attributes`() {
        val richString = RichString(text = paragraphText)

        val actual =
            richString.edit {
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

        val headingRuns = actual.runs(HeadingKey).toList()
        headingRuns shouldHaveSize 1
        headingRuns[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = HeadingLevel.H1)

        val blockquoteRuns = actual.runs(BlockquoteKey).toList()
        blockquoteRuns shouldHaveSize 1
        blockquoteRuns[0] shouldBe RichRun(text = "Line2\n", range = 6..11, value = Unit)

        val alignRuns = actual.runs(TextAlignmentKey).toList()
        alignRuns shouldHaveSize 1
        alignRuns[0] shouldBe RichRun(text = "Line3", range = 12..16, value = TextAlignment.Center)
    }

    @Test
    fun `paragraph DSL clear methods correctly remove attributes`() {
        val richString =
            RichString(text = paragraphText).edit {
                // Apply attributes to the entire text
                editAttributes(range = paragraphText.indices) {
                    headingLevel(HeadingLevel.H1)
                    textAlignment(TextAlignment.Center)
                }
            }

        val actual =
            richString.edit {
                // Clear attributes only from the middle paragraph ("Line2")
                val line2Start = paragraphText.indexOf("Line2")
                editAttributes(range = line2Start..line2Start) {
                    clearHeadingLevel()
                    clearTextAlignment()
                }
            }

        // The first paragraph ("Line1\n") and the third paragraph ("Line3")
        // should retain the attributes. The middle paragraph ("Line2\n") is cleared.
        val headingRuns = actual.runs(HeadingKey).toList()
        headingRuns shouldHaveSize 2
        headingRuns[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = HeadingLevel.H1)
        headingRuns[1] shouldBe RichRun(text = "Line3", range = 12..16, value = HeadingLevel.H1)
    }

    @Test
    fun `setParagraphAttribute excludes attributes of the same category`() {
        val richString = RichString(text = "Paragraph")

        val actual =
            richString.edit {
                // Setup: apply OrderedList (BlockType)
                editAttributes {
                    orderedList(ListIndentLevel.Level1)
                }
                // Action: apply Heading (BlockType)
                editAttributes {
                    headingLevel(HeadingLevel.H1)
                }
            }

        val orderedListRuns = actual.runs(OrderedListKey).toList()
        orderedListRuns shouldHaveSize 0

        val headingRuns = actual.runs(HeadingKey).toList()
        headingRuns shouldHaveSize 1
        headingRuns[0] shouldBe RichRun(text = "Paragraph", range = 0..8, value = HeadingLevel.H1)
    }

    @Test
    fun `setParagraphAttribute coexists with attributes of different categories`() {
        val richString = RichString(text = "Paragraph")

        val actual =
            richString.edit {
                // Setup: apply Heading (BlockType)
                editAttributes {
                    headingLevel(HeadingLevel.H1)
                }
                // Action: apply TextAlignment (Alignment)
                editAttributes {
                    textAlignment(TextAlignment.Center)
                }
            }

        val headingRuns = actual.runs(HeadingKey).toList()
        headingRuns shouldHaveSize 1
        headingRuns[0] shouldBe RichRun(text = "Paragraph", range = 0..8, value = HeadingLevel.H1)

        val alignRuns = actual.runs(TextAlignmentKey).toList()
        alignRuns shouldHaveSize 1
        alignRuns[0] shouldBe RichRun(text = "Paragraph", range = 0..8, value = TextAlignment.Center)
    }

    @Test
    fun `setParagraphAttribute correctly splits spans when applied to middle paragraph`() {
        val richString = RichString(text = paragraphText) // Line1\nLine2\nLine3

        val actual =
            richString.edit {
                // Setup: apply OrderedList to all
                editAttributes {
                    orderedList(ListIndentLevel.Level1)
                }
                // Action: apply Heading to the middle paragraph only
                val line2Start = paragraphText.indexOf("Line2")
                editAttributes(range = line2Start..line2Start) {
                    headingLevel(HeadingLevel.H2)
                }
            }

        val orderedListRuns = actual.runs(OrderedListKey).toList()
        orderedListRuns shouldHaveSize 2
        orderedListRuns[0] shouldBe RichRun(text = "Line1\n", range = 0..5, value = ListIndentLevel.Level1)
        orderedListRuns[1] shouldBe RichRun(text = "Line3", range = 12..16, value = ListIndentLevel.Level1)

        val headingRuns = actual.runs(HeadingKey).toList()
        headingRuns shouldHaveSize 1
        headingRuns[0] shouldBe RichRun(text = "Line2\n", range = 6..11, value = HeadingLevel.H2)
    }

    @Test
    fun `setParagraphAttribute is idempotent when reapplying the same attribute`() {
        val richString = RichString(text = "Paragraph")

        val actual =
            richString.edit {
                // Setup: apply Heading (BlockType)
                editAttributes {
                    headingLevel(HeadingLevel.H1)
                }
                // Action: apply Heading again
                editAttributes {
                    headingLevel(HeadingLevel.H1)
                }
            }

        val headingRuns = actual.runs(HeadingKey).toList()
        headingRuns shouldHaveSize 1
        headingRuns[0] shouldBe RichRun(text = "Paragraph", range = 0..8, value = HeadingLevel.H1)
    }
}
