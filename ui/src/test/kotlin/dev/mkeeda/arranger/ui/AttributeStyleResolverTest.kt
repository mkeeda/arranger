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

    @Test
    fun `resolver maps matching attributes and merges them`() {
        val resolver =
            AttributeStyleResolver {
                spanStyle(BoldAttributeKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                spanStyle(ColorAttributeKey) { color -> SpanStyle(color = color) }
                paragraphStyle(AlignAttributeKey) { align -> ParagraphStyle(textAlign = align) }
            }

        // Test with multiple attributes for span and paragraph
        val container1 =
            attributeContainerOf(
                BoldAttributeKey to Unit,
                ColorAttributeKey to Color.Red,
                AlignAttributeKey to TextAlign.Center,
            )

        val resolved = resolver.resolve(container1)
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.spanStyle?.color shouldBe Color.Red
        resolved.paragraphStyle?.textAlign shouldBe TextAlign.Center

        // Test with single attribute
        val container2 = attributeContainerOf(BoldAttributeKey to Unit)
        val singleResolved = resolver.resolve(container2)
        singleResolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        singleResolved.spanStyle?.color shouldBe Color.Unspecified
        singleResolved.paragraphStyle.shouldBeNull()

        // Test with missing attributes
        val emptyContainer = attributeContainerOf()
        val emptyResolved = resolver.resolve(emptyContainer)
        emptyResolved.spanStyle.shouldBeNull()
        emptyResolved.paragraphStyle.shouldBeNull()
    }
}
