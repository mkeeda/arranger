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

        var newSpans = spans

        for (i in 0 until buffer.changes.changeCount) {
            val originalRange = buffer.changes.getOriginalRange(i)
            val range = buffer.changes.getRange(i)

            // Sub-step 2 handles basic insertions: originalRange is empty, range is not empty
            if (originalRange.collapsed && !range.collapsed) {
                val insertPos = originalRange.start
                val length = range.length

                newSpans =
                    newSpans.map { span ->
                        val spanStart = span.range.first
                        val spanEnd = span.range.last

                        when {
                            insertPos <= spanStart -> {
                                // Inserted before or exactly at the start of the span
                                // Shift the entire span to the right
                                span.copy(range = (spanStart + length)..(spanEnd + length))
                            }
                            insertPos <= spanEnd -> {
                                // Inserted inside the span or exactly at its end
                                // Expand the span
                                span.copy(range = spanStart..(spanEnd + length))
                            }
                            else -> {
                                // Inserted after the span
                                span
                            }
                        }
                    }
            }
        }

        this.spans = newSpans
    }
}
