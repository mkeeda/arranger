package dev.mkeeda.arranger.core.text

/**
 * A builder class used to safely mutate the attributes of a [RichString] within an `edit` block.
 * All mutations are accumulated internally and used to produce a completely new, immutable [RichString] instance
 * when the block completes.
 */
public class RichStringBuilder internal constructor(
    private var currentSpans: List<RichSpan>,
    private val textLength: Int,
) {
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

    private fun checkRange(range: IntRange) {
        require(!range.isEmpty()) { "Range must not be empty: $range" }
        require(range.first >= 0) { "Range start must not be negative: ${range.first}" }
        require(range.last < textLength) { "Range end must be within text bounds: ${range.last} >= $textLength" }
    }

    internal fun build(text: String): RichString {
        return RichString(text = text, spans = currentSpans)
    }
}
