package dev.mkeeda.arranger.richtext.html

import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.RichTextExporter
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.UnderlineKey

internal class HtmlExporter : RichTextExporter<String> {
    override fun export(richString: RichString): String {
        if (richString.text.isEmpty()) return ""

        val lines = richString.text.split('\n')
        val out = StringBuilder()
        var currentOffset = 0

        // Track lists hierarchy
        data class ListContext(val isOrdered: Boolean, val level: Int)
        val listStack = mutableListOf<ListContext>()

        for ((index, line) in lines.withIndex()) {
            val lineRange = currentOffset until (currentOffset + line.length)
            val lineSpans =
                richString.spans.filter { span ->
                    maxOf(lineRange.first, span.range.first) <= minOf(lineRange.last, span.range.last)
                }

            val heading = lineSpans.firstNotNullOfOrNull { it.attributes[HeadingKey] }
            val isBlockquote = lineSpans.any { it.attributes.containsKey(BlockquoteKey) }
            val bulletList = lineSpans.firstNotNullOfOrNull { it.attributes[BulletListKey] }
            val orderedList = lineSpans.firstNotNullOfOrNull { it.attributes[OrderedListKey] }
            val alignment = lineSpans.firstNotNullOfOrNull { it.attributes[TextAlignmentKey] }

            val listLevel =
                when {
                    bulletList != null && bulletList != ListIndentLevel.Unspecified -> bulletList.ordinal + 1
                    orderedList != null && orderedList != ListIndentLevel.Unspecified -> orderedList.ordinal + 1
                    else -> 0
                }
            val isOrdered = orderedList != null

            if (listLevel > 0) {
                // Adjust list stack to reach target listLevel
                while (listStack.size > listLevel) {
                    val top = listStack.removeAt(listStack.size - 1)
                    val tag = if (top.isOrdered) "ol" else "ul"
                    out.append("</li></$tag>")
                }

                // If the list type changed at the same level, close the previous list
                if (listStack.isNotEmpty() && listStack.size == listLevel && listStack.last().isOrdered != isOrdered) {
                    val top = listStack.removeAt(listStack.size - 1)
                    val tag = if (top.isOrdered) "ol" else "ul"
                    out.append("</li></$tag>")
                }

                if (listStack.size < listLevel) {
                    for (lvl in (listStack.size + 1)..listLevel) {
                        val tag = if (isOrdered) "ol" else "ul"
                        out.append("<$tag><li>")
                        listStack.add(ListContext(isOrdered, lvl))
                    }
                } else {
                    out.append("</li><li>")
                }

                exportInlineHtml(line, currentOffset, lineSpans, out)
            } else {
                // Close any open lists
                while (listStack.isNotEmpty()) {
                    val top = listStack.removeAt(listStack.size - 1)
                    val tag = if (top.isOrdered) "ol" else "ul"
                    out.append("</li></$tag>")
                }

                val alignStyle =
                    when (alignment) {
                        TextAlignment.Left -> " style=\"text-align: left;\""
                        TextAlignment.Center -> " style=\"text-align: center;\""
                        TextAlignment.Right -> " style=\"text-align: right;\""
                        TextAlignment.Justify -> " style=\"text-align: justify;\""
                        else -> ""
                    }

                when {
                    heading != null && heading != HeadingLevel.Unspecified -> {
                        val tag = "h${heading.ordinal + 1}"
                        out.append("<$tag$alignStyle>")
                        exportInlineHtml(line, currentOffset, lineSpans, out)
                        out.append("</$tag>")
                    }

                    isBlockquote -> {
                        out.append("<blockquote><p$alignStyle>")
                        exportInlineHtml(line, currentOffset, lineSpans, out)
                        out.append("</p></blockquote>")
                    }

                    else -> {
                        out.append("<p$alignStyle>")
                        exportInlineHtml(line, currentOffset, lineSpans, out)
                        out.append("</p>")
                    }
                }
            }

            currentOffset += line.length + 1
        }

        // Close any remaining open lists
        while (listStack.isNotEmpty()) {
            val top = listStack.removeAt(listStack.size - 1)
            val tag = if (top.isOrdered) "ol" else "ul"
            out.append("</li></$tag>")
        }

        return out.toString()
    }

    private fun exportInlineHtml(
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

        for (slice in slices) {
            val attrs = slice.attributes
            var openTags = ""
            var closeTags = ""

            // Link
            val link = attrs[LinkKey]
            if (!link.isNullOrEmpty()) {
                openTags += "<a href=\"${escapeHtmlAttribute(link)}\">"
                closeTags = "</a>$closeTags"
            }

            // CSS Styles (color, background-color, font-size)
            val styles = mutableListOf<String>()
            val textColor = attrs[TextColorKey]
            if (textColor != null && textColor != RgbaColor.Unspecified) {
                styles.add("color: ${formatColorHex(textColor)};")
            }
            val bgColor = attrs[BackgroundColorKey]
            if (bgColor != null && bgColor != RgbaColor.Unspecified) {
                styles.add("background-color: ${formatColorHex(bgColor)};")
            }
            val fontSize = attrs[FontSizeKey]
            if (fontSize != null && !fontSize.sp.isNaN()) {
                styles.add("font-size: ${fontSize.sp}sp;")
            }

            if (styles.isNotEmpty()) {
                val styleString = styles.joinToString(" ")
                openTags += "<span style=\"$styleString\">"
                closeTags = "</span>$closeTags"
            }

            // Bold
            if (attrs.containsKey(BoldKey)) {
                openTags += "<strong>"
                closeTags = "</strong>$closeTags"
            }

            // Italic
            if (attrs.containsKey(ItalicKey)) {
                openTags += "<em>"
                closeTags = "</em>$closeTags"
            }

            // Strikethrough
            if (attrs.containsKey(StrikethroughKey)) {
                openTags += "<s>"
                closeTags = "</s>$closeTags"
            }

            // Underline
            if (attrs.containsKey(UnderlineKey)) {
                openTags += "<u>"
                closeTags = "</u>$closeTags"
            }

            out.append(openTags)
            out.append(escapeHtmlText(slice.text))
            out.append(closeTags)
        }
    }

    private fun formatColorHex(color: RgbaColor): String {
        val r = ((color.value shr 16) and 0xFF).toString(16).padStart(2, '0')
        val g = ((color.value shr 8) and 0xFF).toString(16).padStart(2, '0')
        val b = (color.value and 0xFF).toString(16).padStart(2, '0')
        return "#$r$g$b"
    }

    private fun escapeHtmlText(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun escapeHtmlAttribute(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}

private data class PrecomputedSpan(val range: IntRange, val attributes: AttributeContainer)

private data class Slice(val text: String, val attributes: AttributeContainer)
