package dev.mkeeda.arranger.richtext

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

/**
 * Returns a lazily evaluated sequence of all occurrence ranges of the specified [target] within this character sequence.
 */
public fun CharSequence.rangesOf(
    target: String,
    ignoreCase: Boolean = false,
): Sequence<IntRange> = sequence {
    var currentIndex = 0
    while (true) {
        val startIndex = indexOf(target, startIndex = currentIndex, ignoreCase = ignoreCase)
        if (startIndex == -1) break
        
        val endIndex = startIndex + target.length
        yield(startIndex until endIndex)
        currentIndex = endIndex
    }
}

/**
 * Returns a lazily evaluated sequence of all occurrence ranges of the specified [regex] within this character sequence.
 */
public fun CharSequence.rangesOf(regex: Regex): Sequence<IntRange> {
    return regex.findAll(this).map { it.range }
}
