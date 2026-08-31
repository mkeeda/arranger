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
import dev.mkeeda.arranger.richtext.attributeContainerOf
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
        richString.spans shouldBe emptyList()
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
        richString.spans.any { it.attributes.containsKey(BoldKey) && 0 in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(BoldKey) && 6 in it.range } shouldBe true
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
        richString.spans.any { it.attributes.containsKey(ItalicKey) && 0 in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(ItalicKey) && 6 in it.range } shouldBe true
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
        val oneIndex = richString.text.indexOf("One")
        val twoIndex = richString.text.indexOf("Two")
        val threeIndex = richString.text.indexOf("Three")
        richString.spans.any { it.attributes.containsKey(StrikethroughKey) && oneIndex in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(StrikethroughKey) && twoIndex in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(StrikethroughKey) && threeIndex in it.range } shouldBe true
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
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 6..10
        richString.spans[0].attributes shouldBe attributeContainerOf(UnderlineKey to Unit)
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
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 6..11
        richString.spans[0].attributes shouldBe attributeContainerOf(LinkKey to "https://google.com")
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
        val span = richString.spans.first()
        span.attributes[TextColorKey] shouldBe RgbaColor(0xFFFF0000)
        span.attributes[BackgroundColorKey] shouldBe RgbaColor(0xFFFFFF00)
        span.attributes[FontSizeKey] shouldBe TextSize(18f)
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

        val spans = richString.spans
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H1 && 0 in it.range } shouldBe true
        val h2Index = richString.text.indexOf("Heading 2")
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H2 && h2Index in it.range } shouldBe true
        val h3Index = richString.text.indexOf("Heading 3")
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H3 && h3Index in it.range } shouldBe true
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
        richString.spans shouldHaveSize 1
        richString.spans[0].attributes.containsKey(BlockquoteKey) shouldBe true
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

        val centerIndex = richString.text.indexOf("Centered")
        val rightIndex = richString.text.indexOf("Right")

        richString.spans.any { it.attributes[TextAlignmentKey] == TextAlignment.Center && centerIndex in it.range } shouldBe true
        richString.spans.any { it.attributes[TextAlignmentKey] == TextAlignment.Right && rightIndex in it.range } shouldBe true
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

        val item1Index = richString.text.indexOf("Item 1")
        val item2Index = richString.text.indexOf("Item 2")
        val item21Index = richString.text.indexOf("Item 2.1")

        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && item1Index in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && item2Index in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level2 && item21Index in it.range } shouldBe true
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

        val firstIndex = richString.text.indexOf("First")
        val secondIndex = richString.text.indexOf("Second")
        val nestedIndex = richString.text.indexOf("Nested")

        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level1 && firstIndex in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level1 && secondIndex in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level2 && nestedIndex in it.range } shouldBe true
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

        reimported.text shouldBe richString.text
        reimported.spans.size shouldBe richString.spans.size
    }

    @Test
    fun `importing empty html returns empty rich string`() {
        val richString = RichString.fromHtml("")
        richString.text shouldBe ""
        richString.spans shouldBe emptyList()
    }

    @Test
    fun `exporting empty rich string returns empty html`() {
        val richString = RichString("")
        richString.toHtml() shouldBe ""
    }
}
