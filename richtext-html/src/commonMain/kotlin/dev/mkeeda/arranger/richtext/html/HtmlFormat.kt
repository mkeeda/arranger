package dev.mkeeda.arranger.richtext.html

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
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
import dev.mkeeda.arranger.richtext.RichTextFormat
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.export
import dev.mkeeda.arranger.richtext.import

/**
 * A bi-directional [RichTextFormat] for converting between [RichString] and HTML format.
 */
public object HtmlFormat : RichTextFormat<String> {
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

        // Compute attribute container for each character in the line
        val charAttributes =
            Array(line.length) { charIdx ->
                val globalIdx = lineOffset + charIdx
                var container = AttributeContainer.empty()
                for (span in lineSpans) {
                    if (globalIdx in span.range) {
                        val spanAttrs = span.attributes.filterKeys { it is SpanAttributeKey<*> }
                        container += spanAttrs
                    }
                }
                container
            }

        // Group into contiguous slices
        data class Slice(val text: String, val attributes: AttributeContainer)
        val slices = mutableListOf<Slice>()
        var sliceStart = 0
        while (sliceStart < line.length) {
            var sliceEnd = sliceStart + 1
            while (sliceEnd < line.length && charAttributes[sliceEnd] == charAttributes[sliceStart]) {
                sliceEnd++
            }
            slices.add(Slice(line.substring(sliceStart, sliceEnd), charAttributes[sliceStart]))
            sliceStart = sliceEnd
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

    override fun import(input: String): RichString {
        if (input.isEmpty()) return RichString("")

        val document = Ksoup.parseBodyFragment(input)
        val body = document.body()

        val textBuilder = StringBuilder()
        val spans = mutableListOf<RichSpan>()

        parseElementChildren(
            parent = body,
            textBuilder = textBuilder,
            spans = spans,
            activeSpanAttributes = AttributeContainer.empty(),
            listDepth = 0,
            isOrderedList = false,
        )

        return RichString(
            text = textBuilder.toString(),
            spans = spans,
        )
    }

    private fun parseElementChildren(
        parent: Element,
        textBuilder: StringBuilder,
        spans: MutableList<RichSpan>,
        activeSpanAttributes: AttributeContainer,
        listDepth: Int,
        isOrderedList: Boolean,
    ) {
        for (node in parent.childNodes()) {
            parseNode(node, textBuilder, spans, activeSpanAttributes, listDepth, isOrderedList)
        }
    }

    private fun parseNode(
        node: Node,
        textBuilder: StringBuilder,
        spans: MutableList<RichSpan>,
        activeSpanAttributes: AttributeContainer,
        listDepth: Int,
        isOrderedList: Boolean,
    ) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotEmpty()) {
                    val start = textBuilder.length
                    textBuilder.append(text)
                    val end = textBuilder.length
                    if (start < end && activeSpanAttributes.isNotEmpty()) {
                        spans.add(RichSpan(start until end, activeSpanAttributes))
                    }
                }
            }

            is Element -> {
                val tagName = node.tagName().lowercase()
                val inlineAttrs = extractInlineAttributes(node, activeSpanAttributes)

                when (tagName) {
                    "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        val level =
                            when (tagName) {
                                "h1" -> HeadingLevel.H1
                                "h2" -> HeadingLevel.H2
                                "h3" -> HeadingLevel.H3
                                "h4" -> HeadingLevel.H4
                                "h5" -> HeadingLevel.H5
                                else -> HeadingLevel.H6
                            }
                        if (textBuilder.isNotEmpty() && !textBuilder.endsWith('\n')) {
                            textBuilder.append('\n')
                        }
                        val start = textBuilder.length
                        parseElementChildren(node, textBuilder, spans, inlineAttrs, listDepth, isOrderedList)
                        val end = textBuilder.length
                        if (start < end) {
                            var paraAttrs = attributeContainerOf(HeadingKey to level)
                            val align = parseAlignment(node)
                            if (align != null) paraAttrs += TextAlignmentKey to align
                            spans.add(RichSpan(start until end, paraAttrs))
                        }
                    }

                    "blockquote" -> {
                        if (textBuilder.isNotEmpty() && !textBuilder.endsWith('\n')) {
                            textBuilder.append('\n')
                        }
                        val start = textBuilder.length
                        parseElementChildren(node, textBuilder, spans, inlineAttrs, listDepth, isOrderedList)
                        val end = textBuilder.length
                        if (start < end) {
                            var paraAttrs = attributeContainerOf(BlockquoteKey to Unit)
                            val align = parseAlignment(node)
                            if (align != null) paraAttrs += TextAlignmentKey to align
                            spans.add(RichSpan(start until end, paraAttrs))
                        }
                    }

                    "ul" -> {
                        parseElementChildren(node, textBuilder, spans, inlineAttrs, listDepth + 1, isOrderedList = false)
                    }

                    "ol" -> {
                        parseElementChildren(node, textBuilder, spans, inlineAttrs, listDepth + 1, isOrderedList = true)
                    }

                    "li" -> {
                        if (textBuilder.isNotEmpty() && !textBuilder.endsWith('\n')) {
                            textBuilder.append('\n')
                        }
                        val start = textBuilder.length
                        val indentLevel =
                            when (listDepth) {
                                1 -> ListIndentLevel.Level1
                                2 -> ListIndentLevel.Level2
                                3 -> ListIndentLevel.Level3
                                4 -> ListIndentLevel.Level4
                                5 -> ListIndentLevel.Level5
                                else -> ListIndentLevel.Level6
                            }

                        val nonListNodes = mutableListOf<Node>()
                        val listNodes = mutableListOf<Node>()
                        for (child in node.childNodes()) {
                            if (child is Element && (
                                    child.tagName().equals(
                                        "ul",
                                        ignoreCase = true,
                                    ) || child.tagName().equals("ol", ignoreCase = true)
                                )
                            ) {
                                listNodes.add(child)
                            } else {
                                nonListNodes.add(child)
                            }
                        }

                        for (child in nonListNodes) {
                            parseNode(child, textBuilder, spans, inlineAttrs, listDepth, isOrderedList)
                        }

                        val end = textBuilder.length
                        if (start < end) {
                            val key = if (isOrderedList) OrderedListKey else BulletListKey

                            @Suppress("UNCHECKED_CAST")
                            var paraAttrs = attributeContainerOf(key to indentLevel)
                            val align = parseAlignment(node)
                            if (align != null) paraAttrs += TextAlignmentKey to align
                            spans.add(RichSpan(start until end, paraAttrs))
                        }

                        for (child in listNodes) {
                            parseNode(child, textBuilder, spans, inlineAttrs, listDepth, isOrderedList)
                        }
                    }

                    "p" -> {
                        if (textBuilder.isNotEmpty() && !textBuilder.endsWith('\n')) {
                            textBuilder.append('\n')
                        }
                        val start = textBuilder.length
                        parseElementChildren(node, textBuilder, spans, inlineAttrs, listDepth, isOrderedList)
                        val end = textBuilder.length
                        val align = parseAlignment(node)
                        if (start < end && align != null) {
                            spans.add(RichSpan(start until end, attributeContainerOf(TextAlignmentKey to align)))
                        }
                    }

                    "br" -> {
                        textBuilder.append('\n')
                    }

                    else -> {
                        parseElementChildren(node, textBuilder, spans, inlineAttrs, listDepth, isOrderedList)
                    }
                }
            }
        }
    }

    private fun extractInlineAttributes(
        element: Element,
        parentAttrs: AttributeContainer,
    ): AttributeContainer {
        var result = parentAttrs
        val tag = element.tagName().lowercase()

        when (tag) {
            "strong", "b" -> {
                result += BoldKey to Unit
            }

            "em", "i" -> {
                result += ItalicKey to Unit
            }

            "s", "del", "strike" -> {
                result += StrikethroughKey to Unit
            }

            "u" -> {
                result += UnderlineKey to Unit
            }

            "a" -> {
                val href = element.attr("href")
                if (href.isNotEmpty()) {
                    result += LinkKey to href
                }
            }
        }

        val styleAttr = element.attr("style")
        if (styleAttr.isNotEmpty()) {
            val styleMap = parseCssStyle(styleAttr)

            val color = styleMap["color"]?.let { parseCssColor(it) }
            if (color != null) result += TextColorKey to color

            val bgColor = styleMap["background-color"]?.let { parseCssColor(it) }
            if (bgColor != null) result += BackgroundColorKey to bgColor

            val fontSize = styleMap["font-size"]?.let { parseCssFontSize(it) }
            if (fontSize != null) result += FontSizeKey to fontSize
        }

        return result
    }

    private fun parseAlignment(element: Element): TextAlignment? {
        val styleAttr = element.attr("style")
        if (styleAttr.isNotEmpty()) {
            val styleMap = parseCssStyle(styleAttr)
            when (styleMap["text-align"]?.lowercase()) {
                "left" -> return TextAlignment.Left
                "center" -> return TextAlignment.Center
                "right" -> return TextAlignment.Right
                "justify" -> return TextAlignment.Justify
            }
        }

        val alignAttr = element.attr("align").lowercase()
        return when (alignAttr) {
            "left" -> TextAlignment.Left
            "center" -> TextAlignment.Center
            "right" -> TextAlignment.Right
            "justify" -> TextAlignment.Justify
            else -> null
        }
    }

    private fun parseCssStyle(style: String): Map<String, String> {
        return style.split(';')
            .mapNotNull { declaration ->
                val colonIdx = declaration.indexOf(':')
                if (colonIdx > 0) {
                    val key = declaration.substring(0, colonIdx).trim().lowercase()
                    val value = declaration.substring(colonIdx + 1).trim()
                    key to value
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun parseCssColor(colorStr: String): RgbaColor? {
        val trimmed = colorStr.trim().lowercase()
        if (trimmed.startsWith("#")) {
            val hex = trimmed.removePrefix("#")
            return when (hex.length) {
                3 -> {
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    RgbaColor(0xFF000000L or ((r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()))
                }

                6 -> {
                    val value = hex.toLongOrNull(16) ?: return null
                    RgbaColor(0xFF000000L or value)
                }

                8 -> {
                    val value = hex.toLongOrNull(16) ?: return null
                    RgbaColor(value)
                }

                else -> {
                    null
                }
            }
        } else if (trimmed.startsWith("rgb")) {
            val content = trimmed.substringAfter('(').substringBefore(')')
            val parts = content.split(',').map { it.trim() }
            if (parts.size >= 3) {
                val r = parts[0].toIntOrNull() ?: 0
                val g = parts[1].toIntOrNull() ?: 0
                val b = parts[2].toIntOrNull() ?: 0
                val a = if (parts.size >= 4) (parts[3].toFloatOrNull() ?: 1f) else 1f
                val alphaInt = (a * 255).toInt().coerceIn(0, 255)
                return RgbaColor((alphaInt.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
            }
        }
        return null
    }

    private fun parseCssFontSize(fontSizeStr: String): TextSize? {
        val cleaned =
            fontSizeStr.trim().lowercase()
                .removeSuffix("sp")
                .removeSuffix("px")
                .removeSuffix("pt")
        val floatVal = cleaned.toFloatOrNull() ?: return null
        return TextSize(floatVal)
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

/**
 * Converts this [RichString] to an HTML-formatted [String].
 */
public fun RichString.toHtml(): String = export(HtmlFormat)

/**
 * Parses HTML-formatted text into a [RichString].
 */
public fun RichString.Companion.fromHtml(html: String): RichString = import(html, HtmlFormat)
