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
                BackgroundColorKey to RgbaColor(0xFF00FF00),
                TextColorKey to RgbaColor(0xFFFF0000),
            )

        val mention: RgbaColor? = container.getOrNull(BackgroundColorKey)
        val color: RgbaColor? = container.getOrNull(TextColorKey)

        mention shouldBe RgbaColor(0xFF00FF00)
        color shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `provides intuitive and type-safe access using extension functions and properties`() {
        val container =
            attributeContainerOf(
                BackgroundColorKey to RgbaColor(0xFF00FF00),
                TextColorKey to RgbaColor(0xFF0000FF),
            )

        container.getOrDefault(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
        container.getOrDefault(TextColorKey) shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `returns defaultValue or null when retrieving an unset attribute`() {
        val emptyContainer = attributeContainerOf()

        // getOrDefault
        emptyContainer.getOrDefault(BackgroundColorKey) shouldBe RgbaColor.Unspecified
        emptyContainer.getOrDefault(TextColorKey) shouldBe RgbaColor.Unspecified

        // getOrNull
        emptyContainer.getOrNull(BackgroundColorKey).shouldBeNull()
        emptyContainer.getOrNull(TextColorKey).shouldBeNull()
    }

    @Test
    fun `retains the newly set value when overwriting an attribute with the same name`() {
        val container =
            attributeContainerOf(
                TextColorKey to RgbaColor(0xFFFF0000),
                TextColorKey to RgbaColor(0xFF0000FF),
            )

        container.getOrDefault(TextColorKey) shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `plus returns new container maintaining immutability`() {
        val c1 = attributeContainerOf()
        val c2 = c1.plus(BackgroundColorKey, RgbaColor(0xFF00FFFF))
        val c3 = c2.plus(TextColorKey, RgbaColor(0xFFFF0000))

        c1.getOrNull(BackgroundColorKey).shouldBeNull()

        c2.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FFFF)
        c2.getOrNull(TextColorKey).shouldBeNull()

        c3.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FFFF)
        c3.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `plus operator merges two containers properly`() {
        val c1 = attributeContainerOf(BackgroundColorKey to RgbaColor(0xFF00FF00))
        val c2 = attributeContainerOf(TextColorKey to RgbaColor(0xFF0000FF))

        val merged = c1 + c2

        merged.getOrDefault(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
        merged.getOrDefault(TextColorKey) shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `isEmpty correctly reports empty state`() {
        val empty = attributeContainerOf()
        empty.isEmpty() shouldBe true

        val notEmpty = empty + (BackgroundColorKey to RgbaColor(0xFF00FF00))
        notEmpty.isEmpty() shouldBe false
    }

    @Test
    fun `minus correctly removes attribute and maintains immutability`() {
        val c1 = attributeContainerOf(BackgroundColorKey to RgbaColor(0xFF00FF00), TextColorKey to RgbaColor(0xFFFF0000))

        val c2 = c1 - TextColorKey

        c1.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        c2.getOrNull(TextColorKey).shouldBeNull()
        c2.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
    }

    @Test
    fun `minus on missing key returns same instance`() {
        val c1 = attributeContainerOf(BackgroundColorKey to RgbaColor(0xFF00FF00))
        val c2 = c1 - TextColorKey

        c2 shouldBeSameInstanceAs c1
    }

    @Test
    fun `attributeContainerOf factory functions create correct containers type-safely`() {
        val empty = attributeContainerOf()
        empty.isEmpty() shouldBe true

        val onePair = attributeContainerOf(BackgroundColorKey to RgbaColor(0xFF00FF00))
        onePair.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)

        val twoPairs =
            attributeContainerOf(
                BackgroundColorKey to RgbaColor(0xFF00FF00),
                TextColorKey to RgbaColor(0xFFFF0000),
            )
        twoPairs.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
        twoPairs.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
    }
}
