package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.BaseRichTextEditor
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.DefaultListMarkerResolver
import dev.mkeeda.arranger.richtext.editor.ListMarkerResolver
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.SpanClickEvent

/**
 * A WYSIWYG text editor component that automatically formats Markdown syntax into rich text styling in real time.
 *
 * @param state The [RichTextState] holding the text and its attributes.
 * @param modifier The modifier to be applied to the text field.
 * @param enabled Controls the enabled state of the text field.
 * @param readOnly Controls whether the editor is in read-only mode.
 * @param textStyle The default text style applied to the text field.
 * @param keyboardOptions Software keyboard options.
 * @param onKeyboardAction Called when the user performs an action on the software keyboard.
 * @param lineLimits Whether the text field is single line or multiline.
 * @param onTextLayout Callback invoked when text layout is calculated.
 * @param scrollState The scroll state for scrolling text.
 * @param interactionSource The [MutableInteractionSource] representing the stream of Interactions for this text field.
 * @param cursorBrush The brush used to draw the cursor.
 * @param decorator Allows adding decorations around the text field.
 * @param styleResolver A resolver that specifies how [AttributeContainer]s should be translated into visually rendered Compose styles.
 * @param listMarkerResolver A resolver that specifies how list markers should be rendered.
 * @param onSpanClick Optional callback invoked when a [dev.mkeeda.arranger.richtext.RichSpan] is tapped or clicked.
 */
@Composable
public fun WysiwygEditor(
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
    listMarkerResolver: ListMarkerResolver = DefaultListMarkerResolver,
    onSpanClick: ((SpanClickEvent) -> Unit)? = null,
) {
    val wysiwygState = remember(state) { WysiwygState() }

    val transformation =
        remember(state, wysiwygState) {
            WysiwygInputTransformation(state, wysiwygState)
        }

    val keyModifier =
        remember(state, wysiwygState) {
            Modifier.onPreviewKeyEvent { event ->
                handleWysiwygKeyEvent(event, state, wysiwygState)
            }
        }

    BaseRichTextEditor(
        state = state,
        modifier = modifier.then(keyModifier),
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = transformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = onTextLayout,
        scrollState = scrollState,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorator = decorator,
        styleResolver = styleResolver,
        listMarkerResolver = listMarkerResolver,
        onSpanClick = onSpanClick,
    )
}

internal fun handleWysiwygKeyEvent(
    event: KeyEvent,
    state: RichTextState,
    wysiwygState: WysiwygState,
): Boolean {
    return handleWysiwygKey(
        isKeyDown = event.type == KeyEventType.KeyDown,
        key = event.key,
        state = state,
        wysiwygState = wysiwygState,
    )
}

internal fun handleWysiwygKey(
    isKeyDown: Boolean,
    key: Key,
    state: RichTextState,
    wysiwygState: WysiwygState,
): Boolean {
    if (isKeyDown && key == Key.Backspace) {
        return wysiwygState.revert(state)
    }
    return false
}
