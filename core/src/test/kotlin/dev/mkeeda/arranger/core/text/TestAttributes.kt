package dev.mkeeda.arranger.core.text

// Shared dummy attribute definitions for testing

internal enum class TextColor { Red, Blue, Unspecified }

internal object MentionAttributeKey : RichAttributeKey<String> {
    override val name: String = "Mention"
    override val defaultValue: String = ""
}

internal object ColorAttributeKey : RichAttributeKey<TextColor> {
    override val name: String = "Color"
    override val defaultValue: TextColor = TextColor.Unspecified
}

// Extension properties / functions for convenience

internal val AttributeContainer.mention: String
    get() = getOrDefault(MentionAttributeKey)

internal val AttributeContainer.textColor: TextColor
    get() = getOrDefault(ColorAttributeKey)

internal fun AttributeContainer.withMention(username: String): AttributeContainer =
    with(MentionAttributeKey, username)

internal fun AttributeContainer.withTextColor(color: TextColor): AttributeContainer =
    with(ColorAttributeKey, color)
