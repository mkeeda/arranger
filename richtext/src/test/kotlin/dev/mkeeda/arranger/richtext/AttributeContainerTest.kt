package dev.mkeeda.arranger.richtext

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class AttributeContainerTest {
    @Test
    fun `stores and retrieves attributes in a type-safe manner`() {
        val container =
            attributeContainerOf(
                BackgroundColorKey to HexColor("#00FF00"),
                TextColorKey to HexColor("#FF0000"),
            )

        val mention: HexColor? = container.getOrNull(BackgroundColorKey)
        val color: HexColor? = container.getOrNull(TextColorKey)

        mention shouldBe HexColor("#00FF00")
        color shouldBe HexColor("#FF0000")
    }

    @Test
    fun `provides intuitive and type-safe access using extension functions and properties`() {
        val container =
            attributeContainerOf(
                BackgroundColorKey to HexColor("#00FF00"),
                TextColorKey to HexColor("#0000FF"),
            )

        container.getOrDefault(BackgroundColorKey) shouldBe HexColor("#00FF00")
        container.getOrDefault(TextColorKey) shouldBe HexColor("#0000FF")
    }

    @Test
    fun `returns defaultValue or null when retrieving an unset attribute`() {
        val emptyContainer = attributeContainerOf()

        // getOrDefault
        emptyContainer.getOrDefault(BackgroundColorKey) shouldBe HexColor.Unspecified
        emptyContainer.getOrDefault(TextColorKey) shouldBe HexColor.Unspecified

        // getOrNull
        emptyContainer.getOrNull(BackgroundColorKey).shouldBeNull()
        emptyContainer.getOrNull(TextColorKey).shouldBeNull()
    }

    @Test
    fun `retains the newly set value when overwriting an attribute with the same name`() {
        val container =
            attributeContainerOf(
                TextColorKey to HexColor("#FF0000"),
                TextColorKey to HexColor("#0000FF"),
            )

        container.getOrDefault(TextColorKey) shouldBe HexColor("#0000FF")
    }

    @Test
    fun `plus returns new container maintaining immutability`() {
        val c1 = attributeContainerOf()
        val c2 = c1.plus(BackgroundColorKey, HexColor("#00FFFF"))
        val c3 = c2.plus(TextColorKey, HexColor("#FF0000"))

        c1.getOrNull(BackgroundColorKey).shouldBeNull()

        c2.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FFFF")
        c2.getOrNull(TextColorKey).shouldBeNull()

        c3.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FFFF")
        c3.getOrNull(TextColorKey) shouldBe HexColor("#FF0000")
    }

    @Test
    fun `plus operator merges two containers properly`() {
        val c1 = attributeContainerOf(BackgroundColorKey to HexColor("#00FF00"))
        val c2 = attributeContainerOf(TextColorKey to HexColor("#0000FF"))

        val merged = c1 + c2

        merged.getOrDefault(BackgroundColorKey) shouldBe HexColor("#00FF00")
        merged.getOrDefault(TextColorKey) shouldBe HexColor("#0000FF")
    }

    @Test
    fun `isEmpty correctly reports empty state`() {
        val empty = attributeContainerOf()
        empty.isEmpty() shouldBe true

        val notEmpty = empty + (BackgroundColorKey to HexColor("#00FF00"))
        notEmpty.isEmpty() shouldBe false
    }

    @Test
    fun `minus correctly removes attribute and maintains immutability`() {
        val c1 = attributeContainerOf(BackgroundColorKey to HexColor("#00FF00"), TextColorKey to HexColor("#FF0000"))

        val c2 = c1 - TextColorKey

        c1.getOrNull(TextColorKey) shouldBe HexColor("#FF0000")
        c2.getOrNull(TextColorKey).shouldBeNull()
        c2.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FF00")
    }

    @Test
    fun `minus on missing key returns same instance`() {
        val c1 = attributeContainerOf(BackgroundColorKey to HexColor("#00FF00"))
        val c2 = c1 - TextColorKey

        c2 shouldBeSameInstanceAs c1
    }

    @Test
    fun `attributeContainerOf factory functions create correct containers type-safely`() {
        val empty = attributeContainerOf()
        empty.isEmpty() shouldBe true

        val onePair = attributeContainerOf(BackgroundColorKey to HexColor("#00FF00"))
        onePair.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FF00")

        val twoPairs =
            attributeContainerOf(
                BackgroundColorKey to HexColor("#00FF00"),
                TextColorKey to HexColor("#FF0000"),
            )
        twoPairs.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FF00")
        twoPairs.getOrNull(TextColorKey) shouldBe HexColor("#FF0000")
    }
}
