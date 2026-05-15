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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import dev.mkeeda.arranger.richtext.ListItem
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

    val drawModifier =
        Modifier
            .clipToBounds()
            .drawBehind {
                val layoutResult = textLayoutResult ?: return@drawBehind

                translate(top = -scrollState.value.toFloat()) {
                    drawListItems(listItems, layoutResult, textMeasurer, currentTextStyle, listMarkerResolver, workarounds)
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isCommandOrCtrl = event.isCtrlPressed || event.isMetaPressed
                    if (isCommandOrCtrl) {
                        when {
                            event.key == Key.Z && event.isShiftPressed -> {
                                if (state.canRedo) state.redo()
                                true
                            }

                            event.key == Key.Z -> {
                                if (state.canUndo) state.undo()
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    } else {
                        false
                    }
                } else {
                    false
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

internal class ComposeParagraphWorkarounds {
    private var emptyParagraphIndices: List<Int> = emptyList()

    /**
     * Maps an original character index to its new index in the transformed text buffer.
     * This is used when targeting a specific character that may have been shifted by insertions.
     */
    fun mapCharacterIndex(originalIndex: Int): Int {
        return originalIndex + emptyParagraphIndices.count { it <= originalIndex }
    }

    /**
     * Maps a style boundary offset to its new position in the transformed text buffer.
     * This is used for ranges where we want the style to cover newly inserted workaround characters.
     */
    fun mapStyleOffset(originalOffset: Int): Int {
        return originalOffset + emptyParagraphIndices.count { it < originalOffset }
    }

    fun apply(
        buffer: TextFieldBuffer,
        getParagraphStyleAt: (Int) -> ParagraphStyle?,
    ) {
        // Reset at the start so callers always see a consistent state in case of mid-frame reads
        emptyParagraphIndices = emptyList()

        val originalText = buffer.asCharSequence().toString()
        val originalLength = originalText.length

        // Workaround 1: Compose TextLayoutResult ignores ParagraphStyle for completely empty paragraphs.
        // This breaks cursor positioning (e.g. list indentation) when typing a newline at the end of a list item.
        // We find all empty paragraphs and insert a Zero-Width Space (\u200B) to force the style application.
        // We iterate backwards to avoid index shifting during insertion.
        val emptyIndices = mutableListOf<Int>()
        for (i in originalLength downTo 0) {
            val isLineEmpty = (i == originalLength || originalText[i] == '\n') && (i == 0 || originalText[i - 1] == '\n')
            if (isLineEmpty && getParagraphStyleAt(i) != null) {
                emptyIndices.add(i)
                buffer.replace(i, i, "\u200B")
            }
        }
        emptyParagraphIndices = emptyIndices

        // Workaround 2: Compose interprets `\n` within or at the end of a `ParagraphStyle` span as a hard paragraph separator.
        // When two adjacent lines have different `ParagraphStyle`s, keeping the `\n` between them causes Compose
        // to render an unintended extra empty line (double spacing).
        // By replacing the boundary `\n` with a zero-width non-breaking space (`\uFEFF`) right before rendering,
        // we eliminate the explicit newline character while letting the style change handle the visual line break.
        var searchStartIndex = 0
        while (true) {
            val i = originalText.indexOf('\n', searchStartIndex)
            if (i == -1) break

            val styleAtI = getParagraphStyleAt(i)
            val styleAtNext = getParagraphStyleAt(i + 1)

            val isBoundary = styleAtI != styleAtNext
            // If the `\n` is the very last character in the text, replacing it would completely remove the trailing
            // empty line (since there is no text after it to break to).
            // We only replace it if there is a `ParagraphStyle` spanning the empty region after the `\n`,
            // because that empty style block itself will force Compose to render the trailing empty line.
            val isSafeToReplace =
                if (i == originalLength - 1) {
                    styleAtNext != null
                } else {
                    true
                }

            if (isBoundary && isSafeToReplace) {
                val mappedI = mapCharacterIndex(i)
                buffer.replace(mappedI, mappedI + 1, "\uFEFF")
            }

            searchStartIndex = i + 1
        }
    }
}

internal class RichTextOutputTransformation(
    private val state: RichTextState,
    private val styleResolver: AttributeStyleResolver,
    private val workarounds: ComposeParagraphWorkarounds,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        // Pre-resolve styles for all spans to avoid redundant object allocations
        val resolvedSpans =
            state.richString.spans.map { span ->
                span to styleResolver.resolve(span.attributes)
            }

        fun getParagraphStyleAt(index: Int): ParagraphStyle? {
            val resolvedSpan = resolvedSpans.find { index in it.first.range }
            return resolvedSpan?.second?.paragraphStyle
        }

        workarounds.apply(this, ::getParagraphStyleAt)

        state.richString.spans.forEach { span ->
            val resolved = resolvedSpans.find { it.first == span }?.second ?: return@forEach
            val originalStart = span.range.first
            val originalEnd = span.range.last + 1

            val start = workarounds.mapStyleOffset(originalStart).coerceIn(0, length)
            val end = workarounds.mapStyleOffset(originalEnd).coerceIn(0, length)

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
