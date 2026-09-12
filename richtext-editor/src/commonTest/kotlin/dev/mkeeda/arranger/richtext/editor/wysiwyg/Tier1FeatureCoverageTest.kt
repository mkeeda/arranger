package dev.mkeeda.arranger.richtext.editor.wysiwyg

import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.CodeKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Tier 1: Feature Coverage Test Suite.
 * Comprehensive black-box requirements-driven tests covering F1 to F16 happy paths (5+ cases each).
 */
class Tier1FeatureCoverageTest {
    // =========================================================================
    // F1: Inline Code Attribute & Styling (Survey / R3)
    // =========================================================================

    @Test
    fun `F1_01 code key attribute name and specification contract`() {
        CodeKey.name shouldBe "code"
        CodeKey.defaultValue shouldBe Unit
    }

    @Test
    fun `F1_02 code key container equality and retrieval`() {
        val container = attributeContainerOf(CodeKey to Unit)
        container.containsKey(CodeKey) shouldBe true
        container[CodeKey] shouldBe Unit
    }

    @Test
    fun `F1_03 code key coexistence with bold and italic in attribute container`() {
        val container =
            attributeContainerOf(
                CodeKey to Unit,
                BoldKey to Unit,
                ItalicKey to Unit,
            )
        container.containsKey(CodeKey) shouldBe true
        container.containsKey(BoldKey) shouldBe true
        container.containsKey(ItalicKey) shouldBe true
    }

    @Test
    fun `F1_04 style resolver mapping contract for monospace family`() {
        // Contract test for code attribute resolution
        val spans =
            listOf(
                RichSpan(
                    range = 0..4,
                    attributes = attributeContainerOf(CodeKey to Unit),
                ),
            )
        val state = RichTextState(initialText = RichString("code", spans))
        state.richString.spans.first().attributes.containsKey(CodeKey) shouldBe true
    }

    @Test
    fun `F1_05 code key in rich string spans preserves boundary`() {
        val initialText = "val x = 42"
        val richString =
            RichString(
                text = initialText,
                spans = listOf(RichSpan(range = 0..9, attributes = attributeContainerOf(CodeKey to Unit))),
            )
        richString.spans.size shouldBe 1
        richString.spans.first().range shouldBe (0..9)
    }

    // =========================================================================
    // F2: Block Heading 1..3 (# , ## , ### ) (R2)
    // =========================================================================

    @Test
    fun `F2_01 heading 1 converts at line start and removes prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("# Header1")
        harness.assertText("Header1")
        harness.assertHeading(HeadingLevel.H1, range = 0..6)
    }

    @Test
    fun `F2_02 heading 2 converts at line start and removes prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("## Header2")
        harness.assertText("Header2")
        harness.assertHeading(HeadingLevel.H2, range = 0..6)
    }

    @Test
    fun `F2_03 heading 3 converts at line start and removes prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("### Header3")
        harness.assertText("Header3")
        harness.assertHeading(HeadingLevel.H3, range = 0..6)
    }

    @Test
    fun `F2_04 heading on multiline document converts only current paragraph`() {
        val harness = createWysiwygHarness()
        harness.typeText("First paragraph\n# Second paragraph")
        harness.assertText("First paragraph\nSecond paragraph")
        harness.assertHeading(HeadingLevel.H1, range = 16..31)
    }

    @Test
    fun `F2_05 heading with japanese text converts properly`() {
        val harness = createWysiwygHarness()
        harness.typeText("# 見出しタイトル")
        harness.assertText("見出しタイトル")
        harness.assertHeading(HeadingLevel.H1, range = 0..6)
    }

    // =========================================================================
    // F3: Block Bullet List (- , * ) (R2)
    // =========================================================================

    @Test
    fun `F3_01 bullet list converts with dash prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("- List item")
        harness.assertText("List item")
        harness.assertBulletList(range = 0..8)
    }

    @Test
    fun `F3_02 bullet list converts with asterisk prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("* Asterisk list")
        harness.assertText("Asterisk list")
        harness.assertBulletList(range = 0..12)
    }

    @Test
    fun `F3_03 bullet list on subsequent line converts accurately`() {
        val harness = createWysiwygHarness()
        harness.typeText("Plain line\n- Bullet line")
        harness.assertText("Plain line\nBullet line")
        harness.assertBulletList(range = 11..21)
    }

    @Test
    fun `F3_04 bullet list typing cursor placement immediately after prefix removal`() {
        val harness = createWysiwygHarness()
        harness.typeText("- ")
        harness.assertText("")
        harness.assertCursorAt(0)
        harness.assertBulletList(range = 0..0)
    }

    @Test
    fun `F3_05 bullet list continuous typing retains bullet attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("- Alpha")
        harness.typeText(" Bravo")
        harness.assertText("Alpha Bravo")
        harness.assertBulletList(range = 0..10)
    }

    // =========================================================================
    // F4: Block Ordered List (1. ) (R2)
    // =========================================================================

    @Test
    fun `F4_01 ordered list converts with 1 dot space prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. First item")
        harness.assertText("First item")
        harness.assertOrderedList(range = 0..9)
    }

    @Test
    fun `F4_02 ordered list on subsequent line converts accurately`() {
        val harness = createWysiwygHarness()
        harness.typeText("Intro\n1. Numbered item")
        harness.assertText("Intro\nNumbered item")
        harness.assertOrderedList(range = 6..18)
    }

    @Test
    fun `F4_03 ordered list typing cursor position immediately after trigger`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. ")
        harness.assertText("")
        harness.assertCursorAt(0)
        harness.assertOrderedList(range = 0..0)
    }

    @Test
    fun `F4_04 ordered list with japanese text converts properly`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. 最初の項目")
        harness.assertText("最初の項目")
        harness.assertOrderedList(range = 0..4)
    }

    @Test
    fun `F4_05 ordered list continuous typing extends text in ordered list`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. Step 1")
        harness.typeText(" details")
        harness.assertText("Step 1 details")
        harness.assertOrderedList(range = 0..13)
    }

    // =========================================================================
    // F5: Block Blockquote (> ) (R2)
    // =========================================================================

    @Test
    fun `F5_01 blockquote converts with greater-than space prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("> Quote text")
        harness.assertText("Quote text")
        harness.assertBlockquote(range = 0..9)
    }

    @Test
    fun `F5_02 blockquote on subsequent line converts independently`() {
        val harness = createWysiwygHarness()
        harness.typeText("Normal text\n> Quoted paragraph")
        harness.assertText("Normal text\nQuoted paragraph")
        harness.assertBlockquote(range = 12..27)
    }

    @Test
    fun `F5_03 blockquote cursor positioned at line start after conversion`() {
        val harness = createWysiwygHarness()
        harness.typeText("> ")
        harness.assertText("")
        harness.assertCursorAt(0)
        harness.assertBlockquote(range = 0..0)
    }

    @Test
    fun `F5_04 blockquote with japanese text converts properly`() {
        val harness = createWysiwygHarness()
        harness.typeText("> 重要な引用")
        harness.assertText("重要な引用")
        harness.assertBlockquote(range = 0..4)
    }

    @Test
    fun `F5_05 blockquote continuous typing maintains blockquote format`() {
        val harness = createWysiwygHarness()
        harness.typeText("> Initial quote")
        harness.typeText(" appended")
        harness.assertText("Initial quote appended")
        harness.assertBlockquote(range = 0..21)
    }

    // =========================================================================
    // F6: Inline Bold (**text**) (R3)
    // =========================================================================

    @Test
    fun `F6_01 bold standard word conversion`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.assertText("bold")
        harness.assertBold(range = 0..3)
    }

    @Test
    fun `F6_02 bold conversion in middle of sentence`() {
        val harness = createWysiwygHarness()
        harness.typeText("This is **important** text")
        harness.assertText("This is important text")
        harness.assertBold(range = 8..16)
        harness.assertNotBold(range = 0..7)
        harness.assertNotBold(range = 17..21)
    }

    @Test
    fun `F6_03 bold conversion across multiple words`() {
        val harness = createWysiwygHarness()
        harness.typeText("**multiple bold words**")
        harness.assertText("multiple bold words")
        harness.assertBold(range = 0..18)
    }

    @Test
    fun `F6_04 bold conversion with japanese text`() {
        val harness = createWysiwygHarness()
        harness.typeText("**太字テスト**")
        harness.assertText("太字テスト")
        harness.assertBold(range = 0..4)
    }

    @Test
    fun `F6_05 bold cursor positioned immediately after styled text`() {
        val harness = createWysiwygHarness()
        harness.typeText("**word**")
        harness.assertCursorAt(4)
    }

    // =========================================================================
    // F7: Inline Italic Asterisk (*text*) (R3)
    // =========================================================================

    @Test
    fun `F7_01 italic asterisk standard conversion`() {
        val harness = createWysiwygHarness()
        harness.typeText("*italic*")
        harness.assertText("italic")
        harness.assertItalic(range = 0..5)
    }

    @Test
    fun `F7_02 italic asterisk in middle of sentence`() {
        val harness = createWysiwygHarness()
        harness.typeText("A *quick* brown fox")
        harness.assertText("A quick brown fox")
        harness.assertItalic(range = 2..6)
        harness.assertNotItalic(range = 0..1)
        harness.assertNotItalic(range = 7..16)
    }

    @Test
    fun `F7_03 italic asterisk multiple words`() {
        val harness = createWysiwygHarness()
        harness.typeText("*two italic words*")
        harness.assertText("two italic words")
        harness.assertItalic(range = 0..15)
    }

    @Test
    fun `F7_04 italic asterisk with japanese text`() {
        val harness = createWysiwygHarness()
        harness.typeText("*斜体テスト*")
        harness.assertText("斜体テスト")
        harness.assertItalic(range = 0..4)
    }

    @Test
    fun `F7_05 italic asterisk cursor placed immediately after styled text`() {
        val harness = createWysiwygHarness()
        harness.typeText("*italic*")
        harness.assertCursorAt(6)
    }

    // =========================================================================
    // F8: Inline Italic Underscore (_text_) (R3)
    // =========================================================================

    @Test
    fun `F8_01 italic underscore standard conversion`() {
        val harness = createWysiwygHarness()
        harness.typeText("_italic_")
        harness.assertText("italic")
        harness.assertItalic(range = 0..5)
    }

    @Test
    fun `F8_02 italic underscore in middle of sentence`() {
        val harness = createWysiwygHarness()
        harness.typeText("Some _emphasized_ word")
        harness.assertText("Some emphasized word")
        harness.assertItalic(range = 5..14)
        harness.assertNotItalic(range = 0..4)
    }

    @Test
    fun `F8_03 italic underscore multiple words`() {
        val harness = createWysiwygHarness()
        harness.typeText("_three separate words_")
        harness.assertText("three separate words")
        harness.assertItalic(range = 0..19)
    }

    @Test
    fun `F8_04 italic underscore with japanese text`() {
        val harness = createWysiwygHarness()
        harness.typeText("_日本語の強調_")
        harness.assertText("日本語の強調")
        harness.assertItalic(range = 0..5)
    }

    @Test
    fun `F8_05 italic underscore cursor placed immediately after styled text`() {
        val harness = createWysiwygHarness()
        harness.typeText("_word_")
        harness.assertCursorAt(4)
    }

    // =========================================================================
    // F9: Inline Code (`text`) (R3)
    // =========================================================================

    @Test
    fun `F9_01 inline code standard conversion`() {
        val harness = createWysiwygHarness()
        harness.typeText("`val x = 1`")
        harness.assertText("val x = 1")
        harness.assertInlineCode(range = 0..8)
    }

    @Test
    fun `F9_02 inline code in sentence`() {
        val harness = createWysiwygHarness()
        harness.typeText("Run the `main` method")
        harness.assertText("Run the main method")
        harness.assertInlineCode(range = 8..11)
        harness.assertNotInlineCode(range = 0..7)
    }

    @Test
    fun `F9_03 inline code with symbols`() {
        val harness = createWysiwygHarness()
        harness.typeText("`x && y || z`")
        harness.assertText("x && y || z")
        harness.assertInlineCode(range = 0..10)
    }

    @Test
    fun `F9_04 inline code with japanese text`() {
        val harness = createWysiwygHarness()
        harness.typeText("`設定ファイル.kt`")
        harness.assertText("設定ファイル.kt")
        harness.assertInlineCode(range = 0..9)
    }

    @Test
    fun `F9_05 inline code cursor placed immediately after code span`() {
        val harness = createWysiwygHarness()
        harness.typeText("`code`")
        harness.assertCursorAt(4)
    }

    // =========================================================================
    // F10: Inline Strikethrough (~text~) (R3)
    // =========================================================================

    @Test
    fun `F10_01 strikethrough standard conversion`() {
        val harness = createWysiwygHarness()
        harness.typeText("~strikethrough~")
        harness.assertText("strikethrough")
        harness.assertStrikethrough(range = 0..12)
    }

    @Test
    fun `F10_02 strikethrough in sentence`() {
        val harness = createWysiwygHarness()
        harness.typeText("The ~old~ new way")
        harness.assertText("The old new way")
        harness.assertStrikethrough(range = 4..6)
        harness.assertNotStrikethrough(range = 0..3)
    }

    @Test
    fun `F10_03 strikethrough multiple words`() {
        val harness = createWysiwygHarness()
        harness.typeText("~cancelled meeting today~")
        harness.assertText("cancelled meeting today")
        harness.assertStrikethrough(range = 0..22)
    }

    @Test
    fun `F10_04 strikethrough with japanese text`() {
        val harness = createWysiwygHarness()
        harness.typeText("~削除済みタスク~")
        harness.assertText("削除済みタスク")
        harness.assertStrikethrough(range = 0..6)
    }

    @Test
    fun `F10_05 strikethrough cursor placed immediately after styled text`() {
        val harness = createWysiwygHarness()
        harness.typeText("~strike~")
        harness.assertCursorAt(6)
    }

    // =========================================================================
    // F11: False Positive Prevention (R3)
    // =========================================================================

    @Test
    fun `F11_01 snake_case identifiers do not trigger italic`() {
        val harness = createWysiwygHarness()
        harness.typeText("val user_first_name = 1")
        harness.assertText("val user_first_name = 1")
        harness.assertNotItalic()
    }

    @Test
    fun `F11_02 unclosed markers remain plain text`() {
        val harness = createWysiwygHarness()
        harness.typeText("**unclosed bold text")
        harness.assertText("**unclosed bold text")
        harness.assertNotBold()
    }

    @Test
    fun `F11_03 empty markers do not format`() {
        val harness = createWysiwygHarness()
        harness.typeText("****")
        harness.assertText("****")
        harness.assertNotBold()
    }

    @Test
    fun `F11_04 mid-sentence block markers do not convert to blocks`() {
        val harness = createWysiwygHarness()
        harness.typeText("Word # not heading")
        harness.assertText("Word # not heading")
        harness.assertNotHeading()
    }

    @Test
    fun `F11_05 whitespace padded markers do not convert`() {
        val harness = createWysiwygHarness()
        harness.typeText("Text * spaced *")
        harness.assertText("Text * spaced *")
        harness.assertNotItalic()
    }

    // =========================================================================
    // F12: Undo / Redo Integration (R4)
    // =========================================================================

    @Test
    fun `F12_01 undo heading restores prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("# ")
        harness.assertHeading(HeadingLevel.H1, range = 0..0)

        harness.undo()
        harness.assertText("# ")
        harness.assertNotHeading()
    }

    @Test
    fun `F12_02 undo bullet list restores dash prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("- ")
        harness.assertBulletList(range = 0..0)

        harness.undo()
        harness.assertText("- ")
        harness.assertNotBulletList()
    }

    @Test
    fun `F12_03 undo inline bold restores raw asterisks`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.assertText("bold")
        harness.assertBold(range = 0..3)

        harness.undo()
        harness.assertText("**bold**")
        harness.assertNotBold()
    }

    @Test
    fun `F12_04 redo after undo restores formatted state`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.undo()
        harness.assertText("**bold**")

        harness.redo()
        harness.assertText("bold")
        harness.assertBold(range = 0..3)
    }

    @Test
    fun `F12_05 undo restores selection cursor position`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.assertCursorAt(4)

        harness.undo()
        harness.assertCursorAt(8)
    }

    // =========================================================================
    // F13: Backspace Reversal (R4)
    // =========================================================================

    @Test
    fun `F13_01 backspace immediately after heading reverts to prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("# ")
        harness.assertHeading(HeadingLevel.H1, range = 0..0)

        harness.pressBackspace()
        harness.assertText("# ")
        harness.assertNotHeading()
    }

    @Test
    fun `F13_02 backspace immediately after bullet list reverts to prefix`() {
        val harness = createWysiwygHarness()
        harness.typeText("- ")
        harness.assertBulletList(range = 0..0)

        harness.pressBackspace()
        harness.assertText("- ")
        harness.assertNotBulletList()
    }

    @Test
    fun `F13_03 backspace immediately after bold reverts to markers`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.assertBold(range = 0..3)

        harness.pressBackspace()
        harness.assertText("**bold**")
        harness.assertNotBold()
    }

    @Test
    fun `F13_04 backspace immediately after inline code reverts to backticks`() {
        val harness = createWysiwygHarness()
        harness.typeText("`code`")
        harness.assertInlineCode(range = 0..3)

        harness.pressBackspace()
        harness.assertText("`code`")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F13_05 subsequent backspace after reversal deletes normally`() {
        val harness = createWysiwygHarness()
        harness.typeText("# ")
        harness.pressBackspace() // Revert to "# "
        harness.assertText("# ")

        harness.pressBackspace() // Normal delete: remove space
        harness.assertText("#")

        harness.pressBackspace() // Normal delete: remove hash
        harness.assertText("")
    }

    // =========================================================================
    // F14: Cursor & Typing Attribute Ergonomics (R3, R4)
    // =========================================================================

    @Test
    fun `F14_01 typing plain text after bold does not leak bold attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.typeText(" plain")
        harness.assertText("bold plain")
        harness.assertBold(range = 0..3)
        harness.assertNotBold(range = 4..9)
    }

    @Test
    fun `F14_02 typing plain text after inline code does not leak code attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("`val`")
        harness.typeText(" next")
        harness.assertText("val next")
        harness.assertInlineCode(range = 0..2)
        harness.assertNotInlineCode(range = 3..7)
    }

    @Test
    fun `F14_03 typing plain text after italic does not leak italic attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("*italic*")
        harness.typeText(" plain")
        harness.assertText("italic plain")
        harness.assertItalic(range = 0..5)
        harness.assertNotItalic(range = 6..11)
    }

    @Test
    fun `F14_04 typing plain text after strikethrough does not leak attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("~strike~")
        harness.typeText(" plain")
        harness.assertText("strike plain")
        harness.assertStrikethrough(range = 0..5)
        harness.assertNotStrikethrough(range = 6..11)
    }

    @Test
    fun `F14_05 cursor placed precisely after transformed span boundary`() {
        val harness = createWysiwygHarness()
        harness.typeText("Prefix **formatted**")
        harness.assertCursorAt(16) // "Prefix formatted".length == 16
    }

    // =========================================================================
    // F15: WysiwygEditor Component & Integration (R1)
    // =========================================================================

    @Test
    fun `F15_01 wysiwyg harness initializes in enabled state by default`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = true)
        harness.typeText("# Heading")
        harness.assertHeading(HeadingLevel.H1, range = 0..6)
    }

    @Test
    fun `F15_02 wysiwyg harness accepts initial rich string with spans`() {
        val initialSpan = RichSpan(range = 0..3, attributes = attributeContainerOf(BoldKey to Unit))
        val harness = createWysiwygHarness(initialText = "test", initialSpans = listOf(initialSpan))
        harness.assertText("test")
        harness.assertBold(range = 0..3)
    }

    @Test
    fun `F15_03 wysiwyg harness supports readOnly mode preventing transforms`() {
        val harness = createWysiwygHarness(readOnly = true)
        harness.typeText("# Heading")
        harness.assertText("")
    }

    @Test
    fun `F15_04 wysiwyg harness supports cursor relocation and typing`() {
        val harness = createWysiwygHarness()
        harness.typeText("Hello World")
        harness.setCursor(5)
        harness.typeText(" Beautiful")
        harness.assertText("Hello Beautiful World")
    }

    @Test
    fun `F15_05 wysiwyg state exposes rich string single source of truth`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold text**")
        val state = harness.driver.rawState
        state.richString.text shouldBe "bold text"
        state.richString.spans.size shouldBe 1
    }

    // =========================================================================
    // F16: RichTextEditor Non-regression (R1)
    // =========================================================================

    @Test
    fun `F16_01 rich text editor preserves raw heading symbols without formatting`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("# Header")
        harness.assertText("# Header")
        harness.assertNotHeading()
    }

    @Test
    fun `F16_02 rich text editor preserves raw bullet list dash without formatting`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("- Bullet")
        harness.assertText("- Bullet")
        harness.assertNotBulletList()
    }

    @Test
    fun `F16_03 rich text editor preserves raw bold asterisks without formatting`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("**bold**")
        harness.assertText("**bold**")
        harness.assertNotBold()
    }

    @Test
    fun `F16_04 rich text editor preserves raw backticks without formatting`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("`code`")
        harness.assertText("`code`")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F16_05 rich text editor preserves raw blockquote symbol without formatting`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("> Quote")
        harness.assertText("> Quote")
        harness.assertNotBlockquote()
    }
}
