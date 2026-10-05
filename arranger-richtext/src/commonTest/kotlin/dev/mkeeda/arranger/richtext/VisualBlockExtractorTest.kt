package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class VisualBlockExtractorTest {
    @Test
    fun `extractVisualBlocks merges contiguous code block paragraphs sharing CodeBlockKey`() {
        val text = "fun foo() {\n    return 42\n}"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    codeBlock(language = "kotlin")
                }
            }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.CodeBlock(
                range = 0..text.lastIndex,
                language = "kotlin",
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates distinct code blocks divided by normal paragraph`() {
        val text = "val a = 1\n\nval b = 2"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("val a = 1")) {
                        codeBlock(language = "kotlin")
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("val b = 2")) {
                        codeBlock(language = "kotlin")
                    }
                }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.CodeBlock(
                range = text.rangeOf("val a = 1\n"),
                language = "kotlin",
            ),
            VisualBlock.CodeBlock(
                range = text.rangeOf("val b = 2"),
                language = "kotlin",
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates adjacent blockquote and code block`() {
        val text = "Quote line\nCode line"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Quote line")) {
                        blockquote()
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Code line")) {
                        codeBlock(language = "kotlin")
                    }
                }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.Blockquote(
                range = text.rangeOf("Quote line\n"),
            ),
            VisualBlock.CodeBlock(
                range = text.rangeOf("Code line"),
                language = "kotlin",
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates contiguous code blocks with different languages`() {
        val text = "println(\"kt\")\nprint(\"py\")"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("println(\"kt\")")) {
                        codeBlock(language = "kotlin")
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("print(\"py\")")) {
                        codeBlock(language = "python")
                    }
                }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.CodeBlock(
                range = text.rangeOf("println(\"kt\")\n"),
                language = "kotlin",
            ),
            VisualBlock.CodeBlock(
                range = text.rangeOf("print(\"py\")"),
                language = "python",
            ),
        )
    }

    @Test
    fun `extractVisualBlocks merges contiguous blockquote paragraphs`() {
        val text = "Quote paragraph 1\nQuote paragraph 2"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    blockquote()
                }
            }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.Blockquote(
                range = 0..text.lastIndex,
            ),
        )
    }

    @Test
    fun `extractVisualBlocks returns empty list when text has no visual block attributes`() {
        val text = "Heading\nNormal body paragraph\nList item"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Heading")) {
                        headingLevel(level = HeadingLevel.H1)
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("List item")) {
                        bulletList(level = ListIndentLevel.Level1)
                    }
                }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldBeEmpty()
    }

    @Test
    fun `extractVisualBlocks returns empty list for empty rich text`() {
        val richString = RichString(text = "")

        val blocks = richString.extractVisualBlocks()

        blocks.shouldBeEmpty()
    }

    @Test
    fun `extractVisualBlocks handles multiple spans within the same visual block paragraph`() {
        val text = "val x = **important** + 1"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.indices) {
                        codeBlock(language = "kotlin")
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("important")) {
                        bold()
                    }
                }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.CodeBlock(
                range = 0..text.lastIndex,
                language = "kotlin",
            ),
        )
    }

    @Test
    fun `extractVisualBlocks preserves code block containing empty lines`() {
        val text = "fun foo() {\n\n    return 42\n}"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    codeBlock(language = "kotlin")
                }
            }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.CodeBlock(
                range = 0..text.lastIndex,
                language = "kotlin",
            ),
        )
    }

    @Test
    fun `extractVisualBlocks merges code block paragraphs when empty line paragraph is styled individually`() {
        val text = "fun foo() {\n\n    return 42\n}"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = 0..11) { codeBlock(language = "kotlin") }
                }
                .edit {
                    editAttributes(range = 12..12) { codeBlock(language = "kotlin") }
                }
                .edit {
                    editAttributes(range = 13..27) { codeBlock(language = "kotlin") }
                }
                .edit {
                    editAttributes(range = 28..28) { codeBlock(language = "kotlin") }
                }

        val blocks = richString.extractVisualBlocks()

        blocks.shouldContainExactly(
            VisualBlock.CodeBlock(
                range = 0..text.lastIndex,
                language = "kotlin",
            ),
        )
    }
}
