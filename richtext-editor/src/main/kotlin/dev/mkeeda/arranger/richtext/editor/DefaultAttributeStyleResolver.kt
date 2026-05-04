package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.UnderlineKey

internal const val ListIndentStepSp = 24f

internal fun ListIndentLevel.toIndent(): TextUnit =
    when (this) {
        ListIndentLevel.Unspecified -> 0.sp
        else -> ((this.ordinal + 1) * ListIndentStepSp).sp
    }

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
        spanStyle(HeadingKey) { level ->
            when (level) {
                HeadingLevel.H1 -> SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
                HeadingLevel.H2 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                HeadingLevel.H3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                HeadingLevel.H4 -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                HeadingLevel.H5 -> SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                HeadingLevel.H6 -> SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                HeadingLevel.Unspecified -> SpanStyle()
            }
        }
        paragraphStyle(HeadingKey) { level ->
            when (level) {
                HeadingLevel.H1 -> ParagraphStyle(lineHeight = 38.sp)
                HeadingLevel.H2 -> ParagraphStyle(lineHeight = 28.sp)
                HeadingLevel.H3 -> ParagraphStyle(lineHeight = 24.sp)
                HeadingLevel.H4 -> ParagraphStyle(lineHeight = 20.sp)
                HeadingLevel.H5 -> ParagraphStyle(lineHeight = 18.sp)
                HeadingLevel.H6 -> ParagraphStyle(lineHeight = 16.sp)
                HeadingLevel.Unspecified -> ParagraphStyle()
            }
        }
        paragraphStyle(TextAlignmentKey) { alignment ->
            val textAlign =
                when (alignment) {
                    TextAlignment.Left -> TextAlign.Left
                    TextAlignment.Center -> TextAlign.Center
                    TextAlignment.Right -> TextAlign.Right
                    TextAlignment.Justify -> TextAlign.Justify
                    TextAlignment.Unspecified -> TextAlign.Unspecified
                }
            ParagraphStyle(textAlign = textAlign)
        }
        paragraphStyle(BlockquoteKey) {
            ParagraphStyle(textIndent = TextIndent(firstLine = 16.sp, restLine = 16.sp))
        }
        spanStyle(BlockquoteKey) {
            SpanStyle(
                fontStyle = FontStyle.Italic,
                color = Color.Unspecified.copy(alpha = 0.5f),
            )
        }
        paragraphStyle(BulletListKey) { level ->
            val indent = level.toIndent()
            ParagraphStyle(textIndent = TextIndent(firstLine = indent, restLine = indent))
        }
        paragraphStyle(OrderedListKey) { level ->
            val indent = level.toIndent()
            ParagraphStyle(textIndent = TextIndent(firstLine = indent, restLine = indent))
        }
    }
