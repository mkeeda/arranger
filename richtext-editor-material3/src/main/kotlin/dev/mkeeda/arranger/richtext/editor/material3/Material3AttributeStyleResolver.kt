package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver

/**
 * Creates and remembers an [AttributeStyleResolver] that maps Arranger attributes
 * to Material 3 typography and colors.
 *
 * It reads the current [MaterialTheme.typography] and [MaterialTheme.colorScheme]
 * and updates automatically when the theme changes.
 */
@Composable
public fun rememberMaterial3AttributeStyleResolver(): AttributeStyleResolver {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    return remember(typography, colorScheme) {
        AttributeStyleResolver {
            // Heading levels map to Material 3 display/headline/title typography
            paragraphStyle(HeadingKey) { level ->
                when (level) {
                    HeadingLevel.H1 -> typography.displayLarge.toParagraphStyle()
                    HeadingLevel.H2 -> typography.displayMedium.toParagraphStyle()
                    HeadingLevel.H3 -> typography.headlineLarge.toParagraphStyle()
                    HeadingLevel.H4 -> typography.headlineMedium.toParagraphStyle()
                    HeadingLevel.H5 -> typography.titleLarge.toParagraphStyle()
                    HeadingLevel.H6 -> typography.titleMedium.toParagraphStyle()
                    HeadingLevel.Unspecified -> ParagraphStyle()
                }
            }
            spanStyle(HeadingKey) { level ->
                when (level) {
                    HeadingLevel.H1 -> typography.displayLarge.toSpanStyle()
                    HeadingLevel.H2 -> typography.displayMedium.toSpanStyle()
                    HeadingLevel.H3 -> typography.headlineLarge.toSpanStyle()
                    HeadingLevel.H4 -> typography.headlineMedium.toSpanStyle()
                    HeadingLevel.H5 -> typography.titleLarge.toSpanStyle()
                    HeadingLevel.H6 -> typography.titleMedium.toSpanStyle()
                    HeadingLevel.Unspecified -> SpanStyle()
                }
            }

            // Blockquote maps to bodyMedium with onSurfaceVariant color
            paragraphStyle(BlockquoteKey) {
                typography.bodyMedium.toParagraphStyle()
            }
            spanStyle(BlockquoteKey) {
                typography.bodyMedium.toSpanStyle().copy(color = colorScheme.onSurfaceVariant)
            }
        }
    }
}
