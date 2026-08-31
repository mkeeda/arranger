package dev.mkeeda.arranger.richtext.markdown

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
import dev.mkeeda.arranger.richtext.RichTextImporter
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

internal class MarkdownImporter : RichTextImporter<String> {
    override fun import(input: String): RichString {
        if (input.isEmpty()) return RichString("")

        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(input)

        val builder = StringBuilder()
        val spans = mutableListOf<RichSpan>()
        val parseContext = ParseContext()

        parseNode(
            node = parsedTree,
            markdown = input,
            textBuilder = builder,
            spans = spans,
            activeSpanAttributes = emptyMap(),
            listDepth = 0,
            context = parseContext,
        )

        if (parseContext.underlineStart in 0 until builder.length) {
            spans.add(RichSpan(parseContext.underlineStart until builder.length, attributeContainerOf(UnderlineKey to Unit)))
        }

        return RichString(
            text = builder.toString(),
            spans = spans,
        )
    }

    private companion object {
        private val flavour = GFMFlavourDescriptor()
    }

    private class ParseContext {
        var underlineStart: Int = -1
    }

    private fun parseNode(
        node: ASTNode,
        markdown: String,
        textBuilder: StringBuilder,
        spans: MutableList<RichSpan>,
        activeSpanAttributes: Map<SpanAttributeKey<*>, Any>,
        listDepth: Int,
        context: ParseContext,
    ) {
        val type = node.type

        when (type) {
            MarkdownElementTypes.MARKDOWN_FILE -> {
                for (child in node.children) {
                    if (child.type != MarkdownTokenTypes.EOL && child.type != MarkdownTokenTypes.WHITE_SPACE) {
                        if (textBuilder.isNotEmpty() && !textBuilder.endsWith('\n')) {
                            textBuilder.append('\n')
                        }
                    }
                    parseNode(child, markdown, textBuilder, spans, activeSpanAttributes, listDepth, context)
                }
            }

            MarkdownElementTypes.ATX_1,
            MarkdownElementTypes.ATX_2,
            MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4,
            MarkdownElementTypes.ATX_5,
            MarkdownElementTypes.ATX_6,
            -> {
                val level =
                    when (type) {
                        MarkdownElementTypes.ATX_1 -> HeadingLevel.H1
                        MarkdownElementTypes.ATX_2 -> HeadingLevel.H2
                        MarkdownElementTypes.ATX_3 -> HeadingLevel.H3
                        MarkdownElementTypes.ATX_4 -> HeadingLevel.H4
                        MarkdownElementTypes.ATX_5 -> HeadingLevel.H5
                        else -> HeadingLevel.H6
                    }
                val start = textBuilder.length
                for (child in node.children) {
                    if (child.type == MarkdownTokenTypes.ATX_CONTENT) {
                        parseInlineChildren(
                            child,
                            markdown,
                            textBuilder,
                            spans,
                            activeSpanAttributes,
                            context,
                            trimLeadingWhitespace = true,
                        )
                    }
                }
                val end = textBuilder.length
                if (start < end) {
                    spans.add(
                        RichSpan(
                            range = start until end,
                            attributes = attributeContainerOf(HeadingKey to level),
                        ),
                    )
                }
            }

            MarkdownElementTypes.SETEXT_1,
            MarkdownElementTypes.SETEXT_2,
            -> {
                val level = if (type == MarkdownElementTypes.SETEXT_1) HeadingLevel.H1 else HeadingLevel.H2
                val start = textBuilder.length
                for (child in node.children) {
                    val childText = markdown.substring(child.startOffset, child.endOffset).trim()
                    if (childText.isNotEmpty() && !childText.all { it == '=' || it == '-' }) {
                        parseInlineChildren(child, markdown, textBuilder, spans, activeSpanAttributes, context)
                    }
                }
                val end = textBuilder.length
                if (start < end) {
                    spans.add(
                        RichSpan(
                            range = start until end,
                            attributes = attributeContainerOf(HeadingKey to level),
                        ),
                    )
                }
            }

            MarkdownElementTypes.BLOCK_QUOTE -> {
                val start = textBuilder.length
                for (child in node.children) {
                    if (child.type != MarkdownTokenTypes.BLOCK_QUOTE) {
                        parseNode(child, markdown, textBuilder, spans, activeSpanAttributes, listDepth, context)
                    }
                }
                val end = textBuilder.length
                if (start < end) {
                    spans.add(
                        RichSpan(
                            range = start until end,
                            attributes = attributeContainerOf(BlockquoteKey to Unit),
                        ),
                    )
                }
            }

            MarkdownElementTypes.UNORDERED_LIST -> {
                for (child in node.children) {
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        parseListItem(
                            node = child,
                            markdown = markdown,
                            textBuilder = textBuilder,
                            spans = spans,
                            activeSpanAttributes = activeSpanAttributes,
                            listDepth = listDepth + 1,
                            isOrdered = false,
                            context = context,
                        )
                    }
                }
            }

            MarkdownElementTypes.ORDERED_LIST -> {
                for (child in node.children) {
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        parseListItem(
                            node = child,
                            markdown = markdown,
                            textBuilder = textBuilder,
                            spans = spans,
                            activeSpanAttributes = activeSpanAttributes,
                            listDepth = listDepth + 1,
                            isOrdered = true,
                            context = context,
                        )
                    }
                }
            }

            MarkdownElementTypes.PARAGRAPH -> {
                parseInlineChildren(node, markdown, textBuilder, spans, activeSpanAttributes, context)
            }

            else -> {
                parseInlineChildren(node, markdown, textBuilder, spans, activeSpanAttributes, context)
            }
        }
    }

    private fun parseListItem(
        node: ASTNode,
        markdown: String,
        textBuilder: StringBuilder,
        spans: MutableList<RichSpan>,
        activeSpanAttributes: Map<SpanAttributeKey<*>, Any>,
        listDepth: Int,
        isOrdered: Boolean,
        context: ParseContext,
    ) {
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

        val nestedLists = mutableListOf<ASTNode>()
        var seenBulletOrNumber = false
        var skippedPostBulletWhitespace = false

        for (child in node.children) {
            if (!seenBulletOrNumber) {
                if (child.type == MarkdownTokenTypes.LIST_BULLET || child.type == MarkdownTokenTypes.LIST_NUMBER) {
                    seenBulletOrNumber = true
                }
                continue
            }
            if (!skippedPostBulletWhitespace && child.type == MarkdownTokenTypes.WHITE_SPACE) {
                skippedPostBulletWhitespace = true
                continue
            }

            if (child.type == MarkdownElementTypes.UNORDERED_LIST || child.type == MarkdownElementTypes.ORDERED_LIST) {
                nestedLists.add(child)
            } else if (child.type == MarkdownElementTypes.PARAGRAPH) {
                parseInlineChildren(child, markdown, textBuilder, spans, activeSpanAttributes, context)
            } else {
                parseInlineChildren(child, markdown, textBuilder, spans, activeSpanAttributes, context)
            }
        }

        val end = textBuilder.length
        if (start < end) {
            val key = if (isOrdered) OrderedListKey else BulletListKey
            @Suppress("UNCHECKED_CAST")
            spans.add(
                RichSpan(
                    range = start until end,
                    attributes = attributeContainerOf(key to indentLevel),
                ),
            )
        }

        // Process nested lists after recording this item's span
        for (nestedList in nestedLists) {
            parseNode(nestedList, markdown, textBuilder, spans, activeSpanAttributes, listDepth, context)
        }
    }

    private fun parseInlineChildren(
        node: ASTNode,
        markdown: String,
        textBuilder: StringBuilder,
        spans: MutableList<RichSpan>,
        activeSpanAttributes: Map<SpanAttributeKey<*>, Any>,
        context: ParseContext,
        trimLeadingWhitespace: Boolean = false,
    ) {
        val type = node.type

        when (type) {
            MarkdownElementTypes.STRONG -> {
                val start = textBuilder.length
                val newActive = activeSpanAttributes + (BoldKey to Unit)
                for (child in node.children) {
                    if (child.type != MarkdownTokenTypes.EMPH) {
                        parseInlineChildren(child, markdown, textBuilder, spans, newActive, context)
                    }
                }
                val end = textBuilder.length
                if (start < end) {
                    spans.add(RichSpan(start until end, attributeContainerOf(BoldKey to Unit)))
                }
            }

            MarkdownElementTypes.EMPH -> {
                val start = textBuilder.length
                val newActive = activeSpanAttributes + (ItalicKey to Unit)
                for (child in node.children) {
                    if (child.type != MarkdownTokenTypes.EMPH) {
                        parseInlineChildren(child, markdown, textBuilder, spans, newActive, context)
                    }
                }
                val end = textBuilder.length
                if (start < end) {
                    spans.add(RichSpan(start until end, attributeContainerOf(ItalicKey to Unit)))
                }
            }

            GFMElementTypes.STRIKETHROUGH -> {
                val start = textBuilder.length
                val newActive = activeSpanAttributes + (StrikethroughKey to Unit)
                for (child in node.children) {
                    if (child.type != GFMTokenTypes.TILDE) {
                        parseInlineChildren(child, markdown, textBuilder, spans, newActive, context)
                    }
                }
                val end = textBuilder.length
                if (start < end) {
                    spans.add(RichSpan(start until end, attributeContainerOf(StrikethroughKey to Unit)))
                }
            }

            MarkdownElementTypes.INLINE_LINK -> {
                val linkTextNode = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                val destinationNode = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
                val url = destinationNode?.let { markdown.substring(it.startOffset, it.endOffset) } ?: ""

                val start = textBuilder.length
                val newActive = if (url.isNotEmpty()) activeSpanAttributes + (LinkKey to url) else activeSpanAttributes

                if (linkTextNode != null) {
                    for (child in linkTextNode.children) {
                        if (child.type != MarkdownTokenTypes.LBRACKET && child.type != MarkdownTokenTypes.RBRACKET) {
                            parseInlineChildren(child, markdown, textBuilder, spans, newActive, context)
                        }
                    }
                }
                val end = textBuilder.length
                if (start < end && url.isNotEmpty()) {
                    spans.add(RichSpan(start until end, attributeContainerOf(LinkKey to url)))
                }
            }

            MarkdownElementTypes.AUTOLINK -> {
                val text = markdown.substring(node.startOffset, node.endOffset).removeSurrounding("<", ">")
                val start = textBuilder.length
                textBuilder.append(text)
                val end = textBuilder.length
                if (start < end) {
                    spans.add(RichSpan(start until end, attributeContainerOf(LinkKey to text)))
                }
            }

            MarkdownTokenTypes.HTML_TAG -> {
                val rawHtml = markdown.substring(node.startOffset, node.endOffset)
                if (rawHtml.equals("<u>", ignoreCase = true)) {
                    context.underlineStart = textBuilder.length
                } else if (rawHtml.equals("</u>", ignoreCase = true)) {
                    if (context.underlineStart >= 0 && context.underlineStart < textBuilder.length) {
                        spans.add(RichSpan(context.underlineStart until textBuilder.length, attributeContainerOf(UnderlineKey to Unit)))
                    }
                    context.underlineStart = -1
                } else {
                    textBuilder.append(rawHtml)
                }
            }

            MarkdownTokenTypes.TEXT -> {
                var text = markdown.substring(node.startOffset, node.endOffset)
                if (trimLeadingWhitespace) {
                    text = text.trimStart()
                }
                handleTextWithInlineHtml(text, textBuilder, spans, activeSpanAttributes, context)
            }

            MarkdownTokenTypes.WHITE_SPACE -> {
                if (!trimLeadingWhitespace) {
                    val ws = markdown.substring(node.startOffset, node.endOffset)
                    textBuilder.append(ws)
                }
            }

            MarkdownElementTypes.CODE_SPAN -> {
                for (child in node.children) {
                    if (child.type != MarkdownTokenTypes.BACKTICK) {
                        textBuilder.append(markdown.substring(child.startOffset, child.endOffset))
                    }
                }
                if (node.children.isEmpty()) {
                    val raw = markdown.substring(node.startOffset, node.endOffset).removeSurrounding("`")
                    textBuilder.append(raw)
                }
            }

            else -> {
                if (node.children.isNotEmpty()) {
                    for ((idx, child) in node.children.withIndex()) {
                        parseInlineChildren(
                            child,
                            markdown,
                            textBuilder,
                            spans,
                            activeSpanAttributes,
                            context,
                            trimLeadingWhitespace = trimLeadingWhitespace && idx == 0,
                        )
                    }
                } else {
                    var text = markdown.substring(node.startOffset, node.endOffset)
                    if (trimLeadingWhitespace) {
                        text = text.trimStart()
                    }
                    handleTextWithInlineHtml(text, textBuilder, spans, activeSpanAttributes, context)
                }
            }
        }
    }

    private fun handleTextWithInlineHtml(
        text: String,
        textBuilder: StringBuilder,
        spans: MutableList<RichSpan>,
        activeSpanAttributes: Map<SpanAttributeKey<*>, Any>,
        context: ParseContext,
    ) {
        if (!text.contains("<u>", ignoreCase = true)) {
            textBuilder.append(text)
            return
        }

        // Parse <u> ... </u> within text
        var remaining = text
        while (remaining.isNotEmpty()) {
            val uStart = remaining.indexOf("<u>", ignoreCase = true)
            if (uStart == -1) {
                textBuilder.append(remaining)
                break
            }

            textBuilder.append(remaining.substring(0, uStart))
            val afterOpen = remaining.substring(uStart + 3)
            val uEnd = afterOpen.indexOf("</u>", ignoreCase = true)
            if (uEnd == -1) {
                val start = textBuilder.length
                textBuilder.append(afterOpen)
                val end = textBuilder.length
                if (start < end) {
                    spans.add(RichSpan(start until end, attributeContainerOf(UnderlineKey to Unit)))
                }
                break
            }

            val underlinedContent = afterOpen.substring(0, uEnd)
            val start = textBuilder.length
            textBuilder.append(underlinedContent)
            val end = textBuilder.length
            if (start < end) {
                spans.add(RichSpan(start until end, attributeContainerOf(UnderlineKey to Unit)))
            }

            remaining = afterOpen.substring(uEnd + 4)
        }
    }
}
