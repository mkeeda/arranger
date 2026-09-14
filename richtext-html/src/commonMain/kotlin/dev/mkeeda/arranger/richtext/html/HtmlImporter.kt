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
import dev.mkeeda.arranger.richtext.RichTextImporter
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf

internal class HtmlImporter : RichTextImporter<String> {
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
}
