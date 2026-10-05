package dev.mkeeda.arranger.editor

import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.codeBlock
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class VisualBlockExtractorTest {
    private enum class CalloutType {
        Info,
        Warning,
    }

    private object CalloutKey : BlockTypeAttributeKey<CalloutType> {
        override val name: String = "callout"
        override val defaultValue: CalloutType = CalloutType.Info
    }

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
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = 0..text.lastIndex,
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
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = text.rangeOf("val a = 1\n"),
            ),
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = text.rangeOf("val b = 2"),
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
            VisualBlockItem(
                key = BlockquoteKey,
                value = Unit,
                range = text.rangeOf("Quote line\n"),
            ),
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = text.rangeOf("Code line"),
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
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = text.rangeOf("println(\"kt\")\n"),
            ),
            VisualBlockItem(
                key = CodeBlockKey,
                value = "python",
                range = text.rangeOf("print(\"py\")"),
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
            VisualBlockItem(
                key = BlockquoteKey,
                value = Unit,
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
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = 0..text.lastIndex,
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
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = 0..text.lastIndex,
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
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = 0..text.lastIndex,
            ),
        )
    }

    @Test
    fun `extractVisualBlocks merges contiguous paragraphs with identical custom attribute key and value`() {
        val text = "Callout line 1\nCallout line 2"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    setParagraphAttribute(CalloutKey, CalloutType.Info)
                }
            }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = 0..text.lastIndex,
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates contiguous paragraphs with different custom attribute values`() {
        val text = "Callout info\nCallout warning"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Callout info")) {
                        setParagraphAttribute(CalloutKey, CalloutType.Info)
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Callout warning")) {
                        setParagraphAttribute(CalloutKey, CalloutType.Warning)
                    }
                }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = text.rangeOf("Callout info\n"),
            ),
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Warning,
                range = text.rangeOf("Callout warning"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks ignores paragraphs with attributes not present in keys`() {
        val text = "Quote paragraph\nCallout line"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Quote paragraph")) {
                        blockquote()
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Callout line")) {
                        setParagraphAttribute(CalloutKey, CalloutType.Info)
                    }
                }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = text.rangeOf("Callout line"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks returns empty list when given keys set is empty`() {
        val text = "Quote paragraph"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    blockquote()
                }
            }

        val blocks = richString.extractVisualBlocks(keys = emptySet<AttributeKey<*>>())

        blocks.shouldBeEmpty()
    }

    private data class PanelMetadata(
        val title: String,
        val level: Int,
    )

    private object PanelKey : BlockTypeAttributeKey<PanelMetadata> {
        override val name: String = "panel"
        override val defaultValue: PanelMetadata = PanelMetadata("default", 0)
    }

    private object NullableNoticeKey : BlockTypeAttributeKey<String?> {
        override val name: String = "nullable_notice"
        override val defaultValue: String? = null
    }

    @Test
    fun `extractVisualBlocks separates multiple distinct custom attribute blocks that are adjacent`() {
        val text = "Callout line\nPanel line\nval x = 1"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Callout line")) {
                        setParagraphAttribute(CalloutKey, CalloutType.Info)
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Panel line")) {
                        setParagraphAttribute(PanelKey, PanelMetadata("Warning", 1))
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("val x = 1")) {
                        codeBlock(language = "kotlin")
                    }
                }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey, PanelKey, CodeBlockKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = text.rangeOf("Callout line\n"),
            ),
            VisualBlockItem(
                key = PanelKey,
                value = PanelMetadata("Warning", 1),
                range = text.rangeOf("Panel line\n"),
            ),
            VisualBlockItem(
                key = CodeBlockKey,
                value = "kotlin",
                range = text.rangeOf("val x = 1"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks merges contiguous paragraphs sharing identical complex data class attribute`() {
        val text = "Section 1\nSection 2"
        val metadata = PanelMetadata(title = "Alert", level = 2)
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    setParagraphAttribute(PanelKey, metadata)
                }
            }

        val blocks = richString.extractVisualBlocks(keys = setOf(PanelKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = PanelKey,
                value = metadata,
                range = 0..text.lastIndex,
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates contiguous paragraphs with different complex data class attribute values`() {
        val text = "Header panel\nDetail panel"
        val meta1 = PanelMetadata(title = "Alert", level = 1)
        val meta2 = PanelMetadata(title = "Alert", level = 2)
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Header panel")) {
                        setParagraphAttribute(PanelKey, meta1)
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Detail panel")) {
                        setParagraphAttribute(PanelKey, meta2)
                    }
                }

        val blocks = richString.extractVisualBlocks(keys = setOf(PanelKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = PanelKey,
                value = meta1,
                range = text.rangeOf("Header panel\n"),
            ),
            VisualBlockItem(
                key = PanelKey,
                value = meta2,
                range = text.rangeOf("Detail panel"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks merges contiguous paragraphs sharing null attribute value for nullable key`() {
        val text = "Null notice 1\nNull notice 2"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    setParagraphAttribute(NullableNoticeKey, null)
                }
            }

        val blocks = richString.extractVisualBlocks(keys = setOf(NullableNoticeKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = NullableNoticeKey,
                value = null,
                range = 0..text.lastIndex,
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates paragraphs when one has null and another has non-null value`() {
        val text = "Null notice\nCustom notice"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Null notice")) {
                        setParagraphAttribute(NullableNoticeKey, null)
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Custom notice")) {
                        setParagraphAttribute(NullableNoticeKey, "important")
                    }
                }

        val blocks = richString.extractVisualBlocks(keys = setOf(NullableNoticeKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = NullableNoticeKey,
                value = null,
                range = text.rangeOf("Null notice\n"),
            ),
            VisualBlockItem(
                key = NullableNoticeKey,
                value = "important",
                range = text.rangeOf("Custom notice"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks correctly extracts single-character block at start of text`() {
        val text = "A\nSecond line"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.rangeOf("A")) {
                    setParagraphAttribute(CalloutKey, CalloutType.Info)
                }
            }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = text.rangeOf("A\n"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks correctly extracts single-character block at end of text`() {
        val text = "First line\nZ"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.rangeOf("Z")) {
                    setParagraphAttribute(CalloutKey, CalloutType.Warning)
                }
            }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Warning,
                range = text.rangeOf("Z"),
            ),
        )
    }

    @Test
    fun `extractVisualBlocks merges multiple consecutive empty newline paragraphs when styled with the same attribute`() {
        val text = "\n\n\n"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    setParagraphAttribute(CalloutKey, CalloutType.Info)
                }
            }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = 0..text.lastIndex,
            ),
        )
    }

    @Test
    fun `extractVisualBlocks separates blocks when unstyled newline paragraph is placed between styled paragraphs`() {
        val text = "Block 1\n\nBlock 2"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.rangeOf("Block 1")) {
                        setParagraphAttribute(CalloutKey, CalloutType.Info)
                    }
                }
                .edit {
                    editAttributes(range = text.rangeOf("Block 2")) {
                        setParagraphAttribute(CalloutKey, CalloutType.Info)
                    }
                }

        val blocks = richString.extractVisualBlocks(keys = setOf(CalloutKey))

        blocks.shouldContainExactly(
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = text.rangeOf("Block 1\n"),
            ),
            VisualBlockItem(
                key = CalloutKey,
                value = CalloutType.Info,
                range = text.rangeOf("Block 2"),
            ),
        )
    }
}
