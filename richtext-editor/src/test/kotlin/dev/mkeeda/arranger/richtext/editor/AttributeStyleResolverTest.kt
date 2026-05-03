package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.AlignmentAttributeKey
import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttributeStyleResolverTest {
    private object TestSpanKey : SpanAttributeKey<String> {
        override val name: String = "TestSpan"
        override val defaultValue: String = ""
    }

    private object TestParagraphKey : AlignmentAttributeKey<TextAlign> {
        override val name: String = "TestParagraph"
        override val defaultValue: TextAlign = TextAlign.Unspecified
    }

    private object TestParagraphAndSpanKey : BlockTypeAttributeKey<Unit> {
        override val name: String = "TestCombined"
        override val defaultValue: Unit = Unit
    }

    private val resolver =
        AttributeStyleResolver {
            spanStyle(TestSpanKey) { spanValue ->
                SpanStyle(color = Color(spanValue.toLong(16)))
            }
            paragraphStyle(TestParagraphKey) { align ->
                ParagraphStyle(textAlign = align)
            }
            spanStyle(TestParagraphAndSpanKey) {
                SpanStyle(fontWeight = FontWeight.Bold)
            }
            paragraphStyle(TestParagraphAndSpanKey) {
                ParagraphStyle(lineHeight = 24.sp)
            }
        }

    @Test
    fun `resolve returns merged styles when multiple attributes match`() {
        val container =
            attributeContainerOf(
                TestSpanKey to "FFFF0000",
                TestParagraphKey to TextAlign.Center,
                TestParagraphAndSpanKey to Unit,
            )

        val resolved = resolver.resolve(container)
        resolved.spanStyle?.color shouldBe Color(0xFFFF0000)
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold // From TestParagraphAndSpanKey
        resolved.paragraphStyle?.textAlign shouldBe TextAlign.Center
        resolved.paragraphStyle?.lineHeight shouldBe 24.sp // From TestParagraphAndSpanKey
    }

    @Test
    fun `resolve returns matching style when only span attribute matches`() {
        val container = attributeContainerOf(TestSpanKey to "FF00FF00")
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color(0xFF00FF00)
        resolved.spanStyle?.fontWeight.shouldBeNull()
        resolved.paragraphStyle.shouldBeNull()
    }

    @Test
    fun `resolve returns matching style when only paragraph attribute matches`() {
        val container = attributeContainerOf(TestParagraphKey to TextAlign.Right)
        val resolved = resolver.resolve(container)

        resolved.spanStyle.shouldBeNull()
        resolved.paragraphStyle?.textAlign shouldBe TextAlign.Right
    }

    @Test
    fun `resolve returns matching span and paragraph styles when combined attribute matches`() {
        val container = attributeContainerOf(TestParagraphAndSpanKey to Unit)
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.paragraphStyle?.lineHeight shouldBe 24.sp
    }

    @Test
    fun `AttributeStyleResolver allows custom resolver to override base default`() {
        val overridingResolver =
            AttributeStyleResolver(base = resolver) {
                spanStyle(TestParagraphAndSpanKey) {
                    SpanStyle(fontWeight = FontWeight.Normal)
                }
            }

        val container = attributeContainerOf(TestParagraphAndSpanKey to Unit)
        val resolved = overridingResolver.resolve(container)

        // Custom resolver overrides the base
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Normal
        // Base resolver properties that are not overridden still apply
        resolved.paragraphStyle?.lineHeight shouldBe 24.sp
    }

    @Test
    fun `resolve returns nulls when attributes are empty`() {
        val emptyContainer = attributeContainerOf()
        val resolved = resolver.resolve(emptyContainer)

        resolved.spanStyle.shouldBeNull()
        resolved.paragraphStyle.shouldBeNull()
    }
}
