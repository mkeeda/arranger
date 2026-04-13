package dev.mkeeda.arranger.core.text

/**
 * Returns the index range of the first occurrence of the specified [substring] within this character sequence.
 * This is useful for easily specifying ranges when applying rich text attributes.
 *
 * @throws IllegalArgumentException if the substring is not found.
 */
public fun CharSequence.rangeOf(substring: String): IntRange {
    val startIndex = this.indexOf(substring)
    require(startIndex >= 0) { "Substring '$substring' not found in '$this'" }
    return startIndex until (startIndex + substring.length)
}
