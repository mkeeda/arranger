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
     * Applies the specified attribute [key] and [value] to the given [range].
     * Any existing attributes of the same key within this range are completely overwritten.
     */
    public fun <T> setAttribute(
        key: AttributeKey<T>,
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
     * Removes any attributes associated with the specified [key] within the given [range].
     */
    public fun <T> removeAttribute(
        key: AttributeKey<T>,
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
        key: AttributeKey<T>,
        value: T,
        range: IntRange = 0 until textLength,
    ) {
        checkRange(range)
        val snappedRange = range.snapToParagraphs(text)
        setAttribute(key, value, snappedRange)
    }

    private fun IntRange.snapToParagraphs(text: String): IntRange {
        var start = this.first
        var end = this.last

        while (start > 0 && text[start - 1] != '\n') {
            start--
        }

        while (end < text.lastIndex && text[end] != '\n') {
            end++
        }

        return start..end
    }

    /**
     * Removes any paragraph attributes associated with the specified [key] within the given [range].
     * The [range] is automatically expanded to span the entire paragraphs (separated by `\n`)
     * it intersects with.
     */
    public fun <T> removeParagraphAttribute(
        key: AttributeKey<T>,
        range: IntRange = 0 until textLength,
    ) {
        checkRange(range)
        val snappedRange = range.snapToParagraphs(text)
        removeAttribute(key, snappedRange)
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
