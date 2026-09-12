package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Empirical stress tests for [DefaultAttributeStyleResolver] focusing on:
 * 1. Property completeness and orthogonality of InlineCodeKey
 * 2. Style inheritance and non-destruction with HeadingKey and paragraph styles
 * 3. Multi-span overlap and style synthesis verification
 */
class DefaultAttributeStyleResolverStressTest {
    // =========================================================================
    // 1. Property Completeness and Orthogonality of InlineCodeKey
    // =========================================================================

    @Test
    fun `InlineCodeKey resolves isolated Monospace font and subtle background without polluting other properties`() {
        val container = attributeContainerOf(InlineCodeKey to Unit)
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        val spanStyle = resolved.spanStyle.shouldNotBeNull()

        // Explicitly resolved properties
        spanStyle.fontFamily shouldBe FontFamily.Monospace
        spanStyle.background shouldBe Color(0x14000000)

        // Orthogonal properties MUST remain unspecified/null so they don't overwrite parent/inherited styles
        spanStyle.color shouldBe Color.Unspecified
        spanStyle.fontSize shouldBe TextUnit.Unspecified
        spanStyle.fontWeight.shouldBeNull()
        spanStyle.fontStyle.shouldBeNull()
        spanStyle.letterSpacing shouldBe TextUnit.Unspecified
        spanStyle.baselineShift.shouldBeNull()
        spanStyle.textDecoration.shouldBeNull()
        spanStyle.shadow.shouldBeNull()
        spanStyle.drawStyle.shouldBeNull()

        // InlineCodeKey is an inline span attribute, so paragraphStyle MUST be null
        resolved.paragraphStyle.shouldBeNull()
    }

    @Test
    fun `InlineCodeKey merges orthogonally with TextColorKey and FontSizeKey`() {
        val customColor = RgbaColor(0xFFFF5722)
        val customSize = TextSize(22f)
        val container =
            attributeContainerOf(
                InlineCodeKey to Unit,
                TextColorKey to customColor,
                FontSizeKey to customSize,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)
        val span = resolved.spanStyle.shouldNotBeNull()

        span.fontFamily shouldBe FontFamily.Monospace
        span.background shouldBe Color(0x14000000)
        span.color shouldBe Color(0xFFFF5722)
        span.fontSize shouldBe 22.sp
        resolved.paragraphStyle.shouldBeNull()
    }

    @Test
    fun `InlineCodeKey merges orthogonally with LinkKey`() {
        val container =
            attributeContainerOf(
                InlineCodeKey to Unit,
                LinkKey to "https://arranger.dev",
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)
        val span = resolved.spanStyle.shouldNotBeNull()

        span.fontFamily shouldBe FontFamily.Monospace
        span.background shouldBe Color(0x14000000)
        span.color shouldBe Color(0xFF1E88E5)
        span.textDecoration shouldBe TextDecoration.Underline
    }

    @Test
    fun `Adversarial Check - BackgroundColorKey and InlineCodeKey coexistence behavior in same container`() {
        val customBg = RgbaColor(0xFF00FF00) // Green
        val container =
            attributeContainerOf(
                BackgroundColorKey to customBg,
                InlineCodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)
        val span = resolved.spanStyle.shouldNotBeNull()

        span.fontFamily shouldBe FontFamily.Monospace
        // Verify which background wins: InlineCodeKey is registered after BackgroundColorKey in DefaultAttributeStyleResolver
        // so SpanStyle.merge causes InlineCodeKey's subtle background to override the custom BackgroundColorKey.
        span.background shouldBe Color(0x14000000)
    }

    // =========================================================================
    // 2. HeadingKey & Paragraph Style Inheritance (Font Size & Line Height Preservation)
    // =========================================================================

    @Test
    fun `InlineCodeKey merged with all Heading levels H1 through H6 preserves font sizes bold weight and line heights`() {
        data class HeadingSpec(
            val level: HeadingLevel,
            val expectedFontSize: TextUnit,
            val expectedLineHeight: TextUnit,
        )

        val headingSpecs =
            listOf(
                HeadingSpec(HeadingLevel.H1, 32.sp, 40.sp),
                HeadingSpec(HeadingLevel.H2, 24.sp, 32.sp),
                HeadingSpec(HeadingLevel.H3, 20.sp, 28.sp),
                HeadingSpec(HeadingLevel.H4, 16.sp, 24.sp),
                HeadingSpec(HeadingLevel.H5, 14.sp, 20.sp),
                HeadingSpec(HeadingLevel.H6, 12.sp, 16.sp),
            )

        for (spec in headingSpecs) {
            val container =
                attributeContainerOf(
                    HeadingKey to spec.level,
                    InlineCodeKey to Unit,
                )
            val resolved = DefaultAttributeStyleResolver.resolve(container)

            val span = resolved.spanStyle.shouldNotBeNull()
            span.fontSize shouldBe spec.expectedFontSize
            span.fontWeight shouldBe FontWeight.Bold
            span.fontFamily shouldBe FontFamily.Monospace
            span.background shouldBe Color(0x14000000)

            val paragraph = resolved.paragraphStyle.shouldNotBeNull()
            paragraph.lineHeight shouldBe spec.expectedLineHeight
            paragraph.lineBreak shouldBe LineBreak.Heading
        }
    }

    @Test
    fun `InlineCodeKey merged with BlockquoteKey preserves blockquote indent line height italic and alpha`() {
        val container =
            attributeContainerOf(
                BlockquoteKey to Unit,
                InlineCodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        val span = resolved.spanStyle.shouldNotBeNull()
        span.fontFamily shouldBe FontFamily.Monospace
        span.background shouldBe Color(0x14000000)
        span.fontStyle shouldBe FontStyle.Italic
        span.color shouldBe Color.Unspecified.copy(alpha = 0.5f)

        val paragraph = resolved.paragraphStyle.shouldNotBeNull()
        paragraph.textIndent shouldBe TextIndent(firstLine = 16.sp, restLine = 16.sp)
        paragraph.lineHeight shouldBe 24.sp
        paragraph.lineBreak shouldBe LineBreak.Paragraph
    }

    @Test
    fun `InlineCodeKey merged with BulletListKey preserves list indent and line height`() {
        val container =
            attributeContainerOf(
                BulletListKey to ListIndentLevel.Level1,
                InlineCodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        val span = resolved.spanStyle.shouldNotBeNull()
        span.fontFamily shouldBe FontFamily.Monospace
        span.background shouldBe Color(0x14000000)

        val paragraph = resolved.paragraphStyle.shouldNotBeNull()
        paragraph.textIndent shouldBe TextIndent(firstLine = 24.sp, restLine = 24.sp)
        paragraph.lineHeight shouldBe 24.sp
        paragraph.lineBreak shouldBe LineBreak.Paragraph
    }

    @Test
    fun `InlineCodeKey merged with OrderedListKey preserves list indent and line height`() {
        val container =
            attributeContainerOf(
                OrderedListKey to ListIndentLevel.Level2,
                InlineCodeKey to Unit,
            )
        val resolved = DefaultAttributeStyleResolver.resolve(container)

        val span = resolved.spanStyle.shouldNotBeNull()
        span.fontFamily shouldBe FontFamily.Monospace
        span.background shouldBe Color(0x14000000)

        val paragraph = resolved.paragraphStyle.shouldNotBeNull()
        paragraph.textIndent shouldBe TextIndent(firstLine = 48.sp, restLine = 48.sp)
        paragraph.lineHeight shouldBe 24.sp
    }

    @Test
    fun `InlineCodeKey merged with TextAlignmentKey preserves alignment`() {
        for (align in listOf(TextAlignment.Left, TextAlignment.Center, TextAlignment.Right, TextAlignment.Justify)) {
            val container =
                attributeContainerOf(
                    TextAlignmentKey to align,
                    InlineCodeKey to Unit,
                )
            val resolved = DefaultAttributeStyleResolver.resolve(container)

            val span = resolved.spanStyle.shouldNotBeNull()
            span.fontFamily shouldBe FontFamily.Monospace
            span.background shouldBe Color(0x14000000)

            val paragraph = resolved.paragraphStyle.shouldNotBeNull()
            val expectedTextAlign =
                when (align) {
                    TextAlignment.Left -> TextAlign.Left
                    TextAlignment.Center -> TextAlign.Center
                    TextAlignment.Right -> TextAlign.Right
                    TextAlignment.Justify -> TextAlign.Justify
                    TextAlignment.Unspecified -> TextAlign.Unspecified
                }
            paragraph.textAlign shouldBe expectedTextAlign
        }
    }

    // =========================================================================
    // 3. Multi-Span Overlap & Synthesis Verification
    // =========================================================================

    @Test
    fun `Separate Heading span and inline Code span synthesize without destroying font size or line height`() {
        // In Arranger's RichString model, a heading is often a span over the whole paragraph (e.g. 0..19),
        // and inline code is a sub-span (e.g. 5..10).
        val headingAttributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)
        val codeAttributes = attributeContainerOf(InlineCodeKey to Unit)

        val headingResolved = DefaultAttributeStyleResolver.resolve(headingAttributes)
        val codeResolved = DefaultAttributeStyleResolver.resolve(codeAttributes)

        val headingSpanStyle = headingResolved.spanStyle.shouldNotBeNull()
        val codeSpanStyle = codeResolved.spanStyle.shouldNotBeNull()

        // When Compose merges heading style with nested code span style:
        val synthesized = headingSpanStyle.merge(codeSpanStyle)

        synthesized.fontSize shouldBe 32.sp // Preserved from H1!
        synthesized.fontWeight shouldBe FontWeight.Bold // Preserved from H1!
        synthesized.fontFamily shouldBe FontFamily.Monospace // Applied from InlineCodeKey!
        synthesized.background shouldBe Color(0x14000000) // Applied from InlineCodeKey!

        // Paragraph style remains intact from heading
        val paragraph = headingResolved.paragraphStyle.shouldNotBeNull()
        paragraph.lineHeight shouldBe 40.sp
        paragraph.lineBreak shouldBe LineBreak.Heading
    }

    @Test
    fun `Three overlapping inline spans Bold Code and Italic synthesize correctly across intersection segments`() {
        // Scenario:
        // Text: "01234567890123456789" (20 chars)
        // Span 1 (Bold):   0..10
        // Span 2 (Code):   5..15
        // Span 3 (Italic): 10..19
        // Segments:
        // [0..4]: Bold only
        // [5..9]: Bold + Code
        // [10..10]: Bold + Code + Italic
        // [11..15]: Code + Italic
        // [16..19]: Italic only

        val boldResolved = DefaultAttributeStyleResolver.resolve(attributeContainerOf(BoldKey to Unit)).spanStyle.shouldNotBeNull()
        val codeResolved = DefaultAttributeStyleResolver.resolve(attributeContainerOf(InlineCodeKey to Unit)).spanStyle.shouldNotBeNull()
        val italicResolved = DefaultAttributeStyleResolver.resolve(attributeContainerOf(ItalicKey to Unit)).spanStyle.shouldNotBeNull()

        // Segment [0..4]: Bold only
        boldResolved.fontWeight shouldBe FontWeight.Bold
        boldResolved.fontFamily.shouldBeNull()
        boldResolved.fontStyle.shouldBeNull()

        // Segment [5..9]: Bold + Code
        val boldCode = boldResolved.merge(codeResolved)
        boldCode.fontWeight shouldBe FontWeight.Bold
        boldCode.fontFamily shouldBe FontFamily.Monospace
        boldCode.background shouldBe Color(0x14000000)
        boldCode.fontStyle.shouldBeNull()

        // Segment [10..10]: Bold + Code + Italic
        val triple = boldResolved.merge(codeResolved).merge(italicResolved)
        triple.fontWeight shouldBe FontWeight.Bold
        triple.fontFamily shouldBe FontFamily.Monospace
        triple.background shouldBe Color(0x14000000)
        triple.fontStyle shouldBe FontStyle.Italic

        // Segment [11..15]: Code + Italic
        val codeItalic = codeResolved.merge(italicResolved)
        codeItalic.fontWeight.shouldBeNull()
        codeItalic.fontFamily shouldBe FontFamily.Monospace
        codeItalic.background shouldBe Color(0x14000000)
        codeItalic.fontStyle shouldBe FontStyle.Italic

        // Segment [16..19]: Italic only
        italicResolved.fontWeight.shouldBeNull()
        italicResolved.fontFamily.shouldBeNull()
        italicResolved.fontStyle shouldBe FontStyle.Italic
    }

    @Test
    fun `RichString with Heading and embedded InlineCodeKey splits into 3 non-overlapping chunks and resolves styles consistently`() {
        val richString =
            RichString("Hello World of Code")
                .edit {
                    setParagraphAttribute(HeadingKey, HeadingLevel.H2, range = 0..18)
                    setSpanAttribute(InlineCodeKey, Unit, range = 6..10) // "World"
                }

        // SpanMerger splits overlapping ranges into distinct non-overlapping chunks:
        // Chunk 0: 0..5 ("Hello ") -> Heading H2
        // Chunk 1: 6..10 ("World") -> Heading H2 + InlineCodeKey
        // Chunk 2: 11..18 (" of Code") -> Heading H2
        richString.spans.size shouldBe 3

        val chunk0 = richString.spans[0]
        chunk0.range shouldBe 0..5
        val style0 = DefaultAttributeStyleResolver.resolve(chunk0.attributes)
        style0.spanStyle?.fontSize shouldBe 24.sp
        style0.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style0.spanStyle?.fontFamily.shouldBeNull()
        style0.paragraphStyle?.lineHeight shouldBe 32.sp

        val chunk1 = richString.spans[1]
        chunk1.range shouldBe 6..10
        val style1 = DefaultAttributeStyleResolver.resolve(chunk1.attributes)
        style1.spanStyle?.fontSize shouldBe 24.sp // Preserved!
        style1.spanStyle?.fontWeight shouldBe FontWeight.Bold // Preserved!
        style1.spanStyle?.fontFamily shouldBe FontFamily.Monospace // Applied!
        style1.spanStyle?.background shouldBe Color(0x14000000) // Applied!
        style1.paragraphStyle?.lineHeight shouldBe 32.sp // Preserved!

        val chunk2 = richString.spans[2]
        chunk2.range shouldBe 11..19
        val style2 = DefaultAttributeStyleResolver.resolve(chunk2.attributes)
        style2.spanStyle?.fontSize shouldBe 24.sp
        style2.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style2.spanStyle?.fontFamily.shouldBeNull()
        style2.paragraphStyle?.lineHeight shouldBe 32.sp
    }

    @Test
    fun `RichTextState toggleFormat with InlineCodeKey applies Monospace style and toggle off clears it`() {
        val text = "Inline Code Test"
        val state = RichTextState(initialText = RichString(text))

        // Select "Code" (range 7..10)
        state.textFieldState.edit {
            selection = TextRange(7, 11)
        }

        // Toggle on InlineCodeKey
        state.toggleFormat(InlineCodeKey)

        state.richString.spans.size shouldBe 1
        val span = state.richString.spans.first()
        span.range shouldBe 7..10
        val resolvedOn = DefaultAttributeStyleResolver.resolve(span.attributes)
        resolvedOn.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolvedOn.spanStyle?.background shouldBe Color(0x14000000)

        // Toggle off InlineCodeKey
        state.toggleFormat(InlineCodeKey)
        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `RichTextState with Heading H1 and embedded InlineCodeKey preserves heading font size and line height across entire paragraph`() {
        val text = "Heading with code inside"
        val state = RichTextState(initialText = RichString(text))

        // Apply Heading 1 to the paragraph
        state.applyFormat(HeadingKey, HeadingLevel.H1)

        // Select "code" (range 13..16)
        state.textFieldState.edit {
            selection = TextRange(13, 17)
        }
        state.toggleFormat(InlineCodeKey)

        // Should split into 3 segments:
        // [0..12]: "Heading with " (H1 only)
        // [13..16]: "code" (H1 + InlineCodeKey)
        // [17..24]: " inside" (H1 only)
        state.richString.spans.size shouldBe 3

        val chunk0 = state.richString.spans[0]
        val style0 = DefaultAttributeStyleResolver.resolve(chunk0.attributes)
        style0.spanStyle?.fontSize shouldBe 32.sp
        style0.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style0.spanStyle?.fontFamily.shouldBeNull()
        style0.paragraphStyle?.lineHeight shouldBe 40.sp

        val chunk1 = state.richString.spans[1]
        val style1 = DefaultAttributeStyleResolver.resolve(chunk1.attributes)
        style1.spanStyle?.fontSize shouldBe 32.sp // H1 font size preserved!
        style1.spanStyle?.fontWeight shouldBe FontWeight.Bold // H1 bold preserved!
        style1.spanStyle?.fontFamily shouldBe FontFamily.Monospace // Code monospace applied!
        style1.spanStyle?.background shouldBe Color(0x14000000) // Code background applied!
        style1.paragraphStyle?.lineHeight shouldBe 40.sp // H1 line height preserved!

        val chunk2 = state.richString.spans[2]
        val style2 = DefaultAttributeStyleResolver.resolve(chunk2.attributes)
        style2.spanStyle?.fontSize shouldBe 32.sp
        style2.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style2.spanStyle?.fontFamily.shouldBeNull()
        style2.paragraphStyle?.lineHeight shouldBe 40.sp
    }

    @Test
    fun `RichTextState sequential overlapping formats Bold Code and Italic resolve orthogonal styles cleanly`() {
        val text = "Overlapping Formats Test"
        val state = RichTextState(initialText = RichString(text))

        // 1. Bold whole text (0..24)
        state.textFieldState.edit { selection = TextRange(0, 24) }
        state.toggleFormat(BoldKey)

        // 2. Code first word "Overlapping" (0..11)
        state.textFieldState.edit { selection = TextRange(0, 11) }
        state.toggleFormat(InlineCodeKey)

        // 3. Italic "Formats" (12..19)
        state.textFieldState.edit { selection = TextRange(12, 19) }
        state.toggleFormat(ItalicKey)

        // Resulting segments:
        // [0..10]: Bold + Code ("Overlapping")
        // [11..11]: Bold (" ")
        // [12..18]: Bold + Italic ("Formats")
        // [19..23]: Bold (" Test")
        state.richString.spans.size shouldBe 4

        // Check chunk 0: Bold + Code
        val chunk0 = state.richString.spans[0]
        val style0 = DefaultAttributeStyleResolver.resolve(chunk0.attributes)
        style0.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style0.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        style0.spanStyle?.background shouldBe Color(0x14000000)
        style0.spanStyle?.fontStyle.shouldBeNull()

        // Check chunk 1: Bold only
        val chunk1 = state.richString.spans[1]
        val style1 = DefaultAttributeStyleResolver.resolve(chunk1.attributes)
        style1.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style1.spanStyle?.fontFamily.shouldBeNull()

        // Check chunk 2: Bold + Italic
        val chunk2 = state.richString.spans[2]
        val style2 = DefaultAttributeStyleResolver.resolve(chunk2.attributes)
        style2.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style2.spanStyle?.fontStyle shouldBe FontStyle.Italic
        style2.spanStyle?.fontFamily.shouldBeNull()

        // Check chunk 3: Bold only
        val chunk3 = state.richString.spans[3]
        val style3 = DefaultAttributeStyleResolver.resolve(chunk3.attributes)
        style3.spanStyle?.fontWeight shouldBe FontWeight.Bold
        style3.spanStyle?.fontFamily.shouldBeNull()
    }
}
