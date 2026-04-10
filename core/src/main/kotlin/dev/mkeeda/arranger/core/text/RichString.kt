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
     * Creates a new [RichString] having the given [initialAttributes] applied to the entire text.
     */
    public constructor(text: String, initialAttributes: AttributeContainer) : this(
        text = text,
        spans =
            if (text.isEmpty() || initialAttributes.isEmpty()) {
                emptyList()
            } else {
                listOf(RichSpan(range = text.indices, attributes = initialAttributes))
            },
    )

    /**
     * Returns a snapshot of all [RichSpan]s currently held by this [RichString].
     */
    public fun getSpans(): List<RichSpan> = spans

    /**
     * Retrieves all contiguous runs of the requested attribute [key], merging internally split spans.
     * Adjacent internal spans that possess the EXACT SAME attribute value for [key] will be combined
     * into a single [RichRun], ignoring differences in other attributes.
     */
    public fun <T> runs(key: AttributeKey<T>): List<RichRun<T>> {
        // Isolate the requested attribute, allowing `transformSpans` to automatically
        // drop spans where it is absent and coalesce contiguous spans where the value is identical.
        val isolatedSpans =
            spans.transformSpans(targetRange = text.indices) { attributes ->
                val value = attributes.getOrNull(key)
                if (value != null) {
                    AttributeContainer.empty().plus(key, value)
                } else {
                    AttributeContainer.empty()
                }
            }

        return isolatedSpans.map { span ->
            @Suppress("UNCHECKED_CAST")
            RichRun(
                text = text.substring(span.range),
                range = span.range,
                value = span.attributes.getOrNull(key) as T,
            )
        }
    }

    /**
     * Executes the given [block] on a [RichStringBuilder] scoped to this [RichString],
     * and returns a completely new [RichString] carrying the modifications.
     */
    public fun edit(block: RichStringBuilder.() -> Unit): RichString {
        val builder = RichStringBuilder(currentSpans = spans, textLength = text.length)
        builder.block()
        return builder.build(text = text)
    }
}
