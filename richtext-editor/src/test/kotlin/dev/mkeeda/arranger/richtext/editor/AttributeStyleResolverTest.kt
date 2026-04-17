package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.HexColor
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class AttributeStyleResolverTest {
    private object AlignAttributeKey : AttributeKey<TextAlign> {
        override val name: String = "Align"
        override val defaultValue: TextAlign = TextAlign.Unspecified
    }

    private val resolver =
        AttributeStyleResolver {
            spanStyle(BoldKey) { SpanStyle(fontWeight = FontWeight.Bold) }
            spanStyle(TextColorKey) { hexColor ->
                SpanStyle(
                    color =
                        if (hexColor == HexColor.Unspecified) {
                            Color.Unspecified
                        } else {
                            Color(
                                hexColor.value.removePrefix("#").toLong(16) or 0xFF000000,
                            )
                        },
                )
            }
            paragraphStyle(AlignAttributeKey) { align -> ParagraphStyle(textAlign = align) }
        }

    @Test
    fun `resolve returns merged styles when multiple attributes match`() {
        val container =
            attributeContainerOf(
                BoldKey to Unit,
                TextColorKey to HexColor("#FF0000"),
                AlignAttributeKey to TextAlign.Center,
            )

        val resolved = resolver.resolve(container)
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.spanStyle?.color shouldBe Color(0xFFFF0000)
        resolved.paragraphStyle?.textAlign shouldBe TextAlign.Center
    }

    @Test
    fun `resolve returns matching style when only single attribute matches`() {
        val container = attributeContainerOf(BoldKey to Unit)
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
