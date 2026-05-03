package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test

class ListItemExtractorTest {
    @Test
    fun `extract list items bullet list split by newline`() {
        val text = "Item 1\nItem 2\nItem 3"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(
                    key = BulletListKey,
                    value = ListIndentLevel.Level1,
                    range = text.indices,
                )
            }

        val items = richString.extractListItems()

        items.shouldContainExactly(
            BulletListItem(
                textIndex = text.rangeOf("Item 1\n").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
            ),
            BulletListItem(
                textIndex = text.rangeOf("Item 2\n").first, // After "Item 1\n"
                indentLevel = ListIndentLevel.Level1,
                color = null,
            ),
            BulletListItem(
                textIndex = text.rangeOf("Item 3").first, // After "Item 2\n"
                indentLevel = ListIndentLevel.Level1,
                color = null,
            ),
        )
    }

    @Test
    fun `extract list items ordered list count up`() {
        val text = "First\nSecond\nThird"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.indices,
                )
            }

        val items = richString.extractListItems()

        items.shouldContainExactly(
            OrderedListItem(
                textIndex = text.rangeOf("First\n").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 1,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Second\n").first, // After "First\n"
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 2,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Third").first, // After "Second\n"
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 3,
            ),
        )
    }

    @Test
    fun `extract list items ordered list reset on gap`() {
        // "First" is ordered list, "Gap" is plain text, "Second" is ordered list again
        val text = "First\nGap\nSecond"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.rangeOf("First\n"),
                )
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.rangeOf("Second"),
                )
            }

        val items = richString.extractListItems()

        items.shouldContainExactly(
            OrderedListItem(
                textIndex = text.rangeOf("First\n").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 1,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Second").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 1, // Resets because of the gap
            ),
        )
    }

    @Test
    fun `extract list items ordered list reset on nesting`() {
        val text = "Level1-1\nLevel2-1\nLevel2-2\nLevel1-2"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.rangeOf("Level1-1\n"),
                )
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level2,
                    range = text.rangeOf("Level2-1\nLevel2-2\n"),
                )
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.rangeOf("Level1-2"),
                )
            }

        val items = richString.extractListItems()

        items.shouldContainExactly(
            OrderedListItem(
                textIndex = text.rangeOf("Level1-1\n").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 1,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Level2-1\n").first,
                indentLevel = ListIndentLevel.Level2,
                color = null,
                index = 1, // New level starts at 1
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Level2-2\n").first,
                indentLevel = ListIndentLevel.Level2,
                color = null,
                index = 2,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Level1-2").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 2, // Resumes previous level
            ),
        )
    }

    @Test
    fun `extract list items ordered list reset on reentering nesting`() {
        val text = "Level1-1\nLevel2-1\nLevel2-2\nLevel1-2\nLevel2-1"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.rangeOf("Level1-1\n"),
                )
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level2,
                    range = text.rangeOf("Level2-1\nLevel2-2\n"),
                )
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level1,
                    range = text.rangeOf("Level1-2\n"),
                )
                setParagraphAttribute(
                    key = OrderedListKey,
                    value = ListIndentLevel.Level2,
                    range = text.lastIndexOf("Level2-1").let { it until (it + "Level2-1".length) },
                )
            }

        val items = richString.extractListItems()

        items.shouldContainExactly(
            OrderedListItem(
                textIndex = text.rangeOf("Level1-1\n").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 1,
            ),
            OrderedListItem(
                textIndex = text.indexOf("Level2-1\n"),
                indentLevel = ListIndentLevel.Level2,
                color = null,
                index = 1,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Level2-2\n").first,
                indentLevel = ListIndentLevel.Level2,
                color = null,
                index = 2,
            ),
            OrderedListItem(
                textIndex = text.rangeOf("Level1-2\n").first,
                indentLevel = ListIndentLevel.Level1,
                color = null,
                index = 2,
            ),
            OrderedListItem(
                textIndex = text.lastIndexOf("Level2-1"),
                indentLevel = ListIndentLevel.Level2,
                color = null,
                index = 1, // Counter should restart at 1 when re-entering deeper level
            ),
        )
    }

    @Test
    fun `extract list items with text color`() {
        val color = RgbaColor(value = 0xFFFF0000)
        val text = "Red Bullet"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(
                    key = BulletListKey,
                    value = ListIndentLevel.Level1,
                    range = text.indices,
                )
                setSpanAttribute(
                    key = TextColorKey,
                    value = color,
                    range = text.rangeOf("Red"), // Color applied to "Red"
                )
            }

        val items = richString.extractListItems()

        items.shouldContainExactly(
            BulletListItem(
                textIndex = text.rangeOf("Red Bullet").first,
                indentLevel = ListIndentLevel.Level1,
                color = color,
            ),
        )
    }
}
