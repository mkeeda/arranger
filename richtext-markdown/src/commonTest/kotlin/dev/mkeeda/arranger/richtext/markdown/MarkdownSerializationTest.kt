package dev.mkeeda.arranger.richtext.markdown

import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.codeBlock
import dev.mkeeda.arranger.richtext.export
import dev.mkeeda.arranger.richtext.import
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MarkdownSerializationTest {
    @Test
    fun `exporting RichString via MarkdownExporter directly produces expected markdown`() {
        val richString =
            RichString("Hello World").edit {
                setSpanAttribute(key = BoldKey, value = Unit, range = 0..4)
            }
        val markdown = richString.export(exporter = MarkdownExporter)
        markdown shouldBe "**Hello** World"
    }

    @Test
    fun `importing markdown via MarkdownImporter directly produces expected RichString`() {
        val markdown = "**Hello** World"
        val richString = RichString.import(input = markdown, importer = MarkdownImporter)
        richString.text shouldBe "Hello World"
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
        )
    }

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
        richString.spans.shouldBeEmpty()
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 6..10, attributes = attributeContainerOf(ItalicKey to Unit)),
        )
    }

    @Test
    fun `importing underscore italic markdown produces italic span`() {
        val markdown = "Hello _World_"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Hello World"
        richString.spans.shouldContainExactly(
            RichSpan(range = 6..10, attributes = attributeContainerOf(ItalicKey to Unit)),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..4, attributes = attributeContainerOf(StrikethroughKey to Unit)),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 6..10, attributes = attributeContainerOf(UnderlineKey to Unit)),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 6..11, attributes = attributeContainerOf(LinkKey to "https://google.com")),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 6..10, attributes = attributeContainerOf(ItalicKey to Unit)),
            RichSpan(range = 0..10, attributes = attributeContainerOf(BoldKey to Unit)),
        )
    }

    @Test
    fun `importing nested bold and italic inside link`() {
        val markdown = "[***Bold and Italic Link***](https://example.com)"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Bold and Italic Link"
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..19, attributes = attributeContainerOf(BoldKey to Unit)),
            RichSpan(range = 0..19, attributes = attributeContainerOf(ItalicKey to Unit)),
            RichSpan(range = 0..19, attributes = attributeContainerOf(LinkKey to "https://example.com")),
        )
    }

    @Test
    fun `exporting link with bold and italic produces properly nested delimiters`() {
        val text = "Bold Italic Link"
        val richString =
            RichString(text).edit {
                setSpanAttribute(LinkKey, "https://example.com", 0 until text.length)
                setSpanAttribute(BoldKey, Unit, 0 until text.length)
                setSpanAttribute(ItalicKey, Unit, 0 until text.length)
            }

        val exported = richString.toMarkdown()
        exported shouldBe "[***Bold Italic Link***](https://example.com)"

        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe text
        reimported.spans.shouldContainExactly(
            RichSpan(range = 0..15, attributes = attributeContainerOf(BoldKey to Unit)),
            RichSpan(range = 0..15, attributes = attributeContainerOf(ItalicKey to Unit)),
            RichSpan(range = 0..15, attributes = attributeContainerOf(LinkKey to "https://example.com")),
        )
    }

    @Test
    fun `exporting all five inline styles combined produces valid markdown`() {
        val text = "All Styles Combined"
        val richString =
            RichString(text).edit {
                setSpanAttribute(LinkKey, "https://example.com", 0 until text.length)
                setSpanAttribute(UnderlineKey, Unit, 0 until text.length)
                setSpanAttribute(StrikethroughKey, Unit, 0 until text.length)
                setSpanAttribute(BoldKey, Unit, 0 until text.length)
                setSpanAttribute(ItalicKey, Unit, 0 until text.length)
            }

        val exported = richString.toMarkdown()
        exported shouldBe "[<u>~~***All Styles Combined***~~</u>](https://example.com)"

        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe text
        reimported.spans.shouldContainExactly(
            RichSpan(range = 0..18, attributes = attributeContainerOf(BoldKey to Unit)),
            RichSpan(range = 0..18, attributes = attributeContainerOf(ItalicKey to Unit)),
            RichSpan(range = 0..18, attributes = attributeContainerOf(StrikethroughKey to Unit)),
            RichSpan(range = 0..18, attributes = attributeContainerOf(UnderlineKey to Unit)),
            RichSpan(range = 0..18, attributes = attributeContainerOf(LinkKey to "https://example.com")),
        )
    }

    @Test
    fun `exporting partially overlapping spans handles stack transitions cleanly`() {
        val text = "ABCDEF"
        val richString =
            RichString(text).edit {
                setSpanAttribute(BoldKey, Unit, 0..3) // ABCD
                setSpanAttribute(ItalicKey, Unit, 2..5) // CDEF
            }

        val exported = richString.toMarkdown()
        exported shouldBe "**AB*CD****EF*"
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..8, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)),
            RichSpan(range = 10..18, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H2)),
            RichSpan(range = 20..28, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H3)),
        )
    }

    @Test
    fun `importing setext headings for level 1 and level 2`() {
        val markdown =
            """
            Setext Heading 1
            ================

            Setext Heading 2
            ----------------
            """.trimIndent()

        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Setext Heading 1\n\nSetext Heading 2"
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..15, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1)),
            RichSpan(range = 18..33, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H2)),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..14, attributes = attributeContainerOf(BlockquoteKey to Unit)),
        )
    }

    @Test
    fun `importing blockquote with multiple paragraphs and inline formatting`() {
        val markdown =
            """
            > Quote paragraph 1 with **bold**.
            >
            > Quote paragraph 2 with *italic*.
            """.trimIndent()

        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Quote paragraph 1 with bold.\n>\n> Quote paragraph 2 with italic."
        richString.spans.shouldContainExactly(
            RichSpan(range = 23..26, attributes = attributeContainerOf(BoldKey to Unit)),
            RichSpan(range = 56..61, attributes = attributeContainerOf(ItalicKey to Unit)),
            RichSpan(range = 0..62, attributes = attributeContainerOf(BlockquoteKey to Unit)),
        )
    }

    @Test
    fun `exporting multiline blockquote prefixes each line with gt`() {
        val text = "Line 1\nLine 2\nLine 3"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BlockquoteKey, Unit, 0 until text.length)
            }

        val exported = richString.toMarkdown()
        exported shouldBe "> Line 1\n> Line 2\n> Line 3"
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..5, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
            RichSpan(range = 7..13, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
            RichSpan(range = 14..21, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level2)),
        )
    }

    @Test
    fun `importing deeply nested mixed lists across 4 levels`() {
        val markdown =
            """
            * Level 1 Bullet
              1. Level 2 Ordered
                 * Level 3 Bullet
                   1. Level 4 Ordered
            """.trimIndent()

        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Level 1 Bullet\nLevel 2 Ordered\nLevel 3 Bullet\nLevel 4 Ordered"
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..14, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
            RichSpan(range = 15..30, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)),
            RichSpan(range = 31..45, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level3)),
            RichSpan(range = 46..60, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level4)),
        )
    }

    @Test
    fun `exporting and re-importing deeply nested pure bullet lists preserves 4 levels`() {
        val text = "Level 1\nLevel 2\nLevel 3\nLevel 4"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, text.rangeOf("Level 1"))
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level2, text.rangeOf("Level 2"))
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level3, text.rangeOf("Level 3"))
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level4, text.rangeOf("Level 4"))
            }

        val exported = richString.toMarkdown()
        exported shouldBe "* Level 1\n  * Level 2\n    * Level 3\n      * Level 4"

        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe text
        reimported.spans.shouldContainExactly(
            RichSpan(range = 0..7, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
            RichSpan(range = 8..15, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level2)),
            RichSpan(range = 16..23, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level3)),
            RichSpan(range = 24..30, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level4)),
        )
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
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..4, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
            RichSpan(range = 6..12, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
            RichSpan(range = 13..18, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)),
        )
    }

    @Test
    fun `exporting and re-importing deeply nested pure ordered lists preserves 4 levels`() {
        val text = "Level 1\nLevel 2\nLevel 3\nLevel 4"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, text.rangeOf("Level 1"))
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level2, text.rangeOf("Level 2"))
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level3, text.rangeOf("Level 3"))
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level4, text.rangeOf("Level 4"))
            }

        val exported = richString.toMarkdown()
        exported shouldBe "1. Level 1\n   1. Level 2\n      1. Level 3\n         1. Level 4"

        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe text
        reimported.spans.shouldContainExactly(
            RichSpan(range = 0..7, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
            RichSpan(range = 8..15, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level2)),
            RichSpan(range = 16..23, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level3)),
            RichSpan(range = 24..30, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level4)),
        )
    }

    @Test
    fun `importing list items containing inline formatting`() {
        val markdown =
            """
            * **Bold Item** with [Link](https://example.com)
            * *Italic Item* with <u>Underline</u> and ~~Strike~~
            """.trimIndent()

        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "Bold Item with Link\nItalic Item with Underline and Strike"
        richString.spans.shouldContainExactly(
            RichSpan(range = 0..8, attributes = attributeContainerOf(BoldKey to Unit)),
            RichSpan(range = 15..18, attributes = attributeContainerOf(LinkKey to "https://example.com")),
            RichSpan(range = 0..18, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
            RichSpan(range = 20..30, attributes = attributeContainerOf(ItalicKey to Unit)),
            RichSpan(range = 37..45, attributes = attributeContainerOf(UnderlineKey to Unit)),
            RichSpan(range = 51..56, attributes = attributeContainerOf(StrikethroughKey to Unit)),
            RichSpan(range = 20..56, attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1)),
        )
    }

    @Test
    fun `importing text with math equations containing asterisks`() {
        val markdown = "Calculation: 2 * 3 = 6 and 4 * 5 = 20."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Calculation: 2 * 3 = 6 and 4 * 5 = 20."
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing text with snake case identifiers containing underscores`() {
        val markdown = "The variable_name_here should not be italicized."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "The variable_name_here should not be italicized."
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing text with hashtags mid-sentence`() {
        val markdown = "I love programming in C# and #kotlin is great."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "I love programming in C# and #kotlin is great."
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing text with comparison operators`() {
        val markdown = "Condition: x > 5 and y < 10."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Condition: x > 5 and y < 10."
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing text with brackets not forming links`() {
        val markdown = "Access element array[0] or [index]."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Access element array[0] or [index]."
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `round tripping rich string with multiple inline and block styles`() {
        val markdown = "# Title\n\n> A quote with **bold** and *italic*\n\n* Item 1\n* Item 2 with [Link](https://example.com)"
        val richString = RichString.fromMarkdown(markdown)
        val exportedMarkdown = richString.toMarkdown()
        val reimported = RichString.fromMarkdown(exportedMarkdown)

        reimported.text shouldBe richString.text
        reimported.spans shouldContainExactly richString.spans
    }

    @Test
    fun `exporting and importing 10000 characters long paragraph with formatting`() {
        val chunk = "Hello **World** with [Link](https://example.com) and *Italic*. "
        val markdown = chunk.repeat(150) // ~9,600 chars

        val richString = RichString.fromMarkdown(markdown)
        richString.text.length shouldBe ("Hello World with Link and Italic. ".length * 150)
        richString.spans shouldHaveSize (3 * 150) // Bold, Link, Italic per chunk

        val exported = richString.toMarkdown()
        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe richString.text
        reimported.spans shouldContainExactly richString.spans
    }

    @Test
    fun `importing multiple consecutive empty lines and whitespace only lines`() {
        val markdown = "\n\n\n   \n\nHello\n\n\n"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "\n\n\n   \n\nHello\n\n\n"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `exporting rich string with consecutive empty lines preserves line count`() {
        val text = "First\n\n\nSecond"
        val richString = RichString(text)
        val exported = richString.toMarkdown()

        exported shouldBe "First\n\n\nSecond"
    }

    @Test
    fun `single character document export and import`() {
        val richString =
            RichString("X").edit {
                setSpanAttribute(BoldKey, Unit, 0..0)
            }
        val exported = richString.toMarkdown()
        exported shouldBe "**X**"

        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe "X"
        reimported.spans.shouldContainExactly(
            RichSpan(range = 0..0, attributes = attributeContainerOf(BoldKey to Unit)),
        )
    }

    @Test
    fun `importing unclosed bold tag does not crash and treats as plain text`() {
        val markdown = "**unclosed bold text"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "**unclosed bold text"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing unclosed italic tag does not crash`() {
        val markdown = "*unclosed italic text"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "*unclosed italic text"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing unclosed link tag does not crash`() {
        val markdown = "[unclosed link destination(https://example.com"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "[unclosed link destination(https://example.com"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing unclosed underline html tag gracefully extracts content`() {
        val markdown = "Some <u>unclosed underline text"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Some unclosed underline text"
        richString.spans.shouldContainExactly(
            RichSpan(range = 5..27, attributes = attributeContainerOf(UnderlineKey to Unit)),
        )
    }

    @Test
    fun `importing empty heading or empty blockquote does not crash`() {
        val markdown = "# \n> \n* "
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "\n\n"
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing empty markdown returns empty rich string`() {
        val richString = RichString.fromMarkdown("")
        richString.text shouldBe ""
        richString.spans.shouldBeEmpty()
    }

    @Test
    fun `importing code block with language produces CodeBlockKey span with language`() {
        val markdown = "```kotlin\nval x = 1\nval y = 2\n```"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "val x = 1\nval y = 2"
        richString.spans.shouldContainExactly(
            RichSpan(
                range = 0..18,
                attributes = attributeContainerOf(CodeBlockKey to "kotlin"),
            ),
        )
    }

    @Test
    fun `importing code block without language produces CodeBlockKey span with null language`() {
        val markdown = "```\necho hi\n```"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "echo hi"
        richString.spans.shouldContainExactly(
            RichSpan(
                range = 0..6,
                attributes = attributeContainerOf(CodeBlockKey to null),
            ),
        )
    }

    @Test
    fun `exporting code block with language produces fenced markdown with language`() {
        val text = "val x = 1\nval y = 2"
        val richString =
            RichString(text).edit {
                editAttributes(text.indices) {
                    codeBlock("kotlin")
                }
            }

        val markdown = richString.toMarkdown()
        markdown shouldBe "```kotlin\nval x = 1\nval y = 2\n```"
    }

    @Test
    fun `exporting code block without language produces fenced markdown without language`() {
        val text = "echo hi"
        val richString =
            RichString(text).edit {
                editAttributes(text.indices) {
                    codeBlock()
                }
            }

        val markdown = richString.toMarkdown()
        markdown shouldBe "```\necho hi\n```"
    }

    @Test
    fun `round tripping code block preserves code block and language`() {
        val input = "```kotlin\nprintln(42)\n```"
        val richString = RichString.fromMarkdown(input)
        val exported = richString.toMarkdown()

        exported shouldBe input
    }

    @Test
    fun `importing indented code block produces CodeBlockKey span without language`() {
        val markdown = "    val a = 1\n    val b = 2"
        val richString = RichString.fromMarkdown(markdown)
        richString.text shouldBe "val a = 1\nval b = 2"
        richString.spans.shouldContainExactly(
            RichSpan(
                range = 0..18,
                attributes = attributeContainerOf(CodeBlockKey to null),
            ),
        )
    }

    @Test
    fun `code block preserves special markdown characters in import and export`() {
        val input = "```markdown\n# Heading\n* bullet\n**bold**\n```"
        val richString = RichString.fromMarkdown(input)
        richString.text shouldBe "# Heading\n* bullet\n**bold**"
        val exported = richString.toMarkdown()
        exported shouldBe input
    }

    @Test
    fun `exporting contiguous code blocks with different languages separates code fences`() {
        val text = "println(\"kt\")\nprint(\"py\")"
        val richString =
            RichString(text)
                .edit { editAttributes(text.rangeOf("println(\"kt\")")) { codeBlock("kotlin") } }
                .edit { editAttributes(text.rangeOf("print(\"py\")")) { codeBlock("python") } }

        val markdown = richString.toMarkdown()
        markdown shouldBe "```kotlin\nprintln(\"kt\")\n```\n```python\nprint(\"py\")\n```"
    }

    @Test
    fun `exporting code block with blank line inside preserves single code block`() {
        val input = "```kotlin\nval x = 1\n\nval y = 2\n```"
        val richString = RichString.fromMarkdown(input)
        val exported = richString.toMarkdown()
        exported shouldBe input
    }

    @Test
    fun `exporting code block with multiple blank lines preserves single code block`() {
        val input = "```kotlin\nval x = 1\n\n\nval y = 2\n```"
        val richString = RichString.fromMarkdown(input)
        val exported = richString.toMarkdown()
        exported shouldBe input
    }

    @Test
    fun `exporting blockquote with blank line inside preserves blockquote prefix`() {
        val text = "Line 1\n\nLine 2"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BlockquoteKey, Unit, 0 until text.length)
            }
        val exported = richString.toMarkdown()
        exported shouldBe "> Line 1\n> \n> Line 2"
    }

    @Test
    fun `MarkdownExporter is stateless and resets list numbering across consecutive invocations`() {
        val orderedList1 =
            RichString("First 1\nFirst 2").edit {
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 0..6)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 8..14)
            }
        val plainString = RichString("Plain line")
        val orderedList2 =
            RichString("Second 1\nSecond 2").edit {
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 0..7)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 9..16)
            }

        val md1 = MarkdownExporter.export(orderedList1)
        val mdPlain = MarkdownExporter.export(plainString)
        val md2 = MarkdownExporter.export(orderedList2)

        md1 shouldBe "1. First 1\n2. First 2"
        mdPlain shouldBe "Plain line"
        md2 shouldBe "1. Second 1\n2. Second 2"
    }

    @Test
    fun `MarkdownImporter is stateless and parses varying documents deterministically`() {
        val listMd = "* Bullet A\n* Bullet B"
        val quoteMd = "> Quote text"
        val styledMd = "**Bold** and *Italic*"

        val doc1 = MarkdownImporter.import(listMd)
        val doc2 = MarkdownImporter.import(quoteMd)
        val doc3 = MarkdownImporter.import(styledMd)
        val doc1Repeat = MarkdownImporter.import(listMd)

        doc1.text shouldBe "Bullet A\nBullet B"
        doc1.spans shouldHaveSize 2
        doc2.text shouldBe "Quote text"
        doc2.spans shouldHaveSize 1
        doc3.text shouldBe "Bold and Italic"
        doc3.spans shouldHaveSize 2
        doc1Repeat.text shouldBe doc1.text
        doc1Repeat.spans shouldContainExactly doc1.spans
    }

    @Test
    fun `direct MarkdownExporter export matches extension toMarkdown for complex document`() {
        val richString =
            RichString("Heading\nQuote line\nItem").edit {
                setParagraphAttribute(HeadingKey, HeadingLevel.H1, 0..6)
                setParagraphAttribute(BlockquoteKey, Unit, 8..17)
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 19..22)
            }

        val directExport = richString.export(MarkdownExporter)
        val extensionExport = richString.toMarkdown()

        directExport shouldBe extensionExport
        directExport shouldBe "# Heading\n> Quote line\n* Item"
    }

    @Test
    fun `direct MarkdownImporter import matches extension fromMarkdown for complex document`() {
        val markdown = "# Heading\n> Quote line\n* Item"

        val directImport = RichString.import(markdown, MarkdownImporter)
        val extensionImport = RichString.fromMarkdown(markdown)

        directImport.text shouldBe extensionImport.text
        directImport.spans shouldContainExactly extensionImport.spans
    }

    @Test
    fun `exporting transition from heading to ordered list then blockquote formats cleanly`() {
        val text = "Section Title\nStep 1\nStep 2\nImportant note"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(HeadingKey, HeadingLevel.H3, 0..12)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 14..19)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 21..26)
                setParagraphAttribute(BlockquoteKey, Unit, 28..41)
            }

        val markdown = richString.toMarkdown()
        markdown shouldBe "### Section Title\n1. Step 1\n2. Step 2\n> Important note"

        val reimported = RichString.fromMarkdown(markdown)
        reimported.text shouldBe text
        reimported.spans.shouldContainExactly(
            RichSpan(range = 0..12, attributes = attributeContainerOf(HeadingKey to HeadingLevel.H3)),
            RichSpan(range = 14..19, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
            RichSpan(range = 21..26, attributes = attributeContainerOf(OrderedListKey to ListIndentLevel.Level1)),
            RichSpan(range = 28..41, attributes = attributeContainerOf(BlockquoteKey to Unit)),
        )
    }
}
