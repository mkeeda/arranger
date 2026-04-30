package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import dev.mkeeda.arranger.richtext.AttributeEditScope
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.RichRun
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichStringScope
import dev.mkeeda.arranger.richtext.SpanAttributeKey

/**
 * A buffer for safely mutating both text and attributes of a [RichTextState] within an `edit` block.
 * This class provides attribute operations by delegating to [RichStringScope] and adds text operations
 * like insert, delete, and replace.
 */
public class RichTextBuffer internal constructor(
    private var currentSpans: List<RichSpan>,
    private val textFieldBuffer: TextFieldBuffer,
) {
    public val textLength: Int
        get() = textFieldBuffer.length

    public val text: String
        get() = textFieldBuffer.toString()

    private inline fun withAttributeScope(block: RichStringScope.() -> Unit) {
        val scope = RichStringScope(currentSpans, textFieldBuffer.toString())
        scope.block()
        currentSpans = scope.spans
    }

    /**
     * Applies the specified character span attribute [key] and [value] to the given [range].
     * Any existing span attributes of the same key within this range are completely overwritten.
     */
    public fun <T> setSpanAttribute(
        key: SpanAttributeKey<T>,
        value: T,
        range: IntRange = 0 until textLength,
    ) {
        withAttributeScope { setSpanAttribute(key, value, range) }
    }

    /**
     * Removes any character span attributes associated with the specified [key] within the given [range].
     */
    public fun <T> removeSpanAttribute(
        key: SpanAttributeKey<T>,
        range: IntRange = 0 until textLength,
    ) {
        withAttributeScope { removeSpanAttribute(key, range) }
    }

    /**
     * Applies the specified paragraph attribute [key] and [value] to the given [range].
     * The [range] is automatically expanded to span the entire paragraphs (separated by `\n`)
     * it intersects with.
     */
    public fun <T> setParagraphAttribute(
        key: ParagraphAttributeKey<T>,
        value: T,
        range: IntRange = 0 until textLength,
    ) {
        withAttributeScope { setParagraphAttribute(key, value, range) }
    }

    /**
     * Removes any paragraph attributes associated with the specified [key] within the given [range].
     * The [range] is automatically expanded to span the entire paragraphs (separated by `\n`)
     * it intersects with.
     */
    public fun <T> removeParagraphAttribute(
        key: ParagraphAttributeKey<T>,
        range: IntRange = 0 until textLength,
    ) {
        withAttributeScope { removeParagraphAttribute(key, range) }
    }

    /**
     * Applies a set of attribute mutations to the specified [range] using a DSL builder.
     */
    public fun editAttributes(
        range: IntRange = 0 until textLength,
        editAction: AttributeEditScope.() -> Unit,
    ) {
        withAttributeScope { editAttributes(range, editAction) }
    }

    /**
     * Applies a set of attribute mutations to all specified [ranges] using a DSL builder.
     */
    public fun editAll(
        ranges: Sequence<IntRange>,
        editAction: AttributeEditScope.() -> Unit,
    ) {
        withAttributeScope { editAll(ranges, editAction) }
    }

    /**
     * Applies a set of attribute mutations to all specified [runs] using a DSL builder.
     * The [editAction] receives each [RichRun] to allow conditional logic based on existing attributes.
     */
    public fun <T : Any> editAll(
        runs: Sequence<RichRun<T>>,
        editAction: AttributeEditScope.(RichRun<T>) -> Unit,
    ) {
        withAttributeScope { editAll(runs, editAction) }
    }

    /**
     * Inserts the given [text] at the specified [index].
     */
    public fun insert(index: Int, text: String) {
        textFieldBuffer.insert(index, text)
        shiftSpansEagerly(
            editStart = index,
            editEnd = index,
            newLength = text.length,
            offsetDiff = text.length,
        )
    }

    /**
     * Inserts the given [text] at the specified [index] and applies attributes
     * to the inserted text atomically.
     */
    public fun insert(
        index: Int,
        text: String,
        editAction: AttributeEditScope.() -> Unit,
    ) {
        insert(index, text)
        val insertedRange = index until (index + text.length)
        if (!insertedRange.isEmpty()) {
            editAttributes(insertedRange, editAction)
        }
    }

    /**
     * Deletes the text within the specified [range].
     */
    public fun delete(range: IntRange) {
        if (range.isEmpty()) return
        textFieldBuffer.delete(range.first, range.last + 1)
        shiftSpansEagerly(
            editStart = range.first,
            editEnd = range.last + 1,
            newLength = 0,
            offsetDiff = -(range.last - range.first + 1),
        )
    }

    /**
     * Replaces the text within the specified [range] with the new [text].
     */
    public fun replace(range: IntRange, text: String) {
        if (range.isEmpty() && text.isEmpty()) return
        textFieldBuffer.replace(range.first, range.last + 1, text)
        shiftSpansEagerly(
            editStart = range.first,
            editEnd = range.last + 1,
            newLength = text.length,
            offsetDiff = text.length - (range.last - range.first + 1),
        )
    }

    /**
     * Replaces the text within the specified [range] with the new [text] and applies
     * attributes to the newly inserted text atomically.
     */
    public fun replace(
        range: IntRange,
        text: String,
        editAction: AttributeEditScope.() -> Unit,
    ) {
        replace(range, text)
        val insertedRange = range.first until (range.first + text.length)
        if (!insertedRange.isEmpty()) {
            editAttributes(insertedRange, editAction)
        }
    }

    private fun shiftSpansEagerly(
        editStart: Int,
        editEnd: Int,
        newLength: Int,
        offsetDiff: Int,
    ) {
        currentSpans =
            currentSpans.mapNotNull { span ->
                shiftSpan(
                    span = span,
                    editStart = editStart,
                    editEnd = editEnd,
                    newLength = newLength,
                    offsetDiff = offsetDiff,
                )
            }
    }

    internal val spans: List<RichSpan>
        get() = currentSpans
}
