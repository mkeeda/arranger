package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Stress test suite for WYSIWYG input transformations.
 * Thoroughly exercises block triggers, block transitions, inline triggers,
 * false-positive mining, and ergonomic undo/redo behaviors.
 */
class WysiwygInputTransformationStressTest {
    private fun createEngine(): Triple<RichTextState, WysiwygState, WysiwygInputTransformation> {
        val state = RichTextState(initialText = RichString(""))
        val wysiwygState = WysiwygState()
        val transformation = WysiwygInputTransformation(state, wysiwygState)
        return Triple(state, wysiwygState, transformation)
    }

    private fun typeChar(
        state: RichTextState,
        transformation: WysiwygInputTransformation,
        char: Char,
    ) {
        state.textFieldState.edit {
            insert(selection.start, char.toString())
            with(transformation) {
                transformInput()
            }
        }
    }

    private fun typeText(
        state: RichTextState,
        transformation: WysiwygInputTransformation,
        text: String,
    ) {
        for (char in text) {
            typeChar(state, transformation, char)
        }
    }

    private fun pressBackspace(
        state: RichTextState,
        wysiwygState: WysiwygState,
    ) {
        if (wysiwygState.canRevert(state)) {
            wysiwygState.revert(state)
        } else {
            state.textFieldState.edit {
                if (selection.collapsed) {
                    if (selection.start > 0) {
                        delete(selection.start - 1, selection.start)
                    }
                } else {
                    delete(selection.start, selection.end)
                }
            }
            wysiwygState.clearLastAutoFormat()
        }
    }

    // =========================================================================
    // 1. Block Auto-Formatting Triggers Matrix (# , ## , ### , - , * , 1. , > )
    // =========================================================================

    @Test
    fun `all block triggers clean application and typing attributes`() {
        val triggers =
            listOf(
                "# " to (HeadingKey to HeadingLevel.H1),
                "## " to (HeadingKey to HeadingLevel.H2),
                "### " to (HeadingKey to HeadingLevel.H3),
                "- " to (BulletListKey to ListIndentLevel.Level1),
                "* " to (BulletListKey to ListIndentLevel.Level1),
                "1. " to (OrderedListKey to ListIndentLevel.Level1),
                "> " to (BlockquoteKey to Unit),
            )

        for ((trigger, expectedAttr) in triggers) {
            val (state, wysiwygState, transformation) = createEngine()
            typeText(state, transformation, trigger)

            state.textFieldState.text.toString() shouldBe ""
            state.selection shouldBe TextRange(0)
            wysiwygState.canRevert(state) shouldBe true
            state.typingAttributes?.get(expectedAttr.first) shouldBe expectedAttr.second

            // Now type text and ensure the attribute is in richString.spans
            typeText(state, transformation, "Content")
            state.textFieldState.text.toString() shouldBe "Content"
            val runs = state.richString.runs(expectedAttr.first).toList()
            runs.size shouldBe 1
            runs[0].value shouldBe expectedAttr.second
            runs[0].range shouldBe 0..6
        }
    }

    // =========================================================================
    // 2. Comprehensive Block Attribute Transitions Matrix
    // =========================================================================

    @Test
    fun `transition from quote to heading`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "> Quote text\n")
        typeText(state, transformation, "# Heading text")

        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
        state.richString.runs(HeadingKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from quote to bullet`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "> Quote text\n")
        typeText(state, transformation, "- Bullet item")

        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
        state.richString.runs(BulletListKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from quote to ordered`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "> Quote text\n")
        typeText(state, transformation, "1. Ordered item")

        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
        state.richString.runs(OrderedListKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from ordered to bullet`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "1. First\n")
        typeText(state, transformation, "- Second")

        state.richString.runs(OrderedListKey).toList().size shouldBe 1
        state.richString.runs(BulletListKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from ordered to heading`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "1. First\n")
        typeText(state, transformation, "## Second")

        state.richString.runs(OrderedListKey).toList().size shouldBe 1
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H2
    }

    @Test
    fun `transition from ordered to quote`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "1. First\n")
        typeText(state, transformation, "> Second")

        state.richString.runs(OrderedListKey).toList().size shouldBe 1
        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from heading to quote`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "# Header\n")
        typeText(state, transformation, "> Blockquote")

        state.richString.runs(HeadingKey).toList().size shouldBe 1
        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from heading to ordered`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "# Header\n")
        typeText(state, transformation, "1. First Item")

        state.richString.runs(HeadingKey).toList().size shouldBe 1
        state.richString.runs(OrderedListKey).toList().size shouldBe 1
    }

    @Test
    fun `transition from bullet to quote`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "- Bullet\n")
        typeText(state, transformation, "> Quote")

        state.richString.runs(BulletListKey).toList().size shouldBe 1
        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
    }

    @Test
    fun `heading levels transition`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "# Heading 1\n")
        typeText(state, transformation, "## Heading 2\n")
        typeText(state, transformation, "### Heading 3")

        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 3
        headingRuns[0].value shouldBe HeadingLevel.H1
        headingRuns[1].value shouldBe HeadingLevel.H2
        headingRuns[2].value shouldBe HeadingLevel.H3
    }

    @Test
    fun `same line in place revert and switch block type`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Type "- " -> Bullet List
        typeText(state, transformation, "- ")
        state.typingAttributes?.get(BulletListKey) shouldBe ListIndentLevel.Level1

        // 2. Backspace revert -> "- " raw
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "- "

        // 3. Backspace twice to clear line
        pressBackspace(state, wysiwygState)
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe ""

        // 4. Type "> " -> Blockquote
        typeText(state, transformation, "> ")
        state.typingAttributes?.get(BlockquoteKey) shouldBe Unit
        state.typingAttributes?.get(BulletListKey) shouldBe null

        // 5. Type text
        typeText(state, transformation, "A quote")
        state.richString.runs(BlockquoteKey).toList().size shouldBe 1
        state.richString.runs(BulletListKey).toList().size shouldBe 0
    }

    // =========================================================================
    // 3. Inline Auto-Formatting Triggers Matrix (**, *, _, `, ~)
    // =========================================================================

    @Test
    fun `all inline triggers`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "**bold** ")
        typeText(state, transformation, "*italic* ")
        typeText(state, transformation, "_emphasis_ ")
        typeText(state, transformation, "`code` ")
        typeText(state, transformation, "~strike~")

        val expected = "bold italic emphasis code strike"
        state.textFieldState.text.toString() shouldBe expected

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..3

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 2 // *italic* and _emphasis_
        italicRuns[0].range shouldBe 5..10
        italicRuns[1].range shouldBe 12..19

        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 21..24

        val strikeRuns = state.richString.runs(StrikethroughKey).toList()
        strikeRuns.size shouldBe 1
        strikeRuns[0].range shouldBe 26..31
    }

    // =========================================================================
    // 4. False Positive & Boundary Mining
    // =========================================================================

    @Test
    fun `snake case and internal underscores no format`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "foo_bar_baz AND my_variable_name")

        state.textFieldState.text.toString() shouldBe "foo_bar_baz AND my_variable_name"
        state.richString.runs(ItalicKey).toList().isEmpty() shouldBe true
    }

    @Test
    fun `unclosed markers do not format`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**unclosed bold\n")
        typeText(state, transformation, "*unclosed italic\n")
        typeText(state, transformation, "_unclosed underscore\n")
        typeText(state, transformation, "`unclosed code\n")
        typeText(state, transformation, "~unclosed strike")

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `empty markers do not format`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "empty: **** and ** and __ and `` and ~~")

        state.textFieldState.text.toString() shouldBe "empty: **** and ** and __ and `` and ~~"
        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `whitespace inside markers no format`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "** leading space** ")
        typeText(state, transformation, "**trailing space ** ")
        typeText(state, transformation, "` code with leading ` ")

        state.textFieldState.text.toString() shouldBe "** leading space** **trailing space ** ` code with leading ` "
        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `escaped markers do not format`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, """\*not italic\* and \*\*not bold\*\* and \`not code\`""")

        state.textFieldState.text.toString() shouldBe """\*not italic\* and \*\*not bold\*\* and \`not code\`"""
        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `double backslash escaped allows formatting`() {
        val (state, _, transformation) = createEngine()
        // Double backslash means escaped backslash, so marker is active: \\*italic*
        typeText(state, transformation, """\\*italic*""")

        state.textFieldState.text.toString() shouldBe """\\italic"""
        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 2..7
    }

    // =========================================================================
    // 5. Stress & Real-World Edge Cases
    // =========================================================================

    @Test
    fun `triple asterisk bold and italic`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "***bold and italic***")

        state.textFieldState.text.toString() shouldBe "bold and italic"
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..14

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 0..14
    }

    @Test
    fun `nested bold and underscore italic`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold and _italic_ text**")

        state.textFieldState.text.toString() shouldBe "bold and italic text"
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..19

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 9..14
    }

    @Test
    fun `ten paragraphs alternating blocks stress`() {
        val (state, _, transformation) = createEngine()

        for (i in 1..10) {
            when (i % 5) {
                1 -> typeText(state, transformation, "# Heading $i with **Bold$i**\n")
                2 -> typeText(state, transformation, "- Bullet $i with `Code$i`\n")
                3 -> typeText(state, transformation, "1. Ordered $i with *Italic$i*\n")
                4 -> typeText(state, transformation, "> Quote $i with ~Strike$i~\n")
                0 -> typeText(state, transformation, "## HeadingSub $i with _Em${i}_\n")
            }
        }

        val hRuns = state.richString.runs(HeadingKey).toList()
        hRuns.size shouldBe 4 // 1, 5(0), 6(1), 10(0)
        state.richString.runs(BulletListKey).toList().size shouldBe 2 // 2, 7
        state.richString.runs(OrderedListKey).toList().size shouldBe 2 // 3, 8
        state.richString.runs(BlockquoteKey).toList().size shouldBe 2 // 4, 9

        state.richString.runs(BoldKey).toList().size shouldBe 2
        state.richString.runs(InlineCodeKey).toList().size shouldBe 2
        state.richString.runs(ItalicKey).toList().size shouldBe 4 // 2 asterisk + 2 underscore
        state.richString.runs(StrikethroughKey).toList().size shouldBe 2
    }
}
