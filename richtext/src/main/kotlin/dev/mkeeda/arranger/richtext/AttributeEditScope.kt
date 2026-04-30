package dev.mkeeda.arranger.richtext

/**
 * A builder DSL for safely mutating multiple text attributes within a specific range.
 */
public class AttributeEditScope internal constructor(
    private val buffer: RichStringScope,
    private val range: IntRange,
) {
    /**
     * Sets or removes a character span attribute for the given [key].
     * If [value] is null, the span attribute is removed from the specified range.
     */
    public fun <T : Any> setSpanAttribute(
        key: SpanAttributeKey<T>,
        value: T?,
    ) {
        if (value == null) {
            buffer.removeSpanAttribute(key, range)
        } else {
            buffer.setSpanAttribute(key, value, range)
        }
    }

    /**
     * Sets or removes a paragraph-level attribute for the given [key].
     * The underlying range is automatically snapped to paragraph boundaries.
     */
    public fun <T : Any> setParagraphAttribute(
        key: ParagraphAttributeKey<T>,
        value: T?,
    ) {
        if (value == null) {
            buffer.removeParagraphAttribute(key, range)
        } else {
            buffer.setParagraphAttribute(key, value, range)
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
        setSpanAttribute(TextColorKey, color)
    }
}

/**
 * Convenience function to remove the text color attribute in the range.
 */
public fun AttributeEditScope.clearTextColor() {
    setSpanAttribute(TextColorKey, null)
}

/**
 * Convenience function to set the background color within this builder.
 */
public fun AttributeEditScope.backgroundColor(color: RgbaColor) {
    if (color == RgbaColor.Unspecified) {
        clearBackgroundColor()
    } else {
        setSpanAttribute(BackgroundColorKey, color)
    }
}

/**
 * Convenience function to remove the background color attribute in the range.
 */
public fun AttributeEditScope.clearBackgroundColor() {
    setSpanAttribute(BackgroundColorKey, null)
}

/**
 * Convenience function to set the font size within this builder.
 */
public fun AttributeEditScope.fontSize(size: TextSize) {
    if (size == TextSize.Unspecified) {
        clearFontSize()
    } else {
        setSpanAttribute(FontSizeKey, size)
    }
}

/**
 * Convenience function to remove the font size attribute in the range.
 */
public fun AttributeEditScope.clearFontSize() {
    setSpanAttribute(FontSizeKey, null)
}

/**
 * Convenience function to apply the bold text attribute within this builder.
 */
public fun AttributeEditScope.bold() {
    setSpanAttribute(BoldKey, Unit)
}

/**
 * Convenience function to remove the bold attribute in the range.
 */
public fun AttributeEditScope.clearBold() {
    setSpanAttribute(BoldKey, null)
}

/**
 * Convenience function to apply the underline text attribute within this builder.
 */
public fun AttributeEditScope.underline() {
    setSpanAttribute(UnderlineKey, Unit)
}

/**
 * Convenience function to remove the underline attribute in the range.
 */
public fun AttributeEditScope.clearUnderline() {
    setSpanAttribute(UnderlineKey, null)
}

/**
 * Convenience function to apply the italic text attribute within this builder.
 */
public fun AttributeEditScope.italic() {
    setSpanAttribute(ItalicKey, Unit)
}

/**
 * Convenience function to remove the italic attribute in the range.
 */
public fun AttributeEditScope.clearItalic() {
    setSpanAttribute(ItalicKey, null)
}

/**
 * Convenience function to apply the strikethrough text attribute within this builder.
 */
public fun AttributeEditScope.strikethrough() {
    setSpanAttribute(StrikethroughKey, Unit)
}

/**
 * Convenience function to remove the strikethrough attribute in the range.
 */
public fun AttributeEditScope.clearStrikethrough() {
    setSpanAttribute(StrikethroughKey, null)
}

/**
 * Convenience function to set the heading level of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.headingLevel(level: HeadingLevel?) {
    setParagraphAttribute(HeadingKey, level)
}

/**
 * Convenience function to remove the heading attribute in the range.
 */
public fun AttributeEditScope.clearHeadingLevel() {
    setParagraphAttribute(HeadingKey, null)
}

/**
 * Convenience function to set the text alignment of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.textAlignment(alignment: TextAlignment?) {
    setParagraphAttribute(TextAlignmentKey, alignment)
}

/**
 * Convenience function to remove the text alignment attribute in the range.
 */
public fun AttributeEditScope.clearTextAlignment() {
    setParagraphAttribute(TextAlignmentKey, null)
}

/**
 * Convenience function to set the blockquote attribute of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.blockquote() {
    setParagraphAttribute(BlockquoteKey, Unit)
}

/**
 * Convenience function to remove the blockquote attribute in the range.
 */
public fun AttributeEditScope.clearBlockquote() {
    setParagraphAttribute(BlockquoteKey, null)
}
