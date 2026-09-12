package dev.mkeeda.arranger.richtext

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AttributesTest {
    @Test
    fun `InlineCodeKey has correct metadata and default value`() {
        InlineCodeKey.name shouldBe "inlineCode"
        InlineCodeKey.defaultValue shouldBe Unit
    }

    @Test
    fun `InlineCodeKey implements SpanAttributeKey and not ParagraphAttributeKey`() {
        val key: AttributeKey<*> = InlineCodeKey
        (key is SpanAttributeKey<*>) shouldBe true
        (key is ParagraphAttributeKey<*>) shouldBe false
    }

    @Test
    fun `InlineCodeKey can be stored in and retrieved from AttributeContainer`() {
        val container = attributeContainerOf(InlineCodeKey to Unit)
        container[InlineCodeKey] shouldBe Unit
        container.getOrDefault(InlineCodeKey) shouldBe Unit
        container.containsKey(InlineCodeKey) shouldBe true
    }

    @Test
    fun `InlineCodeKey returns defaultValue or null from AttributeContainer when unset`() {
        val container = attributeContainerOf()
        container[InlineCodeKey].shouldBeNull()
        container.getOrDefault(InlineCodeKey) shouldBe Unit
        container.containsKey(InlineCodeKey) shouldBe false
    }

    @Test
    fun `InlineCodeKey can be removed from AttributeContainer using minus operator`() {
        val container = attributeContainerOf(InlineCodeKey to Unit)
        val removed = container - InlineCodeKey
        removed[InlineCodeKey].shouldBeNull()
        removed.containsKey(InlineCodeKey) shouldBe false
    }

    @Test
    fun `InlineCodeKey is preserved by filterSpanAttributes and filtered out by filterParagraphAttributes`() {
        val container =
            attributeContainerOf(
                InlineCodeKey to Unit,
                HeadingKey to HeadingLevel.H1,
            )
        val spanOnly = container.filterSpanAttributes()
        spanOnly.containsKey(InlineCodeKey) shouldBe true
        spanOnly.containsKey(HeadingKey) shouldBe false

        val paragraphOnly = container.filterParagraphAttributes()
        paragraphOnly.containsKey(InlineCodeKey) shouldBe false
        paragraphOnly.containsKey(HeadingKey) shouldBe true
    }

    @Test
    fun `InlineCodeKey data object provides meaningful string representation and equality`() {
        InlineCodeKey.toString() shouldBe "InlineCodeKey"
        (InlineCodeKey == InlineCodeKey) shouldBe true
    }
}
