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
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.collections.shouldBeEmpty
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
    fun `importing nested bold and italic inside link`() {
        val markdown = "[***Bold and Italic Link***](https://example.com)"
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Bold and Italic Link"
        richString.spans.any { it.attributes[LinkKey] == "https://example.com" } shouldBe true
        richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
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
        reimported.spans.any { it.attributes[LinkKey] == "https://example.com" } shouldBe true
        reimported.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        reimported.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
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
        reimported.spans.any { it.attributes[LinkKey] == "https://example.com" } shouldBe true
        reimported.spans.any { it.attributes.containsKey(UnderlineKey) } shouldBe true
        reimported.spans.any { it.attributes.containsKey(StrikethroughKey) } shouldBe true
        reimported.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        reimported.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
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

        val spans = richString.spans
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H1 && 0 in it.range } shouldBe true
        val h2Index = richString.text.indexOf("Heading 2")
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H2 && h2Index in it.range } shouldBe true
        val h3Index = richString.text.indexOf("Heading 3")
        spans.any { it.attributes[HeadingKey] == HeadingLevel.H3 && h3Index in it.range } shouldBe true
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
        val h1Idx = richString.text.indexOf("Setext Heading 1")
        val h2Idx = richString.text.indexOf("Setext Heading 2")

        richString.spans.any { it.attributes[HeadingKey] == HeadingLevel.H1 && h1Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[HeadingKey] == HeadingLevel.H2 && h2Idx in it.range } shouldBe true
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
    fun `importing blockquote with multiple paragraphs and inline formatting`() {
        val markdown =
            """
            > Quote paragraph 1 with **bold**.
            >
            > Quote paragraph 2 with *italic*.
            """.trimIndent()

        val richString = RichString.fromMarkdown(markdown)
        val p1Idx = richString.text.indexOf("Quote paragraph 1")
        val p2Idx = richString.text.indexOf("Quote paragraph 2")

        richString.spans.any { it.attributes.containsKey(BlockquoteKey) && p1Idx in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(BlockquoteKey) && p2Idx in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
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

        val item1Index = richString.text.indexOf("Item 1")
        val item2Index = richString.text.indexOf("Item 2")
        val item21Index = richString.text.indexOf("Item 2.1")

        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && item1Index in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && item2Index in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level2 && item21Index in it.range } shouldBe true
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
        val lines = richString.text.split('\n')
        lines shouldHaveSize 4

        val l1Idx = richString.text.indexOf("Level 1 Bullet")
        val l2Idx = richString.text.indexOf("Level 2 Ordered")
        val l3Idx = richString.text.indexOf("Level 3 Bullet")
        val l4Idx = richString.text.indexOf("Level 4 Ordered")

        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && l1Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level2 && l2Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level3 && l3Idx in it.range } shouldBe true
        richString.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level4 && l4Idx in it.range } shouldBe true
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
        val l1Idx = reimported.text.indexOf("Level 1")
        val l2Idx = reimported.text.indexOf("Level 2")
        val l3Idx = reimported.text.indexOf("Level 3")
        val l4Idx = reimported.text.indexOf("Level 4")

        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level1 && l1Idx in it.range } shouldBe true
        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level2 && l2Idx in it.range } shouldBe true
        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level3 && l3Idx in it.range } shouldBe true
        reimported.spans.any { it.attributes[BulletListKey] == ListIndentLevel.Level4 && l4Idx in it.range } shouldBe true
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
        val l1Idx = reimported.text.indexOf("Level 1")
        val l2Idx = reimported.text.indexOf("Level 2")
        val l3Idx = reimported.text.indexOf("Level 3")
        val l4Idx = reimported.text.indexOf("Level 4")

        reimported.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level1 && l1Idx in it.range } shouldBe true
        reimported.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level2 && l2Idx in it.range } shouldBe true
        reimported.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level3 && l3Idx in it.range } shouldBe true
        reimported.spans.any { it.attributes[OrderedListKey] == ListIndentLevel.Level4 && l4Idx in it.range } shouldBe true
    }

    @Test
    fun `importing list items containing inline formatting`() {
        val markdown =
            """
            * **Bold Item** with [Link](https://example.com)
            * *Italic Item* with <u>Underline</u> and ~~Strike~~
            """.trimIndent()

        val richString = RichString.fromMarkdown(markdown)
        val boldIdx = richString.text.indexOf("Bold Item")
        val linkIdx = richString.text.indexOf("Link")
        val italicIdx = richString.text.indexOf("Italic Item")
        val underlineIdx = richString.text.indexOf("Underline")
        val strikeIdx = richString.text.indexOf("Strike")

        richString.spans.any { it.attributes.containsKey(BoldKey) && boldIdx in it.range } shouldBe true
        richString.spans.any { it.attributes[LinkKey] == "https://example.com" && linkIdx in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(ItalicKey) && italicIdx in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(UnderlineKey) && underlineIdx in it.range } shouldBe true
        richString.spans.any { it.attributes.containsKey(StrikethroughKey) && strikeIdx in it.range } shouldBe true
    }

    @Test
    fun `importing text with math equations containing asterisks`() {
        val markdown = "Calculation: 2 * 3 = 6 and 4 * 5 = 20."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Calculation: 2 * 3 = 6 and 4 * 5 = 20."
        richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe false
    }

    @Test
    fun `importing text with snake case identifiers containing underscores`() {
        val markdown = "The variable_name_here should not be italicized."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "The variable_name_here should not be italicized."
        richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe false
    }

    @Test
    fun `importing text with hashtags mid-sentence`() {
        val markdown = "I love programming in C# and #kotlin is great."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "I love programming in C# and #kotlin is great."
        richString.spans.any { it.attributes.containsKey(HeadingKey) } shouldBe false
    }

    @Test
    fun `importing text with comparison operators`() {
        val markdown = "Condition: x > 5 and y < 10."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Condition: x > 5 and y < 10."
        richString.spans.any { it.attributes.containsKey(BlockquoteKey) } shouldBe false
    }

    @Test
    fun `importing text with brackets not forming links`() {
        val markdown = "Access element array[0] or [index]."
        val richString = RichString.fromMarkdown(markdown)

        richString.text shouldBe "Access element array[0] or [index]."
        richString.spans.any { it.attributes.containsKey(LinkKey) } shouldBe false
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
    fun `exporting and importing 10000 characters long paragraph with formatting`() {
        val chunk = "Hello **World** with [Link](https://example.com) and *Italic*. "
        val markdown = chunk.repeat(150) // ~9,600 chars

        val richString = RichString.fromMarkdown(markdown)
        richString.text.length shouldBe ("Hello World with Link and Italic. ".length * 150)
        richString.spans.size shouldBe (3 * 150) // Bold, Link, Italic per chunk

        val exported = richString.toMarkdown()
        val reimported = RichString.fromMarkdown(exported)
        reimported.text shouldBe richString.text
        reimported.spans.size shouldBe richString.spans.size
    }

    @Test
    fun `importing multiple consecutive empty lines and whitespace only lines`() {
        val markdown = "\n\n\n   \n\nHello\n\n\n"
        val richString = RichString.fromMarkdown(markdown)

        richString.text.contains("Hello") shouldBe true
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
        reimported.spans shouldHaveSize 1
        reimported.spans.first().attributes.containsKey(BoldKey) shouldBe true
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
        val span = richString.spans.firstOrNull { it.attributes.containsKey(UnderlineKey) }
        span?.range shouldBe (5 until richString.text.length)
    }

    @Test
    fun `importing empty heading or empty blockquote does not crash`() {
        val markdown = "# \n> \n* "
        val richString = RichString.fromMarkdown(markdown)

        // Must not throw any exception
        (richString.text.isNotEmpty() || richString.text.isEmpty()) shouldBe true
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
