package dev.mkeeda.arranger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
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

    @Test
    fun `resolver maps matching attributes and merges them`() {
        val resolver =
            AttributeStyleResolver {
                resolve(BoldAttributeKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                resolve(ColorAttributeKey) { color -> SpanStyle(color = color) }
            }

        // Test with multiple attributes
        val container1 =
            attributeContainerOf(
                BoldAttributeKey to Unit,
                ColorAttributeKey to Color.Red,
            )

        val mergedStyle = resolver.resolve(container1)
        mergedStyle?.fontWeight shouldBe FontWeight.Bold
        mergedStyle?.color shouldBe Color.Red

        // Test with single attribute
        val container2 = attributeContainerOf(BoldAttributeKey to Unit)
        val singleStyle = resolver.resolve(container2)
        singleStyle?.fontWeight shouldBe FontWeight.Bold
        singleStyle?.color shouldBe Color.Unspecified

        // Test with missing attributes
        val emptyContainer = attributeContainerOf()
        resolver.resolve(emptyContainer).shouldBeNull()
    }
}
