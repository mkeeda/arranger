package dev.mkeeda.arranger.editor.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.editor.BlockContainer
import dev.mkeeda.arranger.editor.BlockDecorator
import dev.mkeeda.arranger.richtext.VisualBlock

/**
 * Creates a [BlockDecorator] styled using Material 3 design tokens.
 *
 * - [VisualBlock.Blockquote]: Rendered with a vertical accent bar using [ColorScheme.outlineVariant].
 * - [VisualBlock.CodeBlock]: Rendered with a rounded container background using [ColorScheme.surfaceVariant]
 *   and a subtle border using [ColorScheme.outlineVariant].
 *
 * @param colorScheme The Material 3 [ColorScheme] providing semantic colors.
 * @param typography The Material 3 [Typography] providing text styles.
 */
public fun material3BlockDecorator(
    colorScheme: ColorScheme,
    @Suppress("UNUSED_PARAMETER") typography: Typography? = null,
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
                                    )
                                    .testTag("blockquote_bar"),
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
                                    )
                                    .testTag("code_block_background"),
                        )
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
