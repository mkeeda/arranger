package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.delete
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

class WysiwygChallengerStressTest {
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
    // 1. 同一行での連続フォーマット適用（見出し + 太字 + インラインコード）
    // =========================================================================

    @Test
    fun test_same_line_heading_and_bold_and_code() {
        val (state, _, transformation) = createEngine()

        // 1. 行頭で # を入力 -> 見出し1
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""

        // 2. テキスト "Hello " を入力
        typeText(state, transformation, "Hello ")
        state.textFieldState.text.toString() shouldBe "Hello "

        // 3. 太字 "**bold**" を入力
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "Hello bold"

        // 4. " and " を入力
        typeText(state, transformation, " and ")
        state.textFieldState.text.toString() shouldBe "Hello bold and "

        // 5. インラインコード "`code`" を入力
        typeText(state, transformation, "`code`")
        state.textFieldState.text.toString() shouldBe "Hello bold and code"

        // 検証:
        // - 全体が Heading 1 であること
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1
        headingRuns[0].range.first shouldBe 0
        (headingRuns[0].range.last >= 18) shouldBe true

        // - "bold" (range 6..9) が Bold であること
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 6..9

        // - "code" (range 15..18) が Code であること
        val codeRuns = state.richString.runs(CodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 15..18
    }

    @Test
    fun test_empty_heading_then_immediate_bold() {
        val (state, _, transformation) = createEngine()

        // 行頭で # を入力 -> 空行見出し1
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""

        // 空行の直後に **bold** を入力
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"

        // 後続テキスト " text" を入力
        typeText(state, transformation, " text")
        state.textFieldState.text.toString() shouldBe "bold text"

        // 検証: 段落全体が Heading 1 を維持しているか？
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1
        headingRuns[0].range.first shouldBe 0
        (headingRuns[0].range.last >= 8) shouldBe true

        // "bold" (0..3) のみ Bold であること
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..3
    }

    @Test
    fun test_existing_text_turned_into_heading_then_inline_formatting() {
        val (state, _, transformation) = createEngine()

        // 既存テキスト "Title with **star**" をまず普通に入力（先頭にスペースなし）
        typeText(state, transformation, "Title with star")

        // カーソルを行頭に移動
        state.textFieldState.edit {
            selection = TextRange(0)
        }

        // 行頭に "# " を入力
        typeText(state, transformation, "# ")

        // 見出し1になり、プレフィックス "# " が消去されていること
        state.textFieldState.text.toString() shouldBe "Title with star"
        val headingRuns = state.richString.runs(HeadingKey).toList()
        headingRuns.size shouldBe 1
        headingRuns[0].value shouldBe HeadingLevel.H1

        // カーソルを末尾へ移動
        state.textFieldState.edit {
            selection = TextRange(state.textFieldState.text.length)
        }

        // 末尾に " and `code`" を入力
        typeText(state, transformation, " and `code`")
        state.textFieldState.text.toString() shouldBe "Title with star and code"

        val codeRuns = state.richString.runs(CodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 20..23
    }

    // =========================================================================
    // 2. 複数行にわたる高速タイピングシミュレーション
    // =========================================================================

    @Test
    fun test_multiline_rapid_typing_simulation() {
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
        println("CODE RUNS: ${state.richString.runs(CodeKey).toList()}")
        println("ITALIC RUNS: ${state.richString.runs(ItalicKey).toList()}")
        println("STRIKE RUNS: ${state.richString.runs(StrikethroughKey).toList()}")

        val expectedText =
            "First Line with Bold\n" +
                "Item with Code inside\n" +
                "Ordered with Italic\n" +
                "Quote with Strike"

        state.textFieldState.text.toString() shouldBe expectedText

        // 各行の段落属性を検証
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

        // 各行のインライン属性を検証
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1

        val codeRuns = state.richString.runs(CodeKey).toList()
        println("CODE RUNS: " + codeRuns)
        codeRuns.size shouldBe 1

        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
    }

    @Test
    fun test_list_to_ordered_list_transition_bug() {
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
    fun test_heading_to_bullet_list_transition() {
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
    fun test_bullet_list_to_heading_transition() {
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
    // 3. Undo/Redo を繰り返した際のスタック整合性とカーソル復元
    // =========================================================================

    @Test
    fun test_undo_redo_stack_consistency_and_cursor_restoration() {
        val (state, _, transformation) = createEngine()

        // 1. タイピング: "**hello**"
        typeText(state, transformation, "**hello**")
        state.textFieldState.text.toString() shouldBe "hello"
        state.selection shouldBe TextRange(5)
        state.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) } shouldNotBe null

        println("Undo stack size before undo: canUndo=${state.undoState.canUndo}")

        // 2. 1回目の Undo -> State B ("**hello**", selection: 9)
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

        // 4. Undo を実行 (State C -> State B)
        state.undoState.undo()
        println(
            "After undo to State B: text='${state.textFieldState.text}', canUndo=${state.undoState.canUndo}, canRedo=${state.undoState.canRedo}",
        )
        state.textFieldState.text.toString() shouldBe "**hello**"
    }

    // =========================================================================
    // 4. Backspace 巻き戻しと通常削除の境界動作
    // =========================================================================

    @Test
    fun test_backspace_reversal_then_normal_backspace() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. "# " を入力して見出し変換発動
        typeText(state, transformation, "# ")
        state.textFieldState.text.toString() shouldBe ""
        wysiwygState.canRevert(state) shouldBe true

        // 2. 直後に Backspace を押下 -> State B ("# ") へ復元
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "# "
        state.selection shouldBe TextRange(2)
        wysiwygState.canRevert(state) shouldBe false

        // 3. もう一度 Backspace を押下 -> 通常削除で末尾スペースが消える ("#")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "#"
        state.selection shouldBe TextRange(1)

        // 4. もう一度 Backspace を押下 -> "#" が消える ("")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe ""
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun test_backspace_inline_reversal_then_normal_backspace() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. "**bold**" を入力
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)
        wysiwygState.canRevert(state) shouldBe true

        // 2. Backspace -> 生記号テキスト "**bold**" に復元
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "**bold**"
        state.selection shouldBe TextRange(8)
        wysiwygState.canRevert(state) shouldBe false

        // 3. 2回目の Backspace -> 通常削除で末尾の "*" が削除される ("**bold*")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "**bold*"
        state.selection shouldBe TextRange(7)
    }

    @Test
    fun test_backspace_after_cursor_move_does_not_revert_autoformat() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. "**bold**" を入力
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        wysiwygState.canRevert(state) shouldBe true

        // 2. カーソルを途中に移動 (offset 2)
        state.textFieldState.edit {
            selection = TextRange(2)
        }

        // 3. カーソル移動後は canRevert が false になること
        wysiwygState.canRevert(state) shouldBe false

        // 4. Backspace を押すと通常削除が行われる ("bld")
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bld"
        state.selection shouldBe TextRange(1)
    }

    @Test
    fun test_backspace_after_additional_typing_does_not_revert_autoformat() {
        val (state, wysiwygState, transformation) = createEngine()

        // 1. "**bold**" を入力
        typeText(state, transformation, "**bold**")
        state.textFieldState.text.toString() shouldBe "bold"
        wysiwygState.canRevert(state) shouldBe true

        // 2. 続けて文字 "!" を入力
        typeText(state, transformation, "!")
        state.textFieldState.text.toString() shouldBe "bold!"
        wysiwygState.canRevert(state) shouldBe false

        // 3. Backspace を押すと "!" が通常削除される
        pressBackspace(state, wysiwygState)
        state.textFieldState.text.toString() shouldBe "bold"
        state.selection shouldBe TextRange(4)
    }

    // =========================================================================
    // 5. 敵対的極限ケース (Japanese, Single Char, Escapes, Asterisk Nesting)
    // =========================================================================

    @Test
    fun test_japanese_multibyte_inline_formatting() {
        val (state, _, transformation) = createEngine()

        // 日本語テキストの太字 "**日本語**"
        typeText(state, transformation, "**日本語**")
        state.textFieldState.text.toString() shouldBe "日本語"
        val boldRuns = state.richString.runs(BoldKey).toList()
        boldRuns.size shouldBe 1
        boldRuns[0].range shouldBe 0..2

        // 日本語テキストのインラインコード "`コード`"
        typeText(state, transformation, "と`コード`")
        state.textFieldState.text.toString() shouldBe "日本語とコード"
        val codeRuns = state.richString.runs(CodeKey).toList()
        codeRuns.size shouldBe 1
        codeRuns[0].range shouldBe 4..6
    }

    @Test
    fun test_single_character_inline_formatting() {
        val (state, _, transformation) = createEngine()

        // 1文字だけの装飾: "*a*"
        typeText(state, transformation, "*a*")
        state.textFieldState.text.toString() shouldBe "a"
        state.selection shouldBe TextRange(1)
        val italicRuns = state.richString.runs(ItalicKey).toList()
        italicRuns.size shouldBe 1
        italicRuns[0].range shouldBe 0..0
    }
}
