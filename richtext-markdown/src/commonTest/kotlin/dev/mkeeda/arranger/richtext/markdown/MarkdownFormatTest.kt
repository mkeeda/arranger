package dev.mkeeda.arranger.richtext.markdown

import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MarkdownFormatTest {
    @Test
    fun `exporting plain text produces plain markdown`() {
        val richString = RichString("Hello World")
        val markdown = richString.toMarkdown()
        markdown shouldBe "Hello World"
    }

    @Test
    fun `importing plain markdown produces plain rich string`() {
        val markdown = "Hello World"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans shouldBe emptyList()
    }

    @Test
    fun `exporting bold text produces markdown asterisks`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(BoldKey, Unit, 0..4)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "**Hello** World"
    }

    @Test
    fun `importing bold markdown produces bold span`() {
        val markdown = "**Hello** World"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 0..4
        richString.spans[0].attributes shouldBe attributeContainerOf(BoldKey to Unit)
    }

    @Test
    fun `exporting italic text produces markdown asterisks`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(ItalicKey, Unit, 6..10)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "Hello *World*"
    }

    @Test
    fun `importing italic markdown produces italic span`() {
        val markdown = "Hello *World*"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 6..10
        richString.spans[0].attributes shouldBe attributeContainerOf(ItalicKey to Unit)
    }

    @Test
    fun `importing underscore italic markdown produces italic span`() {
        val markdown = "Hello _World_"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 6..10
        richString.spans[0].attributes shouldBe attributeContainerOf(ItalicKey to Unit)
    }

    @Test
    fun `exporting strikethrough text produces tildes`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(StrikethroughKey, Unit, 0..4)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "~~Hello~~ World"
    }

    @Test
    fun `importing strikethrough markdown produces strikethrough span`() {
        val markdown = "~~Hello~~ World"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 0..4
        richString.spans[0].attributes shouldBe attributeContainerOf(StrikethroughKey to Unit)
    }

    @Test
    fun `exporting underline text produces html u tag`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(UnderlineKey, Unit, 6..10)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "Hello <u>World</u>"
    }

    @Test
    fun `importing underline markdown produces underline span`() {
        val markdown = "Hello <u>World</u>"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 6..10
        richString.spans[0].attributes shouldBe attributeContainerOf(UnderlineKey to Unit)
    }

    @Test
    fun `exporting hyperlink produces markdown link`() {
        val richString =
            RichString("Visit Google").edit {
                setSpanAttribute(LinkKey, "https://google.com", 6..11)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "Visit [Google](https://google.com)"
    }

    @Test
    fun `importing markdown link produces hyperlink span`() {
        val markdown = "Visit [Google](https://google.com)"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Visit Google"
        richString.spans shouldHaveSize 1
        richString.spans[0].range shouldBe 6..11
        richString.spans[0].attributes shouldBe attributeContainerOf(LinkKey to "https://google.com")
    }

    @Test
    fun `exporting combined inline styles formats correctly`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(BoldKey, Unit, 0..10)
                setSpanAttribute(ItalicKey, Unit, 6..10)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "**Hello *World***"
    }

    @Test
    fun `importing combined inline styles produces multiple spans`() {
        val markdown = "**Hello *World***"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans.any { it.attributes.containsKey(BoldKey) && 0 in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(ItalicKey) && 6 in it.range } shouldBe true
    }

    @Test
    fun `exporting heading 1 to 6 produces corresponding hashes`() {
        val headings =
            listOf(
                HeadingLevel.H1 to "# Header 1",
                HeadingLevel.H2 to "## Header 2",
                HeadingLevel.H3 to "### Header 3",
                HeadingLevel.H4 to "#### Header 4",
                HeadingLevel.H5 to "##### Header 5",
                HeadingLevel.H6 to "###### Header 6",
            )

        for ((level, expectedMd) in headings) {
            val text = "Header ${level.ordinal + 1}"
            val richString =
                RichString(text).edit {
                    setParagraphAttribute(HeadingKey, level, 0 until text.length)
                }
            richString.toMarkdown() shouldBe expectedMd
        }
    }

    @Test
    fun `importing headings produces heading paragraph attributes`() {
        val markdown = "# Heading 1\n## Heading 2\n### Heading 3"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Heading 1\nHeading 2\nHeading 3"

        val spans = richString.spans
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H1 && 0 in it.range } shouldBe true
        val h2Index = richString.text.indexOf("Heading 2")
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H2 && h2Index in it.range } shouldBe true
        val h3Index = richString.text.indexOf("Heading 3")
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H3 && h3Index in it.range } shouldBe true
    }

    @Test
    fun `exporting blockquote produces greater than prefix`() {
        val richString =
            RichString("This is a quote").edit {
                setParagraphAttribute(BlockquoteKey, Unit, 0..14)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "> This is a quote"
    }

    @Test
    fun `importing blockquote produces blockquote attribute`() {
        val markdown = "> This is a quote"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "This is a quote"
        richString.spans shouldHaveSize 1
        richString.spans[0].attributes.containsKey(BlockquoteKey) shouldBe true
    }

    @Test
    fun `exporting bullet list with multiple levels produces correct indentation`() {
        val text = "Item 1\nItem 2\nItem 2.1"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..5)
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 7..12)
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level2, 14..21)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "* Item 1\n* Item 2\n  * Item 2.1"
    }

    @Test
    fun `importing bullet list with multiple levels produces bullet list attributes with levels`() {
        val markdown = "* Item 1\n* Item 2\n  * Item 2.1"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Item 1\nItem 2\nItem 2.1"

        val item1Index = richString.text.indexOf("Item 1")
        val item2Index = richString.text.indexOf("Item 2")
        val item21Index = richString.text.indexOf("Item 2.1")

        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && item1Index in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && item2Index in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level2 && item21Index in it.range } shouldBe true
    }

    @Test
    fun `exporting ordered list produces numbered prefixes`() {
        val text = "First\nSecond\nNested"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 0..4)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 6..11)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level2, 13..18)
            }
        val markdown = richString.toMarkdown()
        markdown shouldBe "1. First\n2. Second\n   1. Nested"
    }

    @Test
    fun `importing ordered list produces ordered list attributes with levels`() {
        val markdown = "1. First\n2. Second\n   1. Nested"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "First\nSecond\nNested"

        val firstIndex = richString.text.indexOf("First")
        val secondIndex = richString.text.indexOf("Second")
        val nestedIndex = richString.text.indexOf("Nested")

        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level1 && firstIndex in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level1 && secondIndex in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level2 && nestedIndex in it.range } shouldBe true
    }

    @Test
    fun `round tripping rich string with multiple inline and block styles`() {
        val markdown = "# Title\n\n> A quote with **bold** and *italic*\n\n* Item 1\n* Item 2 with [Link](https://example.com)"
        val richString = RichString.fromMarkdown(markdown)
        val exportedMarkdown = richString.toMarkdown()
        val reimported = RichString.fromMarkdown(exportedMarkdown)

        reimported.text shouldBe richString.text
        reimported.spans.size shouldBe richString.spans.size
    }

    @Test
    fun `importing empty markdown returns empty rich string`() {
        val richString = RichString.fromMarkdown("")
        richString.text shouldBe ""
        richString.spans shouldBe emptyList()
    }

    @Test
    fun `exporting empty rich string returns empty markdown`() {
        val richString = RichString("")
        richString.toMarkdown() shouldBe ""
    }
}
