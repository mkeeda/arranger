package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.VisualBlock
import dev.mkeeda.arranger.richtext.editor.BlockContainer
import dev.mkeeda.arranger.richtext.editor.BlockDecorator

/**
 * Creates a [BlockDecorator] styled using Material 3 design tokens.
 *
 * - [VisualBlock.Blockquote]: Rendered with a vertical accent bar using [ColorScheme.outlineVariant].
 * - [VisualBlock.CodeBlock]: Rendered with a rounded container background using [ColorScheme.surfaceVariant],
 *   a subtle border using [ColorScheme.outlineVariant], and an optional language badge at the top end.
 *
 * @param colorScheme The Material 3 [ColorScheme] providing semantic colors.
 * @param typography The Material 3 [Typography] providing text styles.
 */
public fun material3BlockDecorator(
    colorScheme: ColorScheme,
    typography: Typography,
): BlockDecorator =
    BlockDecorator { block, context ->
        when (block) {
            is VisualBlock.Blockquote -> {
                BlockContainer(
                    context = context,
                    leading = {
                        Box(
                            modifier =
                                Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(1.5.dp),
                                    ),
                        )
                    },
                )
            }

            is VisualBlock.CodeBlock -> {
                BlockContainer(
                    context = context,
                    background = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp),
                                    ),
                        )
                    },
                    header = {
                        block.language?.let { lang ->
                            Text(
                                text = lang.uppercase(),
                                style = typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 8.dp, top = 4.dp),
                            )
                        }
                    },
                )
            }
        }
    }

/**
 * Creates and remembers a [BlockDecorator] styled using the current [MaterialTheme] tokens.
 *
 * It automatically updates whenever [MaterialTheme.colorScheme] or [MaterialTheme.typography] changes.
 */
@Composable
public fun rememberMaterial3BlockDecorator(): BlockDecorator {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    return remember(colorScheme, typography) {
        material3BlockDecorator(
            colorScheme = colorScheme,
            typography = typography,
        )
    }
}
