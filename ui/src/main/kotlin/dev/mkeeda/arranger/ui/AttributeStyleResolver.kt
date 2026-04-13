package dev.mkeeda.arranger.ui

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import dev.mkeeda.arranger.core.text.AttributeContainer
import dev.mkeeda.arranger.core.text.AttributeKey

/**
 * A container representing the resolved visual styles for a set of attributes.
 */
public class ResolvedRichStyle(
    public val spanStyle: SpanStyle? = null,
    public val paragraphStyle: ParagraphStyle? = null,
)

/**
 * Resolves an [AttributeContainer] to Compose styles ([SpanStyle] and/or [ParagraphStyle])
 * for rendering rich text.
 */
public fun interface AttributeStyleResolver {
    public fun resolve(attributes: AttributeContainer): ResolvedRichStyle
}

/**
 * Creates an [AttributeStyleResolver] using a declarative DSL.
 */
public fun AttributeStyleResolver(
    builder: AttributeStyleBuilder.() -> Unit,
): AttributeStyleResolver {
    return AttributeStyleBuilder().apply(builder).build()
}

/**
 * Builder for creating custom [AttributeStyleResolver] instances.
 */
public class AttributeStyleBuilder internal constructor() {
    private val spanResolvers = mutableListOf<(AttributeContainer) -> SpanStyle?>()
    private val paragraphResolvers = mutableListOf<(AttributeContainer) -> ParagraphStyle?>()

    /**
     * Registers a [SpanStyle] mapping for a specific [AttributeKey].
     * When the [AttributeContainer] contains this key, the [mapper] is invoked with the key's value.
     */
    public fun <T> spanStyle(
        key: AttributeKey<T>,
        mapper: (T) -> SpanStyle,
    ) {
        spanResolvers.add { container ->
            val value = container.getOrNull(key)
            if (value != null) mapper(value) else null
        }
    }

    /**
     * Registers a [ParagraphStyle] mapping for a specific [AttributeKey].
     * When the [AttributeContainer] contains this key, the [mapper] is invoked with the key's value.
     */
    public fun <T> paragraphStyle(
        key: AttributeKey<T>,
        mapper: (T) -> ParagraphStyle,
    ) {
        paragraphResolvers.add { container ->
            val value = container.getOrNull(key)
            if (value != null) mapper(value) else null
        }
    }

    internal fun build(): AttributeStyleResolver =
        AttributeStyleResolver { attributes ->
            var mergedSpan: SpanStyle? = null
            for (resolver in spanResolvers) {
                val style = resolver(attributes)
                if (style != null) {
                    mergedSpan = mergedSpan?.merge(style) ?: style
                }
            }

            var mergedParagraph: ParagraphStyle? = null
            for (resolver in paragraphResolvers) {
                val style = resolver(attributes)
                if (style != null) {
                    mergedParagraph = mergedParagraph?.merge(style) ?: style
                }
            }

            ResolvedRichStyle(
                spanStyle = mergedSpan,
                paragraphStyle = mergedParagraph,
            )
        }
}
