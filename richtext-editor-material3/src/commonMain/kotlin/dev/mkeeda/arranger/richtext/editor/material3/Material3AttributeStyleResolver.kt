package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver

/**
 * Creates an [AttributeStyleResolver] that maps Arranger attributes
 * to Material 3 [typography] and [colorScheme].
 */
public fun material3AttributeStyleResolver(
    typography: Typography,
    colorScheme: ColorScheme,
): AttributeStyleResolver =
    AttributeStyleResolver(base = DefaultAttributeStyleResolver) {
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
        spanStyle(BlockquoteKey) {
            typography.bodyMedium.toSpanStyle().copy(color = colorScheme.onSurfaceVariant)
        }

        // Link maps to primary color with underline
        spanStyle(LinkKey) {
            SpanStyle(
                color = colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            )
        }

        // Inline code maps to surfaceContainerHighest background, onSurfaceVariant color, and monospace font
        spanStyle(InlineCodeKey) {
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = colorScheme.surfaceContainerHighest,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }

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
        material3AttributeStyleResolver(
            typography = typography,
            colorScheme = colorScheme,
        )
    }
}
