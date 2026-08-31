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
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HtmlFormatTest {
    @Test
    fun `exporting plain text produces plain html paragraph`() {
        val richString = RichString("Hello World")
        val html = richString.toHtml()
        html shouldBe "<p>Hello World</p>"
    }

    @Test
    fun `importing plain html produces plain rich string`() {
        val html = "<p>Hello World</p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Hello World"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `exporting bold text produces strong tag`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(BoldKey, Unit, 0..4)
            }
        val html = richString.toHtml()
        html shouldBe "<p><strong>Hello</strong> World</p>"
    }

    @Test
    fun `importing strong and b tags produces bold span`() {
        val html = "<p><strong>Hello</strong> <b>World</b></p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Hello World"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
                RichSpan(range = 6..10, attributes = attributeContainerOf(BoldKey to Unit)),
            )
    }

    @Test
    fun `exporting italic text produces em tag`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(ItalicKey, Unit, 6..10)
            }
        val html = richString.toHtml()
        html shouldBe "<p>Hello <em>World</em></p>"
    }

    @Test
    fun `importing em and i tags produces italic span`() {
        val html = "<p><em>Hello</em> <i>World</i></p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Hello World"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..4, attributes = attributeContainerOf(ItalicKey to Unit)),
                RichSpan(range = 6..10, attributes = attributeContainerOf(ItalicKey to Unit)),
            )
    }

    @Test
    fun `exporting strikethrough text produces s tag`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(StrikethroughKey, Unit, 0..4)
            }
        val html = richString.toHtml()
        html shouldBe "<p><s>Hello</s> World</p>"
    }

    @Test
    fun `importing s del strike tags produces strikethrough span`() {
        val html = "<p><s>One</s> <del>Two</del> <strike>Three</strike></p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "One Two Three"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..2, attributes = attributeContainerOf(StrikethroughKey to Unit)),
                RichSpan(range = 4..6, attributes = attributeContainerOf(StrikethroughKey to Unit)),
                RichSpan(range = 8..12, attributes = attributeContainerOf(StrikethroughKey to Unit)),
            )
    }

    @Test
    fun `exporting underline text produces u tag`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(UnderlineKey, Unit, 6..10)
            }
        val html = richString.toHtml()
        html shouldBe "<p>Hello <u>World</u></p>"
    }

    @Test
    fun `importing u tag produces underline span`() {
        val html = "<p>Hello <u>World</u></p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Hello World"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 6..10, attributes = attributeContainerOf(UnderlineKey to Unit)),
            )
    }

    @Test
    fun `exporting hyperlink produces a tag`() {
        val richString =
            RichString("Visit Google").edit {
                setSpanAttribute(LinkKey, "https://google.com", 6..11)
            }
        val html = richString.toHtml()
        html shouldBe "<p>Visit <a href=\"https://google.com\">Google</a></p>"
    }

    @Test
    fun `importing a tag produces hyperlink span`() {
        val html = "<p>Visit <a href=\"https://google.com\">Google</a></p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Visit Google"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 6..11, attributes = attributeContainerOf(LinkKey to "https://google.com")),
            )
    }

    @Test
    fun `exporting text color and background color produces span with styles`() {
        val redColor = RgbaColor(0xFFFF0000)
        val yellowColor = RgbaColor(0xFFFFFF00)
        val richString =
            RichString("Colored Text").edit {
                setSpanAttribute(TextColorKey, redColor, 0..6)
                setSpanAttribute(BackgroundColorKey, yellowColor, 8..11)
            }
        val html = richString.toHtml()
        html shouldBe "<p><span style=\"color: #ff0000;\">Colored</span> <span style=\"background-color: #ffff00;\">Text</span></p>"
    }

    @Test
    fun `importing style color and background-color and font-size produces corresponding spans`() {
        val html = "<p><span style=\"color: #ff0000; background-color: #ffff00; font-size: 18.0sp;\">Styled</span></p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Styled"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..5,
                    attributes =
                        attributeContainerOf(
                            TextColorKey to RgbaColor(0xFFFF0000),
                            BackgroundColorKey to RgbaColor(0xFFFFFF00),
                            FontSizeKey to TextSize(18f),
                        ),
                ),
            )
    }

    @Test
    fun `importing 3-digit hex color`() {
        val html = "<p><span style=\"color: #f00;\">Red Text</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Red Text"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..7,
                    attributes = attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000)),
                ),
            )
    }

    @Test
    fun `importing 8-digit hex color with alpha`() {
        val html = "<p><span style=\"color: #80ff0000;\">Semi-transparent Red</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Semi-transparent Red"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..19,
                    attributes = attributeContainerOf(TextColorKey to RgbaColor(0x80FF0000)),
                ),
            )
    }

    @Test
    fun `importing rgb and rgba colors`() {
        val html = "<p><span style=\"color: rgb(0, 255, 0); background-color: rgba(0, 0, 255, 0.5);\">Green on Blue</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Green on Blue"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..12,
                    attributes =
                        attributeContainerOf(
                            TextColorKey to RgbaColor(0xFF00FF00),
                            BackgroundColorKey to RgbaColor(0x7F0000FF),
                        ),
                ),
            )
    }

    @Test
    fun `importing font-size in px and pt units`() {
        val html = "<p><span style=\"font-size: 24px;\">24px Text</span> <span style=\"font-size: 12pt;\">12pt Text</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "24px Text 12pt Text"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..8,
                    attributes = attributeContainerOf(FontSizeKey to TextSize(24f)),
                ),
                RichSpan(
                    range = 10..18,
                    attributes = attributeContainerOf(FontSizeKey to TextSize(12f)),
                ),
            )
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
        reimported.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..11,
                    attributes =
                        attributeContainerOf(
                            LinkKey to url,
                            TextColorKey to color,
                            BackgroundColorKey to bgColor,
                            FontSizeKey to fontSize,
                            BoldKey to Unit,
                            ItalicKey to Unit,
                            UnderlineKey to Unit,
                            StrikethroughKey to Unit,
                        ),
                ),
            )
    }

    @Test
    fun `exporting headings produces h1 to h6 tags`() {
        val headings =
            listOf(
                HeadingLevel.H1 to "<h1>Header 1</h1>",
                HeadingLevel.H2 to "<h2>Header 2</h2>",
                HeadingLevel.H3 to "<h3>Header 3</h3>",
                HeadingLevel.H4 to "<h4>Header 4</h4>",
                HeadingLevel.H5 to "<h5>Header 5</h5>",
                HeadingLevel.H6 to "<h6>Header 6</h6>",
            )

        for ((level, expectedHtml) in headings) {
            val text = "Header ${level.ordinal + 1}"
            val richString =
                RichString(text).edit {
                    setParagraphAttribute(HeadingKey, level, 0 until text.length)
                }
            richString.toHtml() shouldBe expectedHtml
        }
    }

    @Test
    fun `importing headings produces heading paragraph attributes`() {
        val html = "<h1>Heading 1</h1><h2>Heading 2</h2><h3>Heading 3</h3>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Heading 1\nHeading 2\nHeading 3"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..8, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)),
                RichSpan(range = 10..18, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H2)),
                RichSpan(range = 20..28, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H3)),
            )
    }

    @Test
    fun `exporting blockquote produces blockquote tag`() {
        val richString =
            RichString("This is a quote").edit {
                setParagraphAttribute(BlockquoteKey, Unit, 0..14)
            }
        val html = richString.toHtml()
        html shouldBe "<blockquote><p>This is a quote</p></blockquote>"
    }

    @Test
    fun `importing blockquote produces blockquote attribute`() {
        val html = "<blockquote><p>This is a quote</p></blockquote>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "This is a quote"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..14, attributes = attributeContainerOf(BlockquoteKey to Unit)),
            )
    }

    @Test
    fun `importing blockquote with multiple paragraphs preserves blockquote attribute across paragraphs`() {
        val html = "<blockquote><p>Quote 1</p><p>Quote 2</p></blockquote>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Quote 1\nQuote 2"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..14, attributes = attributeContainerOf(BlockquoteKey to Unit)),
            )
    }

    @Test
    fun `importing heading inside blockquote preserves heading and blockquote`() {
        val html = "<blockquote><h1>Heading in Quote</h1></blockquote>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Heading in Quote"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..15, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)),
                RichSpan(range = 0..15, attributes = attributeContainerOf(BlockquoteKey to Unit)),
            )
    }

    @Test
    fun `exporting text alignment produces style on paragraph`() {
        val richString =
            RichString("Centered").edit {
                setParagraphAttribute(TextAlignmentKey, TextAlignment.Center, 0..7)
            }
        val html = richString.toHtml()
        html shouldBe "<p style=\"text-align: center;\">Centered</p>"
    }

    @Test
    fun `importing text alignment produces text alignment attribute`() {
        val html = "<p style=\"text-align: center;\">Centered</p><p style=\"text-align: right;\">Right</p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Centered\nRight"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..7, attributes = attributeContainerOf(TextAlignmentKey to TextAlignment.Center)),
                RichSpan(range = 9..13, attributes = attributeContainerOf(TextAlignmentKey to TextAlignment.Right)),
            )
    }

    @Test
    fun `importing legacy align attribute on paragraph`() {
        val html = "<p align=\"justify\">Justified text</p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Justified text"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..13, attributes = attributeContainerOf(TextAlignmentKey to TextAlignment.Justify)),
            )
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
        reimported.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..14,
                    attributes =
                        attributeContainerOf(
                            HeadingKey to HeadingLevel.H2,
                            TextAlignmentKey to TextAlignment.Center,
                        ),
                ),
            )
    }

    @Test
    fun `exporting bullet list produces nested ul and li tags`() {
        val text = "Item 1\nItem 2\nItem 2.1"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..5)
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 7..12)
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level2, 14..21)
            }
        val html = richString.toHtml()
        html shouldBe "<ul><li>Item 1</li><li>Item 2<ul><li>Item 2.1</li></ul></li></ul>"
    }

    @Test
    fun `importing bullet list produces bullet list attributes with levels`() {
        val html = "<ul><li>Item 1</li><li>Item 2<ul><li>Item 2.1</li></ul></li></ul>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Item 1\nItem 2\nItem 2.1"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..5, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
                RichSpan(range = 7..12, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
                RichSpan(range = 14..21, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level2)),
            )
    }

    @Test
    fun `importing 4 levels of nested mixed lists in HTML`() {
        val html = "<ul><li>Level 1<ol><li>Level 2<ul><li>Level 3<ol><li>Level 4</li></ol></li></ul></li></ol></li></ul>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Level 1\nLevel 2\nLevel 3\nLevel 4"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..6, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
                RichSpan(range = 8..14, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)),
                RichSpan(range = 16..22, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level3)),
                RichSpan(range = 24..30, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level4)),
            )
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
        reimported.spans shouldBe
            listOf(
                RichSpan(range = 0..5, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
                RichSpan(range = 7..14, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)),
                RichSpan(range = 16..25, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level3)),
            )
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

        reimported.text shouldBe text
        reimported.spans shouldBe
            listOf(
                RichSpan(range = 0..10, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
                RichSpan(range = 12..23, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
            )
    }

    @Test
    fun `exporting ordered list produces nested ol and li tags`() {
        val text = "First\nSecond\nNested"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 0..4)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 6..11)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level2, 13..18)
            }
        val html = richString.toHtml()
        html shouldBe "<ol><li>First</li><li>Second<ol><li>Nested</li></ol></li></ol>"
    }

    @Test
    fun `importing ordered list produces ordered list attributes with levels`() {
        val html = "<ol><li>First</li><li>Second<ol><li>Nested</li></ol></li></ol>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "First\nSecond\nNested"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..4, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
                RichSpan(range = 6..11, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
                RichSpan(range = 13..18, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)),
            )
    }

    @Test
    fun `exporting special characters encodes html entities`() {
        val richString = RichString("Tom & Jerry <cartoon> \"classic\"")
        val html = richString.toHtml()
        html shouldBe "<p>Tom &amp; Jerry &lt;cartoon&gt; &quot;classic&quot;</p>"
    }

    @Test
    fun `importing html entities decodes characters properly`() {
        val html = "<p>Tom &amp; Jerry &lt;cartoon&gt; &quot;classic&quot; &#39;quote&#39;</p>"
        val richString = RichString.fromHtml(html)
        richString.text shouldBe "Tom & Jerry <cartoon> \"classic\" 'quote'"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing unclosed strong tag parses gracefully without crash`() {
        val html = "<p><strong>Unclosed bold text"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Unclosed bold text"
        richString.spans shouldBe
            listOf(
                RichSpan(range = 0..17, attributes = attributeContainerOf(BoldKey to Unit)),
            )
    }

    @Test
    fun `importing mismatched tags parses gracefully`() {
        val html = "<p><b><i>Mismatched</b></i></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Mismatched"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..9,
                    attributes =
                        attributeContainerOf(
                            BoldKey to Unit,
                            ItalicKey to Unit,
                        ),
                ),
            )
    }

    @Test
    fun `importing malformed CSS styles ignores invalid properties and retains valid ones`() {
        val html = "<p><span style=\";;color: not-a-color; background-color: #00ff00; font-size: invalid;\">Test</span></p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Test"
        richString.spans shouldBe
            listOf(
                RichSpan(
                    range = 0..3,
                    attributes = attributeContainerOf(BackgroundColorKey to RgbaColor(0xFF00FF00)),
                ),
            )
    }

    @Test
    fun `importing unknown HTML tags extracts text content safely`() {
        val html = "<div class=\"wrapper\"><section><article><custom-tag>Hello Custom</custom-tag></article></section></div>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Hello Custom"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing HTML tables extracts text content`() {
        val html = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Cell 1Cell 2"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing HTML comments ignores comment text`() {
        val html = "<p>Visible <!-- Hidden Comment -->Text</p>"
        val richString = RichString.fromHtml(html)

        richString.text shouldBe "Visible Text"
        richString.spans.shouldBeEmpty()
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
        reimported.spans shouldBe
            listOf(
                RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
                RichSpan(range = 8..10, attributes = attributeContainerOf(ItalicKey to Unit)),
            )
    }

    @Test
    fun `exporting and importing large 10000 characters HTML document with formatting`() {
        val chunk = "<p>Text with <strong>Bold</strong>, <em>Italic</em>, and <a href=\"https://example.com\">Link</a>.</p>"
        val html = chunk.repeat(100)

        val richString = RichString.fromHtml(html)
        richString.text.isNotEmpty() shouldBe true
        richString.spans shouldHaveSize 300

        val exported = richString.toHtml()
        val reimported = RichString.fromHtml(exported)
        reimported.text shouldBe richString.text
        reimported.spans shouldBe richString.spans
    }

    @Test
    fun `round tripping rich string with html format`() {
        val richString =
            RichString("Hello World\nSecond Paragraph").edit {
                setSpanAttribute(BoldKey, Unit, 0..4)
                setParagraphAttribute(HeadingKey, HeadingLevel.H1, 0..10)
                setSpanAttribute(ItalicKey, Unit, 12..17)
            }
        val html = richString.toHtml()
        val reimported = RichString.fromHtml(html)

        reimported.text shouldBe "Hello World\nSecond Paragraph"
        reimported.spans shouldBe
            listOf(
                RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
                RichSpan(range = 0..10, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)),
                RichSpan(range = 12..17, attributes = attributeContainerOf(ItalicKey to Unit)),
            )
    }

    @Test
    fun `importing empty html returns empty rich string`() {
        val richString = RichString.fromHtml("")
        richString.text shouldBe ""
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `exporting empty rich string returns empty html`() {
        val richString = RichString("")
        richString.toHtml() shouldBe ""
    }
}
