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
        key: SpanAttributeKey<T>,
        value: T?,
    ) {
        if (value == null) {
            builder.removeAttribute(key, range)
        } else {
            builder.setAttribute(key, value, range)
        }
    }

    /**
     * Sets or removes a paragraph-level attribute for the given [key].
     * The underlying range is automatically snapped to paragraph boundaries.
     */
    public fun <T : Any> setParagraph(
        key: ParagraphAttributeKey<T>,
        value: T?,
    ) {
        if (value == null) {
            builder.removeParagraphAttribute(key, range)
        } else {
            builder.setParagraphAttribute(key, value, range)
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

/**
 * Convenience function to set the heading level of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.headingLevel(level: HeadingLevel?) {
    setParagraph(HeadingKey, level)
}

/**
 * Convenience function to remove the heading attribute in the range.
 */
public fun AttributeEditScope.clearHeadingLevel() {
    setParagraph(HeadingKey, null)
}

/**
 * Convenience function to set the text alignment of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.textAlignment(alignment: TextAlignment?) {
    setParagraph(TextAlignmentKey, alignment)
}

/**
 * Convenience function to remove the text alignment attribute in the range.
 */
public fun AttributeEditScope.clearTextAlignment() {
    setParagraph(TextAlignmentKey, null)
}

/**
 * Convenience function to set the blockquote attribute of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.blockquote() {
    setParagraph(BlockquoteKey, Unit)
}

/**
 * Convenience function to remove the blockquote attribute in the range.
 */
public fun AttributeEditScope.clearBlockquote() {
    setParagraph(BlockquoteKey, null)
}
