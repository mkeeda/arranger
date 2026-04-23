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
    public fun <T : Any> runs(key: AttributeKey<T>): Sequence<RichRun<T>> {
        return extractRuns { attributes -> attributes.getOrNull(key) }
    }

    /**
     * Retrieves all contiguous runs that satisfy the given [predicate], merging internally split spans.
     * Adjacent internal spans that satisfy the predicate and possess EXACTLY THE SAME attributes
     * will be combined into a single [RichRun].
     */
    public fun runs(predicate: (AttributeContainer) -> Boolean): Sequence<RichRun<AttributeContainer>> {
        return extractRuns { attributes ->
            if (predicate(attributes)) attributes else null
        }
    }

    private fun <T : Any> extractRuns(
        extractValue: (AttributeContainer) -> T?,
    ): Sequence<RichRun<T>> =
        sequence {
            var mergedRangeStart = -1
            var mergedRangeEnd = -1
            var mergedValue: T? = null

            for (span in spans) {
                val value = extractValue(span.attributes)
                if (value != null) {
                    if (mergedValue != null) {
                        if (mergedRangeEnd + 1 == span.range.first && mergedValue == value) {
                            mergedRangeEnd = span.range.last
                        } else {
                            val range = mergedRangeStart..mergedRangeEnd
                            yield(
                                RichRun(
                                    text = text.substring(range),
                                    range = range,
                                    value = mergedValue,
                                ),
                            )
                            mergedRangeStart = span.range.first
                            mergedRangeEnd = span.range.last
                            mergedValue = value
                        }
                    } else {
                        mergedRangeStart = span.range.first
                        mergedRangeEnd = span.range.last
                        mergedValue = value
                    }
                } else {
                    if (mergedValue != null) {
                        val range = mergedRangeStart..mergedRangeEnd
                        yield(
                            RichRun(
                                text = text.substring(range),
                                range = range,
                                value = mergedValue,
                            ),
                        )
                        mergedValue = null
                    }
                }
            }

            if (mergedValue != null) {
                val range = mergedRangeStart..mergedRangeEnd
                yield(
                    RichRun(
                        text = text.substring(range),
                        range = range,
                        value = mergedValue,
                    ),
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
