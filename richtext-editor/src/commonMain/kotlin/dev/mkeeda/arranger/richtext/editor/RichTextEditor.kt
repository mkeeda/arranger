package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.RichSpan

/**
 * A basic text editor component tailored for editing and displaying RichText content.
 *
 * @param state The [RichTextState] holding the text and its attributes.
 * @param modifier The modifier to be applied to the text field.
 * @param enabled Controls the enabled state of the text field. When false, the text field is not focusable or editable, and pointer interactions are disabled.
 * @param readOnly Controls the editable state of the text field. When true, the text cannot be modified, but it can still be focused and spans can still be clicked.
 * @param textStyle The [TextStyle] to be applied to the text.
 * @param keyboardOptions Software keyboard options that configure keyboard behaviors such as keyboard type and IME action.
 * @param onKeyboardAction Called when the user triggers an IME action on the software keyboard.
 * @param lineLimits Whether the text field should be single line, or multi line with specific bounds.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param scrollState Scroll state that manages the vertical scroll position of the editor content.
 * @param interactionSource The [MutableInteractionSource] representing the stream of interactions for this text field.
 * @param cursorBrush The brush used to draw the cursor.
 * @param decorator Allows modifying how the text field is drawn with decorations around it (e.g. placeholder, border).
 * @param styleResolver A resolver that specifies how [AttributeContainer]s should be translated into visually rendered Compose [SpanStyle]s.
 * @param attributeStyleResolver Alternative/alias parameter for [styleResolver] for backward compatibility.
 * @param listMarkerResolver A resolver that specifies how list markers (e.g. bullets, ordered numbers) should be rendered.
 * @param onSpanClick Optional callback invoked when a [RichSpan] is tapped or clicked.
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
    styleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
    attributeStyleResolver: AttributeStyleResolver = styleResolver,
    listMarkerResolver: ListMarkerResolver = DefaultListMarkerResolver,
    onSpanClick: ((SpanClickEvent) -> Unit)? = null,
) {
    val effectiveStyleResolver =
        if (attributeStyleResolver !== DefaultAttributeStyleResolver) {
            attributeStyleResolver
        } else {
            styleResolver
        }

    BaseRichTextEditor(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = null,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = onTextLayout,
        scrollState = scrollState,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorator = decorator,
        styleResolver = effectiveStyleResolver,
        listMarkerResolver = listMarkerResolver,
        onSpanClick = onSpanClick,
    )
}
