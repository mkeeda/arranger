package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListItem
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.extractListItems

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
    listMarkerResolver: ListMarkerResolver = DefaultListMarkerResolver,
) {
    val workarounds = remember { ComposeParagraphWorkarounds() }

    val outputTransformation =
        remember(state, styleResolver, workarounds) {
            RichTextOutputTransformation(state, styleResolver, workarounds)
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

    val uriHandler = LocalUriHandler.current
    val linkTapModifier =
        remember(state, textLayoutResult, uriHandler, workarounds) {
            Modifier.linkTapHandler(state, textLayoutResult, uriHandler, workarounds)
        }

    val drawModifier =
        Modifier
            .clipToBounds()
            .then(linkTapModifier)
            .drawBehind {
                val layoutResult = textLayoutResult ?: return@drawBehind

                translate(top = -scrollState.value.toFloat()) {
                    drawListItems(listItems, layoutResult, textMeasurer, currentTextStyle, listMarkerResolver, workarounds)
                }
            }
            .richTextKeyboardShortcuts(state)

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
    listMarkerResolver: ListMarkerResolver,
    workarounds: ComposeParagraphWorkarounds,
) {
    listItems.forEach { item ->
        val mappedIndex = workarounds.mapCharacterIndex(item.textIndex)
        val line = layoutResult.getLineForOffset(mappedIndex)
        val top = layoutResult.getLineTop(line)
        val bottom = layoutResult.getLineBottom(line)
        val yCenter = top + (bottom - top) / 2f

        val levelIndex = item.indentLevel.ordinal + 1
        val previousIndentPx = (levelIndex - 1) * ListIndentStepSp * density * fontScale
        val xCenter = previousIndentPx + (ListIndentStepSp / 2f) * density * fontScale

        val itemColor = item.color
        val textColor =
            if (itemColor != null && itemColor != RgbaColor.Unspecified) {
                itemColor.toColor()
            } else {
                currentTextStyle.color
            }

        val markerText = listMarkerResolver.resolve(item)

        val textLayout = textMeasurer.measure(markerText, style = currentTextStyle.copy(color = textColor))
        val canvas = drawContext.canvas
        canvas.save()
        canvas.translate(dx = xCenter - textLayout.size.width / 2f, dy = yCenter - textLayout.size.height / 2f)
        TextPainter.paint(canvas = canvas, textLayoutResult = textLayout)
        canvas.restore()
    }
}

private fun Modifier.linkTapHandler(
    state: RichTextState,
    textLayoutResult: TextLayoutResult?,
    uriHandler: UriHandler,
    workarounds: ComposeParagraphWorkarounds,
): Modifier =
    pointerInput(state, textLayoutResult, uriHandler) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToUp() && !change.isConsumed) {
                    val tapOffset = change.position
                    val layoutResult = textLayoutResult ?: continue
                    val rawOffset = layoutResult.getOffsetForPosition(tapOffset)
                    val unmappedOffset = workarounds.unmapCharacterIndex(rawOffset)
                    val targetSpan =
                        state.richString.spans.find { span ->
                            span.attributes.containsKey(LinkKey) && unmappedOffset in span.range
                        }
                    val url = targetSpan?.attributes?.get(LinkKey)
                    if (!url.isNullOrEmpty()) {
                        change.consume()
                        uriHandler.openUri(url)
                    }
                }
            }
        }
    }
