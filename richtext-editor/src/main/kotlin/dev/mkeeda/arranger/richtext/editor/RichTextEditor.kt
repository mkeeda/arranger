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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import dev.mkeeda.arranger.richtext.BulletListItem
import dev.mkeeda.arranger.richtext.ListItem
import dev.mkeeda.arranger.richtext.OrderedListItem
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.extractListItems

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
    styleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
) {
    val outputTransformation =
        remember(state, styleResolver) {
            RichTextOutputTransformation(state, styleResolver)
        }

    val inputTransformation =
        remember(state) {
            RichTextInputTransformation(state)
        }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val internalOnTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit) = { getResult ->
        textLayoutResult = getResult()
        onTextLayout?.invoke(this, getResult)
    }

    val textMeasurer = rememberTextMeasurer()
    val currentTextStyle = textStyle.copy(color = textStyle.color.takeOrElse { Color.Black })

    val listItems = remember(state.richString) { state.richString.extractListItems() }

    val drawModifier =
        Modifier.drawBehind {
            val layoutResult = textLayoutResult ?: return@drawBehind

            val outerDrawScope = this
            translate(top = -scrollState.value.toFloat()) {
                drawListItems(listItems, layoutResult, textMeasurer, currentTextStyle)
            }
        }

    BasicTextField(
        state = state.textFieldState,
        modifier = modifier.then(drawModifier),
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = internalOnTextLayout,
        scrollState = scrollState,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        outputTransformation = outputTransformation,
        decorator = decorator,
    )
}

private fun DrawScope.drawListItems(
    listItems: List<ListItem>,
    layoutResult: TextLayoutResult,
    textMeasurer: TextMeasurer,
    currentTextStyle: TextStyle,
) {
    listItems.forEach { item ->
        val line = layoutResult.getLineForOffset(item.textIndex)
        val top = layoutResult.getLineTop(line)
        val bottom = layoutResult.getLineBottom(line)
        val yCenter = top + (bottom - top) / 2f

        val levelIndex = item.indentLevel.ordinal + 1
        val previousIndentPx = (levelIndex - 1) * 24f * density * fontScale
        val xCenter = previousIndentPx + 12f * density * fontScale

        val itemColor = item.color
        val textColor =
            if (itemColor != null && itemColor != RgbaColor.Unspecified) {
                itemColor.toColor()
            } else {
                currentTextStyle.color
            }

        val markerText =
            when (item) {
                is BulletListItem -> "・"
                is OrderedListItem -> "${item.index}."
            }

        val textLayout = textMeasurer.measure(markerText, style = currentTextStyle.copy(color = textColor))
        val canvas = drawContext.canvas
        canvas.save()
        canvas.translate(dx = xCenter - textLayout.size.width / 2f, dy = yCenter - textLayout.size.height / 2f)
        TextPainter.paint(canvas = canvas, textLayoutResult = textLayout)
        canvas.restore()
    }
}

private class RichTextOutputTransformation(
    private val state: RichTextState,
    private val styleResolver: AttributeStyleResolver,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        for (span in state.richString.spans) {
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
