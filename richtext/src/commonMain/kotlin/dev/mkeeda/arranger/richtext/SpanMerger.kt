package dev.mkeeda.arranger.richtext

/**
 * Applies a given [transform] to the attributes of this list of spans within the [targetRange].
 *
 * This function handles partial overlaps and full coverage by splitting spans as needed
 * to apply the transformation exclusively within the [targetRange].
 *
 * It utilizes a sweep-line style interval chunking algorithm:
 * 1. Extract interval boundaries (`start` and `end + 1`) from all existing
 *    spans and the [targetRange].
 * 2. Uniquely sort these boundary points.
 * 3. Iterate through adjacent pairs of boundary points to create perfectly
 *    tessellated, non-overlapping sub-ranges (chunks).
 * 4. For each chunk, determine its coverage:
 *    - Inside both target range and existing spans: apply transform to the existing attributes.
 *    - Inside target range only: apply transform to an Empty container.
 *    - Inside existing span only: keep existing attributes untouched.
 * 5. If the resulting attribute container is completely empty for a chunk, it is omitted.
 * 6. Contiguous spans with identical attributes are coalesced optimizations.
 */
internal fun List<RichSpan>.transformSpans(
    targetRange: IntRange,
    transform: (AttributeContainer) -> AttributeContainer,
): List<RichSpan> {
    if (this.isEmpty() && targetRange.isEmpty()) return emptyList()
    val nonEmptyTargetRange = if (targetRange.isEmpty()) null else targetRange

    // 1. Extract boundaries and sort uniquely
    val boundaries =
        buildList {
            if (nonEmptyTargetRange != null) {
                add(nonEmptyTargetRange.first)
                add(nonEmptyTargetRange.last + 1)
            }

            for (span in this@transformSpans) {
                add(span.range.first)
                add(span.range.last + 1)
            }
        }.distinct().sorted()

    // 2. Derive chunks and calculate properties
    val resultSpans = mutableListOf<RichSpan>()

    for (i in 0 until boundaries.size - 1) {
        val chunkStart = boundaries[i]
        val chunkEnd = boundaries[i + 1] - 1

        val isInsideTarget = nonEmptyTargetRange != null && chunkStart >= nonEmptyTargetRange.first && chunkEnd <= nonEmptyTargetRange.last

        var chunkAttributes = AttributeContainer.empty()
        for (span in this@transformSpans) {
            if (span.range.first <= chunkStart && chunkEnd <= span.range.last) {
                chunkAttributes += span.attributes
            }
        }

        val transformedAttributes =
            if (isInsideTarget) {
                transform(chunkAttributes)
            } else {
                chunkAttributes
            }

        if (transformedAttributes.isEmpty()) {
            continue
        }

        val nextSpan = RichSpan(range = chunkStart..chunkEnd, attributes = transformedAttributes)

        if (resultSpans.isNotEmpty()) {
            val lastSpan = resultSpans.last()
            // Optimize: Merge with the previous span if they are adjacent and have exactly the same attributes
            if (
                lastSpan.range.last + 1 == nextSpan.range.first &&
                lastSpan.attributes == nextSpan.attributes
            ) {
                resultSpans[resultSpans.size - 1] =
                    RichSpan(
                        range = lastSpan.range.first..nextSpan.range.last,
                        attributes = lastSpan.attributes,
                    )
            } else {
                resultSpans.add(nextSpan)
            }
        } else {
            resultSpans.add(nextSpan)
        }
    }

    return resultSpans
}

/**
 * Merges a [newSpan] into this list of spans.
 * Convenience extension that wraps [transformSpans].
 *
 * Note: This function is public to allow cross-module usage within the library
 * (e.g., from the `richtext-editor` module), but is marked with `@InternalArrangerApi`
 * as it is not intended to be a stable public API for library consumers.
 */
@InternalArrangerApi
public fun List<RichSpan>.mergeSpan(newSpan: RichSpan): List<RichSpan> {
    return transformSpans(targetRange = newSpan.range) { existingAttributes ->
        existingAttributes + newSpan.attributes
    }
}
