package dev.mkeeda.arranger.core.text

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

// ==========================================
// 1. Definition of dummy attributes (keys) for testing
// ==========================================

internal enum class TextColor { Red, Blue, Unspecified }

internal object MentionAttributeKey : RichAttributeKey<String> {
    override val name: String = "Mention"
    override val defaultValue: String = ""
}

internal object ColorAttributeKey : RichAttributeKey<TextColor> {
    override val name: String = "Color"
    override val defaultValue: TextColor = TextColor.Unspecified
}

// ==========================================
// 2. Examples of API operations using extension properties/functions
// ==========================================

internal val AttributeContainer.mention: String
    get() = getOrDefault(MentionAttributeKey)

internal val AttributeContainer.textColor: TextColor
    get() = getOrDefault(ColorAttributeKey)

internal fun AttributeContainer.withMention(username: String): AttributeContainer =
    with(MentionAttributeKey, username)

internal fun AttributeContainer.withTextColor(color: TextColor): AttributeContainer =
    with(ColorAttributeKey, color)

// ==========================================
// 3. Test body
// ==========================================

class AttributeContainerTest {
    @Test
    fun `stores and retrieves attributes in a type-safe manner`() {
        val container =
            AttributeContainer.empty()
                .with(MentionAttributeKey, "@mkeeda")
                .with(ColorAttributeKey, TextColor.Red)

        val mention: String? = container.getOrNull(MentionAttributeKey)
        val color: TextColor? = container.getOrNull(ColorAttributeKey)

        mention shouldBe "@mkeeda"
        color shouldBe TextColor.Red
    }

    @Test
    fun `provides intuitive and type-safe access using extension functions and properties`() {
        val container =
            AttributeContainer.empty()
                .withMention("@mkeeda")
                .withTextColor(TextColor.Blue)

        container.mention shouldBe "@mkeeda"
        container.textColor shouldBe TextColor.Blue
    }

    @Test
    fun `returns defaultValue or null when retrieving an unset attribute`() {
        val emptyContainer = AttributeContainer.empty()

        // getOrDefault
        emptyContainer.getOrDefault(MentionAttributeKey) shouldBe ""
        emptyContainer.getOrDefault(ColorAttributeKey) shouldBe TextColor.Unspecified

        // getOrNull
        emptyContainer.getOrNull(MentionAttributeKey).shouldBeNull()
        emptyContainer.getOrNull(ColorAttributeKey).shouldBeNull()
    }

    @Test
    fun `retains the newly set value when overwriting an attribute with the same name`() {
        val container =
            AttributeContainer.empty()
                .withTextColor(TextColor.Red)
                .withTextColor(TextColor.Blue)

        container.textColor shouldBe TextColor.Blue
    }
}
