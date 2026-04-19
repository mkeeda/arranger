package dev.mkeeda.arranger.richtext

/**
 * A builder class used to safely mutate the attributes of a [RichString] within an `edit` block.
 * All mutations are accumulated internally and used to produce a completely new, immutable [RichString] instance
 * when the block completes.
 */
public class RichStringBuilder internal constructor(
    private var currentSpans: List<RichSpan>,
    private val text: String,
) {
    private val textLength: Int = text.length

    /**
     * Applies the specified character span attribute [key] and [value] to the given [range].
     * Any existing span attributes of the same key within this range are completely overwritten.
     */
    public fun <T> setSpanAttribute(
        key: SpanAttributeKey<T>,
        value: T,
        range: IntRange = 0 until textLength,
    ) {
        checkRange(range)
        currentSpans =
            currentSpans.transformSpans(targetRange = range) { attributes ->
                attributes.plus(key, value)
            }
    }

    /**
     * Removes any character span attributes associated with the specified [key] within the given [range].
     */
    public fun <T> removeSpanAttribute(
        key: SpanAttributeKey<T>,
        range: IntRange = 0 until textLength,
    ) {
        checkRange(range)
        currentSpans =
            currentSpans.transformSpans(targetRange = range) { attributes ->
                attributes - key
            }
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
        checkRange(range)
        val snappedRange = range.snapToParagraphs(text)
        currentSpans =
            currentSpans.transformSpans(targetRange = snappedRange) { attributes ->
                attributes.plus(key, value)
            }
    }

    private fun IntRange.snapToParagraphs(text: String): IntRange {
        val start =
            text.lastIndexOf('\n', startIndex = this.first - 1).let {
                if (it == -1) 0 else it + 1
            }
        val end =
            text.indexOf('\n', startIndex = this.last).let {
                if (it == -1) text.lastIndex else it
            }
        return start..end
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
        checkRange(range)
        val snappedRange = range.snapToParagraphs(text)
        currentSpans =
            currentSpans.transformSpans(targetRange = snappedRange) { attributes ->
                attributes - key
            }
    }

    /**
     * Applies a set of attribute mutations to the specified [range] using a DSL builder.
     */
    public fun editAttributes(
        range: IntRange = 0 until textLength,
        editAction: AttributeEditScope.() -> Unit,
    ) {
        AttributeEditScope(this, range).editAction()
    }

    private fun checkRange(range: IntRange) {
        require(!range.isEmpty()) { "Range must not be empty: $range" }
        require(range.first >= 0) { "Range start must not be negative: ${range.first}" }
        require(range.last < textLength) { "Range end must be within text bounds: ${range.last} >= $textLength" }
    }

    internal fun build(text: String): RichString {
        return RichString(text = text, spans = currentSpans)
    }
}
