package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class WysiwygChallenger4ErgonomicsTest {
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
    // 1. Undo (Cmd+Z) immediately after auto-formatting
    // =========================================================================

    @Test
    fun test_undo_heading1_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "# "
        state.selection shouldBe TextRange(2)
        state.typingAttributes?.get(HeadingKey) shouldBe null
        state.richString.spans.none { it.attributes.containsKey(HeadingKey) } shouldBe true
    }

    @Test
    fun test_undo_heading2_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "## ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "## "
        state.selection shouldBe TextRange(3)
        state.typingAttributes?.get(HeadingKey) shouldBe null
    }

    @Test
    fun test_undo_heading3_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "### ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "### "
        state.selection shouldBe TextRange(4)
        state.typingAttributes?.get(HeadingKey) shouldBe null
    }

    @Test
    fun test_undo_bullet_list_dash_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "- ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "- "
        state.selection shouldBe TextRange(2)
        state.typingAttributes?.get(BulletListKey) shouldBe null
    }

    @Test
    fun test_undo_bullet_list_asterisk_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "* ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "* "
        state.selection shouldBe TextRange(2)
        state.typingAttributes?.get(BulletListKey) shouldBe null
    }

    @Test
    fun test_undo_ordered_list_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "1. ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "1. "
        state.selection shouldBe TextRange(3)
        state.typingAttributes?.get(OrderedListKey) shouldBe null
    }

    @Test
    fun test_undo_blockquote_restores_raw_prefix_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "> ")
        state.textFieldState.text.toString() shouldBe ""
        state.undoState.canUndo shouldBe true

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "> "
        state.selection shouldBe TextRange(2)
        state.typingAttributes?.get(BlockquoteKey) shouldBe null
    }

    @Test
    fun test_undo_bold_restores_raw_markers_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "**bold**"
        state.selection shouldBe TextRange(8)
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun test_undo_italic_asterisk_restores_raw_markers_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "*italic*")
        state.textFieldState.text.toString() shouldBe "italic"
        state.selection shouldBe TextRange(6)

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "*italic*"
        state.selection shouldBe TextRange(8)
        state.richString.spans.none { it.attributes.containsKey(ItalicKey) } shouldBe true
    }

    @Test
    fun test_undo_italic_underscore_restores_raw_markers_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "_italic_")
        state.textFieldState.text.toString() shouldBe "italic"
        state.selection shouldBe TextRange(6)

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "_italic_"
        state.selection shouldBe TextRange(8)
        state.richString.spans.none { it.attributes.containsKey(ItalicKey) } shouldBe true
    }

    @Test
    fun test_undo_inline_code_restores_raw_markers_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "`code`")
        state.textFieldState.text.toString() shouldBe "code"
        state.selection shouldBe TextRange(4)

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "`code`"
        state.selection shouldBe TextRange(6)
        state.richString.spans.none { it.attributes.containsKey(CodeKey) } shouldBe true
    }

    @Test
    fun test_undo_strikethrough_restores_raw_markers_and_cursor() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "~strike~")
        state.textFieldState.text.toString() shouldBe "strike"
        state.selection shouldBe TextRange(6)

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "~strike~"
        state.selection shouldBe TextRange(8)
        state.richString.spans.none { it.attributes.containsKey(StrikethroughKey) } shouldBe true
    }

    @Test
    fun test_undo_inline_with_preceding_text() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "Prefix **bold**")
        state.textFieldState.text.toString() shouldBe "Prefix bold"
        state.selection shouldBe TextRange(11)

        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "Prefix **bold**"
        state.selection shouldBe TextRange(15)
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun test_undo_and_redo_cycles_consistency() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**test**")
        state.textFieldState.text.toString() shouldBe "test"
        state.selection shouldBe TextRange(4)

        // Undo 1
        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "**test**"
        state.selection shouldBe TextRange(8)
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true

        // Redo 1
        state.undoState.redo()
        state.textFieldState.text.toString() shouldBe "test"
        state.selection shouldBe TextRange(4)
        state.richString.runs(BoldKey).toList().size shouldBe 1

        // Undo 2
        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "**test**"
        state.selection shouldBe TextRange(8)

        // Redo 2
        state.undoState.redo()
        state.textFieldState.text.toString() shouldBe "test"
        state.selection shouldBe TextRange(4)
        state.richString.runs(BoldKey).toList().size shouldBe 1
    }

    // =========================================================================
    // 2. Backspace immediately after auto-formatting (F13)
    // =========================================================================

    @Test
    fun test_backspace_heading1_reversal() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "# ")
        wysiwygState.canRevert(state) shouldBe true

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "# "
        state.selection shouldBe TextRange(2)
        wysiwygState.canRevert(state) shouldBe false

        // Subsequent backspaces perform normal deletion
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "#"
        state.selection shouldBe TextRange(1)

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun test_backspace_bullet_list_reversal() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "- ")
        wysiwygState.canRevert(state) shouldBe true

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "- "
        state.selection shouldBe TextRange(2)
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun test_backspace_ordered_list_reversal() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "1. ")
        wysiwygState.canRevert(state) shouldBe true

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "1. "
        state.selection shouldBe TextRange(3)
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun test_backspace_blockquote_reversal() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "> ")
        wysiwygState.canRevert(state) shouldBe true

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "> "
        state.selection shouldBe TextRange(2)
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun test_backspace_bold_reversal() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "**bold**")
        wysiwygState.canRevert(state) shouldBe true

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "**bold**"
        state.selection shouldBe TextRange(8)
        wysiwygState.canRevert(state) shouldBe false

        // Subsequent backspace deletes last asterisk
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "**bold*"
        state.selection shouldBe TextRange(7)
    }

    @Test
    fun test_backspace_code_reversal() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "`code`")
        wysiwygState.canRevert(state) shouldBe true

        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "`code`"
        state.selection shouldBe TextRange(6)
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun test_backspace_reversal_invalidated_by_cursor_movement() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "**bold**")
        wysiwygState.canRevert(state) shouldBe true

        // Move cursor back by 1
        state.textFieldState.edit {
            selection = TextRange(3)
        }
        wysiwygState.canRevert(state) shouldBe false

        // Backspace does normal character deletion
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bod"
        state.selection shouldBe TextRange(2)
    }

    @Test
    fun test_backspace_reversal_invalidated_by_further_typing() {
        val (state, wysiwygState, transformation) = createEngine()
        typeText(state, transformation, "**bold**")
        wysiwygState.canRevert(state) shouldBe true

        // Type extra character
        typeText(state, transformation, "!")
        wysiwygState.canRevert(state) shouldBe false

        // Backspace deletes the exclamation mark, not reverting auto-format
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)
    }

    // =========================================================================
    // 3. Typing Ergonomics & Style Leakage Prevention (F14)
    // =========================================================================

    @Test
    fun test_f14_typing_characters_immediately_after_bold_does_not_leak() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"

        // Type subsequent characters
        typeText(state, transformation, "text")
        state.textFieldState.text.toString() shouldBe "boldtext"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 0..3 // Only "bold" is bold
    }

    @Test
    fun test_f14_typing_with_space_after_bold_does_not_leak() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold** and plain")
        state.textFieldState.text.toString() shouldBe "bold and plain"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 0..3
    }

    @Test
    fun test_f14_typing_characters_immediately_after_italic_asterisk_does_not_leak() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "*italic*more")
        state.textFieldState.text.toString() shouldBe "italicmore"

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns shouldHaveSize 1
        italicRuns[0].range shouldBe 0..5
    }

    @Test
    fun test_f14_typing_characters_immediately_after_italic_underscore_does_not_leak() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "_italic_more")
        state.textFieldState.text.toString() shouldBe "italicmore"

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns shouldHaveSize 1
        italicRuns[0].range shouldBe 0..5
    }

    @Test
    fun test_f14_typing_characters_immediately_after_code_does_not_leak() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "`code`more")
        state.textFieldState.text.toString() shouldBe "codemore"

        val codeRuns = state.richString.runs(CodeKey).toList()
        codeRuns shouldHaveSize 1
        codeRuns[0].range shouldBe 0..3
    }

    @Test
    fun test_f14_typing_characters_immediately_after_strikethrough_does_not_leak() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "~strike~more")
        state.textFieldState.text.toString() shouldBe "strikemore"

        val strikeRuns = state.richString.runs(StrikethroughKey).toList()
        strikeRuns shouldHaveSize 1
        strikeRuns[0].range shouldBe 0..5
    }

    @Test
    fun test_f14_inline_formatting_with_surrounding_plain_text() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "Prefix **bold** suffix")
        state.textFieldState.text.toString() shouldBe "Prefix bold suffix"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 7..10
    }

    @Test
    fun test_f14_consecutive_inline_formats_do_not_contaminate_each_other() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold** `code` *italic*")
        state.textFieldState.text.toString() shouldBe "bold code italic"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 0..3

        val codeRuns = state.richString.runs(CodeKey).toList()
        codeRuns shouldHaveSize 1
        codeRuns[0].range shouldBe 5..8

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns shouldHaveSize 1
        italicRuns[0].range shouldBe 10..15
    }

    @Test
    fun test_f14_punctuation_immediately_after_inline_formatting() {
        val (state, _, transformation) = createEngine()
        typeText(state, transformation, "**bold**, `code`: *italic*!")
        state.textFieldState.text.toString() shouldBe "bold, code: italic!"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 0..3

        val codeRuns = state.richString.runs(CodeKey).toList()
        codeRuns shouldHaveSize 1
        codeRuns[0].range shouldBe 6..9

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns shouldHaveSize 1
        italicRuns[0].range shouldBe 12..17
    }

    @Test
    fun test_f14_typing_after_backspacing_a_typed_character() {
        val (state, wysiwygState, transformation) = createEngine()
        // 1. Type **bold**
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"

        // 2. Type 'x' (not bold)
        typeText(state, transformation, "x")
        state.textFieldState.text.toString() shouldBe "boldx"

        // 3. Backspace 'x'
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bold"

        // 4. Type 'y'
        typeText(state, transformation, "y")
        state.textFieldState.text.toString() shouldBe "boldy"

        // Boundary inheritance note: After deleting 'x', cursor is back at index 4 (boundary of bold).
        // Since removedTypingAttributes was consumed on 'x', standard rich text boundary inheritance applies.
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 0..4
    }
}
