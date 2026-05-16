package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.ParagraphStyle

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
