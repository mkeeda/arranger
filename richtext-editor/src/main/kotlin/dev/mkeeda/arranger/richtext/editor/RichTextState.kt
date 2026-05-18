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
import dev.mkeeda.arranger.richtext.EnterKeyContext
import dev.mkeeda.arranger.richtext.InheritParagraphStrategy
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.mergeSpan
import dev.mkeeda.arranger.richtext.resnapParagraphSpans
import dev.mkeeda.arranger.richtext.snapToParagraphs

/**
 * A state object that can be hoisted to control and observe changes to [RichTextEditor].
 *
 * This state manages the underlying [RichString], including the raw text and its associated
 * formatting attributes ([RichSpan]s). It acts as the single source of truth for the editor,
 * integrating seamlessly with standard Compose text APIs.
 *
 * @param initialText The initial [RichString] to display in the editor. Defaults to an empty [RichString].
 */
@Stable
public class RichTextState(initialText: RichString = RichString("")) {
    internal val textFieldState = TextFieldState(initialText.text)

    // The Single Source of Truth for spans
    private var spans: List<RichSpan> by mutableStateOf(initialText.spans.resnapParagraphSpans(initialText.text))

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
                        valueCounts[value] = valueCounts.getOrDefault(value, 0) + overlapLength
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
            collectInheritedAttributes(
                spanInheritIndex = cursorPosition - 1,
                paragraphInheritIndex = cursorPosition,
                spans = spans,
            )

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

        undoState.captureSnapshotBeforeChange(buffer)

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
                        deletedText = buffer.originalText.substring(originalRange.min, originalRange.max),
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

                // Inherit paragraph attributes when newlines are typed or pasted
                val insertedText = buffer.asCharSequence().substring(range.min, range.max)
                if (insertedText.contains('\n')) {
                    updatedSpans =
                        handleNewlineInsertion(
                            insertedText = insertedText,
                            originalRange = originalRange,
                            range = range,
                            currentSpans = currentSpans,
                            updatedSpans = updatedSpans,
                            buffer = buffer,
                            removedAttr = removedAttr,
                        )
                }

                updatedSpans
            }

        this.spans = newSpans.resnapParagraphSpans(buffer.toString())
        clearTypingAttributes()
    }

    /**
     * Manages the undo and redo history for this state.
     * Use this property to programmatically trigger undo/redo operations or observe their availability.
     *
     * See [RichTextUndoState] for details on how the snapshot history is automatically recorded.
     */
    public val undoState: RichTextUndoState =
        RichTextUndoState(
            textFieldState = textFieldState,
            getSpans = { spans },
            setSpans = { spans = it },
            clearTypingAttributes = { clearTypingAttributes() },
        )

    private fun handleNewlineInsertion(
        insertedText: String,
        originalRange: TextRange,
        range: TextRange,
        currentSpans: List<RichSpan>,
        updatedSpans: List<RichSpan>,
        buffer: TextFieldBuffer,
        removedAttr: Set<AttributeKey<*>>?,
    ): List<RichSpan> {
        val spanInheritIndex = if (originalRange.min > 0) originalRange.min - 1 else 0
        val paragraphInheritIndex = originalRange.min
        val attrsBeforeCursor =
            collectInheritedAttributes(
                spanInheritIndex = spanInheritIndex,
                paragraphInheritIndex = paragraphInheritIndex,
                spans = currentSpans,
            )
        val paragraphAttrKeys = attrsBeforeCursor.keys.filterIsInstance<ParagraphAttributeKey<*>>()

        if (insertedText == "\n") {
            // Handle Enter key press using Strategy pattern
            val paragraphRange = (originalRange.min..originalRange.min).snapToParagraphs(buffer.toString())
            val context =
                EnterKeyContext(
                    text = buffer.toString(),
                    cursorPosition = range.min,
                    paragraphRange = paragraphRange,
                    currentAttributes = attrsBeforeCursor,
                )

            val strategy =
                paragraphAttrKeys
                    .map { it.enterKeyStrategy }
                    .firstOrNull { it != InheritParagraphStrategy }
                    ?: InheritParagraphStrategy

            val result = strategy.execute(context)
            return EnterKeyHandler.apply(
                result = result,
                context = context,
                spans = updatedSpans,
                buffer = buffer,
                insertedRange = range.min until range.max,
                removedAttr = removedAttr,
            )
        } else {
            // Handle pasted text containing newlines (fallback to simple inheritance)
            val effectiveParagraphAttrKeys =
                paragraphAttrKeys.filter { key ->
                    removedAttr == null || key !in removedAttr
                }
            val paragraphAttr =
                effectiveParagraphAttrKeys.fold(AttributeContainer.empty()) { acc, key ->
                    @Suppress("UNCHECKED_CAST")
                    val typedKey = key as AttributeKey<Any>
                    acc + (typedKey to attrsBeforeCursor.getOrDefault(typedKey))
                }

            if (paragraphAttr.isNotEmpty()) {
                val snappedRange = (range.min until range.max).snapToParagraphs(buffer.toString())
                return updatedSpans.mergeSpan(
                    RichSpan(
                        range = snappedRange,
                        attributes = paragraphAttr,
                    ),
                )
            }
        }
        return updatedSpans
    }

    private fun collectInheritedAttributes(
        spanInheritIndex: Int,
        paragraphInheritIndex: Int,
        spans: List<RichSpan>,
    ): AttributeContainer {
        return spans.fold(AttributeContainer.empty()) { acc, span ->
            val hasSpanAttrs =
                spanInheritIndex in span.range &&
                    span.attributes.keys.any { it is SpanAttributeKey<*> }
            val hasParagraphAttrs =
                paragraphInheritIndex in span.range &&
                    span.attributes.keys.any { it is ParagraphAttributeKey<*> }

            if (!hasSpanAttrs && !hasParagraphAttrs) return@fold acc

            val filteredAttrs =
                span.attributes.filterKeys { key ->
                    (key is SpanAttributeKey<*> && spanInheritIndex in span.range) ||
                        (key is ParagraphAttributeKey<*> && paragraphInheritIndex in span.range)
                }
            acc + filteredAttrs
        }
    }
}

internal fun List<RichSpan>.shiftSpans(
    editStart: Int,
    editEnd: Int,
    newLength: Int,
    offsetDiff: Int,
    deletedText: String = "",
): List<RichSpan> =
    flatMap { span ->
        shiftSpan(
            span = span,
            editStart = editStart,
            editEnd = editEnd,
            newLength = newLength,
            offsetDiff = offsetDiff,
            deletedText = deletedText,
        )
    }

private fun shiftSpan(
    span: RichSpan,
    editStart: Int,
    editEnd: Int,
    newLength: Int,
    offsetDiff: Int,
    deletedText: String,
): List<RichSpan> {
    val spanStart = span.range.first
    val spanEnd = span.range.last

    // If this span merges into a previous paragraph due to a newline deletion,
    // we should strip its paragraph attributes so the top paragraph's attributes win.
    val isMergedIntoPrevious =
        if (deletedText.contains('\n')) {
            val lastDeletedNewlineIndex = editStart + deletedText.lastIndexOf('\n')
            spanStart > lastDeletedNewlineIndex && spanStart <= editEnd
        } else {
            false
        }

    val spanAttrs = span.attributes.filterKeys { it is SpanAttributeKey<*> }
    val paraAttrs =
        if (isMergedIntoPrevious) {
            AttributeContainer.empty()
        } else {
            span.attributes.filterKeys { it is ParagraphAttributeKey<*> }
        }

    val effectiveAttributes = spanAttrs + paraAttrs
    if (effectiveAttributes.isEmpty()) return emptyList()

    val effectiveSpan = span.copy(attributes = effectiveAttributes)

    return when {
        editEnd <= spanStart -> {
            // Edit happens entirely before the span. Shift it securely.
            listOf(effectiveSpan.copy(range = (spanStart + offsetDiff)..(spanEnd + offsetDiff)))
        }

        editStart > spanEnd + 1 -> {
            // Edit happens entirely after the span with a gap. Unaffected.
            listOf(effectiveSpan)
        }

        else -> {
            // Edit overlaps with the span, or is exactly adjacent (editStart == spanEnd + 1).

            // If it's an adjacent insertion (typing exactly at the end of the span),
            // we want to extend SpanAttributes, but NOT ParagraphAttributes.
            // ParagraphAttributes should only expand if the edit actually overlaps them.
            if (editStart == spanEnd + 1 && editEnd == spanEnd + 1) {
                val result = mutableListOf<RichSpan>()

                // Paragraph attributes stay unaffected (don't expand into adjacent insertions)
                if (paraAttrs.isNotEmpty()) {
                    result.add(effectiveSpan.copy(attributes = paraAttrs))
                }

                // Span attributes expand to cover the insertion
                if (spanAttrs.isNotEmpty()) {
                    result.add(
                        effectiveSpan.copy(
                            range = spanStart..(spanEnd + offsetDiff),
                            attributes = spanAttrs,
                        ),
                    )
                }

                return result
            }

            // Normal overlap
            val newStart =
                when {
                    spanStart < editStart -> spanStart
                    spanStart >= editEnd -> spanStart + offsetDiff
                    else -> editStart
                }

            val newEnd =
                when {
                    spanEnd >= editEnd -> spanEnd + offsetDiff
                    else -> editStart + newLength - 1
                }

            if (newStart > newEnd) {
                emptyList()
            } else {
                listOf(effectiveSpan.copy(range = newStart..newEnd))
            }
        }
    }
}
