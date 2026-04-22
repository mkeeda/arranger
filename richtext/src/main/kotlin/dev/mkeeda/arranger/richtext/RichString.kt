package dev.mkeeda.arranger.richtext

/**
 * An immutable rich text representation that pairs plain text with type-safe attributes
 * applied to specific character ranges.
 *
 * All mutating operations return a new [RichString] instance, preserving the original unchanged.
 *
 * @property text The plain text content.
 * @property spans A list of all [RichSpan]s currently held by this [RichString].
 */
public data class RichString(
    public val text: String,
    public val spans: List<RichSpan> = emptyList(),
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
     * Retrieves all contiguous runs of the requested attribute [key], merging internally split spans.
     * Adjacent internal spans that possess the EXACT SAME attribute value for [key] will be combined
     * into a single [RichRun], ignoring differences in other attributes.
     */
    /**
     * Retrieves all contiguous runs of the requested attribute [key], merging internally split spans.
     * Adjacent internal spans that possess the EXACT SAME attribute value for [key] will be combined
     * into a single [RichRun], ignoring differences in other attributes.
     */
    public fun <T> runs(key: AttributeKey<T>): Sequence<RichRun<T>> {
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

        return isolatedSpans.asSequence().map { span ->
            @Suppress("UNCHECKED_CAST")
            RichRun(
                text = text.substring(span.range),
                range = span.range,
                value = span.attributes.getOrNull(key) as T,
            )
        }
    }

    /**
     * Retrieves all contiguous runs that satisfy the given [predicate], merging internally split spans.
     * Adjacent internal spans that satisfy the predicate and possess EXACTLY THE SAME attributes
     * will be combined into a single [RichRun].
     */
    public fun runs(predicate: (AttributeContainer) -> Boolean): Sequence<RichRun<AttributeContainer>> = sequence {
        var mergedRangeStart = -1
        var mergedRangeEnd = -1
        var mergedAttributes: AttributeContainer? = null

        for (span in spans) {
            if (predicate(span.attributes)) {
                if (mergedAttributes != null) {
                    if (mergedRangeEnd + 1 == span.range.first && mergedAttributes == span.attributes) {
                        // Can merge
                        mergedRangeEnd = span.range.last
                    } else {
                        // Cannot merge, yield current and start new
                        val range = mergedRangeStart..mergedRangeEnd
                        yield(
                            RichRun(
                                text = text.substring(range),
                                range = range,
                                value = mergedAttributes
                            )
                        )
                        mergedRangeStart = span.range.first
                        mergedRangeEnd = span.range.last
                        mergedAttributes = span.attributes
                    }
                } else {
                    // Start new
                    mergedRangeStart = span.range.first
                    mergedRangeEnd = span.range.last
                    mergedAttributes = span.attributes
                }
            } else {
                // Predicate false, yield existing if any
                if (mergedAttributes != null) {
                    val range = mergedRangeStart..mergedRangeEnd
                    yield(
                        RichRun(
                            text = text.substring(range),
                            range = range,
                            value = mergedAttributes
                        )
                    )
                    mergedAttributes = null
                }
            }
        }

        // Final yield
        if (mergedAttributes != null) {
            val range = mergedRangeStart..mergedRangeEnd
            yield(
                RichRun(
                    text = text.substring(range),
                    range = range,
                    value = mergedAttributes
                )
            )
        }
    }

    /**
     * Executes the given [block] on a [RichStringBuilder] scoped to this [RichString],
     * and returns a completely new [RichString] carrying the modifications.
     */
    public fun edit(block: RichStringBuilder.() -> Unit): RichString {
        val builder = RichStringBuilder(currentSpans = spans, text = text)
        builder.block()
        return builder.build(text = text)
    }
}
