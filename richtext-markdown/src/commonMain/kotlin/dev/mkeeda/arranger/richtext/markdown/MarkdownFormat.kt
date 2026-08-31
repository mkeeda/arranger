package dev.mkeeda.arranger.richtext.markdown

import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.RichTextExporter
import dev.mkeeda.arranger.richtext.RichTextFormat
import dev.mkeeda.arranger.richtext.RichTextImporter
import dev.mkeeda.arranger.richtext.export
import dev.mkeeda.arranger.richtext.import

/**
 * A bi-directional [RichTextFormat] for converting between [RichString] and Markdown format.
 */
public object MarkdownFormat :
    RichTextFormat<String>,
    RichTextExporter<String> by MarkdownExporter(),
    RichTextImporter<String> by MarkdownImporter()

/**
 * Converts this [RichString] to a Markdown-formatted [String].
 */
public fun RichString.toMarkdown(): String = export(MarkdownFormat)

/**
 * Parses Markdown-formatted text into a [RichString].
 */
public fun RichString.Companion.fromMarkdown(markdown: String): RichString = import(markdown, MarkdownFormat)
