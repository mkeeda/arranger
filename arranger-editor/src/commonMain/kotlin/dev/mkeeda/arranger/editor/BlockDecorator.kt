package dev.mkeeda.arranger.editor

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.CodeBlockKey

/**
 * Contextual layout information supplied to a [BlockDecorator] for decorating a block.
 *
 * @property range The character index range covering all paragraphs in this block.
 * @property lineCount The number of lines contained in the block.
 * @property modifier Base modifier sized and positioned for the block.
 */
public data class BlockDecorationContext(
    public val range: IntRange,
    public val lineCount: Int,
    public val modifier: Modifier,
) {
    public constructor(
        lineCount: Int,
        modifier: Modifier,
    ) : this(
        range = IntRange.EMPTY,
        lineCount = lineCount,
        modifier = modifier,
    )
}

/**
 * Interface for rendering custom decorations around or behind block-level paragraphs.
 */
public interface BlockDecorator {
    /**
     * Set of [AttributeKey]s that this decorator handles.
     */
    public val supportedKeys: Set<AttributeKey<*>>

    /**
     * Renders decoration content for the specified block attribute [key] and [value].
     */
    @Composable
    public fun Decoration(
        key: AttributeKey<*>,
        value: Any?,
        context: BlockDecorationContext,
    )
}

/**
 * Creates a [BlockDecorator] using a declarative builder DSL.
 *
 * Unhandled block types automatically delegate to the [base] decorator (if provided).
 *
 * @param base The fallback [BlockDecorator] (e.g. [DefaultBlockDecorator] or `rememberMaterial3BlockDecorator()`).
 * @param builder Configuration block registering attribute-specific decoration composables.
 */
public fun BlockDecorator(
    base: BlockDecorator? = null,
    builder: BlockDecoratorBuilder.() -> Unit,
): BlockDecorator {
    val dslBuilder = BlockDecoratorBuilder()
    dslBuilder.builder()
    return dslBuilder.build(base = base)
}

public class BlockDecoratorBuilder internal constructor() {
    private val handlers =
        mutableMapOf<AttributeKey<*>, @Composable (value: Any?, context: BlockDecorationContext) -> Unit>()

    /**
     * Registers a composable decoration for the specified [AttributeKey].
     * Adjacent paragraphs sharing identical [key] values will automatically be merged.
     *
     * @param key The [AttributeKey] identifying block paragraphs to decorate.
     * @param decoration Composable lambda invoked with the typed attribute [value] and layout [context].
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> on(
        key: AttributeKey<T>,
        decoration: @Composable (value: T, context: BlockDecorationContext) -> Unit,
    ) {
        handlers[key] = { value, context ->
            decoration(value as T, context)
        }
    }

    internal fun build(base: BlockDecorator?): BlockDecorator {
        return DslBlockDecorator(
            base = base,
            handlers = handlers.toMap(),
        )
    }
}

private class DslBlockDecorator(
    private val base: BlockDecorator?,
    private val handlers: Map<AttributeKey<*>, @Composable (value: Any?, context: BlockDecorationContext) -> Unit>,
) : BlockDecorator {
    override val supportedKeys: Set<AttributeKey<*>> =
        if (base != null) base.supportedKeys + handlers.keys else handlers.keys

    @Composable
    override fun Decoration(
        key: AttributeKey<*>,
        value: Any?,
        context: BlockDecorationContext,
    ) {
        val handler = handlers[key]
        if (handler != null) {
            handler(value, context)
        } else if (base != null && base.supportedKeys.contains(key)) {
            base.Decoration(key = key, value = value, context = context)
        }
    }
}

/**
 * A slot-based layout helper that simplifies constructing custom decorations for visual blocks.
 *
 * @param context The [BlockDecorationContext] provided by the editor.
 * @param modifier Additional modifier applied to the container.
 * @param background Optional composable content rendered in the background of the entire block.
 * @param leading Optional composable content aligned to the start/leading edge of the block (e.g., vertical bar).
 * @param trailing Optional composable content aligned to the end/trailing edge of the block.
 */
@Composable
public fun BlockContainer(
    context: BlockDecorationContext,
    modifier: Modifier = Modifier,
    background: @Composable (BoxScope.() -> Unit)? = null,
    leading: @Composable (BoxScope.() -> Unit)? = null,
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
    }
}

/**
 * The default [BlockDecorator] providing:
 * - A 3.dp vertical accent bar on the left edge for [BlockquoteKey].
 * - A rounded light translucent background container for [CodeBlockKey].
 */
public val DefaultBlockDecorator: BlockDecorator =
    BlockDecorator {
        on(BlockquoteKey) { _, context ->
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
                                )
                                .testTag("blockquote_bar"),
                    )
                },
            )
        }
        on(CodeBlockKey) { _, context ->
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
                                )
                                .testTag("code_block_background"),
                    )
                },
            )
        }
    }
