package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.clearBulletList
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichTextStateTest {
    @Test
    fun `inserts simple text without attributes`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.edit {
            insertAfter(substring = "Hello", textToInsert = " My")
        }

        val expectedText = "Hello My World"
        state.textFieldState.text.toString() shouldBe expectedText
        state.richString.text shouldBe expectedText
    }

    @Test
    fun `inserts text before an attribute range`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.edit {
            insertBefore(substring = "Hello", textToInsert = "Oh, ")
        }

        val expectedText = "Oh, Hello World"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("World")
    }

    @Test
    fun `inserts text inside an attribute range`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.edit {
            insertAfter(substring = "Wor", textToInsert = "!")
        }

        val expectedText = "Hello Wor!ld"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("Wor!ld")
    }

    @Test
    fun `inserts text after an attribute range - does not inherit`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello"))
                    },
            )

        state.edit {
            insertAfter(substring = "World", textToInsert = "!")
        }

        val expectedText = "Hello World!"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("Hello")
    }

    @Test
    fun `inserts text exactly at the end of an attribute range - inherits attributes`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello"))
                    },
            )

        state.edit {
            // Insert exactly at spanEnd + 1
            insertAfter(substring = "Hello", textToInsert = "!")
        }

        val expectedText = "Hello! World"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // Span should expand to include "!"
        spans.first().range shouldBe expectedText.rangeOf("Hello!")
    }

    @Test
    fun `deletes text before an attribute range`() {
        val initialText = "Oh, Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.edit {
            deleteSubstring("Oh, ")
        }

        val expectedText = "Hello World"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("World")
    }

    @Test
    fun `deletes text inside an attribute range`() {
        val initialText = "Hello Wor!ld"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Wor!ld"))
                    },
            )

        state.edit {
            deleteSubstring("!")
        }

        val expectedText = "Hello World"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("World")
    }

    @Test
    fun `deletes entire text of an attribute range`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.edit {
            deleteSubstring("World")
        }

        val expectedText = "Hello "
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.isEmpty() shouldBe true
    }

    @Test
    fun `deleting a line break snaps the paragraph span to the new paragraph boundaries`() {
        val initialText = "Alpha\nBravo\nCharlie"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(BlockquoteKey, Unit, range = initialText.rangeOf("Bravo"))
                    },
            )

        // Initial span should be "Bravo\n" (indices 6..11)
        state.richString.spans.first().range shouldBe (6..11)

        state.edit {
            // Delete the '\n' between Alpha and Bravo
            deleteSubstring("pha\nBr")
            val insertIndex = text.indexOf("avo\nCharlie")
            insert(insertIndex, "phaBr")
        }

        val expectedText = "AlphaBravo\nCharlie"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // Expected span is the new first paragraph: "AlphaBravo\n" (indices 0..10)
        spans.first().range shouldBe (0..10)
    }

    @Test
    fun `inserting a line break inside a paragraph span expands the span to cover both new paragraphs`() {
        val initialText = "Alpha\nBravo"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(BlockquoteKey, Unit, range = initialText.rangeOf("Bravo"))
                    },
            )

        // Initial span should be "Bravo" (indices 6..11)
        state.richString.spans.first().range shouldBe (6..11)

        state.edit {
            // Insert a '\n' in the middle of Bravo
            insertAfter(substring = "Bra", textToInsert = "\n")
        }

        val expectedText = "Alpha\nBra\nvo"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // Expected span is the combined new paragraphs: "Bra\nvo" (indices 6..12)
        spans.first().range shouldBe (6..12)
    }

    @Test
    fun `deleting text inside a paragraph span maintains the span boundaries`() {
        val initialText = "Alpha\nBravo\nCharlie"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(BlockquoteKey, Unit, range = initialText.rangeOf("Bravo"))
                    },
            )

        state.edit {
            deleteSubstring("av")
        }

        val expectedText = "Alpha\nBro\nCharlie"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // Expected span is "Bro\n" (indices 6..9)
        spans.first().range shouldBe (6..9)
    }

    @Test
    fun `span attribute is unaffected by paragraph re-snapping`() {
        val initialText = "Alpha\nBravo"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Bravo"))
                        setParagraphAttribute(BlockquoteKey, Unit, range = initialText.rangeOf("Bravo"))
                    },
            )

        state.edit {
            // Delete the '\n' between Alpha and Bravo
            // Instead of overlapping Bravo, we delete just '\n' and insert a space
            delete(5..5) // index 5 is '\n'
            insert(5, " ")
        }

        val expectedText = "Alpha Bravo"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans

        // Due to SpanMerger, the spans are tessellated chunks.
        val firstSpan = spans[0]
        val secondSpan = spans[1]
        firstSpan.attributes.containsKey(BlockquoteKey) shouldBe true
        firstSpan.attributes.containsKey(BoldKey) shouldBe false

        secondSpan.range shouldBe (6..10)
        secondSpan.attributes.containsKey(BlockquoteKey) shouldBe true
        secondSpan.attributes.containsKey(BoldKey) shouldBe true

        // Paragraph attributes are now extended to text.length (11)
        spans.size shouldBe 3
        val thirdSpan = spans[2]
        thirdSpan.range shouldBe (11..11)
        thirdSpan.attributes.containsKey(BlockquoteKey) shouldBe true
        thirdSpan.attributes.containsKey(BoldKey) shouldBe false
    }

    @Test
    fun `atomic text insertion applies attributes immediately`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.edit {
            val insertIndex = text.indexOf("World")
            insert(insertIndex, "Beautiful ", editAction = {
                setSpanAttribute(BoldKey, Unit)
            })
        }

        val expectedText = "Hello Beautiful World"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("Beautiful ")
        spans.first().attributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `currentAttributes returns attributes at cursor position`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        // Cursor inside "World" (e.g., after 'W')
        val cursorPosition = initialText.indexOf("World") + 1
        state.textFieldState.edit {
            selection = TextRange(cursorPosition)
        }

        val attrs = state.currentAttributes
        attrs shouldBe attributeContainerOf(BoldKey to Unit)
    }

    @Test
    fun `currentAttributes returns empty when cursor is at position 0`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello"))
                    },
            )

        state.textFieldState.edit {
            selection = TextRange(0)
        }

        state.currentAttributes shouldBe attributeContainerOf()
    }

    @Test
    fun `currentAttributes returns empty when text is empty`() {
        val state = RichTextState(initialText = RichString(text = ""))
        state.currentAttributes shouldBe attributeContainerOf()
    }

    @Test
    fun `currentAttributes returns intersection of attributes when selection is not collapsed`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        // "Hello " is Bold, "World" is Bold and Italic
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello World"))
                        setSpanAttribute(ItalicKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        // Select "lo Wo"
        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("lo"), initialText.indexOf("rld"))
        }

        // Only Bold is common across the entire selection
        state.currentAttributes shouldBe attributeContainerOf(BoldKey to Unit)
    }

    @Test
    fun `currentAttributes reflects typing attributes`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }

        state.setTypingAttribute(BoldKey, Unit)

        val attrs = state.currentAttributes
        attrs shouldBe attributeContainerOf(BoldKey to Unit)
    }

    @Test
    fun `typing attributes override cursor attributes in currentAttributes`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        // Cursor at the end of "World", where Bold is present
        state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }

        state.currentAttributes.containsKey(BoldKey) shouldBe true

        // Override with typing attribute
        val newColor = RgbaColor(0xFFFF0000)
        state.setTypingAttribute(TextColorKey, newColor)

        val attrs = state.currentAttributes
        attrs shouldBe
            attributeContainerOf(
                BoldKey to Unit,
                TextColorKey to newColor,
            )
    }

    @Test
    fun `typing attributes can be removed`() {
        val state = RichTextState(initialText = RichString(text = ""))

        state.setTypingAttribute(BoldKey, Unit)
        state.currentAttributes.containsKey(BoldKey) shouldBe true

        state.removeTypingAttribute(BoldKey)
        state.currentAttributes shouldBe attributeContainerOf()
    }

    @Test
    fun `typing attributes can be cleared`() {
        val state = RichTextState(initialText = RichString(text = ""))

        state.setTypingAttribute(BoldKey, Unit)
        state.clearTypingAttributes()
        state.currentAttributes shouldBe attributeContainerOf()
    }

    @Test
    fun `typing newline inherits paragraph attributes to the new empty paragraph`() {
        val initialText = "List Item"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, range = initialText.indices)
                    },
            )

        // Move cursor to the end
        state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }

        // Type a newline character
        state.textFieldState.edit {
            replace(initialText.length, initialText.length, "\n")
            selection = TextRange(initialText.length + 1)
            state.updateRichString(this)
        }

        val expectedText = "List Item\n"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // The span should cover up to the end of the text (including the new empty paragraph at index 10)
        spans.first().range shouldBe (0..10)
    }

    @Test
    fun `toggling off paragraph attribute on empty paragraph removes it`() {
        val initialText = "List Item\n"
        val state =
            RichTextState(
                // Span covers 0..10, which includes the empty paragraph at index 10
                initialText =
                    RichString(
                        text = initialText,
                        spans =
                            listOf(
                                RichSpan(
                                    range = 0..10,
                                    attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1),
                                ),
                            ),
                    ),
            )

        // Cursor is at the empty paragraph (index 10)
        state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }

        // Toggle off the paragraph attribute.
        // It should call removeParagraphAttribute.
        state.edit {
            editAttributes(state.selection) {
                clearBulletList()
            }
        }

        val spans = state.richString.spans
        spans.size shouldBe 1
        // The span should now only cover the first paragraph (0..9)
        spans.first().range shouldBe (0..9)
    }

    @Test
    fun `does not inherit removed paragraph attributes upon newline`() {
        val initialText = "Bullet item"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, initialText.indices)
                    },
            )

        // Select the end of the line
        state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }

        // Toggle bullet list off (explicitly disables it)
        state.removeTypingAttribute(BulletListKey)

        // Type a newline and some text
        state.textFieldState.edit {
            replace(length, length, "\nNew line")
        }

        val expectedText = "Bullet item\nNew line"
        state.richString.text shouldBe expectedText

        // Only the first line should have the bullet attribute, since it was disabled before the newline
        val spans = state.richString.spans
        spans.size shouldBe 1
        // The span snaps to the paragraph boundary which includes the newline (length of "Bullet item\n" is 12 -> 0..11)
        spans.first().range shouldBe (0..11)
        spans.first().attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
    }
}

// --- Test Helpers ---

private fun RichTextBuffer.insertAfter(substring: String, textToInsert: String) {
    val index = text.indexOf(substring)
    require(index >= 0) { "Substring '$substring' not found in '$text'" }
    insert(index + substring.length, textToInsert)
}

private fun RichTextBuffer.insertBefore(substring: String, textToInsert: String) {
    val index = text.indexOf(substring)
    require(index >= 0) { "Substring '$substring' not found in '$text'" }
    insert(index, textToInsert)
}

private fun RichTextBuffer.deleteSubstring(substring: String) {
    val index = text.indexOf(substring)
    require(index >= 0) { "Substring '$substring' not found in '$text'" }
    delete(index until (index + substring.length))
}
