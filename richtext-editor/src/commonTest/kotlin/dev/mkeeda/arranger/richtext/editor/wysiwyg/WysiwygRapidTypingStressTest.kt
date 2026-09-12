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
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/**
 * Stress test suite for rapid typing, multi-paragraph input, and stack consistency under auto-formatting.
 */
class WysiwygRapidTypingStressTest {
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
    // 1. Consecutive format application on same line (heading + bold + inline code)
    // =========================================================================

    @Test
    fun `same line heading and bold and code`() {
        val (state, _, transformation) = createEngine()

        // 1. Type # at line start -> Heading 1
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""

        // 2. Type text "Hello "
        typeText(state, transformation, "Hello ")
        state.textFieldState.text.toString() shouldBe "Hello "

        // 3. Type bold "**bold**"
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "Hello bold"

        // 4. Type " and "
        typeText(state, transformation, " and ")
        state.textFieldState.text.toString() shouldBe "Hello bold and "

        // 5. Type inline code "`code`"
        typeText(state, transformation, "`code`")
        state.textFieldState.text.toString() shouldBe "Hello bold and code"

        // Assertions:
        // - Entire text is Heading 1
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1
        headingRuns[0].range.first shouldBe 0
        (headingRuns[0].range.last >= 18) shouldBe true

        // - "bold" (range 6..9) is Bold
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 6..9

        // - "code" (range 15..18) is InlineCode
        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 15..18
    }

    @Test
    fun `empty heading then immediate bold`() {
        val (state, _, transformation) = createEngine()

        // Type # at line start -> empty Heading 1
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""

        // Immediately type **bold** on empty line
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"

        // Type subsequent text " text"
        typeText(state, transformation, " text")
        state.textFieldState.text.toString() shouldBe "bold text"

        // Assertions: 段落全体が Heading 1 を維持しているか？
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1
        headingRuns[0].range.first shouldBe 0
        (headingRuns[0].range.last >= 8) shouldBe true

        // Only "bold" (0..3) is Bold
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..3
    }

    @Test
    fun `existing text turned into heading then inline formatting`() {
        val (state, _, transformation) = createEngine()

        // Type initial text "Title with star" without initial space
        typeText(state, transformation, "Title with star")

        // Move cursor to start of line
        state.textFieldState.edit {
            selection = TextRange(0)
        }

        // Type "# " at start of line
        typeText(state, transformation, "# ")

        // Transformed to Heading 1 and prefix "# " is removed
        state.textFieldState.text.toString() shouldBe "Title with star"
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1

        // Move cursor to end of text
        state.textFieldState.edit {
            selection = TextRange(state.textFieldState.text.length)
        }

        // Type " and `code`" at end of line
        typeText(state, transformation, " and `code`")
        state.textFieldState.text.toString() shouldBe "Title with star and code"

        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 20..23
    }

    // =========================================================================
    // 2. Rapid typing simulation across multiple lines
    // =========================================================================

    @Test
    fun `multiline rapid typing simulation`() {
        val (state, _, transformation) = createEngine()

        // Line 1: Heading 1 + Bold
        typeText(state, transformation, "# First Line with **Bold**\n")

        // Line 2: Bullet List + Inline Code
        typeText(state, transformation, "- Item with `Code` inside\n")

        // Line 3: Ordered List + Italic
        typeText(state, transformation, "1. Ordered with *Italic*\n")

        // Line 4: Blockquote + Strikethrough
        typeText(state, transformation, "> Quote with ~Strike~")

        println("ACTUAL TEXT:\n" + state.textFieldState.text.toString())
        for (span in state.richString.spans) {
            println("SPAN: ${span.range} -> ${span.attributes.keys.map { it.name }}")
        }

        println("HEADING RUNS: ${state.richString.runs(HeadingKey).toList()}")
        println("BULLET RUNS: ${state.richString.runs(BulletListKey).toList()}")
        println("ORDERED RUNS: ${state.richString.runs(OrderedListKey).toList()}")
        println("QUOTE RUNS: ${state.richString.runs(BlockquoteKey).toList()}")
        println("BOLD RUNS: ${state.richString.runs(BoldKey).toList()}")
        println("CODE RUNS: ${state.richString.runs(InlineCodeKey).toList()}")
        println("ITALIC RUNS: ${state.richString.runs(ItalicKey).toList()}")
        println("STRIKE RUNS: ${state.richString.runs(StrikethroughKey).toList()}")

        val expectedText =
            "First Line with Bold\n" +
                "Item with Code inside\n" +
                "Ordered with Italic\n" +
                "Quote with Strike"

        state.textFieldState.text.toString() shouldBe expectedText

        // Verify paragraph attributes for each line
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1

        val bulletRuns = state.richString.runs(BulletListKey).toList()
        bulletRuns.size shouldBe 1
        bulletRuns[0].value shouldBe ListIndentLevel.Level1

        val orderedRuns = state.richString.runs(OrderedListKey).toList()
        orderedRuns.size shouldBe 1
        orderedRuns[0].value shouldBe ListIndentLevel.Level1

        val quoteRuns = state.richString.runs(BlockquoteKey).toList()
        quoteRuns.size shouldBe 1

        // Verify inline attributes for each line
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1

        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        println("CODE RUNS: " + codeRuns)
        codeRuns.size shouldBe 1

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
    }

    @Test
    fun `list to ordered list transition bug`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "- Item 1\n")
        println("=== AFTER '- Item 1\\n' ===")
        println("TEXT: '${state.textFieldState.text}'")
        for (span in state.richString.spans) {
            println("SPAN: ${span.range} -> ${span.attributes.keys.map { it.name }}")
        }
        println("TYPING ATTRS: ${state.typingAttributes?.keys?.map { it.name }}")

        typeText(state, transformation, "1. ")
        println("=== AFTER '1. ' ===")
        println("TEXT: '${state.textFieldState.text}'")
        for (span in state.richString.spans) {
            println("SPAN: ${span.range} -> ${span.attributes.keys.map { it.name }}")
        }
        println("TYPING ATTRS: ${state.typingAttributes?.keys?.map { it.name }}")

        typeText(state, transformation, "Item 2")
        println("=== AFTER 'Item 2' ===")
        println("TEXT: '${state.textFieldState.text}'")
        for (span in state.richString.spans) {
            println("SPAN: ${span.range} -> ${span.attributes.keys.map { it.name }}")
        }

        val orderedRuns = state.richString.runs(OrderedListKey).toList()
        orderedRuns.size shouldBe 1
        orderedRuns[0].range shouldBe 7..13
    }

    @Test
    fun `heading to bullet list transition`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "# Heading 1\n")
        typeText(state, transformation, "- List 1")

        println("=== HEADING TO BULLET ===")
        println("TEXT: '${state.textFieldState.text}'")
        for (span in state.richString.spans) {
            println("SPAN: ${span.range} -> ${span.attributes.keys.map { it.name }}")
        }

        val headingRuns = state.richString.runs(HeadingKey).toList()
        println("HEADING RUNS: $headingRuns")
        val bulletRuns = state.richString.runs(BulletListKey).toList()
        println("BULLET RUNS: $bulletRuns")

        headingRuns.size shouldBe 1
        bulletRuns.size shouldBe 1
    }

    @Test
    fun `bullet list to heading transition`() {
        val (state, _, transformation) = createEngine()

        typeText(state, transformation, "- List 1\n")
        typeText(state, transformation, "# Heading 2")

        println("=== BULLET TO HEADING ===")
        println("TEXT: '${state.textFieldState.text}'")
        for (span in state.richString.spans) {
            println("SPAN: ${span.range} -> ${span.attributes.keys.map { it.name }}")
        }

        val bulletRuns = state.richString.runs(BulletListKey).toList()
        println("BULLET RUNS: $bulletRuns")
        val headingRuns = state.richString.runs(HeadingKey).toList()
        println("HEADING RUNS: $headingRuns")

        bulletRuns.size shouldBe 1
        headingRuns.size shouldBe 1
    }

    // =========================================================================
    // 3. Stack consistency and cursor restoration during repeated Undo/Redo
    // =========================================================================

    @Test
    fun `undo redo stack consistency and cursor restoration`() {
        val (state, _, transformation) = createEngine()

        // 1. Type: "**hello**"
        typeText(state, transformation, "**hello**")
        state.textFieldState.text.toString() shouldBe "hello"
        state.selection shouldBe TextRange(5)
        state.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) } shouldNotBe null

        println("Undo stack size before undo: canUndo=${state.undoState.canUndo}")

        // 2. 1st Undo -> State B ("**hello**", selection: 9)
        state.undoState.undo()
        println(
            "After 1st undo: text='${state.textFieldState.text}', canUndo=${state.undoState.canUndo}, canRedo=${state.undoState.canRedo}",
        )
        state.textFieldState.text.toString() shouldBe "**hello**"
        state.richString.spans.none { it.attributes.containsKey(BoldKey) } shouldBe true

        // 3. Redo -> State C ("hello", selection: 5, Bold)
        state.undoState.redo()
        println("After redo: text='${state.textFieldState.text}', canUndo=${state.undoState.canUndo}, canRedo=${state.undoState.canRedo}")
        state.textFieldState.text.toString() shouldBe "hello"
        state.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) } shouldNotBe null

        // 4. Execute Undo (State C -> State B)
        state.undoState.undo()
        println(
            "After undo to State B: text='${state.textFieldState.text}', canUndo=${state.undoState.canUndo}, canRedo=${state.undoState.canRedo}",
        )
        state.textFieldState.text.toString() shouldBe "**hello**"
    }

    // =========================================================================
    // 4. Boundary behavior between Backspace reversal and normal deletion
    // =========================================================================

    @Test
    fun `backspace reversal then normal backspace`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Type "# " to trigger heading conversion
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        // 2. Immediately press Backspace -> restore State B ("# ")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "# "
        state.selection shouldBe TextRange(2)
        wysiwygState.canRevert(state) shouldBe false

        // 3. Press Backspace again -> normal deletion removes trailing space ("#")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "#"
        state.selection shouldBe TextRange(1)

        // 4. Press Backspace again -> removes "#" ("")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `backspace inline reversal then normal backspace`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Type "**bold**"
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)
        wysiwygState.canRevert(state) shouldBe true

        // 2. Backspace -> restore raw symbol text "**bold**"
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "**bold**"
        state.selection shouldBe TextRange(8)
        wysiwygState.canRevert(state) shouldBe false

        // 3. 2nd Backspace -> normal deletion deletes trailing "*" ("**bold*")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "**bold*"
        state.selection shouldBe TextRange(7)
    }

    @Test
    fun `backspace after cursor move does not revert autoformat`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Type "**bold**"
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        wysiwygState.canRevert(state) shouldBe true

        // 2. Move cursor inside text (offset 2)
        state.textFieldState.edit {
            selection = TextRange(2)
        }

        // 3. After cursor moves, canRevert must be false
        wysiwygState.canRevert(state) shouldBe false

        // 4. Pressing Backspace performs normal deletion ("bld")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bld"
        state.selection shouldBe TextRange(1)
    }

    @Test
    fun `backspace after additional typing does not revert autoformat`() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. Type "**bold**"
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        wysiwygState.canRevert(state) shouldBe true

        // 2. Continuously type character "!"
        typeText(state, transformation, "!")
        state.textFieldState.text.toString() shouldBe "bold!"
        wysiwygState.canRevert(state) shouldBe false

        // 3. Pressing Backspace normally deletes "!"
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)
    }

    // =========================================================================
    // 5. Adversarial edge cases (Multibyte, Single Char, Escapes, Asterisk Nesting)
    // =========================================================================

    @Test
    fun `japanese multibyte inline formatting`() {
        val (state, _, transformation) = createEngine()

        // Multibyte bold text "**日本語**"
        typeText(state, transformation, "**日本語**")
        state.textFieldState.text.toString() shouldBe "日本語"
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..2

        // Multibyte inline code "`コード`"
        typeText(state, transformation, "と`コード`")
        state.textFieldState.text.toString() shouldBe "日本語とコード"
        val codeRuns = state.richString.runs(InlineCodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 4..6
    }

    @Test
    fun `single character inline formatting`() {
        val (state, _, transformation) = createEngine()

        // Single-character formatting: "*a*"
        typeText(state, transformation, "*a*")
        state.textFieldState.text.toString() shouldBe "a"
        state.selection shouldBe TextRange(1)
        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 0..0
    }
}
