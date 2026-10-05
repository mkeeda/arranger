package dev.mkeeda.arranger.editor

import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.RichString

/**
 * Internal representation of a visual block grouping one or more contiguous paragraphs
 * sharing the same block attribute [key] and [value].
 */
internal data class VisualBlockItem(
    val key: AttributeKey<*>,
    val value: Any?,
    val range: IntRange,
)

/**
 * Extracts a list of [VisualBlockItem]s from this [RichString] by merging contiguous paragraphs
 * that share the same visual block attribute in [keys] with identical values.
 */
internal fun RichString.extractVisualBlocks(
    keys: Set<AttributeKey<*>>,
): List<VisualBlockItem> {
    if (spans.isEmpty() || text.isEmpty() || keys.isEmpty()) return emptyList()

    val blocks = mutableListOf<VisualBlockItem>()
    var currentBlock: VisualBlockItem? = null

    for (span in spans) {
        val matchingKey = keys.firstOrNull { span.attributes.containsKey(it) }
        val nextBlock: VisualBlockItem? =
            if (matchingKey != null) {
                VisualBlockItem(
                    key = matchingKey,
                    value = span.attributes[matchingKey],
                    range = span.range,
                )
            } else {
                null
            }

        if (nextBlock == null) {
            if (currentBlock != null) {
                blocks.add(currentBlock)
                currentBlock = null
            }
            continue
        }

        if (currentBlock == null) {
            currentBlock = nextBlock
        } else {
            val isContiguous = currentBlock.range.last + 1 == nextBlock.range.first
            val canMerge =
                isContiguous &&
                    currentBlock.key == nextBlock.key &&
                    currentBlock.value == nextBlock.value

            if (canMerge) {
                currentBlock =
                    currentBlock.copy(
                        range = currentBlock.range.first..nextBlock.range.last,
                    )
            } else {
                blocks.add(currentBlock)
                currentBlock = nextBlock
            }
        }
    }

    if (currentBlock != null) {
        blocks.add(currentBlock)
    }

    return blocks
}

/**
 * Extracts a list of [VisualBlockItem]s from this [RichString] using default visual block keys
 * ([BlockquoteKey] and [CodeBlockKey]).
 */
internal fun RichString.extractVisualBlocks(): List<VisualBlockItem> =
    extractVisualBlocks(setOf(BlockquoteKey, CodeBlockKey))
