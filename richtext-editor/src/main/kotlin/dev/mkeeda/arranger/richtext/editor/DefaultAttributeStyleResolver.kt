package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.UnderlineKey

/**
 * The standard default [AttributeStyleResolver] mapping the semantic attributes to Compose [SpanStyle].
 */
public val DefaultAttributeStyleResolver: AttributeStyleResolver =
    AttributeStyleResolver {
        spanStyle(TextColorKey) { hex ->
            SpanStyle(color = hex.toColor())
        }
        spanStyle(BackgroundColorKey) { hex ->
            SpanStyle(background = hex.toColor())
        }
        spanStyle(FontSizeKey) { size ->
            SpanStyle(fontSize = size.toTextUnit())
        }
        spanStyle(BoldKey) {
            SpanStyle(fontWeight = FontWeight.Bold)
        }
        spanStyle(ItalicKey) {
            SpanStyle(fontStyle = FontStyle.Italic)
        }
        spanStyle(StrikethroughKey) {
            SpanStyle(textDecoration = TextDecoration.LineThrough)
        }
        spanStyle(UnderlineKey) {
            SpanStyle(textDecoration = TextDecoration.Underline)
        }
    }
