package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
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
