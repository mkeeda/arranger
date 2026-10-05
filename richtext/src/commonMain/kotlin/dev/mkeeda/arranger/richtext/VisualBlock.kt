package dev.mkeeda.arranger.richtext

/**
 * Represents a visual block container grouping one or more contiguous paragraphs sharing the same block attribute.
 */
public sealed interface VisualBlock {
    /**
     * The character index range covering all paragraphs in this block.
     */
    public val range: IntRange

    /**
     * Represents a visual blockquote container.
     */
    public data class Blockquote(
        override val range: IntRange,
    ) : VisualBlock

    /**
     * Represents a visual code block container with an optional programming language.
     */
    public data class CodeBlock(
        override val range: IntRange,
        public val language: String? = null,
    ) : VisualBlock
}

/**
 * Extracts a list of [VisualBlock]s from this [RichString] by merging contiguous paragraphs
 * that share the same visual block attribute ([BlockquoteKey] or [CodeBlockKey]).
 */
public fun RichString.extractVisualBlocks(): List<VisualBlock> {
    if (spans.isEmpty() || text.isEmpty()) return emptyList()

    val blocks = mutableListOf<VisualBlock>()
    var currentBlock: VisualBlock? = null

    for (span in spans) {
        val nextBlock: VisualBlock? =
            when {
                span.attributes.containsKey(BlockquoteKey) -> {
                    VisualBlock.Blockquote(range = span.range)
                }

                span.attributes.containsKey(CodeBlockKey) -> {
                    val lang = span.attributes[CodeBlockKey]
                    VisualBlock.CodeBlock(range = span.range, language = lang)
                }

                else -> {
                    null
                }
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
                when {
                    !isContiguous -> {
                        false
                    }

                    currentBlock is VisualBlock.Blockquote && nextBlock is VisualBlock.Blockquote -> {
                        true
                    }

                    currentBlock is VisualBlock.CodeBlock && nextBlock is VisualBlock.CodeBlock -> {
                        currentBlock.language == nextBlock.language
                    }

                    else -> {
                        false
                    }
                }

            if (canMerge) {
                currentBlock =
                    when (currentBlock) {
                        is VisualBlock.Blockquote -> {
                            VisualBlock.Blockquote(range = currentBlock.range.first..nextBlock.range.last)
                        }

                        is VisualBlock.CodeBlock -> {
                            VisualBlock.CodeBlock(
                                range = currentBlock.range.first..nextBlock.range.last,
                                language = currentBlock.language,
                            )
                        }
                    }
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
