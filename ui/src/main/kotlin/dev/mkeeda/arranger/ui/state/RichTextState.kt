package dev.mkeeda.arranger.ui.state

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mkeeda.arranger.core.text.RichSpan
import dev.mkeeda.arranger.core.text.RichString
import dev.mkeeda.arranger.core.text.RichStringBuilder

class RichTextState(initialText: RichString) {
    internal val textFieldState = TextFieldState(initialText.text)

    // The Single Source of Truth for spans
    private var spans: List<RichSpan> by mutableStateOf(initialText.getSpans())

    // Computed property representing the complete rich text state
    val richString: RichString
        get() =
            RichString(
                text = textFieldState.text.toString(),
                spans = spans,
            )

    fun edit(block: RichStringBuilder.() -> Unit) {
        spans = richString.edit(block).getSpans()
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun updateRichString(buffer: TextFieldBuffer) {
        if (buffer.changes.changeCount == 0) return

        // TODO: In subsequent steps, shift spans based on buffer.changes
        // Sub-step 1: Just allow simple insertion, spans remain unchanged (or handled naively)
        // Since Sub-step 1 is without attributes, doing nothing to `spans` is actually correct for now.
    }
}
