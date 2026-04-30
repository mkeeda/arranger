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
            var start = -1
            var end = -1
            var currentVal: T? = null

            for (span in spans) {
                val value = extractValue(span.attributes)
                val canMerge = currentVal != null && value == currentVal && end + 1 == span.range.first

                if (!canMerge) {
                    if (currentVal != null) {
                        val range = start..end
                        yield(RichRun(text.substring(range), range, currentVal))
                        currentVal = null
                    }
                }

                if (value != null) {
                    if (currentVal == null) {
                        start = span.range.first
                        currentVal = value
                    }
                    end = span.range.last
                }
            }

            if (currentVal != null) {
                val range = start..end
                yield(RichRun(text.substring(range), range, currentVal))
            }
        }

    /**
     * Executes the given [block] on a [RichStringScope] scoped to this [RichString],
     * and returns a completely new [RichString] carrying the modifications.
     */
    public fun edit(block: RichStringScope.() -> Unit): RichString {
        val scope = RichStringScope(currentSpans = spans, text = text)
        scope.block()
        return scope.build(text = text)
    }
}
