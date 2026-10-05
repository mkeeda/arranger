package dev.mkeeda.arranger.richtext

/**
 * A builder DSL for safely mutating multiple text attributes within a specific range.
 */
public class AttributeEditScope internal constructor(
    private val buffer: RichStringScope,
    private val range: IntRange,
) {
    /**
     * Sets a character span attribute for the given [key] with the specified [value].
     */
    public fun <T> setSpanAttribute(
        key: SpanAttributeKey<T>,
        value: T,
    ) {
        buffer.setSpanAttribute(key, value, range)
    }

    /**
     * Removes any character span attributes associated with the specified [key] within the range.
     */
    public fun <T> removeSpanAttribute(
        key: SpanAttributeKey<T>,
    ) {
        buffer.removeSpanAttribute(key, range)
    }

    /**
     * Sets a paragraph-level attribute for the given [key] with the specified [value].
     * The underlying range is automatically snapped to paragraph boundaries.
     */
    public fun <T> setParagraphAttribute(
        key: ParagraphAttributeKey<T>,
        value: T,
    ) {
        buffer.setParagraphAttribute(key, value, range)
    }

    /**
     * Removes any paragraph-level attributes associated with the specified [key] within the range.
     * The underlying range is automatically snapped to paragraph boundaries.
     */
    public fun <T> removeParagraphAttribute(
        key: ParagraphAttributeKey<T>,
    ) {
        buffer.removeParagraphAttribute(key, range)
    }

    /**
     * Removes all rich text attributes (both span and paragraph level) in the range.
     * Including user-customized attributes.
     */
    public fun clearAll() {
        buffer.clearAllAttributes(range)
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
    removeSpanAttribute(TextColorKey)
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
    removeSpanAttribute(BackgroundColorKey)
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
    removeSpanAttribute(FontSizeKey)
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
    removeSpanAttribute(BoldKey)
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
    removeSpanAttribute(UnderlineKey)
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
    removeSpanAttribute(ItalicKey)
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
    removeSpanAttribute(StrikethroughKey)
}

/**
 * Convenience function to apply the inline code text attribute within this builder.
 */
public fun AttributeEditScope.inlineCode() {
    setSpanAttribute(InlineCodeKey, Unit)
}

/**
 * Convenience function to remove the inline code attribute in the range.
 */
public fun AttributeEditScope.clearInlineCode() {
    removeSpanAttribute(InlineCodeKey)
}

/**
 * Convenience function to set the heading level of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.headingLevel(level: HeadingLevel?) {
    if (level == null || level == HeadingLevel.Unspecified) {
        clearHeadingLevel()
    } else {
        setParagraphAttribute(HeadingKey, level)
    }
}

/**
 * Convenience function to remove the heading attribute in the range.
 */
public fun AttributeEditScope.clearHeadingLevel() {
    removeParagraphAttribute(HeadingKey)
}

/**
 * Convenience function to set the text alignment of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.textAlignment(alignment: TextAlignment?) {
    if (alignment == null || alignment == TextAlignment.Unspecified) {
        clearTextAlignment()
    } else {
        setParagraphAttribute(TextAlignmentKey, alignment)
    }
}

/**
 * Convenience function to remove the text alignment attribute in the range.
 */
public fun AttributeEditScope.clearTextAlignment() {
    removeParagraphAttribute(TextAlignmentKey)
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
    removeParagraphAttribute(BlockquoteKey)
}

/**
 * Convenience function to set the bullet list attribute of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.bulletList(level: ListIndentLevel) {
    if (level == ListIndentLevel.Unspecified) {
        clearBulletList()
    } else {
        setParagraphAttribute(BulletListKey, level)
    }
}

/**
 * Convenience function to remove the bullet list attribute in the range.
 */
public fun AttributeEditScope.clearBulletList() {
    removeParagraphAttribute(BulletListKey)
}

/**
 * Convenience function to set the ordered list attribute of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 */
public fun AttributeEditScope.orderedList(level: ListIndentLevel) {
    if (level == ListIndentLevel.Unspecified) {
        clearOrderedList()
    } else {
        setParagraphAttribute(OrderedListKey, level)
    }
}

/**
 * Convenience function to remove the ordered list attribute in the range.
 */
public fun AttributeEditScope.clearOrderedList() {
    removeParagraphAttribute(OrderedListKey)
}

/**
 * Convenience function to set the link attribute within this builder.
 */
public fun AttributeEditScope.link(url: String) {
    if (url.isEmpty()) {
        clearLink()
    } else {
        setSpanAttribute(LinkKey, url)
    }
}

/**
 * Convenience function to remove the link attribute in the range.
 */
public fun AttributeEditScope.clearLink() {
    removeSpanAttribute(LinkKey)
}

/**
 * Convenience function to set the code block attribute of the paragraph within this builder.
 * The applied range will automatically snap to paragraph boundaries.
 *
 * @param language The programming language identifier for syntax highlighting or labeling, or null if none.
 */
public fun AttributeEditScope.codeBlock(language: String? = null) {
    setParagraphAttribute(CodeBlockKey, language)
}

/**
 * Convenience function to remove the code block attribute in the range.
 */
public fun AttributeEditScope.clearCodeBlock() {
    removeParagraphAttribute(CodeBlockKey)
}
