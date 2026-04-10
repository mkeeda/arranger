package dev.mkeeda.arranger.core.text

/**
 * Represents a contiguous block of text where a specific attribute is continuously applied.
 *
 * @param T The type of the attribute value.
 * @property text The substring corresponding to this run.
 * @property range The character range (inclusive) within the original text.
 * @property value The value of the specific attribute queried.
 */
public data class RichRun<T>(
    val text: String,
    val range: IntRange,
    val value: T,
)
