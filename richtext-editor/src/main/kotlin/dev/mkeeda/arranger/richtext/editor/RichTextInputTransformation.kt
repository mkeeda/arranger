package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

internal class RichTextInputTransformation(
    private val state: RichTextState,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        state.updateRichString(this)
    }
}
