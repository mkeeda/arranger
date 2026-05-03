package dev.mkeeda.arranger.richtext

/**
 * Represents the semantic structure of a list item within a RichString.
 */
@InternalArrangerApi
public sealed interface ListItem {
    /**
     * The character index of the start of the paragraph that forms this list item.
     */
    public val textIndex: Int

    /**
     * The indentation level of this list item.
     */
    public val indentLevel: ListIndentLevel

    /**
     * The text color applied to this list item, extracted from [TextColorKey].
     */
    public val color: RgbaColor?
}

/**
 * Represents a bullet list item.
 */
@InternalArrangerApi
public data class BulletListItem(
    override val textIndex: Int,
    override val indentLevel: ListIndentLevel,
    override val color: RgbaColor?,
) : ListItem

/**
 * Represents an ordered list item (e.g., 1. 2. 3.).
 */
@InternalArrangerApi
public data class OrderedListItem(
    override val textIndex: Int,
    override val indentLevel: ListIndentLevel,
    override val color: RgbaColor?,
    /**
     * The 1-based index number for this item in the ordered list.
     */
    val index: Int,
) : ListItem

/**
 * Extracts a list of semantic [ListItem]s from the given [RichString].
 */
@InternalArrangerApi
public fun RichString.extractListItems(): List<ListItem> {
    val bulletItems =
        runs(BulletListKey)
            .filter { it.value != ListIndentLevel.Unspecified }
            .flatMap { run ->
                val startIndices = paragraphStartIndices(run.range)

                startIndices.map { startIndex ->
                    val spanAtStart = spans.firstOrNull { startIndex in it.range }
                    val color = spanAtStart?.attributes?.getOrNull(TextColorKey)
                    BulletListItem(
                        textIndex = startIndex,
                        indentLevel = run.value,
                        color = color,
                    )
                }
            }
            .toList()

    val orderedItems =
        buildList {
            val currentNumbers = IntArray(ListIndentLevel.entries.size) { 1 }
            var expectedNextRunStart = -1
            var previousLevelOrdinal = -1

            runs(OrderedListKey)
                .filter { it.value != ListIndentLevel.Unspecified }
                .forEach { run ->
                    if (expectedNextRunStart != -1 && run.range.first != expectedNextRunStart) {
                        currentNumbers.fill(1)
                        previousLevelOrdinal = -1
                    }
                    expectedNextRunStart = run.range.last + 1

                    val startIndices = paragraphStartIndices(run.range)

                    startIndices.forEach { startIndex ->
                        val currentLevelOrdinal = run.value.ordinal

                        if (previousLevelOrdinal != -1 && currentLevelOrdinal > previousLevelOrdinal) {
                            for (i in previousLevelOrdinal + 1..currentLevelOrdinal) {
                                currentNumbers[i] = 1
                            }
                        }
                        previousLevelOrdinal = currentLevelOrdinal

                        val currentNumber = currentNumbers[currentLevelOrdinal]
                        currentNumbers[currentLevelOrdinal]++

                        val spanAtStart = spans.firstOrNull { startIndex in it.range }
                        val color = spanAtStart?.attributes?.getOrNull(TextColorKey)

                        add(
                            OrderedListItem(
                                textIndex = startIndex,
                                indentLevel = run.value,
                                color = color,
                                index = currentNumber,
                            ),
                        )
                    }
                }
        }

    return (bulletItems + orderedItems).sortedBy { it.textIndex }
}

private fun RichString.paragraphStartIndices(range: IntRange): List<Int> =
    listOf(range.first) +
        (range.first until range.last)
            .filter { text[it] == '\n' }
            .map { it + 1 }
