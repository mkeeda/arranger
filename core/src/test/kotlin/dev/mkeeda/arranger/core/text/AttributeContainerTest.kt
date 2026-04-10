package dev.mkeeda.arranger.core.text

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class AttributeContainerTest {
    @Test
    fun `stores and retrieves attributes in a type-safe manner`() {
        val container =
            AttributeContainer.empty()
                .plus(MentionAttributeKey, "@mkeeda")
                .plus(ColorAttributeKey, TextColor.Red)

        val mention: String? = container.getOrNull(MentionAttributeKey)
        val color: TextColor? = container.getOrNull(ColorAttributeKey)

        mention shouldBe "@mkeeda"
        color shouldBe TextColor.Red
    }

    @Test
    fun `provides intuitive and type-safe access using extension functions and properties`() {
        val container =
            AttributeContainer.empty()
                .plusMention("@mkeeda")
                .plusTextColor(TextColor.Blue)

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
                .plusTextColor(TextColor.Red)
                .plusTextColor(TextColor.Blue)

        container.textColor shouldBe TextColor.Blue
    }

    @Test
    fun `plus returns new container maintaining immutability`() {
        val c1 = AttributeContainer.empty()
        val c2 = c1.plus(MentionAttributeKey, "@all")
        val c3 = c2.plus(ColorAttributeKey, TextColor.Red)

        c1.getOrNull(MentionAttributeKey).shouldBeNull()

        c2.getOrNull(MentionAttributeKey) shouldBe "@all"
        c2.getOrNull(ColorAttributeKey).shouldBeNull()

        c3.getOrNull(MentionAttributeKey) shouldBe "@all"
        c3.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
    }

    @Test
    fun `plus operator merges two containers properly`() {
        val c1 = AttributeContainer.empty().plusMention("@mkeeda")
        val c2 = AttributeContainer.empty().plusTextColor(TextColor.Blue)

        val merged = c1 + c2

        merged.getOrDefault(MentionAttributeKey) shouldBe "@mkeeda"
        merged.getOrDefault(ColorAttributeKey) shouldBe TextColor.Blue
    }

    @Test
    fun `isEmpty correctly reports empty state`() {
        val empty = AttributeContainer.empty()
        empty.isEmpty() shouldBe true

        val notEmpty = empty.plusMention("@user")
        notEmpty.isEmpty() shouldBe false
    }

    @Test
    fun `minus correctly removes attribute and maintains immutability`() {
        val c1 = AttributeContainer.empty().plusMention("@user").plusTextColor(TextColor.Red)

        val c2 = c1 - ColorAttributeKey

        c1.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        c2.getOrNull(ColorAttributeKey).shouldBeNull()
        c2.getOrNull(MentionAttributeKey) shouldBe "@user"
    }

    @Test
    fun `minus on missing key returns same instance`() {
        val c1 = AttributeContainer.empty().plusMention("@user")
        val c2 = c1 - ColorAttributeKey

        c2 shouldBeSameInstanceAs c1
    }
}
