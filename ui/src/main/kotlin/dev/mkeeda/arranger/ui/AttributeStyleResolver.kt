package dev.mkeeda.arranger.ui

import androidx.compose.ui.text.SpanStyle
import dev.mkeeda.arranger.core.text.AttributeContainer
import dev.mkeeda.arranger.core.text.AttributeKey

/**
 * Resolves an [AttributeContainer] to a Compose [SpanStyle] for rendering rich text.
 */
public fun interface AttributeStyleResolver {
    public fun resolve(attributes: AttributeContainer): SpanStyle?
}

/**
 * Creates an [AttributeStyleResolver] using a DSL.
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
    private val resolvers = mutableListOf<(AttributeContainer) -> SpanStyle?>()

    /**
     * Registers a mapping for a specific [AttributeKey].
     * When the [AttributeContainer] contains this key, the [mapper] is invoked with the key's value.
     */
    public fun <T> resolve(key: AttributeKey<T>, mapper: (T) -> SpanStyle?) {
        resolvers.add { container ->
            val value = container.getOrNull(key)
            if (value != null) mapper(value) else null
        }
    }

    internal fun build(): AttributeStyleResolver =
        AttributeStyleResolver { attributes ->
            var mergedStyle: SpanStyle? = null
            for (resolver in resolvers) {
                val style = resolver(attributes)
                if (style != null) {
                    mergedStyle = mergedStyle?.merge(style) ?: style
                }
            }
            mergedStyle
        }
}
