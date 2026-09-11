package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.CodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DefaultAttributeStyleResolverTest {
    @Test
    fun `resolves CodeKey to Monospace font and subtle background color`() {
        val container = attributeContainerOf(CodeKey to Unit)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x14000000)
        resolved.paragraphStyle.shouldBeNull()
    }

    @Test
    fun `merges BoldKey and CodeKey orthogonal styles without interference`() {
        val container =
            attributeContainerOf(
                BoldKey to Unit,
                CodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x14000000)
    }

    @Test
    fun `merges ItalicKey and CodeKey orthogonal styles without interference`() {
        val container =
            attributeContainerOf(
                ItalicKey to Unit,
                CodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.fontStyle shouldBe FontStyle.Italic
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x14000000)
    }

    @Test
    fun `merges StrikethroughKey and CodeKey orthogonal styles without interference`() {
        val container =
            attributeContainerOf(
                StrikethroughKey to Unit,
                CodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.textDecoration shouldBe TextDecoration.LineThrough
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x14000000)
    }

    @Test
    fun `merges multiple inline styles including CodeKey simultaneously`() {
        val container =
            attributeContainerOf(
                BoldKey to Unit,
                ItalicKey to Unit,
                StrikethroughKey to Unit,
            ) + (CodeKey to Unit)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        resolved.spanStyle?.fontStyle shouldBe FontStyle.Italic
        resolved.spanStyle?.textDecoration shouldBe TextDecoration.LineThrough
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x14000000)
    }

    @Test
    fun `existing attributes maintain original style without font family`() {
        val boldContainer = attributeContainerOf(BoldKey to Unit)
        val boldResolved = DefaultAttributeStyleResolver.resolve(boldContainer)
        boldResolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
        boldResolved.spanStyle?.fontFamily.shouldBeNull()

        val underlineContainer = attributeContainerOf(UnderlineKey to Unit)
        val underlineResolved = DefaultAttributeStyleResolver.resolve(underlineContainer)
        underlineResolved.spanStyle?.textDecoration shouldBe TextDecoration.Underline
        underlineResolved.spanStyle?.fontFamily.shouldBeNull()
    }

    @Test
    fun `empty attributes resolve to null styles`() {
        val emptyContainer = attributeContainerOf()
        val resolved = DefaultAttributeStyleResolver.resolve(emptyContainer)
        resolved.spanStyle.shouldBeNull()
        resolved.paragraphStyle.shouldBeNull()
    }
}
