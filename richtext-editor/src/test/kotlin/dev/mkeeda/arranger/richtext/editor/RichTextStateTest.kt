package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichTextStateTest {
    @Test
    fun `inserts simple text without attributes`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.simulateTextEdit {
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

        state.simulateTextEdit {
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

        state.simulateTextEdit {
            insertAfter(substring = "Wor", textToInsert = "!")
        }

        val expectedText = "Hello Wor!ld"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("Wor!ld")
    }

    @Test
    fun `inserts text after an attribute range`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello"))
                    },
            )

        state.simulateTextEdit {
            insertAfter(substring = "World", textToInsert = "!")
        }

        val expectedText = "Hello World!"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.rangeOf("Hello")
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

        state.simulateTextEdit {
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

        state.simulateTextEdit {
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

        state.simulateTextEdit {
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

        state.simulateTextEdit {
            // Delete the '\n' between Alpha and Bravo
            deleteSubstring("pha\nBr")
            insert(originalText.indexOf("pha\nBr"), "phaBr")
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

        // Initial span should be "Bravo" (indices 6..10)
        state.richString.spans.first().range shouldBe (6..10)

        state.simulateTextEdit {
            // Insert a '\n' in the middle of Bravo
            insertAfter(substring = "Bra", textToInsert = "\n")
        }

        val expectedText = "Alpha\nBra\nvo"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // Expected span is the combined new paragraphs: "Bra\nvo" (indices 6..11)
        spans.first().range shouldBe (6..11)
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

        state.simulateTextEdit {
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

        state.simulateTextEdit {
            // Delete the '\n' between Alpha and Bravo
            // Instead of overlapping Bravo, we delete just '\n' and insert a space
            delete(5, 6)
            insert(5, " ")
        }

        val expectedText = "Alpha Bravo"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 2

        // Due to SpanMerger, the spans are tessellated chunks.
        val firstSpan = spans[0]
        val secondSpan = spans[1]

        firstSpan.range shouldBe (0..5)
        firstSpan.attributes.containsKey(BlockquoteKey) shouldBe true
        firstSpan.attributes.containsKey(BoldKey) shouldBe false

        secondSpan.range shouldBe (6..10)
        secondSpan.attributes.containsKey(BlockquoteKey) shouldBe true
        secondSpan.attributes.containsKey(BoldKey) shouldBe true
    }
}

// --- Test Helpers ---

internal fun RichTextState.simulateTextEdit(block: TextFieldBuffer.() -> Unit) {
    textFieldState.edit {
        block()
        updateRichString(this)
    }
}

private fun TextFieldBuffer.insertAfter(substring: String, textToInsert: String) {
    val index = originalText.indexOf(substring)
    require(index >= 0) { "Substring '$substring' not found in '$originalText'" }
    insert(index + substring.length, textToInsert)
}

private fun TextFieldBuffer.insertBefore(substring: String, textToInsert: String) {
    val index = originalText.indexOf(substring)
    require(index >= 0) { "Substring '$substring' not found in '$originalText'" }
    insert(index, textToInsert)
}

private fun TextFieldBuffer.deleteSubstring(substring: String) {
    val index = originalText.indexOf(substring)
    require(index >= 0) { "Substring '$substring' not found in '$originalText'" }
    delete(index, index + substring.length)
}
