package dev.mkeeda.arranger.core.text

/**
 * An immutable rich text representation that pairs plain text with type-safe attributes
 * applied to specific character ranges.
 *
 * All mutating operations return a new [RichString] instance, preserving the original unchanged.
 *
 * @property text The plain text content.
 */
public class RichString(
    public val text: String,
    private val spans: List<RichSpan> = emptyList(),
) {
    /**
     * Returns a new [RichString] with the given attribute applied to the entire text.
     */
    public fun <T> with(key: AttributeKey<T>, value: T): RichString {
        return with(key, value, range = text.indices)
    }

    /**
     * Returns a new [RichString] with the given attribute applied to the specified [range].
     */
    public fun <T> with(key: AttributeKey<T>, value: T, range: IntRange): RichString {
        require(!range.isEmpty()) { "Range must not be empty: $range" }
        require(range.first >= 0) { "Range start must not be negative: ${range.first}" }
        require(range.last < text.length) { "Range end must be within text bounds: ${range.last} >= ${text.length}" }

        val newSpan =
            RichSpan(
                range = range,
                attributes = AttributeContainer.empty().with(key, value),
            )
        return RichString(text = text, spans = mergeSpans(spans, newSpan))
    }

    /**
     * Returns a snapshot of all [RichSpan]s currently held by this [RichString].
     */
    public fun getSpans(): List<RichSpan> = spans
}
