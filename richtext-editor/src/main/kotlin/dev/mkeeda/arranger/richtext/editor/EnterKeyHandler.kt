package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.TextFieldBuffer
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.EnterKeyContext
import dev.mkeeda.arranger.richtext.EnterKeyResult
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.mergeSpan
import dev.mkeeda.arranger.richtext.snapToParagraphs

internal object EnterKeyHandler {
    fun apply(
        result: EnterKeyResult,
        context: EnterKeyContext,
        spans: List<RichSpan>,
        buffer: TextFieldBuffer,
        insertedRange: IntRange,
        removedAttr: Set<AttributeKey<*>>?,
    ): List<RichSpan> =
        when (result) {
            is EnterKeyResult.InheritAttributes -> {
                applyInheritAttributes(result.attributes, spans, buffer, insertedRange, removedAttr)
            }

            is EnterKeyResult.ClearAttributes -> {
                applyClearAttributes(context, spans, buffer, insertedRange, removedAttr)
            }

            is EnterKeyResult.Outdent -> {
                applyOutdent(result.attributes, context, spans, buffer, insertedRange)
            }
        }

    private fun applyInheritAttributes(
        attributesToInherit: AttributeContainer,
        spans: List<RichSpan>,
        buffer: TextFieldBuffer,
        insertedRange: IntRange,
        removedAttr: Set<AttributeKey<*>>?,
    ): List<RichSpan> {
        val paragraphAttr =
            attributesToInherit.filterKeys { key ->
                key is ParagraphAttributeKey<*> && (removedAttr == null || key !in removedAttr)
            }

        if (paragraphAttr.isEmpty()) return spans

        val newParagraphStart = insertedRange.last + 1
        val snappedRange = (newParagraphStart..newParagraphStart).snapToParagraphs(buffer.toString())
        return spans.mergeSpan(
            RichSpan(
                range = snappedRange,
                attributes = paragraphAttr,
            ),
        )
    }

    private fun applyClearAttributes(
        context: EnterKeyContext,
        spans: List<RichSpan>,
        buffer: TextFieldBuffer,
        insertedRange: IntRange,
        removedAttr: Set<AttributeKey<*>>?,
    ): List<RichSpan> {
        val blockKeysToClear = context.currentAttributes.keys.filterIsInstance<BlockTypeAttributeKey<*>>()
        val newParagraphStart = insertedRange.last + 1
        val snappedRange = (newParagraphStart..newParagraphStart).snapToParagraphs(buffer.toString())
        val tempBuffer = dev.mkeeda.arranger.richtext.editor.RichTextBuffer(spans, buffer)
        blockKeysToClear.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            tempBuffer.removeParagraphAttribute(key as ParagraphAttributeKey<Any>, snappedRange)
        }
        val clearedSpans = tempBuffer.spans

        // Inherit everything EXCEPT BlockTypeAttributeKey
        val attrsToInherit = context.currentAttributes.filterKeys { it !is BlockTypeAttributeKey<*> }

        return applyInheritAttributes(attrsToInherit, clearedSpans, buffer, insertedRange, removedAttr)
    }

    private fun applyOutdent(
        attributesToOutdent: AttributeContainer,
        context: EnterKeyContext,
        spans: List<RichSpan>,
        buffer: TextFieldBuffer,
        insertedRange: IntRange,
    ): List<RichSpan> {
        // 1. Remove the inserted newline from the buffer
        buffer.replace(insertedRange.first, insertedRange.last + 1, "")

        // 2. Shift spans back to account for the removed newline
        val revertedSpans =
            spans.shiftSpans(
                editStart = insertedRange.first,
                editEnd = insertedRange.last + 1,
                newLength = 0,
                offsetDiff = -(insertedRange.last - insertedRange.first + 1),
            )

        // 3. Apply the outdented attributes to the current paragraph
        val snappedRange = context.paragraphRange.snapToParagraphs(buffer.toString())
        val tempBuffer = dev.mkeeda.arranger.richtext.editor.RichTextBuffer(revertedSpans, buffer)

        // Clear all previous block attributes to ensure we don't leak removed attributes (e.g., when completely outdenting to empty)
        val blockKeysToClear = context.currentAttributes.keys.filterIsInstance<BlockTypeAttributeKey<*>>()
        blockKeysToClear.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            tempBuffer.removeParagraphAttribute(key as ParagraphAttributeKey<Any>, snappedRange)
        }

        return tempBuffer.spans.mergeSpan(
            RichSpan(
                range = snappedRange,
                attributes = attributesToOutdent,
            ),
        )
    }
}
