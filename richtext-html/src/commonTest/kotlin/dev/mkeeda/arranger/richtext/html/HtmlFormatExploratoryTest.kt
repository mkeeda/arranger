package dev.mkeeda.arranger.richtext.html

import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HtmlFormatExploratoryTest {
    // Vector 1: Complex Inline Combinations & CSS Styles

    @Test
    fun `importing 3-digit hex color`() {
        val html = "<p><span style=\"color: #f00;\">Red Text</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Red Text"
        val span = richString.spans.first()
        span.attributes[TextColorKey] shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `importing 8-digit hex color with alpha`() {
        val html = "<p><span style=\"color: #80ff0000;\">Semi-transparent Red</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Semi-transparent Red"
        val span = richString.spans.first()
        span.attributes[TextColorKey] shouldBe RgbaColor(0x80FF0000)
    }

    @Test
    fun `importing rgb and rgba colors`() {
        val html = "<p><span style=\"color: rgb(0, 255, 0); background-color: rgba(0, 0, 255, 0.5);\">Green on Blue</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Green on Blue"
        val span = richString.spans.first()
        span.attributes[TextColorKey] shouldBe RgbaColor(0xFF00FF00)
        span.attributes[BackgroundColorKey] shouldBe RgbaColor(0x7F0000FF)
    }

    @Test
    fun `importing font-size in px and pt units`() {
        val html = "<p><span style=\"font-size: 24px;\">24px Text</span> <span style=\"font-size: 12pt;\">12pt Text</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "24px Text 12pt Text"
        val firstSpan = richString.spans.first { 0 in it.range }
        firstSpan.attributes[FontSizeKey] shouldBe TextSize(24f)

        val secondSpan = richString.spans.first { 10 in it.range }
        secondSpan.attributes[FontSizeKey] shouldBe TextSize(12f)
    }

    @Test
    fun `exporting and importing all inline formats simultaneously`() {
        val text = "Full Styling"
        val color = RgbaColor(0xFFFF0000)
        val bgColor = RgbaColor(0xFF00FF00)
        val fontSize = TextSize(20f)
        val url = "https://example.com"

        val richString =
            RichString(text).edit {
                setSpanAttribute(LinkKey, url, 0 until text.length)
                setSpanAttribute(TextColorKey, color, 0 until text.length)
                setSpanAttribute(BackgroundColorKey, bgColor, 0 until text.length)
                setSpanAttribute(FontSizeKey, fontSize, 0 until text.length)
                setSpanAttribute(BoldKey, Unit, 0 until text.length)
                setSpanAttribute(ItalicKey, Unit, 0 until text.length)
                setSpanAttribute(UnderlineKey, Unit, 0 until text.length)
                setSpanAttribute(StrikethroughKey, Unit, 0 until text.length)
            }

        val html = richString.toHtml()
        val reimported = RichString.fromHtml(html)

        reimported.text shouldBe text
        val span = reimported.spans.first()
        span.attributes[LinkKey] shouldBe url
        span.attributes[TextColorKey] shouldBe color
        span.attributes[BackgroundColorKey] shouldBe bgColor
        span.attributes[FontSizeKey] shouldBe fontSize
        span.attributes.containsKey(BoldKey) shouldBe true
        span.attributes.containsKey(ItalicKey) shouldBe true
        span.attributes.containsKey(UnderlineKey) shouldBe true
        span.attributes.containsKey(StrikethroughKey) shouldBe true
    }

    // Vector 2: Deeply Nested Lists & Switching List Types

    @Test
    fun `importing 4 levels of nested mixed lists in HTML`() {
        val html = "<ul><li>Level 1<ol><li>Level 2<ul><li>Level 3<ol><li>Level 4</li></ol></li></ul></li></ol></li></ul>"
        val richString = RichString.fromHtml(html)

        val lines = richString.text.split('\n')
        lines shouldHaveSize 4

        val l1Idx = richString.text.indexOf("Level 1")
        val l2Idx = richString.text.indexOf("Level 2")
        val l3Idx = richString.text.indexOf("Level 3")
        val l4Idx = richString.text.indexOf("Level 4")

        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && l1Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level2 && l2Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level3 && l3Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level4 && l4Idx in it.range } shouldBe true
    }

    @Test
    fun `exporting and re-importing nested mixed lists in HTML`() {
        val text = "Item 1\nItem 1.1\nItem 1.1.1"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, text.rangeOf("Item 1"))
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level2, text.rangeOf("Item 1.1"))
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level3, text.rangeOf("Item 1.1.1"))
            }

        val html = richString.toHtml()
        html shouldBe "<ul><li>Item 1<ol><li>Item 1.1<ul><li>Item 1.1.1</li></ul></li></ol></li></ul>"

        val reimported = RichString.fromHtml(html)
        reimported.text shouldBe text
        val i1 = reimported.text.indexOf("Item 1")
        val i11 = reimported.text.indexOf("Item 1.1")
        val i111 = reimported.text.indexOf("Item 1.1.1")

        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && i1 in it.range } shouldBe true
        reimported.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level2 && i11 in it.range } shouldBe true
        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level3 && i111 in it.range } shouldBe true
    }

    @Test
    fun `exporting consecutive lists of different types at root level`() {
        val text = "Bullet Item\nOrdered Item"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, text.rangeOf("Bullet Item"))
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, text.rangeOf("Ordered Item"))
            }

        val html = richString.toHtml()
        val reimported = RichString.fromHtml(html)

        val bIdx = reimported.text.indexOf("Bullet Item")
        val oIdx = reimported.text.indexOf("Ordered Item")

        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && bIdx in it.range } shouldBe true
        reimported.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level1 && oIdx in it.range } shouldBe true
    }

    // Vector 3: Malformed & Unclosed HTML Handling

    @Test
    fun `importing unclosed strong tag parses gracefully without crash`() {
        val html = "<p><strong>Unclosed bold text"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Unclosed bold text"
        richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun `importing mismatched tags parses gracefully`() {
        val html = "<p><b><i>Mismatched</b></i></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Mismatched"
        richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
    }

    @Test
    fun `importing malformed CSS styles ignores invalid properties and retains valid ones`() {
        val html = "<p><span style=\";;color: not-a-color; background-color: #00ff00; font-size: invalid;\">Test</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Test"
        val span = richString.spans.first()
        span.attributes.containsKey(TextColorKey) shouldBe false
        span.attributes[BackgroundColorKey] shouldBe RgbaColor(0xFF00FF00)
        span.attributes.containsKey(FontSizeKey) shouldBe false
    }

    // Vector 4: Unknown Tags & Special Entities

    @Test
    fun `importing unknown HTML tags extracts text content safely`() {
        val html = "<div class=\"wrapper\"><section><article><custom-tag>Hello Custom</custom-tag></article></section></div>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Hello Custom"
    }

    @Test
    fun `importing HTML tables extracts text content`() {
        val html = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>"
        val richString = RichString.fromHtml(html)

        richString.text.contains("Cell 1") shouldBe true
        richString.text.contains("Cell 2") shouldBe true
    }

    @Test
    fun `importing HTML comments ignores comment text`() {
        val html = "<p>Visible <!-- Hidden Comment -->Text</p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Visible Text"
    }

    @Test
    fun `importing CJK and emoji characters preserves encoding`() {
        val text = "こんにちは 🌍 世界！"
        val richString =
            RichString(text).edit {
                setSpanAttribute(BoldKey, Unit, 0..4) // "こんにちは"
                setSpanAttribute(ItalicKey, Unit, 8..10) // "世界！"
            }

        val html = richString.toHtml()
        val reimported = RichString.fromHtml(html)

        reimported.text shouldBe text
        reimported.spans.any { it.attributes.containsKey(BoldKey) && 0 in it.range } shouldBe true
        reimported.spans.any { it.attributes.containsKey(ItalicKey) && 8 in it.range } shouldBe true
    }

    // Vector 5: Headings, Blockquotes, Alignments

    @Test
    fun `importing blockquote with multiple paragraphs preserves blockquote attribute across paragraphs`() {
        val html = "<blockquote><p>Quote 1</p><p>Quote 2</p></blockquote>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Quote 1\nQuote 2"
        val q1Idx = richString.text.indexOf("Quote 1")
        val q2Idx = richString.text.indexOf("Quote 2")

        richString.spans.any { it.attributes.containsKey(BlockquoteKey) && q1Idx in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(BlockquoteKey) && q2Idx in it.range } shouldBe true
    }

    @Test
    fun `importing heading inside blockquote preserves heading and blockquote`() {
        val html = "<blockquote><h1>Heading in Quote</h1></blockquote>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Heading in Quote"
        richString.spans.any { it.attributes.containsKey(BlockquoteKey) } shouldBe true
        richString.spans.any { it.attributes[HeadingKey] == HeadingLevel.H1 } shouldBe true
    }

    @Test
    fun `importing legacy align attribute on paragraph`() {
        val html = "<p align=\"justify\">Justified text</p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Justified text"
        richString.spans.first().attributes[TextAlignmentKey] shouldBe TextAlignment.Justify
    }

    @Test
    fun `exporting heading with text alignment`() {
        val text = "Centered Header"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(HeadingKey, HeadingLevel.H2, 0 until text.length)
                setParagraphAttribute(TextAlignmentKey, TextAlignment.Center, 0 until text.length)
            }

        val html = richString.toHtml()
        html shouldBe "<h2 style=\"text-align: center;\">Centered Header</h2>"

        val reimported = RichString.fromHtml(html)
        reimported.text shouldBe text
        reimported.spans.any { it.attributes[HeadingKey] == HeadingLevel.H2 } shouldBe true
        reimported.spans.any { it.attributes[TextAlignmentKey] == TextAlignment.Center } shouldBe true
    }

    // Vector 6: Stress Testing

    @Test
    fun `exporting and importing large 10000 characters HTML document with formatting`() {
        val chunk = "<p>Text with <strong>Bold</strong>, <em>Italic</em>, and <a href=\"https://example.com\">Link</a>.</p>"
        val html = chunk.repeat(100)

        val richString = RichString.fromHtml(html)
        richString.text.isNotEmpty() shouldBe true

        val exported = richString.toHtml()
        val reimported = RichString.fromHtml(exported)
        reimported.text shouldBe richString.text
        reimported.spans.size shouldBe richString.spans.size
    }
}
