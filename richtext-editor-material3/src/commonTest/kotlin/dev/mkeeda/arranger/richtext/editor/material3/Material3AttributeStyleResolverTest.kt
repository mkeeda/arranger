package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.toRgbaColor
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class Material3AttributeStyleResolverTest {
    private val typography = Typography()
    private val colorScheme = lightColorScheme()
    private val resolver = material3AttributeStyleResolver(typography = typography, colorScheme = colorScheme)

    @Test
    fun `heading maps to display and headline typography from MaterialTheme`() {
        var resolved = resolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H1))
        resolved.spanStyle?.fontSize shouldBe typography.displayLarge.fontSize

        resolved = resolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H3))
        resolved.spanStyle?.fontSize shouldBe typography.headlineLarge.fontSize

        resolved = resolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H6))
        resolved.spanStyle?.fontWeight shouldBe typography.titleMedium.fontWeight
    }

    @Test
    fun `heading preserves custom TextColorKey`() {
        val container =
            AttributeContainer.empty() +
                (HeadingKey to HeadingLevel.H1) +
                (TextColorKey to Color.Red.toRgbaColor())
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.fontSize shouldBe typography.displayLarge.fontSize
        resolved.spanStyle?.color shouldBe Color.Red
    }

    @Test
    fun `heading preserves custom BackgroundColorKey`() {
        val container =
            AttributeContainer.empty() +
                (HeadingKey to HeadingLevel.H2) +
                (BackgroundColorKey to Color.Green.toRgbaColor())
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.fontSize shouldBe typography.displayMedium.fontSize
        resolved.spanStyle?.background shouldBe Color.Green
    }

    @Test
    fun `heading merges with multiple span attributes including colors and font styling`() {
        val container =
            AttributeContainer.empty() +
                (HeadingKey to HeadingLevel.H3) +
                (ItalicKey to Unit) +
                (TextColorKey to Color.Blue.toRgbaColor()) +
                (BackgroundColorKey to Color.Yellow.toRgbaColor())
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.fontSize shouldBe typography.headlineLarge.fontSize
        resolved.spanStyle?.fontWeight shouldBe typography.headlineLarge.fontWeight
        resolved.spanStyle?.fontStyle shouldBe FontStyle.Italic
        resolved.spanStyle?.color shouldBe Color.Blue
        resolved.spanStyle?.background shouldBe Color.Yellow
    }

    @Test
    fun `unspecified heading level resolves without altering other attributes`() {
        val container =
            AttributeContainer.empty() +
                (HeadingKey to HeadingLevel.Unspecified) +
                (TextColorKey to Color.Magenta.toRgbaColor())
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.color shouldBe Color.Magenta
        resolved.spanStyle?.fontSize shouldBe TextUnit.Unspecified
    }

    @Test
    fun `heading uses explicit color from typography when configured`() {
        val darkTypography =
            Typography(
                displayLarge = Typography().displayLarge.copy(color = Color.LightGray),
            )
        val darkResolver = material3AttributeStyleResolver(typography = darkTypography, colorScheme = darkColorScheme())
        val resolved = darkResolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H1))

        resolved.spanStyle?.color shouldBe Color.LightGray
        resolved.spanStyle?.fontSize shouldBe darkTypography.displayLarge.fontSize
    }

    @Test
    fun `blockquote maps to bodyMedium style with onSurfaceVariant color`() {
        val resolved = resolver.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))

        resolved.spanStyle?.fontSize shouldBe typography.bodyMedium.fontSize
        resolved.spanStyle?.color shouldBe colorScheme.onSurfaceVariant
    }

    @Test
    fun `blockquote preserves custom BackgroundColorKey while applying onSurfaceVariant color`() {
        val container =
            AttributeContainer.empty() +
                (BlockquoteKey to Unit) +
                (BackgroundColorKey to Color.DarkGray.toRgbaColor())
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.color shouldBe colorScheme.onSurfaceVariant
        resolved.spanStyle?.background shouldBe Color.DarkGray
        resolved.spanStyle?.fontSize shouldBe typography.bodyMedium.fontSize
    }

    @Test
    fun `link maps to primary color with underline`() {
        val resolved = resolver.resolve(AttributeContainer.empty() + (LinkKey to "https://example.com"))

        resolved.spanStyle?.color shouldBe colorScheme.primary
        val decoration = resolved.spanStyle?.textDecoration.shouldNotBeNull()
        (TextDecoration.Underline in decoration) shouldBe true
    }

    @Test
    fun `link preserves custom BackgroundColorKey while applying theme primary color and underline`() {
        val container =
            AttributeContainer.empty() +
                (LinkKey to "https://example.com") +
                (BackgroundColorKey to Color.Yellow.toRgbaColor())
        val resolved = resolver.resolve(container)

        resolved.spanStyle?.color shouldBe colorScheme.primary
        resolved.spanStyle?.background shouldBe Color.Yellow
        val decoration = resolved.spanStyle?.textDecoration.shouldNotBeNull()
        (TextDecoration.Underline in decoration) shouldBe true
    }

    @Test
    fun `inline code maps to monospace font with surfaceContainerHighest background and onSurfaceVariant color`() {
        val resolved = resolver.resolve(AttributeContainer.empty() + (InlineCodeKey to Unit))

        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe colorScheme.surfaceContainerHighest
        resolved.spanStyle?.color shouldBe colorScheme.onSurfaceVariant
    }

    @Test
    fun `resolver respects custom or dark colorScheme`() {
        val customDarkScheme = darkColorScheme(onSurfaceVariant = Color.Red)
        val darkResolver = material3AttributeStyleResolver(typography = typography, colorScheme = customDarkScheme)

        val resolved = darkResolver.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))

        resolved.spanStyle?.color shouldBe Color.Red
    }

    @Test
    fun `wrapping material3AttributeStyleResolver allows overriding specific styles`() {
        val customResolver =
            AttributeStyleResolver(base = resolver) {
                spanStyle(LinkKey) {
                    SpanStyle(color = Color.Cyan)
                }
            }
        val resolved = customResolver.resolve(AttributeContainer.empty() + (LinkKey to "https://example.com"))

        resolved.spanStyle?.color shouldBe Color.Cyan
    }
}
