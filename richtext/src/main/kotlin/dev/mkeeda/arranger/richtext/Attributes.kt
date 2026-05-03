package dev.mkeeda.arranger.richtext

/**
 * Represents a color using a 64-bit ULong to keep the model platform-independent.
 * It perfectly aligns with Compose's Color representation or an ARGB bitmask.
 */
@JvmInline
public value class RgbaColor(public val value: Long) {
    public companion object {
        /**
         * Represents an unspecified color.
         */
        public val Unspecified: RgbaColor = RgbaColor(value = Long.MAX_VALUE)
    }
}

/**
 * Represents a text size in scaled pixels (sp) without depending on Jetpack Compose.
 */
@JvmInline
public value class TextSize(public val sp: Float) {
    public companion object {
        /**
         * Represents an unspecified text size.
         */
        public val Unspecified: TextSize = TextSize(sp = Float.NaN)
    }
}

/**
 * The standard [AttributeKey] to denote bold text.
 */
public object BoldKey : SpanAttributeKey<Unit> {
    override val name: String = "bold"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote italic text.
 */
public object ItalicKey : SpanAttributeKey<Unit> {
    override val name: String = "italic"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote strikethrough text.
 */
public object StrikethroughKey : SpanAttributeKey<Unit> {
    override val name: String = "strikethrough"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote underlined text.
 */
public object UnderlineKey : SpanAttributeKey<Unit> {
    override val name: String = "underline"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote the foreground text color.
 * Contains a [RgbaColor] when specified, or [RgbaColor.Unspecified] otherwise.
 */
public object TextColorKey : SpanAttributeKey<RgbaColor> {
    override val name: String = "textColor"
    override val defaultValue: RgbaColor = RgbaColor.Unspecified
}

/**
 * The standard [AttributeKey] to denote the background color.
 * Contains a [RgbaColor] when specified, or [RgbaColor.Unspecified] otherwise.
 */
public object BackgroundColorKey : SpanAttributeKey<RgbaColor> {
    override val name: String = "backgroundColor"
    override val defaultValue: RgbaColor = RgbaColor.Unspecified
}

/**
 * The standard [AttributeKey] to denote the font size.
 * Contains a [TextSize] when specified, or [TextSize.Unspecified] otherwise.
 */
public object FontSizeKey : SpanAttributeKey<TextSize> {
    override val name: String = "fontSize"
    override val defaultValue: TextSize = TextSize.Unspecified
}

/**
 * Represents heading semantic levels.
 */
public enum class HeadingLevel {
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,
    Unspecified,
}

/**
 * A semantic marker indicating the heading level of the paragraph.
 */
public object HeadingKey : ParagraphAttributeKey<HeadingLevel> {
    override val name: String = "heading"
    override val defaultValue: HeadingLevel = HeadingLevel.Unspecified
}

/**
 * Represents horizontal text alignment within a paragraph.
 */
public enum class TextAlignment {
    Left,
    Center,
    Right,
    Justify,
    Unspecified,
}

/**
 * A semantic marker indicating the horizontal text alignment of the paragraph.
 */
public object TextAlignmentKey : ParagraphAttributeKey<TextAlignment> {
    override val name: String = "textAlignment"
    override val defaultValue: TextAlignment = TextAlignment.Unspecified
}

/**
 * A semantic marker indicating that the paragraph is a blockquote.
 */
public object BlockquoteKey : ParagraphAttributeKey<Unit> {
    override val name: String = "blockquote"
    override val defaultValue: Unit = Unit
}

/**
 * Represents the indent level of a list item.
 */
public enum class ListIndentLevel {
    Level1,
    Level2,
    Level3,
    Level4,
    Level5,
    Level6,
    Unspecified,
}

/**
 * A semantic marker indicating that the paragraph is a bullet list item.
 */
public object BulletListKey : ParagraphAttributeKey<ListIndentLevel> {
    override val name: String = "bulletList"
    override val defaultValue: ListIndentLevel = ListIndentLevel.Unspecified
}

/**
 * A semantic marker indicating that the paragraph is an ordered list item.
 */
public object OrderedListKey : ParagraphAttributeKey<ListIndentLevel> {
    override val name: String = "orderedList"
    override val defaultValue: ListIndentLevel = ListIndentLevel.Unspecified
}
