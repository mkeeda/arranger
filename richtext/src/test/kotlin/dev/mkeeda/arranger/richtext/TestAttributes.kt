package dev.mkeeda.arranger.richtext

// Shared dummy attribute definitions for testing

internal enum class TextColor { Red, Blue, Unspecified }

internal object MentionAttributeKey : AttributeKey<String> {
    override val name: String = "Mention"
    override val defaultValue: String = ""
}

internal object ColorAttributeKey : AttributeKey<TextColor> {
    override val name: String = "Color"
    override val defaultValue: TextColor = TextColor.Unspecified
}

// Extension properties / functions for convenience

internal val AttributeContainer.mention: String
    get() = getOrDefault(MentionAttributeKey)

internal val AttributeContainer.textColor: TextColor
    get() = getOrDefault(ColorAttributeKey)
