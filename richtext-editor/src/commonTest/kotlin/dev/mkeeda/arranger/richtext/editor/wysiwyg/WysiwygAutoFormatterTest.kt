package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.insert
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class WysiwygAutoFormatterTest {
    private fun createEngine(): Triple<RichTextState, WysiwygState, WysiwygInputTransformation> {
        val state = RichTextState(initialText = RichString(""))
        val wysiwygState = WysiwygState()
        val transformation = WysiwygInputTransformation(state, wysiwygState)
        return Triple(state, wysiwygState, transformation)
    }

    private fun typeText(
        state: RichTextState,
        transformation: WysiwygInputTransformation,
        text: String,
    ) {
        for (char in text) {
            state.textFieldState.edit {
                insert(selection.start, char.toString())
                with(transformation) {
                    transformInput()
                }
            }
        }
    }

    // --- Block Auto-Formatting (F2..F5) ---

    @Test
    fun `heading 1 trigger removes hash prefix and sets heading attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "# ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(HeadingKey) shouldBe HeadingLevel.H1
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `heading 2 trigger removes hashes prefix and sets heading attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "## ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(HeadingKey) shouldBe HeadingLevel.H2
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `heading 3 trigger removes hashes prefix and sets heading attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "### ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(HeadingKey) shouldBe HeadingLevel.H3
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `bullet list dash trigger sets bullet list attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "- ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(BulletListKey) shouldBe ListIndentLevel.Level1
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `bullet list asterisk trigger sets bullet list attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "* ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(BulletListKey) shouldBe ListIndentLevel.Level1
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `ordered list trigger sets ordered list attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "1. ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(OrderedListKey) shouldBe ListIndentLevel.Level1
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `blockquote trigger sets blockquote attribute`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "> ")

        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
        state.typingAttributes?.get(BlockquoteKey) shouldBe Unit
        wysiwygState.canRevert(state) shouldBe true
    }

    // --- Block False Positive Prevention (F11) ---

    @Test
    fun `hash in middle of sentence does not trigger heading`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "abc # ")

        state.textFieldState.text.toString() shouldBe "abc # "
        state.typingAttributes?.get(HeadingKey) shouldBe null
    }

    @Test
    fun `heading without space does not trigger`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "#Title")

        state.textFieldState.text.toString() shouldBe "#Title"
        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `heading level 4 does not trigger`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "#### ")

        state.textFieldState.text.toString() shouldBe "#### "
        state.typingAttributes?.get(HeadingKey) shouldBe null
    }

    @Test
    fun `ordered list with number 2 does not trigger`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "2. ")

        state.textFieldState.text.toString() shouldBe "2. "
        state.typingAttributes?.get(OrderedListKey) shouldBe null
    }

    @Test
    fun `horizontal rule does not trigger bullet list`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "--- ")

        state.textFieldState.text.toString() shouldBe "--- "
        state.typingAttributes?.get(BulletListKey) shouldBe null
    }

    // --- Inline Auto-Formatting (F6..F10) ---

    @Test
    fun `bold formatting applies bold span and positions cursor`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "**Hello**")

        state.textFieldState.text.toString() shouldBe "Hello"
        state.selection shouldBe TextRange(5)
        val boldSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) }
        boldSpan shouldNotBe null
        boldSpan?.range shouldBe 0..4
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `italic asterisk formatting applies italic span`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "*Hello*")

        state.textFieldState.text.toString() shouldBe "Hello"
        state.selection shouldBe TextRange(5)
        val italicSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(ItalicKey) }
        italicSpan shouldNotBe null
        italicSpan?.range shouldBe 0..4
    }

    @Test
    fun `italic underscore formatting applies italic span`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "_Hello_")

        state.textFieldState.text.toString() shouldBe "Hello"
        state.selection shouldBe TextRange(5)
        val italicSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(ItalicKey) }
        italicSpan shouldNotBe null
        italicSpan?.range shouldBe 0..4
    }

    @Test
    fun `code formatting applies code span`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "`Hello`")

        state.textFieldState.text.toString() shouldBe "Hello"
        state.selection shouldBe TextRange(5)
        val codeSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(CodeKey) }
        codeSpan shouldNotBe null
        codeSpan?.range shouldBe 0..4
    }

    @Test
    fun `strikethrough formatting applies strikethrough span`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "~Hello~")

        state.textFieldState.text.toString() shouldBe "Hello"
        state.selection shouldBe TextRange(5)
        val strikeSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(StrikethroughKey) }
        strikeSpan shouldNotBe null
        strikeSpan?.range shouldBe 0..4
    }

    // --- Inline False Positive Prevention (F11) ---

    @Test
    fun `snake case does not trigger italic underscore`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "foo_bar_baz")

        state.textFieldState.text.toString() shouldBe "foo_bar_baz"
        state.richString.spans.none { it.attributes.containsKey(ItalicKey) } shouldBe true
    }

    @Test
    fun `empty asterisks do not trigger bold`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "****")

        state.textFieldState.text.toString() shouldBe "****"
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun `space padded text does not trigger bold`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "** bold**")

        state.textFieldState.text.toString() shouldBe "** bold**"
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun `escaped asterisks do not trigger bold`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, """\*\*bold\*\*""")

        state.textFieldState.text.toString() shouldBe """\*\*bold\*\*"""
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    // --- Style Leakage Prevention (F14) ---

    @Test
    fun `typing plain text after bold does not leak bold attribute`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold** after")

        state.textFieldState.text.toString() shouldBe "bold after"
        val boldSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) }
        boldSpan shouldNotBe null
        boldSpan?.range shouldBe 0..3
    }

    // --- Undo and Backspace Reversal (F12, F13) ---

    @Test
    fun `reverting heading auto-format restores raw hash text`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "# ")

        wysiwygState.canRevert(state) shouldBe true
        val reverted = wysiwygState.revert(state)
        reverted shouldBe true

        state.textFieldState.text.toString() shouldBe "# "
        state.selection shouldBe TextRange(2)
        state.typingAttributes?.get(HeadingKey) shouldBe null
    }

    @Test
    fun `reverting bold auto-format restores raw asterisks text`() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "**bold**")

        wysiwygState.canRevert(state) shouldBe true
        val reverted = wysiwygState.revert(state)
        reverted shouldBe true

        state.textFieldState.text.toString() shouldBe "**bold**"
        state.selection shouldBe TextRange(8)
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun `standard undo restores raw text and redo restores formatted state`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold**")

        state.textFieldState.text.toString() shouldBe "bold"
        state.undoState.canUndo shouldBe true

        // Undo to raw Markdown State B
        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "**bold**"
        state.selection shouldBe TextRange(8)

        // Redo back to formatted State C
        state.undoState.redo()
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)
        val boldSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) }
        boldSpan shouldNotBe null
        boldSpan?.range shouldBe 0..3
    }

    @Test
    fun `block transformation on existing line without trailing newline sets correct closed paragraph range`() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "Item")

        // Move cursor to start of line and type "# "
        state.textFieldState.edit {
            selection = TextRange(0)
        }
        typeText(state, transformation, "# ")

        state.textFieldState.text.toString() shouldBe "Item"
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1
        headingRuns[0].range.first shouldBe 0
        (headingRuns[0].range.last <= 4) shouldBe true
    }

    @Test
    fun `block transformation clears conflicting block attributes on paragraph`() {
        val (state, _, transformation) = createEngine()
        // Line 1: Bullet list
        typeText(state, transformation, "- Item 1\n")
        // Line 2: Inherits bullet list, then converts to ordered list
        typeText(state, transformation, "1. ")
        typeText(state, transformation, "Item 2")

        val bulletRuns = state.richString.runs(BulletListKey).toList()
        val orderedRuns = state.richString.runs(OrderedListKey).toList()

        bulletRuns.size shouldBe 1
        orderedRuns.size shouldBe 1
        orderedRuns[0].value shouldBe ListIndentLevel.Level1
    }
}
