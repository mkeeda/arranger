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
public object BoldKey : AttributeKey<Unit> {
    override val name: String = "bold"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote italic text.
 */
public object ItalicKey : AttributeKey<Unit> {
    override val name: String = "italic"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote strikethrough text.
 */
public object StrikethroughKey : AttributeKey<Unit> {
    override val name: String = "strikethrough"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote underlined text.
 */
public object UnderlineKey : AttributeKey<Unit> {
    override val name: String = "underline"
    override val defaultValue: Unit = Unit
}

/**
 * The standard [AttributeKey] to denote the foreground text color.
 * Contains a [RgbaColor] when specified, or [RgbaColor.Unspecified] otherwise.
 */
public object TextColorKey : AttributeKey<RgbaColor> {
    override val name: String = "textColor"
    override val defaultValue: RgbaColor = RgbaColor.Unspecified
}

/**
 * The standard [AttributeKey] to denote the background color.
 * Contains a [RgbaColor] when specified, or [RgbaColor.Unspecified] otherwise.
 */
public object BackgroundColorKey : AttributeKey<RgbaColor> {
    override val name: String = "backgroundColor"
    override val defaultValue: RgbaColor = RgbaColor.Unspecified
}

/**
 * The standard [AttributeKey] to denote the font size.
 * Contains a [TextSize] when specified, or [TextSize.Unspecified] otherwise.
 */
public object FontSizeKey : AttributeKey<TextSize> {
    override val name: String = "fontSize"
    override val defaultValue: TextSize = TextSize.Unspecified
}
