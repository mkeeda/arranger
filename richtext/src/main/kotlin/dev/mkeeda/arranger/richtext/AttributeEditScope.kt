package dev.mkeeda.arranger.richtext

/**
 * A builder DSL for safely mutating multiple text attributes within a specific range.
 */
public class AttributeEditScope internal constructor(
    private val builder: RichStringBuilder,
    private val range: IntRange,
) {
    /**
     * Sets or removes an attribute for the given [key].
     * If [value] is null, the attribute is removed from the specified range.
     */
    public fun <T : Any> set(
        key: AttributeKey<T>,
        value: T?,
    ) {
        if (value == null) {
            builder.removeAttribute(key, range)
        } else {
            builder.setAttribute(key, value, range)
        }
    }
}

/**
 * Convenience function to set the text color within this builder.
 */
public fun AttributeEditScope.textColor(color: RgbaColor) {
    if (color == RgbaColor.Unspecified) {
        clearTextColor()
    } else {
        set(TextColorKey, color)
    }
}

/**
 * Convenience function to remove the text color attribute in the range.
 */
public fun AttributeEditScope.clearTextColor() {
    set(TextColorKey, null)
}

/**
 * Convenience function to set the background color within this builder.
 */
public fun AttributeEditScope.backgroundColor(color: RgbaColor) {
    if (color == RgbaColor.Unspecified) {
        clearBackgroundColor()
    } else {
        set(BackgroundColorKey, color)
    }
}

/**
 * Convenience function to remove the background color attribute in the range.
 */
public fun AttributeEditScope.clearBackgroundColor() {
    set(BackgroundColorKey, null)
}

/**
 * Convenience function to set the font size within this builder.
 */
public fun AttributeEditScope.fontSize(size: TextSize) {
    if (size == TextSize.Unspecified) {
        clearFontSize()
    } else {
        set(FontSizeKey, size)
    }
}

/**
 * Convenience function to remove the font size attribute in the range.
 */
public fun AttributeEditScope.clearFontSize() {
    set(FontSizeKey, null)
}

/**
 * Convenience function to apply the bold text attribute within this builder.
 */
public fun AttributeEditScope.bold() {
    set(BoldKey, Unit)
}

/**
 * Convenience function to remove the bold attribute in the range.
 */
public fun AttributeEditScope.clearBold() {
    set(BoldKey, null)
}

/**
 * Convenience function to apply the underline text attribute within this builder.
 */
public fun AttributeEditScope.underline() {
    set(UnderlineKey, Unit)
}

/**
 * Convenience function to remove the underline attribute in the range.
 */
public fun AttributeEditScope.clearUnderline() {
    set(UnderlineKey, null)
}

/**
 * Convenience function to apply the italic text attribute within this builder.
 */
public fun AttributeEditScope.italic() {
    set(ItalicKey, Unit)
}

/**
 * Convenience function to remove the italic attribute in the range.
 */
public fun AttributeEditScope.clearItalic() {
    set(ItalicKey, null)
}

/**
 * Convenience function to apply the strikethrough text attribute within this builder.
 */
public fun AttributeEditScope.strikethrough() {
    set(StrikethroughKey, Unit)
}

/**
 * Convenience function to remove the strikethrough attribute in the range.
 */
public fun AttributeEditScope.clearStrikethrough() {
    set(StrikethroughKey, null)
}
