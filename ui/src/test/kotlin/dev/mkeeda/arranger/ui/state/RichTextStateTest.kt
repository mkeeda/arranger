package dev.mkeeda.arranger.ui.state

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import dev.mkeeda.arranger.core.text.AttributeKey
import dev.mkeeda.arranger.core.text.RichString
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichTextStateTest {
    private object BoldAttributeKey : AttributeKey<Unit> {
        override val name: String = "Bold"
        override val defaultValue: Unit = Unit
    }

    @Test
    fun `inserts simple text without attributes`() {
        val state = RichTextState(initialText = RichString(text = "Hello World"))

        state.simulateTextEdit {
            insert(5, " My")
        }

        state.textFieldState.text.toString() shouldBe "Hello My World"
        state.richString.text shouldBe "Hello My World"
    }

    @Test
    fun `inserts text before an attribute range`() {
        val initialText =
            RichString(text = "Hello World").edit {
                setAttribute(BoldAttributeKey, Unit, range = 6..10)
            }
        val state = RichTextState(initialText = initialText)

        state.simulateTextEdit {
            insert(0, "Oh, ")
        }

        val spans = state.richString.getSpans()
        spans.size shouldBe 1
        spans.first().range shouldBe 10..14

        state.richString.text shouldBe "Oh, Hello World"
    }

    @Test
    fun `inserts text inside an attribute range`() {
        val initialText =
            RichString(text = "Hello World").edit {
                setAttribute(BoldAttributeKey, Unit, range = 6..10)
            }
        val state = RichTextState(initialText = initialText)

        state.simulateTextEdit {
            insert(9, "!")
        }

        val spans = state.richString.getSpans()
        spans.size shouldBe 1
        spans.first().range shouldBe 6..11

        state.richString.text shouldBe "Hello Wor!ld"
    }

    @Test
    fun `inserts text after an attribute range`() {
        val initialText =
            RichString(text = "Hello World").edit {
                setAttribute(BoldAttributeKey, Unit, range = 0..4)
            }
        val state = RichTextState(initialText = initialText)

        state.simulateTextEdit {
            insert(11, "!")
        }

        val spans = state.richString.getSpans()
        spans.size shouldBe 1
        spans.first().range shouldBe 0..4

        state.richString.text shouldBe "Hello World!"
    }

    @Test
    fun `deletes text before an attribute range`() {
        val initialText =
            RichString(text = "Oh, Hello World").edit {
                setAttribute(BoldAttributeKey, Unit, range = 10..14)
            }
        val state = RichTextState(initialText = initialText)

        state.simulateTextEdit {
            delete(0, 4) // Deletes "Oh, "
        }

        val spans = state.richString.getSpans()
        spans.size shouldBe 1
        spans.first().range shouldBe 6..10

        state.richString.text shouldBe "Hello World"
    }

    @Test
    fun `deletes text inside an attribute range`() {
        val initialText =
            RichString(text = "Hello Wor!ld").edit {
                setAttribute(BoldAttributeKey, Unit, range = 6..11)
            }
        val state = RichTextState(initialText = initialText)

        state.simulateTextEdit {
            delete(9, 10) // Deletes "!"
        }

        val spans = state.richString.getSpans()
        spans.size shouldBe 1
        spans.first().range shouldBe 6..10

        state.richString.text shouldBe "Hello World"
    }

    @Test
    fun `deletes entire text of an attribute range`() {
        val initialText =
            RichString(text = "Hello World").edit {
                setAttribute(BoldAttributeKey, Unit, range = 6..10)
            }
        val state = RichTextState(initialText = initialText)

        state.simulateTextEdit {
            delete(6, 11) // Deletes "World"
        }

        val spans = state.richString.getSpans()
        spans.isEmpty() shouldBe true

        state.richString.text shouldBe "Hello "
    }
}

internal fun RichTextState.simulateTextEdit(block: TextFieldBuffer.() -> Unit) {
    textFieldState.edit {
        block()
        updateRichString(this)
    }
}
