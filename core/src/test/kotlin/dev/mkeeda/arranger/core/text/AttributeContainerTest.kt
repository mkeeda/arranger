package dev.mkeeda.arranger.core.text

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class AttributeContainerTest {
    @Test
    fun `stores and retrieves attributes in a type-safe manner`() {
        val container =
            attributeContainerOf(
                MentionAttributeKey to "@mkeeda",
                ColorAttributeKey to TextColor.Red,
            )

        val mention: String? = container.getOrNull(MentionAttributeKey)
        val color: TextColor? = container.getOrNull(ColorAttributeKey)

        mention shouldBe "@mkeeda"
        color shouldBe TextColor.Red
    }

    @Test
    fun `provides intuitive and type-safe access using extension functions and properties`() {
        val container =
            attributeContainerOf(
                MentionAttributeKey to "@mkeeda",
                ColorAttributeKey to TextColor.Blue,
            )

        container.mention shouldBe "@mkeeda"
        container.textColor shouldBe TextColor.Blue
    }

    @Test
    fun `returns defaultValue or null when retrieving an unset attribute`() {
        val emptyContainer = attributeContainerOf()

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
            attributeContainerOf(
                ColorAttributeKey to TextColor.Red,
                ColorAttributeKey to TextColor.Blue,
            )

        container.textColor shouldBe TextColor.Blue
    }

    @Test
    fun `plus returns new container maintaining immutability`() {
        val c1 = attributeContainerOf()
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
        val c1 = attributeContainerOf(MentionAttributeKey to "@mkeeda")
        val c2 = attributeContainerOf(ColorAttributeKey to TextColor.Blue)

        val merged = c1 + c2

        merged.getOrDefault(MentionAttributeKey) shouldBe "@mkeeda"
        merged.getOrDefault(ColorAttributeKey) shouldBe TextColor.Blue
    }

    @Test
    fun `isEmpty correctly reports empty state`() {
        val empty = attributeContainerOf()
        empty.isEmpty() shouldBe true

        val notEmpty = empty + (MentionAttributeKey to "@user")
        notEmpty.isEmpty() shouldBe false
    }

    @Test
    fun `minus correctly removes attribute and maintains immutability`() {
        val c1 = attributeContainerOf(MentionAttributeKey to "@user", ColorAttributeKey to TextColor.Red)

        val c2 = c1 - ColorAttributeKey

        c1.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        c2.getOrNull(ColorAttributeKey).shouldBeNull()
        c2.getOrNull(MentionAttributeKey) shouldBe "@user"
    }

    @Test
    fun `minus on missing key returns same instance`() {
        val c1 = attributeContainerOf(MentionAttributeKey to "@user")
        val c2 = c1 - ColorAttributeKey

        c2 shouldBeSameInstanceAs c1
    }

    @Test
    fun `attributeContainerOf factory functions create correct containers type-safely`() {
        val empty = attributeContainerOf()
        empty.isEmpty() shouldBe true

        val onePair = attributeContainerOf(MentionAttributeKey to "@user")
        onePair.getOrNull(MentionAttributeKey) shouldBe "@user"

        val twoPairs =
            attributeContainerOf(
                MentionAttributeKey to "@mkeeda",
                ColorAttributeKey to TextColor.Red,
            )
        twoPairs.getOrNull(MentionAttributeKey) shouldBe "@mkeeda"
        twoPairs.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
    }
}
