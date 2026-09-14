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

/**
 * A basic text editor component tailored for editing and displaying RichText content.
 *
 * @param state The [RichTextState] holding the text and its attributes.
 * @param modifier The modifier to be applied to the text field.
 * @param styleResolver A resolver that specifies how [AttributeContainer]s
 * should be translated into visually rendered Compose [SpanStyle]s.
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
    onLinkClick: ((String) -> Unit)? = null,
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
        onLinkClick = onLinkClick,
    )
}
