package dev.mkeeda.arranger.richtext.html

import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.RichTextExporter
import dev.mkeeda.arranger.richtext.RichTextFormat
import dev.mkeeda.arranger.richtext.RichTextImporter
import dev.mkeeda.arranger.richtext.export
import dev.mkeeda.arranger.richtext.import

/**
 * A bi-directional [RichTextFormat] for converting between [RichString] and HTML format.
 */
public object HtmlFormat :
    RichTextFormat<String>,
    RichTextExporter<String> by HtmlExporter(),
    RichTextImporter<String> by HtmlImporter()

/**
 * Converts this [RichString] to an HTML-formatted [String].
 */
public fun RichString.toHtml(): String = export(HtmlFormat)

/**
 * Parses HTML-formatted text into a [RichString].
 */
public fun RichString.Companion.fromHtml(html: String): RichString = import(html, HtmlFormat)
