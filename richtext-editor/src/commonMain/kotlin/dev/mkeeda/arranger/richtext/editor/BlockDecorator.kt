package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.VisualBlock

/**
 * Contextual layout information supplied to a [BlockDecorator] for decorating a [VisualBlock].
 *
 * @property bounds The entire rectangular bounding box of the block within the editor coordinates.
 * @property textBounds The actual bounding box of the text content within the block (accounting for indents).
 * @property lineCount The number of lines contained in the block.
 * @property modifier Base modifier sized and positioned according to [bounds].
 */
public data class BlockDecorationContext(
    public val bounds: Rect,
    public val textBounds: Rect,
    public val lineCount: Int,
    public val modifier: Modifier,
)

/**
 * Interface for rendering custom decorations around or behind visual blocks (e.g. blockquotes, code blocks).
 */
public fun interface BlockDecorator {
    /**
     * Composable function called to render decorations for a given [block].
     *
     * @param block The [VisualBlock] to decorate.
     * @param context Context containing geometry, bounds, and modifier.
     */
    @Composable
    public fun Decoration(
        block: VisualBlock,
        context: BlockDecorationContext,
    )
}

/**
 * A slot-based layout helper that simplifies constructing custom decorations for [VisualBlock]s.
 *
 * @param context The [BlockDecorationContext] provided by the editor.
 * @param modifier Additional modifier applied to the container.
 * @param background Optional composable content rendered in the background of the entire block.
 * @param leading Optional composable content aligned to the start/leading edge of the block (e.g., vertical bar).
 * @param header Optional composable content rendered at the top of the block (e.g., language badge).
 * @param trailing Optional composable content aligned to the end/trailing edge of the block.
 */
@Composable
public fun BlockContainer(
    context: BlockDecorationContext,
    modifier: Modifier = Modifier,
    background: @Composable (BoxScope.() -> Unit)? = null,
    leading: @Composable (BoxScope.() -> Unit)? = null,
    header: @Composable (BoxScope.() -> Unit)? = null,
    trailing: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(modifier = context.modifier.then(modifier)) {
        background?.invoke(this)
        leading?.let {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(),
                content = it,
            )
        }
        trailing?.let {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                content = it,
            )
        }
        header?.invoke(this)
    }
}

/**
 * The default [BlockDecorator] providing:
 * - A 3.dp vertical accent bar on the left edge for [VisualBlock.Blockquote].
 * - A rounded light translucent background container for [VisualBlock.CodeBlock].
 */
public val DefaultBlockDecorator: BlockDecorator =
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
                                        color = Color(0xFFB0BEC5),
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
                                        color = Color(0x0A000000),
                                        shape = RoundedCornerShape(6.dp),
                                    ),
                        )
                    },
                )
            }
        }
    }
