package dev.mkeeda.arranger.richtext

import dev.mkeeda.arranger.richtext.InternalArrangerApi

/**
 * Expands this range to cover the entire paragraph(s) it intersects with
 * in the given [text]. Paragraphs are delimited by '\n'.
 */
@InternalArrangerApi
public fun IntRange.snapToParagraphs(text: String): IntRange {
    val start =
        text.lastIndexOf('\n', startIndex = this.first - 1).let {
            if (it == -1) 0 else it + 1
        }
    val safeLast = maxOf(this.first, this.last)
    val end =
        text.indexOf('\n', startIndex = safeLast).let {
            if (it != -1) {
                it
            } else {
                if (safeLast >= text.length && text.endsWith('\n')) text.length else text.lastIndex
            }
        }
    return start..end
}

/**
 * Re-snaps any spans containing [ParagraphAttributeKey]s to align with
 * paragraph boundaries in the given [text].
 *
 * Spans that contain only [SpanAttributeKey]s are left unchanged.
 * Paragraph spans whose range becomes invalid (e.g., in empty text) are removed.
 */
@InternalArrangerApi
public fun List<RichSpan>.resnapParagraphSpans(text: String): List<RichSpan> {
    if (this.isEmpty()) return this

    val spanOnlySpans =
        mapNotNull { span ->
            val sAttrs = span.attributes.filterSpanAttributes()
            if (sAttrs.isNotEmpty()) {
                span.copy(attributes = sAttrs)
            } else {
                null
            }
        }

    if (text.isEmpty()) return spanOnlySpans

    var resultSpans = spanOnlySpans

    val pSpans =
        this.mapNotNull { span ->
            val pAttrs = span.attributes.filterParagraphAttributes()
            if (pAttrs.isNotEmpty()) {
                RichSpan(range = span.range, attributes = pAttrs)
            } else {
                null
            }
        }

    for (pSpan in pSpans) {
        val clampedFirst = pSpan.range.first.coerceIn(0, text.length)
        val clampedLast = pSpan.range.last.coerceIn(0, text.length)
        if (clampedFirst <= clampedLast) {
            val snappedRange = (clampedFirst..clampedLast).snapToParagraphs(text)
            resultSpans =
                resultSpans.transformSpans(snappedRange) { attrs ->
                    attrs + pSpan.attributes
                }
        }
    }

    return resultSpans
}
