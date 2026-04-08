package dev.mkeeda.arranger.core.text

/**
 * Merges a [newSpan] into a list of [existingSpans], resolving any overlaps.
 *
 * This function handles partial overlaps, complete containments, and full
 * overwriting by guaranteeing that the resulting list of spans is
 * strictly non-overlapping and sorted.
 *
 * It utilizes a sweep-line style interval chunking algorithm:
 * 1. Extract interval boundaries (`start` and `end + 1`) from all existing
 *    spans and the [newSpan].
 * 2. Uniquely sort these boundary points.
 * 3. Iterate through adjacent pairs of boundary points to create perfectly
 *    tessellated, non-overlapping sub-ranges (chunks).
 * 4. For each chunk, determine its coverage:
 *    - Inside both new and existing spans: merge attributes (new overwrites existing).
 *    - Inside only one: map directly to its attributes.
 *    - Inside neither: skip chunk (pure text without attributes).
 * 5. After attribute assessment, consecutive chunks with identically matched
 *    [AttributeContainer]s are consolidated back together for optimization.
 *
 * @param existingSpans The current sorted list of non-overlapping spans.
 * @param newSpan The incoming span to be applied over the existing ones.
 * @return A new, optimized list of strictly non-overlapping spans.
 */
internal fun mergeSpans(existingSpans: List<RichSpan>, newSpan: RichSpan): List<RichSpan> {
    if (existingSpans.isEmpty()) return listOf(newSpan)

    // 1. Extract boundaries and sort uniquely
    val boundaries = mutableListOf<Int>()
    boundaries.add(newSpan.range.first)
    boundaries.add(newSpan.range.last + 1)

    for (span in existingSpans) {
        boundaries.add(span.range.first)
        boundaries.add(span.range.last + 1)
    }

    val sortedBoundaries = boundaries.distinct().sorted()

    // 2. Derive chunks and calculate properties
    var currentSpanIndex = 0
    val resultSpans = mutableListOf<RichSpan>()

    for (i in 0 until sortedBoundaries.size - 1) {
        val chunkStart = sortedBoundaries[i]
        val chunkEnd = sortedBoundaries[i + 1] - 1

        // Advance existing span pointer to catch up with the current chunk
        while (
            currentSpanIndex < existingSpans.size &&
            existingSpans[currentSpanIndex].range.last < chunkStart
        ) {
            currentSpanIndex++
        }

        val isInsideNewSpan = chunkStart >= newSpan.range.first && chunkEnd <= newSpan.range.last

        val isInsideExistingSpan =
            currentSpanIndex < existingSpans.size &&
                chunkStart >= existingSpans[currentSpanIndex].range.first &&
                chunkEnd <= existingSpans[currentSpanIndex].range.last

        val chunkAttributes: AttributeContainer =
            when {
                isInsideNewSpan && isInsideExistingSpan -> existingSpans[currentSpanIndex].attributes + newSpan.attributes
                isInsideNewSpan -> newSpan.attributes
                isInsideExistingSpan -> existingSpans[currentSpanIndex].attributes
                else -> continue // Blank gap (no attributes), skip this chunk
            }

        val nextSpan = RichSpan(range = chunkStart..chunkEnd, attributes = chunkAttributes)

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
