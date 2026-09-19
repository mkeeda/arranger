# Span Merging & Paragraph Snapping

In rich text document architectures, managing overlapping, fragmented, and boundary-misaligned formatting spans is a classic computational challenge.

For example:

- A user applies **Bold** to characters `0..10`.
- Then the user applies *Italic* to characters `5..15`.
- Next, the user removes Bold from characters `3..7`.

Without proper interval normalization, naive span arrays suffer from exponential span fragmentation, conflicting nested spans, and memory bloat. Arranger solves this cleanly using a mathematical **Sweep-line interval partitioning algorithm** combined with **Paragraph Snapping**.

![Attribute Batch Editing](../images/attribute-batch-edit.gif){ width="600" }

---

## Sweep-Line Interval Partitioning

Arranger implements interval chunking in `dev.mkeeda.arranger.richtext.SpanMerger.kt` via `transformSpans`.

```text
Existing Spans:
[==== Span A (Bold) ====]          (0..10)
                 [==== Span B (Italic) ====]  (5..15)

Target Range:
         [=== Transform (Remove Bold) ===]    (3..7)

Sweep-line Boundary Points:
|        |       |      |          |       |
0        3       5      8          11      16
```

### The 6-Step Algorithm

```kotlin
internal fun List<RichSpan>.transformSpans(
    targetRange: IntRange,
    transform: (AttributeContainer) -> AttributeContainer,
): List<RichSpan>
```

1. **Extract Boundaries**: Collect interval start points (`range.first`) and exclusive end points (`range.last + 1`) from all existing spans and the `targetRange`.
2. **Sort and Deduplicate**: Deduplicate and sort all boundary coordinates in ascending order (`boundaries.distinct().sorted()`).
3. **Partition into Tessellated Chunks**: Form non-overlapping sub-intervals `[boundaries[i] .. boundaries[i + 1] - 1]`. Every character inside a given chunk shares the exact same attribute state.
4. **Evaluate and Transform**:
    - For each chunk, collect all overlapping attributes from the original spans.
    - If the chunk lies within `targetRange`, apply the `transform` function (which can add, replace, or remove attributes).
    - If the chunk lies outside `targetRange`, retain its original attributes untouched.
5. **Drop Empty Chunks**: If a chunk's resulting `AttributeContainer` is empty, omit it from the result.
6. **Adjacent Coalescing (Normalization)**: Iterate through surviving chunks. If a chunk immediately abuts the previous chunk (`lastSpan.range.last + 1 == nextSpan.range.first`) and has **identical attributes** (`lastSpan.attributes == nextSpan.attributes`), merge them into a single continuous `RichSpan`.

### Merge Span Convenience Extension

Applying a new span onto an existing span list is simply a transformation that unions the attribute containers:

```kotlin
@InternalArrangerApi
public fun List<RichSpan>.mergeSpan(newSpan: RichSpan): List<RichSpan> {
    return transformSpans(targetRange = newSpan.range) { existingAttributes ->
        existingAttributes + newSpan.attributes
    }
}
```

---

## Paragraph Snapping

Unlike character spans (such as bold, italic, or text color) which can start and end at arbitrary character offsets, **paragraph attributes** (such as headings, lists, blockquotes, and alignment) must strictly cover **entire paragraphs**.

Paragraphs in Arranger are delimited by the newline character `\n`.

### snapToParagraphs

`IntRange.snapToParagraphs(text: String)` expands any character range to cover the surrounding paragraph boundaries:

```kotlin
@InternalArrangerApi
public fun IntRange.snapToParagraphs(text: String): IntRange {
    val start = text.lastIndexOf('\n', startIndex = this.first - 1).let {
        if (it == -1) 0 else it + 1
    }
    val safeLast = maxOf(this.first, this.last)
    val end = text.indexOf('\n', startIndex = safeLast).let {
        if (it != -1) it else text.lastIndex
    }
    return start..end
}
```

### resnapParagraphSpans

During typing, deleting, or pasting, text mutations can shift character indices or introduce newlines within an existing paragraph block.

`resnapParagraphSpans` ensures paragraph attributes maintain valid boundaries:

```kotlin
@InternalArrangerApi
public fun List<RichSpan>.resnapParagraphSpans(text: String): List<RichSpan>
```

1. **Partition Attributes**: Splits spans into pure character `SpanAttributeKey` spans and block-level `ParagraphAttributeKey` spans.
2. **Boundary Realignment**: For each paragraph span, clamps its range to `text.length` and invokes `snapToParagraphs(text)`.
3. **Re-merging**: Re-applies the aligned paragraph attributes across the text using `transformSpans`.
4. **Cleanup**: Paragraph spans whose ranges collapse to empty (for example, when all text in a paragraph is deleted) are safely dropped.

---

## Performance Guarantees

- **Tessellation**: Guarantees zero overlapping spans in memory. Every character index maps to at most one `RichSpan` with composite attributes.
- **Normalization**: Guarantees minimal span count. Adjacent runs with identical attributes are always unified into one span.
- **Predictable Complexity**: Sweep-line interval sorting runs in $O(N \log N)$ where $N$ is the number of active spans in the edit region, keeping operations lightning fast even on large documents.

---

## Summary

- Arranger uses a sweep-line interval partitioning algorithm to maintain non-overlapping, normalized spans.
- Contiguous runs with identical attributes are automatically coalesced.
- `snapToParagraphs` and `resnapParagraphSpans` enforce clean paragraph boundaries for headings, lists, and quotes across all text mutations.
