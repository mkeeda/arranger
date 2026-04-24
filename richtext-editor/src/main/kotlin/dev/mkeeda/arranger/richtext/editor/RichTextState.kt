package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.RichStringBuffer

@Stable
public class RichTextState(initialText: RichString) {
    internal val textFieldState = TextFieldState(initialText.text)

    // The Single Source of Truth for spans
    private var spans: List<RichSpan> by mutableStateOf(initialText.spans)

    // Computed property representing the complete rich text state
    public val richString: RichString
        get() =
            RichString(
                text = textFieldState.text.toString(),
                spans = spans,
            )

    public fun edit(block: RichStringBuffer.() -> Unit) {
        spans = richString.edit(block).spans
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun updateRichString(buffer: TextFieldBuffer) {
        if (buffer.changes.changeCount == 0) return

        this.spans =
            (0 until buffer.changes.changeCount).fold(spans) { currentSpans, i ->
                val originalRange = buffer.changes.getOriginalRange(i)
                val range = buffer.changes.getRange(i)

                val editStart = originalRange.min
                val editEnd = originalRange.max
                val offsetDiff = range.length - originalRange.length

                currentSpans.mapNotNull { span ->
                    shiftSpan(
                        span = span,
                        editStart = editStart,
                        editEnd = editEnd,
                        newLength = range.length,
                        offsetDiff = offsetDiff,
                    )
                }
            }
    }

    private fun shiftSpan(
        span: RichSpan,
        editStart: Int,
        editEnd: Int,
        newLength: Int,
        offsetDiff: Int,
    ): RichSpan? {
        val spanStart = span.range.first
        val spanEnd = span.range.last

        return when {
            editEnd <= spanStart -> {
                // Edit happens entirely before the span. Shift it securely.
                span.copy(range = (spanStart + offsetDiff)..(spanEnd + offsetDiff))
            }
            editStart > spanEnd -> {
                // Edit happens entirely after the span. Unaffected.
                span
            }
            else -> {
                // Edit overlaps with the span.
                val newStart =
                    when {
                        spanStart < editStart -> spanStart
                        spanStart >= editEnd -> spanStart + offsetDiff
                        else -> editStart
                    }

                val newEnd =
                    when {
                        spanEnd < editStart -> spanEnd
                        spanEnd >= editEnd -> spanEnd + offsetDiff
                        else -> editStart + newLength - 1
                    }

                if (newStart > newEnd) {
                    null
                } else {
                    span.copy(range = newStart..newEnd)
                }
            }
        }
    }
}
