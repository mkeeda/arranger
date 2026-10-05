package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.AlignmentAttributeKey
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

    @Test
    fun `DefaultAttributeStyleResolver resolves LinkKey to blue underline`() {
        val container = attributeContainerOf(LinkKey to "https://example.com")
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color(0xFF1E88E5)
        resolved.spanStyle?.textDecoration shouldBe TextDecoration.Underline
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves InlineCodeKey to monospace font and neutral background`() {
        val container = attributeContainerOf(InlineCodeKey to Unit)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x1F888888)
    }

    @Test
    fun `defaultAttributeStyleResolver allows customizing link and inline code colors`() {
        val customResolver =
            defaultAttributeStyleResolver(
                linkColor = Color.Green,
                inlineCodeBackgroundColor = Color.Yellow,
            )
        val linkContainer = attributeContainerOf(LinkKey to "https://example.com")
        val codeContainer = attributeContainerOf(InlineCodeKey to Unit)

        val resolvedLink = customResolver.resolve(linkContainer)
        resolvedLink.spanStyle?.color shouldBe Color.Green

        val resolvedCode = customResolver.resolve(codeContainer)
        resolvedCode.spanStyle?.background shouldBe Color.Yellow
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves both StrikethroughKey and UnderlineKey combined`() {
        val container =
            attributeContainerOf(
                StrikethroughKey to Unit,
                UnderlineKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        val decoration = resolved.spanStyle?.textDecoration.shouldNotBeNull()
        (TextDecoration.LineThrough in decoration) shouldBe true
        (TextDecoration.Underline in decoration) shouldBe true
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves both StrikethroughKey and LinkKey combined`() {
        val container =
            attributeContainerOf(
                StrikethroughKey to Unit,
                LinkKey to "https://example.com",
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color(0xFF1E88E5)
        val decoration = resolved.spanStyle?.textDecoration.shouldNotBeNull()
        (TextDecoration.LineThrough in decoration) shouldBe true
        (TextDecoration.Underline in decoration) shouldBe true
    }

    @Test
    fun `AttributeStyleResolver combines TextDecoration across base and custom resolvers`() {
        val baseResolver =
            AttributeStyleResolver {
                spanStyle(StrikethroughKey) {
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                }
            }
        val combinedResolver =
            AttributeStyleResolver(base = baseResolver) {
                spanStyle(UnderlineKey) {
                    SpanStyle(textDecoration = TextDecoration.Underline)
                }
            }

        val container =
            attributeContainerOf(
                StrikethroughKey to Unit,
                UnderlineKey to Unit,
            )
        val resolved = combinedResolver.resolve(container)

        val decoration = resolved.spanStyle?.textDecoration.shouldNotBeNull()
        (TextDecoration.LineThrough in decoration) shouldBe true
        (TextDecoration.Underline in decoration) shouldBe true
    }

    @Test
    fun `AttributeStyleResolver respects TextDecoration None override`() {
        val baseResolver =
            AttributeStyleResolver {
                spanStyle(UnderlineKey) {
                    SpanStyle(textDecoration = TextDecoration.Underline)
                }
            }
        val overridingResolver =
            AttributeStyleResolver(base = baseResolver) {
                spanStyle(UnderlineKey) {
                    SpanStyle(textDecoration = TextDecoration.None)
                }
            }

        val container = attributeContainerOf(UnderlineKey to Unit)
        val resolved = overridingResolver.resolve(container)

        resolved.spanStyle?.textDecoration shouldBe TextDecoration.None
    }

    @Test
    fun `DefaultAttributeStyleResolver preserves TextColorKey when combined with InlineCodeKey`() {
        val container =
            attributeContainerOf(
                InlineCodeKey to Unit,
                TextColorKey to Color.Red.toRgbaColor(),
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color.Red
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x1F888888)
    }

    @Test
    fun `DefaultAttributeStyleResolver preserves TextColorKey and BackgroundColorKey when combined with HeadingKey`() {
        val container =
            attributeContainerOf(
                HeadingKey to HeadingLevel.H1,
                TextColorKey to Color.Blue.toRgbaColor(),
                BackgroundColorKey to Color.Yellow.toRgbaColor(),
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color.Blue
        resolved.spanStyle?.background shouldBe Color.Yellow
        resolved.spanStyle?.fontSize shouldBe 32.sp
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold
    }

    @Test
    fun `DefaultAttributeStyleResolver preserves BackgroundColorKey when combined with LinkKey`() {
        val container =
            attributeContainerOf(
                LinkKey to "https://example.com",
                BackgroundColorKey to Color.LightGray.toRgbaColor(),
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color(0xFF1E88E5)
        resolved.spanStyle?.background shouldBe Color.LightGray
        val decoration = resolved.spanStyle?.textDecoration.shouldNotBeNull()
        (TextDecoration.Underline in decoration) shouldBe true
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves spanStyle for CodeBlockKey with monospace fontFamily`() {
        val container = attributeContainerOf(CodeBlockKey to "kotlin")
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves paragraphStyle for CodeBlockKey with textIndent`() {
        val container = attributeContainerOf(CodeBlockKey to null)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.paragraphStyle?.textIndent?.firstLine shouldBe 12.sp
        resolved.paragraphStyle?.textIndent?.restLine shouldBe 12.sp
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves paragraphStyle for CodeBlockKey with lineHeight and blockLineHeightStyle`() {
        val container = attributeContainerOf(CodeBlockKey to null)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.paragraphStyle?.lineHeight shouldBe 24.sp
        resolved.paragraphStyle?.lineHeightStyle?.alignment shouldBe LineHeightStyle.Alignment.Center
        resolved.paragraphStyle?.lineHeightStyle?.trim shouldBe LineHeightStyle.Trim.None
    }

    @Test
    fun `DefaultAttributeStyleResolver resolves paragraphStyle for BlockquoteKey with lineHeight and blockLineHeightStyle`() {
        val container = attributeContainerOf(BlockquoteKey to Unit)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        resolved.paragraphStyle?.lineHeight shouldBe 24.sp
        resolved.paragraphStyle?.lineHeightStyle?.alignment shouldBe LineHeightStyle.Alignment.Center
        resolved.paragraphStyle?.lineHeightStyle?.trim shouldBe LineHeightStyle.Trim.None
    }

    @Test
    fun `AttributeStyleBuilder applies blockTypeParagraphStyle to any BlockTypeAttributeKey`() {
        val customResolver =
            AttributeStyleResolver {
                blockTypeParagraphStyle {
                    ParagraphStyle(lineHeight = 32.sp)
                }
            }
        val resolved = customResolver.resolve(attributeContainerOf(TestParagraphAndSpanKey to Unit))
        resolved.paragraphStyle?.lineHeight shouldBe 32.sp
    }

    @Test
    fun `DefaultAttributeStyleResolver applies uniform block padding to custom BlockTypeAttributeKey`() {
        val customBlockKey =
            object : BlockTypeAttributeKey<Unit> {
                override val name: String = "CustomBlock"
                override val defaultValue: Unit = Unit
            }
        val resolved = DefaultAttributeStyleResolver.resolve(attributeContainerOf(customBlockKey to Unit))

        resolved.paragraphStyle?.lineHeight shouldBe DefaultBlockLineHeight
        resolved.paragraphStyle?.lineHeightStyle shouldBe DefaultBlockLineHeightStyle
    }
}
