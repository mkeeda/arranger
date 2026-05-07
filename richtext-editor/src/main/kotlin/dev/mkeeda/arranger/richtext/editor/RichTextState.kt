package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.mergeSpan
import dev.mkeeda.arranger.richtext.resnapParagraphSpans

@Stable
public class RichTextState(initialText: RichString) {
    internal val textFieldState = TextFieldState(initialText.text)

    // The Single Source of Truth for spans
    private var spans: List<RichSpan> by mutableStateOf(initialText.spans)

    /**
     * Attributes that will be applied to the next character typed via the keyboard.
     *
     * These attributes only affect user keyboard input — they are NOT applied
     * by programmatic [edit] operations. Use [edit] with explicit attribute
     * parameters (e.g., `insert(index, text) { bold() }`) for programmatic control.
     *
     * Typing attributes are automatically cleared when the cursor position changes
     * without text input (e.g., tapping a different position, arrow key navigation).
     */
    public var typingAttributes: AttributeContainer? by mutableStateOf(null)
        private set

    /**
     * Attributes explicitly removed by the user at the cursor position.
     * When text is typed, these attributes will be forcefully cleared from the new text
     * so that inherited attributes don't apply.
     */
    private var removedTypingAttributes: Set<AttributeKey<*>>? by mutableStateOf(null)

    /**
     * Records the selection state at the exact moment typing attributes were modified.
     * Used to detect true cursor movement vs. non-text-changing buffer updates.
     */
    private var typingAttributesAnchor: TextRange? = null

    // Computed property representing the complete rich text state
    public val richString: RichString
        get() =
            RichString(
                text = textFieldState.text.toString(),
                spans = spans,
            )

    /**
     * The merged attributes at the current cursor position.
     *
     * When [typingAttributes] is set, returns the cursor's inherited attributes
     * overlaid with the typing overrides. Otherwise, returns the attributes of
     * the character immediately before the cursor.
     *
     * Returns [AttributeContainer.empty] when:
     * - The text is empty
     * - The cursor is at position 0 with no typing attributes
     */
    public val currentAttributes: AttributeContainer by derivedStateOf {
        if (!selection.collapsed) {
            val selStart = selection.min
            val selEnd = selection.max
            val selectionLength = selEnd - selStart

            val activeSpans =
                spans.filter { span ->
                    maxOf(selStart, span.range.first) < minOf(selEnd, span.range.last + 1)
                }
            if (activeSpans.isEmpty()) return@derivedStateOf AttributeContainer.empty()

            // Optimization: Instead of filtering spans for every character index (O(n * m)),
            // we calculate the intersection by tracking the number of characters each
            // attribute value covers within the selection.
            // Since spans with the same attribute key do not overlap in the buffer,
            // if an attribute value's coverage length equals the selection length, it's in the intersection.
            val attributeCounts = mutableMapOf<AttributeKey<*>, MutableMap<Any, Int>>()

            for (span in activeSpans) {
                val overlapStart = maxOf(selStart, span.range.first)
                val overlapEnd = minOf(selEnd, span.range.last + 1)
                val overlapLength = overlapEnd - overlapStart

                if (overlapLength > 0) {
                    for (key in span.attributes.keys) {
                        @Suppress("UNCHECKED_CAST")
                        val k = key as AttributeKey<Any>
                        val value = span.attributes.getOrDefault(k)
                        val valueCounts = attributeCounts.getOrPut(k) { mutableMapOf() }
                        valueCounts[value as Any] = valueCounts.getOrDefault(value, 0) + overlapLength
                    }
                }
            }

            var intersection = AttributeContainer.empty()
            for ((key, valueCounts) in attributeCounts) {
                for ((value, count) in valueCounts) {
                    if (count == selectionLength) {
                        @Suppress("UNCHECKED_CAST")
                        val k = key as AttributeKey<Any>
                        intersection += k to value
                    }
                }
            }
            return@derivedStateOf intersection
        }

        val cursorPosition = selection.start
        val typingAttr = typingAttributes
        val removedAttr = removedTypingAttributes

        val inheritedAttributes =
            if (cursorPosition > 0 && cursorPosition <= textFieldState.text.length) {
                val indexBeforeCursor = cursorPosition - 1
                val spansBeforeCursor = spans.filter { indexBeforeCursor in it.range }
                spansBeforeCursor.fold(AttributeContainer.empty()) { acc, span ->
                    acc + span.attributes
                }
            } else {
                AttributeContainer.empty()
            }

        var finalAttrs = inheritedAttributes
        if (typingAttr != null) {
            finalAttrs += typingAttr
        }
        if (removedAttr != null) {
            removedAttr.forEach { key ->
                finalAttrs -= key
            }
        }
        finalAttrs
    }

    /**
     * Adds or overwrites a single attribute in the current typing attributes.
     * Has no effect when a text range is selected (non-collapsed selection).
     *
     * @see typingAttributes
     */
    public fun <T> setTypingAttribute(
        key: AttributeKey<T>,
        value: T,
    ) {
        if (!selection.collapsed) return
        val current = typingAttributes ?: AttributeContainer.empty()
        typingAttributes = current + (key to value)

        val currentRemoved = removedTypingAttributes
        if (currentRemoved != null && currentRemoved.contains(key)) {
            val updated = currentRemoved - key
            removedTypingAttributes = if (updated.isEmpty()) null else updated
        }

        typingAttributesAnchor = selection
    }

    /**
     * Removes a single attribute from the current typing attributes.
     * Has no effect when a text range is selected (non-collapsed selection).
     *
     * @see typingAttributes
     */
    public fun <T> removeTypingAttribute(key: AttributeKey<T>) {
        if (!selection.collapsed) return

        val currentTyping = typingAttributes
        if (currentTyping != null && currentTyping.containsKey(key)) {
            val updated = currentTyping - key
            typingAttributes = if (updated.isEmpty()) null else updated
        } else {
            val currentRemoved = removedTypingAttributes ?: emptySet()
            removedTypingAttributes = currentRemoved + key
        }
        typingAttributesAnchor = selection
    }

    /**
     * Clears all typing attributes, reverting to the cursor's inherited attributes.
     */
    public fun clearTypingAttributes() {
        typingAttributes = null
        removedTypingAttributes = null
        typingAttributesAnchor = null
    }

    /**
     * The current selection range within the text field.
     * Returns [TextRange.Zero] when no selection is active (cursor at position 0).
     */
    public val selection: TextRange
        get() = textFieldState.selection

    /**
     * Edits the underlying [RichString] state using a builder DSL.
     * This allows you to apply or remove multiple attributes within a [RichTextBuffer],
     * as well as insert, delete, or replace text.
     */
    public fun edit(block: RichTextBuffer.() -> Unit) {
        textFieldState.edit {
            val richTextBuffer = RichTextBuffer(spans, this)
            richTextBuffer.block()
            spans = richTextBuffer.spans.resnapParagraphSpans(this.toString())
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun updateRichString(buffer: TextFieldBuffer) {
        if (buffer.changes.changeCount == 0) {
            // Compose may trigger input transformations for non-text changes.
            // We only clear typing attributes if the cursor actually moved away
            // from where the attributes were originally set.
            if (typingAttributesAnchor != null && buffer.selection != typingAttributesAnchor) {
                clearTypingAttributes()
            }
            return
        }

        val typingAttr = typingAttributes
        val removedAttr = removedTypingAttributes

        val newSpans =
            (0 until buffer.changes.changeCount).fold(spans) { currentSpans, i ->
                val originalRange = buffer.changes.getOriginalRange(i)
                val range = buffer.changes.getRange(i)

                var updatedSpans =
                    currentSpans.shiftSpans(
                        editStart = originalRange.min,
                        editEnd = originalRange.max,
                        newLength = range.length,
                        offsetDiff = range.length - originalRange.length,
                    )

                // Apply typing attributes to the inserted text
                if (typingAttr != null && range.length > originalRange.length) {
                    val insertStart = originalRange.min
                    val insertEnd = insertStart + range.length - 1
                    if (insertStart <= insertEnd) {
                        updatedSpans =
                            updatedSpans.mergeSpan(
                                RichSpan(
                                    range = insertStart..insertEnd,
                                    attributes = typingAttr,
                                ),
                            )
                    }
                }

                // Explicitly remove removed attributes from the inserted text
                if (removedAttr != null && range.length > originalRange.length) {
                    val insertStart = originalRange.min
                    val insertEnd = insertStart + range.length - 1
                    if (insertStart <= insertEnd) {
                        val tempBuffer = RichTextBuffer(updatedSpans, buffer)
                        removedAttr.forEach { key ->
                            if (key is SpanAttributeKey<*>) {
                                @Suppress("UNCHECKED_CAST")
                                tempBuffer.removeSpanAttribute(key as SpanAttributeKey<Any>, insertStart..insertEnd)
                            } else if (key is ParagraphAttributeKey<*>) {
                                @Suppress("UNCHECKED_CAST")
                                tempBuffer.removeParagraphAttribute(key as ParagraphAttributeKey<Any>, insertStart..insertEnd)
                            }
                        }
                        updatedSpans = tempBuffer.spans
                    }
                }

                updatedSpans
            }

        this.spans = newSpans.resnapParagraphSpans(buffer.toString())
        clearTypingAttributes()
    }
}

internal fun List<RichSpan>.shiftSpans(
    editStart: Int,
    editEnd: Int,
    newLength: Int,
    offsetDiff: Int,
): List<RichSpan> =
    mapNotNull { span ->
        shiftSpan(
            span = span,
            editStart = editStart,
            editEnd = editEnd,
            newLength = newLength,
            offsetDiff = offsetDiff,
        )
    }

private fun shiftSpan(
    span: RichSpan,
    editStart: Int,
    editEnd: Int,
    newLength: Int,
    offsetDiff: Int,
): RichSpan? {
    val spanStart = span.range.first
    val spanEnd = span.range.last

    return when {
        editEnd <= spanStart -> {
            // Edit happens entirely before the span. Shift it securely.
            span.copy(range = (spanStart + offsetDiff)..(spanEnd + offsetDiff))
        }

        editStart > spanEnd + 1 -> {
            // Edit happens entirely after the span with a gap. Unaffected.
            // When editStart == spanEnd + 1, it falls through to the overlap branch
            // so that adjacent insertions can inherit the span's attributes.
            span
        }

        else -> {
            // Edit overlaps with the span.
            val newStart =
                when {
                    spanStart < editStart -> spanStart
                    spanStart >= editEnd -> spanStart + offsetDiff
                    else -> editStart
                }

            val newEnd =
                when {
                    spanEnd >= editEnd -> spanEnd + offsetDiff

                    // Adjacent insertion (e.g., typing right after a bold word) inherits the span's attributes.
                    // This matches common rich-text editor behavior where continuing to type at the end
                    // of a styled region extends the style.
                    editStart == spanEnd + 1 && editEnd == spanEnd + 1 -> spanEnd + offsetDiff

                    else -> editStart + newLength - 1
                }

            if (newStart > newEnd) {
                null
            } else {
                span.copy(range = newStart..newEnd)
            }
        }
    }
}
