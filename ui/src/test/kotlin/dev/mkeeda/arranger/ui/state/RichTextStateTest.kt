package dev.mkeeda.arranger.ui.state

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.insert
import dev.mkeeda.arranger.core.text.RichString
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichTextStateTest {
    @Test
    fun `inserts simple text without attributes`() {
        val state = RichTextState(initialText = RichString(text = "Hello World"))

        state.simulateTextEdit {
            insert(5, " My")
        }

        state.textFieldState.text.toString() shouldBe "Hello My World"
        state.richString.text shouldBe "Hello My World"
    }
}

internal fun RichTextState.simulateTextEdit(block: TextFieldBuffer.() -> Unit) {
    textFieldState.edit {
        block()
        updateRichString(this)
    }
}
