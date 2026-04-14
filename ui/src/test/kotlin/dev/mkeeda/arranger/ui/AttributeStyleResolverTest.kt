package dev.mkeeda.arranger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.mkeeda.arranger.core.text.AttributeKey
import dev.mkeeda.arranger.core.text.attributeContainerOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class AttributeStyleResolverTest {
    private object BoldAttributeKey : AttributeKey<Unit> {
        override val name: String = "Bold"
        override val defaultValue: Unit = Unit
    }

    private object ColorAttributeKey : AttributeKey<Color> {
        override val name: String = "Color"
        override val defaultValue: Color = Color.Unspecified
    }

    private object AlignAttributeKey : AttributeKey<TextAlign> {
        override val name: String = "Align"
        override val defaultValue: TextAlign = TextAlign.Unspecified
    }

    private val resolver =
        AttributeStyleResolver {
            spanStyle(BoldAttributeKey) { SpanStyle(fontWeight = FontWeight.Bold) }
            spanStyle(ColorAttributeKey) { color -> SpanStyle(color = color) }
            paragraphStyle(AlignAttributeKey) { align -> ParagraphStyle(textAlign = align) }
        }

    @Test
    fun `resolve returns merged styles when multiple attributes match`() {
        val container =
            attributeContainerOf(
                BoldAttributeKey to Unit,
                ColorAttributeKey to Color.Red,
                AlignAttributeKey to TextAlign.Center,
            )

        val resolved = resolver.resolve(container)
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.spanStyle?.color shouldBe Color.Red
        resolved.paragraphStyle?.textAlign shouldBe TextAlign.Center
    }

    @Test
    fun `resolve returns matching style when only single attribute matches`() {
        val container = attributeContainerOf(BoldAttributeKey to Unit)
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.spanStyle?.color shouldBe Color.Unspecified
        resolved.paragraphStyle.shouldBeNull()
    }

    @Test
    fun `resolve returns nulls when attributes are empty`() {
        val emptyContainer = attributeContainerOf()
        val resolved = resolver.resolve(emptyContainer)

        resolved.spanStyle.shouldBeNull()
        resolved.paragraphStyle.shouldBeNull()
    }
}
