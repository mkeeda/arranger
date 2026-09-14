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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Tier 5: Adversarial Coverage Hardening Test Suite.
 *
 * Exercises the production auto-formatting engine (WysiwygAutoFormatter, WysiwygInputTransformation,
 * WysiwygState, RichTextState) under hostile and boundary conditions:
 * 1. Rapid typing simulations with mixed block and inline formatting
 * 2. Unicode, emoji, surrogate pairs, and CJK text adjacent to markdown trigger tokens
 * 3. Consecutive newlines, leading/trailing whitespace, and tabs
 * 4. Undo/Redo cycles interleaved with typing, backspace reversal, and cursor movement
 * 5. False positive prevention verification (URLs, math formulas, snake_case, identifiers)
 */
class Tier5AdversarialCoverageHardeningTest {
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
        transformation: WysiwygInputTransformation? = null,
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
                if (transformation != null) {
                    with(transformation) {
                        transformInput()
                    }
                }
            }
            wysiwygState.clearLastAutoFormat()
        }
    }

    // =========================================================================
    // Group 1: Rapid Typing Simulations with Mixed Block and Inline Formatting
    // =========================================================================

    @Test
    fun `rapid heading1 with immediate multiple inline decorations`() {
        val (state, _, transformation) = createEngine()

        // 1. Heading 1 trigger
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""

        // 2. Bold followed immediately by Italic without spaces
        typeText(state, transformation, "**bold***italic*")
        state.textFieldState.text.toString() shouldBe "bolditalic"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..3

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 4..9

        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1
    }

    @Test
    fun `rapid block switching via backspace on empty line`() {
        val (state, wysiwygState, transformation) = createEngine()

        // Start with H1
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        // Revert H1 to raw "# "
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "# "

        // Delete space and hash
        pressBackspace(state, wysiwygState, transformation)
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe ""

        // Switch to Bullet List
        typeText(state, transformation, "- ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        // Revert Bullet List
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "- "

        // Delete and switch to Blockquote
        pressBackspace(state, wysiwygState, transformation)
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe ""

        typeText(state, transformation, "> ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        // Type quote body
        typeText(state, transformation, "Quoted wisdom")
        state.textFieldState.text.toString() shouldBe "Quoted wisdom"

        val quoteRuns = state.richString.runs(BlockquoteKey).toList()
        quoteRuns.size shouldBe 1
    }

    @Test
    fun `rapid ordered list with all inline types in single line`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "1. ")
        typeText(state, transformation, "**B** and *I* and `C` and ~S~")

        state.textFieldState.text.toString() shouldBe "B and I and C and S"

        val orderedRuns = state.richString.runs(OrderedListKey).toList()
        orderedRuns.size shouldBe 1
        orderedRuns[0].value shouldBe ListIndentLevel.Level1

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..0

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 6..6

        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 12..12

        val strikeRuns = state.richString.runs(StrikethroughKey).toList()
        strikeRuns.size shouldBe 1
        strikeRuns[0].range shouldBe 18..18
    }

    // =========================================================================
    // Group 2: Unicode, Emoji, Surrogate Pairs, and CJK Text Adjacent to Tokens
    // =========================================================================

    @Test
    fun `cjk kanji and hiragana adjacent to bold and code`() {
        val (state, _, transformation) = createEngine()

        // CJK before and after bold: "私の**最愛の**本" -> "私の最愛の本"
        typeText(state, transformation, "私の**最愛の**本")
        state.textFieldState.text.toString() shouldBe "私の最愛の本"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 2..4

        // CJK before and after code: "、`設定`画面" -> "、設定画面"
        typeText(state, transformation, "、`設定`画面")
        state.textFieldState.text.toString() shouldBe "私の最愛の本、設定画面"

        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 7..8
    }

    @Test
    fun `cjk with underscore italic word boundary protection`() {
        val (state, _, transformation) = createEngine()

        // In Japanese, kanji/kana are letters, so intra-word underscore like "私の_秘密_です"
        // has beforeOpening = 'の' (letter), which triggers letter protection and avoids false conversion!
        typeText(state, transformation, "私の_秘密_です")
        state.textFieldState.text.toString() shouldBe "私の_秘密_です"
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()

        // But space-separated Japanese with underscore: "私の _秘密_ です" DOES convert!
        typeText(state, transformation, " また _秘密_ です")
        state.textFieldState.text.toString() shouldBe "私の_秘密_です また 秘密 です"

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 12..13
    }

    @Test
    fun `emoji surrogate pairs inside and around inline formatting`() {
        val (state, _, transformation) = createEngine()

        // Surrogate pair emoji inside bold: "**🎉**"
        typeText(state, transformation, "**🎉**")
        state.textFieldState.text.toString() shouldBe "🎉"

        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..1 // UTF-16 surrogate pair length = 2 (indices 0..1)

        // Surrogate pair emoji around code: "🚀`build`✨"
        typeText(state, transformation, " 🚀`build`✨")
        state.textFieldState.text.toString() shouldBe "🎉 🚀build✨"

        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 5..9
    }

    @Test
    fun `emoji inside strikethrough and italic`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "~🔥~ and *🌟*")
        state.textFieldState.text.toString() shouldBe "🔥 and 🌟"

        val strikeRuns = state.richString.runs(StrikethroughKey).toList()
        strikeRuns.size shouldBe 1
        strikeRuns[0].range shouldBe 0..1

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 7..8
    }

    @Test
    fun `zenkaku fullwidth space does not trigger block formatting`() {
        val (state, _, transformation) = createEngine()

        // Zenkaku Japanese space '\u3000' must NOT trigger markdown block
        typeText(state, transformation, "#\u3000見出し")
        state.textFieldState.text.toString() shouldBe "#\u3000見出し"
        state.richString.runs(HeadingKey).toList().shouldBeEmpty()

        typeText(state, transformation, "\n-\u3000リスト")
        state.textFieldState.text.toString() shouldBe "#\u3000見出し\n-\u3000リスト"
        state.richString.runs(BulletListKey).toList().shouldBeEmpty()
    }

    // =========================================================================
    // Group 3: Consecutive Newlines, Leading/Trailing Whitespace, and Tabs
    // =========================================================================

    @Test
    fun `consecutive newlines before block triggers`() {
        val (state, _, transformation) = createEngine()

        // Multiple empty lines before heading
        typeText(state, transformation, "\n\n# Heading 3rd line")
        state.textFieldState.text.toString() shouldBe "\n\nHeading 3rd line"

        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].range.first shouldBe 2 // lineStart is 2
    }

    @Test
    fun `leading whitespace and tabs prevent block triggers`() {
        val (state, _, transformation) = createEngine()

        // Leading single space before '#'
        typeText(state, transformation, " # Not a heading\n")
        state.textFieldState.text.toString() shouldBe " # Not a heading\n"
        state.richString.runs(HeadingKey).toList().shouldBeEmpty()

        // Leading tab before '-'
        typeText(state, transformation, "\t- Not a list\n")
        state.textFieldState.text.toString() shouldBe " # Not a heading\n\t- Not a list\n"
        state.richString.runs(BulletListKey).toList().shouldBeEmpty()

        // Leading spaces before blockquote
        typeText(state, transformation, "  > Not a quote")
        state.richString.runs(BlockquoteKey).toList().shouldBeEmpty()
    }

    @Test
    fun `inner whitespace and tabs prevent inline formatting`() {
        val (state, _, transformation) = createEngine()

        // Space after opening or before closing
        typeText(state, transformation, "** spaced ** ")
        state.textFieldState.text.toString() shouldBe "** spaced ** "
        state.richString.runs(BoldKey).toList().shouldBeEmpty()

        typeText(state, transformation, "* italic * ")
        state.textFieldState.text.toString() shouldBe "** spaced ** * italic * "
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()

        typeText(state, transformation, "` code ` ")
        state.richString.runs(InlineCodeKey).toList().shouldBeEmpty()

        typeText(state, transformation, "~ strike ~ ")
        state.richString.runs(StrikethroughKey).toList().shouldBeEmpty()

        // Tab after opening
        typeText(state, transformation, "**\ttab**")
        state.richString.runs(BoldKey).toList().shouldBeEmpty()
    }

    // =========================================================================
    // Group 4: Undo/Redo Cycles Interleaved with Typing, Backspace, and Navigation
    // =========================================================================

    @Test
    fun `undo redo cycles with intermediate navigation`() {
        val (state, wysiwygState, transformation) = createEngine()

        typeText(state, transformation, "**word**")
        state.textFieldState.text.toString() shouldBe "word"
        state.richString.runs(BoldKey).toList().shouldHaveSize(1)

        // Undo -> restores "**word**"
        state.undoState.undo()
        state.textFieldState.text.toString() shouldBe "**word**"
        state.richString.runs(BoldKey).toList().shouldBeEmpty()

        // Redo -> restores "word" with Bold
        state.undoState.redo()
        state.textFieldState.text.toString() shouldBe "word"
        state.richString.runs(BoldKey).toList().shouldHaveSize(1)

        // Navigate cursor to middle (offset 2)
        state.textFieldState.edit {
            selection = TextRange(2)
        }

        // Backspace inside word deletes 'o' -> "wrd"
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "wrd"
        state.selection shouldBe TextRange(1)

        // Bold attribute should still cover "wrd"
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..2
    }

    @Test
    fun `backspace reversal followed by immediate retype and trigger`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Trigger bold
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"

        // 2. Revert with Backspace -> "**bold**"
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "**bold**"

        // 3. Delete trailing asterisk -> "**bold*"
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "**bold*"

        // 4. Type asterisk again -> re-triggers bold!
        typeText(state, transformation, "*")
        state.textFieldState.text.toString() shouldBe "bold"
        state.richString.runs(BoldKey).toList().shouldHaveSize(1)
    }

    @Test
    fun `block reversal followed by immediate retype and trigger`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Trigger H2
        typeText(state, transformation, "## ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        // 2. Revert with Backspace -> "## "
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "## "

        // 3. Delete space -> "##"
        pressBackspace(state, wysiwygState, transformation)
        state.textFieldState.text.toString() shouldBe "##"

        // 4. Re-type space -> re-triggers H2!
        typeText(state, transformation, " ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        typeText(state, transformation, "Heading Text")
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H2
    }

    // =========================================================================
    // Group 5: False Positive Prevention Verification
    // =========================================================================

    @Test
    fun `url with query params and path underscores not formatted`() {
        val (state, _, transformation) = createEngine()

        val testUrl = "https://example.com/api_v2_users/get_profile?user_id=123"
        typeText(state, transformation, testUrl)

        state.textFieldState.text.toString() shouldBe testUrl
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()
        state.richString.runs(BoldKey).toList().shouldBeEmpty()
    }

    @Test
    fun `math expressions with multiplication not formatted`() {
        val (state, _, transformation) = createEngine()

        val expr = "total = (a * b) + (c * d * e)"
        typeText(state, transformation, expr)

        state.textFieldState.text.toString() shouldBe expr
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()
    }

    @Test
    fun `math comparison operators do not trigger blockquote`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "if (x > y && z > 0)")
        state.textFieldState.text.toString() shouldBe "if (x > y && z > 0)"
        state.richString.runs(BlockquoteKey).toList().shouldBeEmpty()
    }

    @Test
    fun `programming identifiers and constants not formatted`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "val DEFAULT_HTTP_TIMEOUT_MS = 5000")
        state.textFieldState.text.toString() shouldBe "val DEFAULT_HTTP_TIMEOUT_MS = 5000"
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()

        typeText(state, transformation, "\nval __internal_cache__ = null")
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()
    }

    @Test
    fun `unclosed and empty markers not formatted`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "*unclosed `code ~tilde **bold")
        state.textFieldState.text.toString() shouldBe "*unclosed `code ~tilde **bold"
        state.richString.runs(ItalicKey).toList().shouldBeEmpty()
        state.richString.runs(InlineCodeKey).toList().shouldBeEmpty()
        state.richString.runs(StrikethroughKey).toList().shouldBeEmpty()
        state.richString.runs(BoldKey).toList().shouldBeEmpty()

        // Empty markers
        typeText(state, transformation, "\n`` and **** and ~~~~")
        state.textFieldState.text.toString() shouldBe "*unclosed `code ~tilde **bold\n`` and **** and ~~~~"
        state.richString.runs(InlineCodeKey).toList().shouldBeEmpty()
        state.richString.runs(BoldKey).toList().shouldBeEmpty()
        state.richString.runs(StrikethroughKey).toList().shouldBeEmpty()
    }

    @Test
    fun `tilde file path and approximate symbol not formatted`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "~/config/settings.json")
        state.textFieldState.text.toString() shouldBe "~/config/settings.json"
        state.richString.runs(StrikethroughKey).toList().shouldBeEmpty()

        typeText(state, transformation, "\nPrice is ~ 100 USD")
        state.richString.runs(StrikethroughKey).toList().shouldBeEmpty()
    }
}
