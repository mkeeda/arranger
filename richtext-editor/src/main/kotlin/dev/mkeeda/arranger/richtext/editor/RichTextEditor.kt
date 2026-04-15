package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density

/**
 * A basic text editor component tailored for editing and displaying RichText content.
 *
 * @param state The [RichTextState] holding the text and its attributes.
 * @param modifier The modifier to be applied to the text field.
 * @param styleResolver A resolver that specifies how [dev.mkeeda.arranger.richtext.AttributeContainer]s
 * should be translated into visually rendered Compose [androidx.compose.ui.text.SpanStyle]s.
 */
@Composable
public fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null,
    styleResolver: AttributeStyleResolver = AttributeStyleResolver { },
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
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = onTextLayout,
        scrollState = scrollState,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        outputTransformation = outputTransformation,
        decorator = decorator,
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
                    addStyle(spanStyle = style, start = start, end = end)
                }
                resolved.paragraphStyle?.let { style ->
                    addStyle(paragraphStyle = style, start = start, end = end)
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
