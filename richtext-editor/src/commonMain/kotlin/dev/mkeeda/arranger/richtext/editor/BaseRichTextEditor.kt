package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import dev.mkeeda.arranger.richtext.ListItem
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.VisualBlock
import dev.mkeeda.arranger.richtext.extractListItems
import dev.mkeeda.arranger.richtext.extractVisualBlocks
import kotlin.math.roundToInt

/**
 * Shared internal base text editor component encapsulating the underlying BasicTextField setup,
 * decoration, cursorBrush, link tapping, list rendering, keyboard shortcuts, and input transformation.
 */
@Composable
internal fun BaseRichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Unspecified),
    decorator: TextFieldDecorator? = null,
    styleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
    listMarkerResolver: ListMarkerResolver = DefaultListMarkerResolver,
    blockDecorator: BlockDecorator = DefaultBlockDecorator,
    onSpanClick: ((SpanClickEvent) -> Unit)? = null,
    autocompleteTriggers: List<AutocompleteTrigger> = emptyList(),
    onAutocompleteChange: ((AutocompleteMatch?) -> Unit)? = null,
) {
    val workarounds = remember { ComposeParagraphWorkarounds() }

    val outputTransformation =
        remember(state, styleResolver, workarounds) {
            RichTextOutputTransformation(state, styleResolver, workarounds)
        }

    val defaultInputTransformation =
        remember(state) {
            RichTextInputTransformation(state)
        }
    val effectiveInputTransformation = inputTransformation ?: defaultInputTransformation

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val internalOnTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit) = { getResult ->
        textLayoutResult = getResult()
        onTextLayout?.invoke(this, getResult)
    }

    val textMeasurer = rememberTextMeasurer()
    val currentTextStyle = textStyle.copy(color = textStyle.color.takeOrElse { Color.Black })
    val effectiveCursorBrush = resolveEffectiveCursorBrush(cursorBrush = cursorBrush, textStyle = textStyle)

    val listItems = remember(state.richString) { state.richString.extractListItems() }
    val visualBlocks = remember(state.richString) { state.richString.extractVisualBlocks() }

    val currentOnSpanClick by rememberUpdatedState(onSpanClick)
    val currentOnAutocompleteChange by rememberUpdatedState(onAutocompleteChange)

    if (autocompleteTriggers.isNotEmpty() && onAutocompleteChange != null) {
        LaunchedEffect(
            state.textFieldState.text,
            state.textFieldState.selection,
            scrollState.value,
            textLayoutResult,
            autocompleteTriggers,
        ) {
            if (!state.selection.collapsed) {
                currentOnAutocompleteChange?.invoke(null)
                return@LaunchedEffect
            }

            val match =
                detectAutocomplete(
                    text = state.textFieldState.text,
                    cursorPosition = state.selection.start,
                    triggers = autocompleteTriggers,
                )

            if (match == null) {
                currentOnAutocompleteChange?.invoke(null)
                return@LaunchedEffect
            }

            val layout = textLayoutResult
            val cursorRect =
                layout?.let {
                    val cursor = state.selection.start.coerceIn(0, it.layoutInput.text.length)
                    val mappedCursor =
                        workarounds.mapCharacterIndex(cursor)
                            .coerceIn(0, it.layoutInput.text.length)
                    val rawRect = it.getCursorRect(mappedCursor)
                    val scrollY = scrollState.value.toFloat()
                    Rect(rawRect.left, rawRect.top - scrollY, rawRect.right, rawRect.bottom - scrollY)
                }

            currentOnAutocompleteChange?.invoke(match.copy(cursorRect = cursorRect))
        }

        DisposableEffect(onAutocompleteChange, autocompleteTriggers) {
            onDispose {
                currentOnAutocompleteChange?.invoke(null)
            }
        }
    }

    val spanTapModifier =
        if (enabled) {
            remember(state, workarounds) {
                Modifier.spanTapHandler(
                    state = state,
                    textLayoutResultProvider = { textLayoutResult },
                    workarounds = workarounds,
                    onSpanClickProvider = { currentOnSpanClick },
                )
            }
        } else {
            Modifier
        }

    val drawModifier =
        Modifier
            .clipToBounds()
            .then(spanTapModifier)
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
        inputTransformation = effectiveInputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = internalOnTextLayout,
        scrollState = scrollState,
        interactionSource = interactionSource,
        cursorBrush = effectiveCursorBrush,
        outputTransformation = outputTransformation,
        decorator = { innerTextField ->
            val decoratedContent: @Composable () -> Unit = {
                if (decorator != null) {
                    decorator.Decoration(innerTextField)
                } else {
                    innerTextField()
                }
            }
            if (visualBlocks.isNotEmpty()) {
                Box(propagateMinConstraints = true) {
                    BlockDecorationsOverlay(
                        visualBlocks = visualBlocks,
                        blockDecorator = blockDecorator,
                        textLayoutResult = textLayoutResult,
                        scrollState = scrollState,
                        workarounds = workarounds,
                        modifier = Modifier.matchParentSize(),
                    )
                    decoratedContent()
                }
            } else {
                decoratedContent()
            }
        },
    )
}

internal fun resolveEffectiveCursorBrush(
    cursorBrush: Brush,
    textStyle: TextStyle,
): Brush =
    if (cursorBrush == SolidColor(Color.Unspecified)) {
        val effectiveColor = textStyle.color.takeOrElse { Color.Black }
        SolidColor(effectiveColor)
    } else {
        cursorBrush
    }

internal fun DrawScope.drawListItems(
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

internal fun Modifier.spanTapHandler(
    state: RichTextState,
    textLayoutResultProvider: () -> TextLayoutResult?,
    workarounds: ComposeParagraphWorkarounds,
    onSpanClickProvider: () -> ((SpanClickEvent) -> Unit)?,
): Modifier =
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToUp() && !change.isConsumed) {
                    val tapOffset = change.position
                    val layoutResult = textLayoutResultProvider() ?: continue
                    val rawOffset = layoutResult.getOffsetForPosition(tapOffset)
                    val unmappedOffset = workarounds.unmapCharacterIndex(rawOffset)
                    val targetSpan =
                        state.richString.spans.find { span ->
                            unmappedOffset in span.range
                        }
                    val onSpanClick = onSpanClickProvider()
                    if (targetSpan != null && onSpanClick != null) {
                        val clickEvent = SpanClickEvent(span = targetSpan)
                        onSpanClick(clickEvent)
                        if (clickEvent.isConsumed) {
                            change.consume()
                        }
                    }
                }
            }
        }
    }

@Composable
internal fun BlockDecorationsOverlay(
    visualBlocks: List<VisualBlock>,
    blockDecorator: BlockDecorator,
    textLayoutResult: TextLayoutResult?,
    scrollState: ScrollState,
    workarounds: ComposeParagraphWorkarounds,
    modifier: Modifier = Modifier,
) {
    val layout = textLayoutResult ?: return
    val totalLength = layout.layoutInput.text.length
    if (totalLength == 0 || layout.size.width <= 0) return

    val density = LocalDensity.current

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            visualBlocks.forEach { block ->
                val startOffset = workarounds.mapCharacterIndex(block.range.first).coerceIn(0, totalLength)
                val rawEndOffset = workarounds.mapCharacterIndex(block.range.last).coerceIn(0, totalLength)
                val effectiveEndOffset =
                    if (rawEndOffset > startOffset && layout.layoutInput.text.getOrNull(rawEndOffset) == '\n') {
                        rawEndOffset - 1
                    } else {
                        rawEndOffset
                    }

                val startLine = layout.getLineForOffset(startOffset)
                val endLine = layout.getLineForOffset(effectiveEndOffset)
                val blockTop = layout.getLineTop(startLine)
                val blockBottom = layout.getLineBottom(endLine)
                val lineCount = maxOf(1, endLine - startLine + 1)
                val editorWidth = layout.size.width.toFloat()

                val bounds =
                    Rect(
                        left = 0f,
                        top = blockTop,
                        right = editorWidth,
                        bottom = blockBottom,
                    )

                var minLeft = Float.MAX_VALUE
                var maxRight = 0f
                for (line in startLine..endLine) {
                    minLeft = minOf(minLeft, layout.getLineLeft(line))
                    maxRight = maxOf(maxRight, layout.getLineRight(line))
                }
                val textBounds =
                    Rect(
                        left = if (minLeft == Float.MAX_VALUE) 0f else minLeft,
                        top = blockTop,
                        right = maxRight,
                        bottom = blockBottom,
                    )

                val context =
                    BlockDecorationContext(
                        bounds = bounds,
                        textBounds = textBounds,
                        lineCount = lineCount,
                        modifier =
                            Modifier.size(
                                width = with(density) { bounds.width.toDp() },
                                height = with(density) { bounds.height.toDp() },
                            ),
                    )

                blockDecorator.Decoration(block = block, context = context)
            }
        },
    ) { measurables, constraints ->
        val placeables =
            measurables.mapIndexed { index, measurable ->
                val block = visualBlocks.getOrNull(index)
                val startOffset =
                    block?.let { workarounds.mapCharacterIndex(it.range.first).coerceIn(0, totalLength) } ?: 0
                val rawEndOffset =
                    block?.let { workarounds.mapCharacterIndex(it.range.last).coerceIn(0, totalLength) } ?: 0
                val effectiveEndOffset =
                    if (rawEndOffset > startOffset && layout.layoutInput.text.getOrNull(rawEndOffset) == '\n') {
                        rawEndOffset - 1
                    } else {
                        rawEndOffset
                    }

                val startLine = layout.getLineForOffset(startOffset)
                val endLine = layout.getLineForOffset(effectiveEndOffset)
                val blockTop = layout.getLineTop(startLine)
                val blockBottom = layout.getLineBottom(endLine)
                val blockHeight = (blockBottom - blockTop).roundToInt().coerceAtLeast(0)
                val blockWidth = layout.size.width

                val childConstraints = Constraints.fixed(width = blockWidth, height = blockHeight)
                measurable.measure(childConstraints)
            }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val scrollY = scrollState.value
            placeables.forEachIndexed { index, placeable ->
                val block = visualBlocks.getOrNull(index) ?: return@forEachIndexed
                val startOffset = workarounds.mapCharacterIndex(block.range.first).coerceIn(0, totalLength)
                val startLine = layout.getLineForOffset(startOffset)
                val blockTop = layout.getLineTop(startLine).roundToInt()

                placeable.place(x = 0, y = blockTop - scrollY)
            }
        }
    }
}
