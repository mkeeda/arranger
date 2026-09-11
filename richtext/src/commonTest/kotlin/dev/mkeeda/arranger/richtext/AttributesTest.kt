package dev.mkeeda.arranger.richtext

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AttributesTest {
    @Test
    fun `CodeKey has correct metadata and default value`() {
        CodeKey.name shouldBe "code"
        CodeKey.defaultValue shouldBe Unit
    }

    @Test
    fun `CodeKey implements SpanAttributeKey and not ParagraphAttributeKey`() {
        val key: AttributeKey<*> = CodeKey
        (key is SpanAttributeKey<*>) shouldBe true
        (key is ParagraphAttributeKey<*>) shouldBe false
    }

    @Test
    fun `CodeKey can be stored in and retrieved from AttributeContainer`() {
        val container = attributeContainerOf(CodeKey to Unit)
        container[CodeKey] shouldBe Unit
        container.getOrDefault(CodeKey) shouldBe Unit
        container.containsKey(CodeKey) shouldBe true
    }

    @Test
    fun `CodeKey returns defaultValue or null from AttributeContainer when unset`() {
        val container = attributeContainerOf()
        container[CodeKey].shouldBeNull()
        container.getOrDefault(CodeKey) shouldBe Unit
        container.containsKey(CodeKey) shouldBe false
    }

    @Test
    fun `CodeKey can be removed from AttributeContainer using minus operator`() {
        val container = attributeContainerOf(CodeKey to Unit)
        val removed = container - CodeKey
        removed[CodeKey].shouldBeNull()
        removed.containsKey(CodeKey) shouldBe false
    }

    @Test
    fun `CodeKey is preserved by filterSpanAttributes and filtered out by filterParagraphAttributes`() {
        val container =
            attributeContainerOf(
                CodeKey to Unit,
                HeadingKey to HeadingLevel.H1,
            )
        val spanOnly = container.filterSpanAttributes()
        spanOnly.containsKey(CodeKey) shouldBe true
        spanOnly.containsKey(HeadingKey) shouldBe false

        val paragraphOnly = container.filterParagraphAttributes()
        paragraphOnly.containsKey(CodeKey) shouldBe false
        paragraphOnly.containsKey(HeadingKey) shouldBe true
    }

    @Test
    fun `CodeKey data object provides meaningful string representation and equality`() {
        CodeKey.toString() shouldBe "CodeKey"
        (CodeKey == CodeKey) shouldBe true
    }
}
