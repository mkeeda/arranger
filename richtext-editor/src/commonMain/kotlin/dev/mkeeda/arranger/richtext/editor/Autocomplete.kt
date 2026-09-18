package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import kotlin.math.roundToInt

/**
 * Defines a trigger pattern for autocompletion (e.g. '@' for mentions, '#' for tags, ':' for emojis).
 *
 * @property prefix The prefix string that activates this trigger (e.g. "@", "#", ":").
 * @property id Unique identifier for this trigger. Defaults to [prefix].
 * @property requireLeadingWhitespace Whether the character preceding [prefix] must be whitespace or start of line/text. Default is true.
 * @property allowSpacesInQuery Whether the query text typed after [prefix] can contain spaces. Default is false.
 * @property maxQueryLength The maximum allowed character count of the query before this trigger is deactivated. Default is 50.
 */
@Immutable
public data class AutocompleteTrigger(
    val prefix: String,
    val id: String = prefix,
    val requireLeadingWhitespace: Boolean = true,
    val allowSpacesInQuery: Boolean = false,
    val maxQueryLength: Int = 50,
) {
    public constructor(
        character: Char,
        id: String = character.toString(),
        requireLeadingWhitespace: Boolean = true,
        allowSpacesInQuery: Boolean = false,
        maxQueryLength: Int = 50,
    ) : this(
        prefix = character.toString(),
        id = id,
        requireLeadingWhitespace = requireLeadingWhitespace,
        allowSpacesInQuery = allowSpacesInQuery,
        maxQueryLength = maxQueryLength,
    )
}

/**
 * Represents an active autocompletion trigger match at the current cursor position.
 *
 * @property trigger The [AutocompleteTrigger] that matched.
 * @property query The search query typed after the trigger prefix up to the cursor.
 * @property range The full range from the trigger prefix start to the cursor position (e.g., 0..4 for "@user").
 * @property queryRange The range corresponding to the [query] string.
 * @property cursorPosition The position of the cursor when this match occurred.
 * @property cursorRect The bounding rectangle of the cursor in local viewport coordinates (scroll-adjusted), if available.
 */
@Immutable
public data class AutocompleteMatch(
    val trigger: AutocompleteTrigger,
    val query: String,
    val range: IntRange,
    val queryRange: IntRange,
    val cursorPosition: Int,
    val cursorRect: Rect? = null,
)

/**
 * Detects whether an active autocompletion trigger pattern is present immediately before the cursor.
 *
 * Scans backwards from [cursorPosition] within the current line to find the nearest trigger defined in [triggers].
 *
 * @param text The full text content.
 * @param cursorPosition The current cursor index.
 * @param triggers The list of autocomplete triggers to evaluate.
 * @return An [AutocompleteMatch] if a trigger pattern is actively matched, or `null` otherwise.
 */
public fun detectAutocomplete(
    text: CharSequence,
    cursorPosition: Int,
    triggers: List<AutocompleteTrigger>,
): AutocompleteMatch? {
    if (triggers.isEmpty() || cursorPosition <= 0 || cursorPosition > text.length) {
        return null
    }

    val lineStart = (text.lastIndexOf('\n', cursorPosition - 1) + 1).coerceAtLeast(0)

    for (i in (cursorPosition - 1) downTo lineStart) {
        val matchingTrigger =
            triggers.firstOrNull { trigger ->
                text.regionMatches(i, trigger.prefix, 0, trigger.prefix.length)
            } ?: continue

        val prefixEnd = i + matchingTrigger.prefix.length
        if (prefixEnd > cursorPosition) continue

        val query = text.subSequence(prefixEnd, cursorPosition).toString()
        if (query.length > matchingTrigger.maxQueryLength) continue

        if (!matchingTrigger.allowSpacesInQuery && query.any { it.isWhitespace() }) {
            continue
        }

        if (matchingTrigger.requireLeadingWhitespace) {
            val isAtStart = i == 0
            val isPrecededByWhitespace = !isAtStart && text[i - 1].isWhitespace()
            if (!isAtStart && !isPrecededByWhitespace) {
                continue
            }
        }

        return AutocompleteMatch(
            trigger = matchingTrigger,
            query = query,
            range = i until cursorPosition,
            queryRange = prefixEnd until cursorPosition,
            cursorPosition = cursorPosition,
        )
    }

    return null
}

/**
 * Creates a [PopupPositionProvider] that positions a suggestion popup below (or above if constrained)
 * the cursor of this [AutocompleteMatch].
 *
 * The popup position automatically prevents overflowing the window boundaries and accounts for viewport scrolling.
 *
 * @param offset Additional pixel offset applied to the popup position.
 */
public fun AutocompleteMatch.createPopupPositionProvider(
    offset: IntOffset = IntOffset.Zero,
): PopupPositionProvider {
    val localCursorRect = this.cursorRect
    return object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val cursorLeft = localCursorRect?.left?.roundToInt() ?: 0
            val cursorTop = localCursorRect?.top?.roundToInt() ?: 0
            val cursorBottom = localCursorRect?.bottom?.roundToInt() ?: anchorBounds.height

            val windowCursorLeft = anchorBounds.left + cursorLeft
            val windowCursorTop = anchorBounds.top + cursorTop
            val windowCursorBottom = anchorBounds.top + cursorBottom

            var x = windowCursorLeft + offset.x
            val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
            x = x.coerceIn(0, maxX)

            val spaceBelow = windowSize.height - (windowCursorBottom + offset.y)
            val spaceAbove = windowCursorTop + offset.y

            val y =
                if (spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove) {
                    windowCursorBottom + offset.y
                } else {
                    windowCursorTop + offset.y - popupContentSize.height
                }

            val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
            val clampedY = y.coerceIn(0, maxY)

            return IntOffset(x, clampedY)
        }
    }
}

/**
 * Replaces the range matched by [match] with the given [replacement] plain text,
 * and positions the cursor immediately after the replacement.
 */
public fun RichTextState.applyCompletion(
    match: AutocompleteMatch,
    replacement: String,
) {
    undoState.captureSnapshot(UndoMergePolicy.Separate)
    val replaceStart = match.range.first
    val replaceEnd = match.range.last + 1
    val newCursor = replaceStart + replacement.length
    edit {
        replace(replaceStart until replaceEnd, replacement)
        selection = TextRange(newCursor)
    }
}

/**
 * Replaces the range matched by [match] with the given [replacement] [RichString],
 * preserving its styling attributes and positioning the cursor immediately after the replacement.
 */
@Suppress("UNCHECKED_CAST")
public fun RichTextState.applyCompletion(
    match: AutocompleteMatch,
    replacement: RichString,
) {
    undoState.captureSnapshot(UndoMergePolicy.Separate)
    val replaceStart = match.range.first
    val replaceEnd = match.range.last + 1
    val newCursor = replaceStart + replacement.text.length
    edit {
        replace(replaceStart until replaceEnd, replacement.text)
        replacement.spans.forEach { span ->
            val shiftedStart = replaceStart + span.range.first
            val shiftedEnd = replaceStart + span.range.last
            val shiftedRange = shiftedStart..shiftedEnd
            for (key in span.attributes.keys) {
                val typedKey = key as AttributeKey<Any>
                val value = span.attributes.getOrDefault(typedKey)
                if (key is SpanAttributeKey<*>) {
                    setSpanAttribute(key as SpanAttributeKey<Any>, value, shiftedRange)
                } else if (key is ParagraphAttributeKey<*>) {
                    setParagraphAttribute(key as ParagraphAttributeKey<Any>, value, shiftedRange)
                }
            }
        }
        selection = TextRange(newCursor)
    }
}

/**
 * Replaces the range matched by [match] with the given [replacement] text,
 * applies attributes using the specified [attributes] to the replacement, and positions the cursor immediately after it.
 */
@Suppress("UNCHECKED_CAST")
public fun RichTextState.applyCompletion(
    match: AutocompleteMatch,
    replacement: String,
    attributes: AttributeContainer,
) {
    undoState.captureSnapshot(UndoMergePolicy.Separate)
    val replaceStart = match.range.first
    val replaceEnd = match.range.last + 1
    val newCursor = replaceStart + replacement.length
    val newRange = replaceStart until newCursor
    edit {
        replace(replaceStart until replaceEnd, replacement)
        if (!newRange.isEmpty()) {
            for (key in attributes.keys) {
                val typedKey = key as AttributeKey<Any>
                val value = attributes.getOrDefault(typedKey)
                if (key is SpanAttributeKey<*>) {
                    setSpanAttribute(key as SpanAttributeKey<Any>, value, newRange)
                } else if (key is ParagraphAttributeKey<*>) {
                    setParagraphAttribute(key as ParagraphAttributeKey<Any>, value, newRange)
                }
            }
        }
        selection = TextRange(newCursor)
    }
}
