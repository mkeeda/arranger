package dev.mkeeda.arranger.runtime

import dev.mkeeda.arranger.runtime.node.HeadingLevel
import dev.mkeeda.arranger.runtime.node.HeadingNode
import dev.mkeeda.arranger.runtime.node.LinkNode
import dev.mkeeda.arranger.runtime.node.MarkupTextNode
import dev.mkeeda.arranger.runtime.node.ParagraphNode
import dev.mkeeda.arranger.runtime.node.RootNode
import dev.mkeeda.arranger.runtime.node.TextNode

/**
 * Simple markup text language syntax
 *
 * Headings
 * ```
 * Level 1: #
 * Level 2: ##
 * Level 3: ###
 * ```
 *
 * Text
 * ```
 * any text
 * ```
 *
 * Link
 * ```
 * any text [https://example.com] text
 * ```
 *
 * Paragraph
 * ```
 * hoge hoge # Paragraph1
 * any text [https://example.com] text # Paragraph2
 * ```
 *
 */

private val headingRegex = Regex("""^(#{1,3}) (.+)$""")
private val linkRegex = Regex("""^(.+)\[(.+)\]\((.+)\)(.+)$""")

internal class SimpleMarkupTextParser : MarkupTextParser {
    override fun parse(markupText: String): MarkupTextNode {
        val blocks = markupText.lines().map { line ->
            parseHeading(line)
                ?: parseLink(line)
                ?: parsePlainText(line)
        }
        return RootNode(children = blocks)
    }

    private fun parseHeading(line: String): HeadingNode? {
        return headingRegex.find(line)?.let {
            val (sharp, title) = it.destructured
            HeadingNode(
                level = when (sharp.length) {
                    1 -> HeadingLevel.H1
                    2 -> HeadingLevel.H2
                    3 -> HeadingLevel.H3
                    else -> error("Not supported heading level")
                },
                title = title
            )
        }
    }

    private fun parseLink(line: String): ParagraphNode? {
        return linkRegex.find(line)?.let {
            val (before, linkText, url, after) = it.destructured
            ParagraphNode(
                children = listOf(
                    TextNode(before),
                    LinkNode(url = url, text = linkText),
                    TextNode(after),
                )
            )
        }
    }

    private fun parsePlainText(line: String): ParagraphNode {
        return ParagraphNode(
            children = listOf(
                TextNode(line)
            )
        )
    }
}
