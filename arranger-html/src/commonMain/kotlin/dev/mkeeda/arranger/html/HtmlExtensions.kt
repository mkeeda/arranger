package dev.mkeeda.arranger.html

import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.export
import dev.mkeeda.arranger.richtext.import

/**
 * Converts this [RichString] to an HTML-formatted [String].
 */
public fun RichString.toHtml(): String = export(HtmlExporter)

/**
 * Parses HTML-formatted text into a [RichString].
 */
public fun RichString.Companion.fromHtml(html: String): RichString = import(html, HtmlImporter)
