package dev.mkeeda.arranger.richtext

/**
 * Represents a span of attributes applied to a specific range within a [RichString].
 *
 * @property range The character range (inclusive) to which the attributes are applied.
 * @property attributes The [AttributeContainer] holding the attribute values for this range.
 */
public data class RichSpan(
    val range: IntRange,
    val attributes: AttributeContainer,
)
