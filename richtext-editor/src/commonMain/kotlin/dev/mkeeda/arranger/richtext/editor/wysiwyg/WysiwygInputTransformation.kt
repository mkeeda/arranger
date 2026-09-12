package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import dev.mkeeda.arranger.richtext.editor.RichTextState

@OptIn(ExperimentalFoundationApi::class)
internal class WysiwygInputTransformation(
    private val state: RichTextState,
    private val wysiwygState: WysiwygState,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) {
            state.updateRichString(this)
            wysiwygState.clearLastAutoFormat()
            return
        }

        val lastChar = extractLastChar(this)

        // New input occurred; clear any prior auto-format event
        wysiwygState.clearLastAutoFormat()

        // 1. Regular RichTextState update (State A -> State B span shift, typing attribute application)
        state.updateRichString(this)

        // 2. Evaluate block-level auto-formatting
        val blockEvent = WysiwygAutoFormatter.formatBlockIfMatched(this, state, lastChar)
        if (blockEvent != null) {
            wysiwygState.recordAutoFormat(blockEvent)
            return
        }

        // 3. Evaluate inline-level auto-formatting
        val inlineEvent = WysiwygAutoFormatter.formatInlineIfMatched(this, state, lastChar)
        if (inlineEvent != null) {
            wysiwygState.recordAutoFormat(inlineEvent)
            return
        }
    }

    private fun extractLastChar(buffer: TextFieldBuffer): Char? {
        if (buffer.changes.changeCount == 0) return null
        val lastRange = buffer.changes.getRange(buffer.changes.changeCount - 1)
        if (lastRange.length == 0) return null
        return buffer.asCharSequence()[lastRange.max - 1]
    }
}
