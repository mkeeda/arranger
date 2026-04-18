package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.AttributeKey

/**
 * A container representing the resolved visual styles for a set of attributes.
 */
public data class ResolvedRichStyle(
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
 * Optionally accepts a [base] resolver. The styles constructed by this builder
 * will take precedence and be merged on top of the styles produced by the [base] resolver.
 */
public fun AttributeStyleResolver(
    base: AttributeStyleResolver? = null,
    builder: AttributeStyleBuilder.() -> Unit,
): AttributeStyleResolver {
    val customResolver = AttributeStyleBuilder().apply(builder).build()
    if (base == null) return customResolver

    return AttributeStyleResolver { attributes ->
        val baseStyle = base.resolve(attributes)
        val customStyle = customResolver.resolve(attributes)

        val mergedSpan =
            if (baseStyle.spanStyle != null) {
                baseStyle.spanStyle.merge(customStyle.spanStyle)
            } else {
                customStyle.spanStyle
            }

        val mergedParagraph =
            if (baseStyle.paragraphStyle != null) {
                baseStyle.paragraphStyle.merge(customStyle.paragraphStyle)
            } else {
                customStyle.paragraphStyle
            }

        ResolvedRichStyle(
            spanStyle = mergedSpan,
            paragraphStyle = mergedParagraph,
        )
    }
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
            val mergedSpan =
                spanResolvers.fold(null as SpanStyle?) { acc, resolver ->
                    val style = resolver(attributes)
                    if (style != null) acc?.merge(style) ?: style else acc
                }

            val mergedParagraph =
                paragraphResolvers.fold(null as ParagraphStyle?) { acc, resolver ->
                    val style = resolver(attributes)
                    if (style != null) acc?.merge(style) ?: style else acc
                }

            ResolvedRichStyle(
                spanStyle = mergedSpan,
                paragraphStyle = mergedParagraph,
            )
        }
}
