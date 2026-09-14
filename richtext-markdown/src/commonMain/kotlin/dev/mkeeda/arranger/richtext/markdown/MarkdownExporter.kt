package dev.mkeeda.arranger.richtext.markdown

import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.RichTextExporter
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.UnderlineKey

internal class MarkdownExporter : RichTextExporter<String> {
    override fun export(richString: RichString): String {
        if (richString.text.isEmpty()) return ""

        val lines = richString.text.split('\n')
        val result = StringBuilder()
        var currentOffset = 0
        val orderedListCounter = mutableMapOf<Int, Int>()

        for ((index, line) in lines.withIndex()) {
            val lineRange = currentOffset until (currentOffset + line.length)
            val lineSpans =
                richString.spans.filter { span ->
                    maxOf(lineRange.first, span.range.first) <= minOf(lineRange.last, span.range.last)
                }

            // Paragraph attributes
            val heading = lineSpans.firstNotNullOfOrNull { it.attributes[HeadingKey] }
            val isBlockquote = lineSpans.any { it.attributes.containsKey(BlockquoteKey) }
            val bulletList = lineSpans.firstNotNullOfOrNull { it.attributes[BulletListKey] }
            val orderedList = lineSpans.firstNotNullOfOrNull { it.attributes[OrderedListKey] }

            // Handle ordered list numbering
            if (orderedList != null) {
                val depth = orderedList.ordinal
                val count = (orderedListCounter[depth] ?: 0) + 1
                orderedListCounter[depth] = count
                orderedListCounter.keys.filter { it > depth }.forEach { orderedListCounter.remove(it) }
            } else {
                orderedListCounter.clear()
            }

            val prefix =
                when {
                    heading != null && heading != HeadingLevel.Unspecified -> {
                        "#".repeat(heading.ordinal + 1) + " "
                    }

                    isBlockquote -> {
                        "> "
                    }

                    bulletList != null && bulletList != ListIndentLevel.Unspecified -> {
                        "  ".repeat(bulletList.ordinal) + "* "
                    }

                    orderedList != null && orderedList != ListIndentLevel.Unspecified -> {
                        val count = orderedListCounter[orderedList.ordinal] ?: 1
                        "   ".repeat(orderedList.ordinal) + "$count. "
                    }

                    else -> {
                        ""
                    }
                }

            result.append(prefix)
            exportInlineSpans(line, currentOffset, lineSpans, result)

            if (index < lines.size - 1) {
                result.append('\n')
            }

            currentOffset += line.length + 1
        }

        return result.toString()
    }

    private fun exportInlineSpans(
        line: String,
        lineOffset: Int,
        lineSpans: List<RichSpan>,
        out: StringBuilder,
    ) {
        if (line.isEmpty()) return

        val precomputedSpans =
            lineSpans.map { span ->
                PrecomputedSpan(
                    range = span.range,
                    attributes = span.attributes.filterKeys { it is SpanAttributeKey<*> },
                )
            }

        val boundaries = mutableListOf(0, line.length)
        for (span in precomputedSpans) {
            boundaries.add(maxOf(0, span.range.first - lineOffset))
            boundaries.add(minOf(line.length, span.range.last + 1 - lineOffset))
        }
        val sortedBoundaries = boundaries.filter { it in 0..line.length }.distinct().sorted()

        val slices = mutableListOf<Slice>()
        for (i in 0 until sortedBoundaries.size - 1) {
            val startOffset = sortedBoundaries[i]
            val endOffset = sortedBoundaries[i + 1]
            if (startOffset < endOffset) {
                val globalIdx = lineOffset + startOffset
                val sliceAttrs =
                    precomputedSpans
                        .filter { globalIdx in it.range }
                        .fold(AttributeContainer.empty()) { acc, s -> acc + s.attributes }
                slices.add(Slice(text = line.substring(startOffset, endOffset), attributes = sliceAttrs))
            }
        }

        val activeStack = mutableListOf<Delimiter>()

        for (slice in slices) {
            val requiredDelimiters = ALL_POSSIBLE_DELIMITERS.mapNotNull { it(slice.attributes) }

            // Find first mismatch in activeStack
            var mismatchIndex = 0
            while (mismatchIndex < activeStack.size &&
                mismatchIndex < requiredDelimiters.size &&
                activeStack[mismatchIndex] == requiredDelimiters[mismatchIndex]
            ) {
                mismatchIndex++
            }

            // Close from top of activeStack down to mismatchIndex
            for (i in activeStack.size - 1 downTo mismatchIndex) {
                out.append(activeStack[i].closeStr)
            }
            while (activeStack.size > mismatchIndex) {
                activeStack.removeAt(activeStack.size - 1)
            }

            // Open remaining required delimiters
            for (i in mismatchIndex until requiredDelimiters.size) {
                val delim = requiredDelimiters[i]
                out.append(delim.openStr)
                activeStack.add(delim)
            }

            out.append(slice.text)
        }

        // Close all remaining delimiters
        for (i in activeStack.size - 1 downTo 0) {
            out.append(activeStack[i].closeStr)
        }
    }
}

private data class PrecomputedSpan(val range: IntRange, val attributes: AttributeContainer)

private data class Slice(val text: String, val attributes: AttributeContainer)

private val ALL_POSSIBLE_DELIMITERS: List<(AttributeContainer) -> Delimiter?> =
    listOf(
        { attrs -> attrs[LinkKey]?.takeIf { it.isNotEmpty() }?.let { Delimiter.Link(it) } },
        { attrs -> if (attrs.containsKey(UnderlineKey)) Delimiter.Underline else null },
        { attrs -> if (attrs.containsKey(StrikethroughKey)) Delimiter.Strikethrough else null },
        { attrs -> if (attrs.containsKey(BoldKey)) Delimiter.Bold else null },
        { attrs -> if (attrs.containsKey(ItalicKey)) Delimiter.Italic else null },
    )

private sealed interface Delimiter {
    val openStr: String
    val closeStr: String

    fun matches(attrs: AttributeContainer): Boolean

    data object Bold : Delimiter {
        override val openStr: String = "**"
        override val closeStr: String = "**"

        override fun matches(attrs: AttributeContainer): Boolean = attrs.containsKey(BoldKey)
    }

    data object Italic : Delimiter {
        override val openStr: String = "*"
        override val closeStr: String = "*"

        override fun matches(attrs: AttributeContainer): Boolean = attrs.containsKey(ItalicKey)
    }

    data object Strikethrough : Delimiter {
        override val openStr: String = "~~"
        override val closeStr: String = "~~"

        override fun matches(attrs: AttributeContainer): Boolean = attrs.containsKey(StrikethroughKey)
    }

    data object Underline : Delimiter {
        override val openStr: String = "<u>"
        override val closeStr: String = "</u>"

        override fun matches(attrs: AttributeContainer): Boolean = attrs.containsKey(UnderlineKey)
    }

    data class Link(val url: String) : Delimiter {
        override val openStr: String = "["
        override val closeStr: String = "]($url)"

        override fun matches(attrs: AttributeContainer): Boolean = attrs[LinkKey] == url
    }
}
