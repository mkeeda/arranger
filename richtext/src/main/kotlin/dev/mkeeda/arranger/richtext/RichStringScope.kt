package dev.mkeeda.arranger.richtext

/**
 * A DSL scope used to safely mutate the attributes of a [RichString] within an `edit` block.
 * All mutations are accumulated internally and used to produce a completely new, immutable [RichString] instance
 * when the block completes.
 *
 * Note: This scope does not mutate the text itself. It is scoped exclusively to attribute operations.
 */
public class RichStringScope
    @InternalArrangerApi
    constructor(
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
         * Removes all attributes (both span and paragraph level) within the given [range].
         * Paragraph attributes will be cleared from their entire respective paragraphs.
         */
        public fun clearAllAttributes(range: IntRange = 0 until textLength) {
            checkRange(range)

            // Clear span attributes exactly in the specified range by keeping only paragraph attributes
            currentSpans =
                currentSpans.transformSpans(targetRange = range) { attributes ->
                    attributes.filterParagraphAttributes()
                }

            // Clear paragraph attributes in the snapped paragraph boundaries by keeping only span attributes
            val snappedRange = range.snapToParagraphs(text)
            currentSpans =
                currentSpans.transformSpans(targetRange = snappedRange) { attributes ->
                    attributes.filterSpanAttributes()
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

        /**
         * Applies a set of attribute mutations to all specified [ranges] using a DSL builder.
         */
        public fun editAll(
            ranges: Sequence<IntRange>,
            editAction: AttributeEditScope.() -> Unit,
        ) {
            for (range in ranges) {
                editAttributes(range, editAction)
            }
        }

        /**
         * Applies a set of attribute mutations to all specified [runs] using a DSL builder.
         * The [editAction] receives each [RichRun] to allow conditional logic based on existing attributes.
         *
         * Note: The [runs] sequence is consumed lazily during iteration. It is expected to be derived
         * from an immutable [RichString] (e.g., via [RichString.runs]), not from any mutable state
         * within this scope.
         */
        public fun <T : Any> editAll(
            runs: Sequence<RichRun<T>>,
            editAction: AttributeEditScope.(RichRun<T>) -> Unit,
        ) {
            for (run in runs) {
                AttributeEditScope(this, run.range).editAction(run)
            }
        }

        private fun checkRange(range: IntRange) {
            require(!range.isEmpty()) { "Range must not be empty: $range" }
            require(range.first >= 0) { "Range start must not be negative: ${range.first}" }
            require(range.last < textLength) { "Range end must be within text bounds: ${range.last} >= $textLength" }
        }

        @InternalArrangerApi
        public val spans: List<RichSpan>
            get() = currentSpans

        internal fun build(text: String): RichString {
            return RichString(text = text, spans = currentSpans)
        }
    }
