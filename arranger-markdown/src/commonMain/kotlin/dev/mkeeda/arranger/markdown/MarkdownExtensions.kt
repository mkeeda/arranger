package dev.mkeeda.arranger.markdown

import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.export
import dev.mkeeda.arranger.richtext.import

/**
 * Converts this [RichString] to a Markdown-formatted [String].
 */
public fun RichString.toMarkdown(): String = export(MarkdownExporter)

/**
 * Parses Markdown-formatted text into a [RichString].
 */
public fun RichString.Companion.fromMarkdown(markdown: String): RichString = import(markdown, MarkdownImporter)
