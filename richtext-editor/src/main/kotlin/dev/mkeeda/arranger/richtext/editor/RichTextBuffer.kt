package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import dev.mkeeda.arranger.richtext.AttributeEditScope
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichStringScope

/**
 * A buffer for safely mutating both text and attributes of a [RichTextState] within an `edit` block.
 * This class inherits all attribute operations from [RichStringScope] and adds text operations
 * like insert, delete, and replace.
 */
public class RichTextBuffer internal constructor(
    spans: List<RichSpan>,
    private val textFieldBuffer: TextFieldBuffer,
) : RichStringScope(spans) {
    override val textLength: Int
        get() = textFieldBuffer.length

    public override val text: String
        get() = textFieldBuffer.toString()

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
