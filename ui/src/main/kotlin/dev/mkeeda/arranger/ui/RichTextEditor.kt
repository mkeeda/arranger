package dev.mkeeda.arranger.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A basic text editor component tailored for editing and displaying RichText content.
 *
 * @param state The [RichTextState] holding the text and its attributes.
 * @param modifier The modifier to be applied to the text field.
 * @param styleResolver A resolver that specifies how [dev.mkeeda.arranger.core.text.AttributeContainer]s
 * should be translated into visually rendered Compose [androidx.compose.ui.text.SpanStyle]s.
 */
@Composable
public fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    styleResolver: AttributeStyleResolver = AttributeStyleResolver { null },
) {
    val outputTransformation =
        remember(state, styleResolver) {
            RichTextOutputTransformation(state, styleResolver)
        }

    val inputTransformation =
        remember(state) {
            RichTextInputTransformation(state)
        }

    BasicTextField(
        state = state.textFieldState,
        modifier = modifier,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
    )
}

private class RichTextOutputTransformation(
    private val state: RichTextState,
    private val styleResolver: AttributeStyleResolver,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        for (span in state.richString.getSpans()) {
            val resolved = styleResolver.resolve(span.attributes)

            // Determine boundaries avoiding out of bounds in case of race conditions or text shrinkage mid-frame.
            val start = span.range.first.coerceIn(0, length)
            val end = (span.range.last + 1).coerceIn(0, length)

            if (start < end) {
                resolved.spanStyle?.let { style ->
                    addStyle(style, start, end)
                }
                resolved.paragraphStyle?.let { style ->
                    addStyle(style, start, end)
                }
            }
        }
    }
}

private class RichTextInputTransformation(
    private val state: RichTextState,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        state.updateRichString(this)
    }
}
