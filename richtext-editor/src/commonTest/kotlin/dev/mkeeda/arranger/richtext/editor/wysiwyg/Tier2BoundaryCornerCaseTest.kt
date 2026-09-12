package dev.mkeeda.arranger.richtext.editor.wysiwyg

import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.attributeContainerOf
import kotlin.test.Test

/**
 * Tier 2: Boundary & Corner Cases Test Suite.
 * Comprehensive black-box tests for boundary conditions, edge cases, escaping,
 * and false-positive prevention across F1 to F16 (5+ cases each, 80+ total).
 */
class Tier2BoundaryCornerCaseTest {
    // =========================================================================
    // F1: Inline Code Boundary (R3)
    // =========================================================================

    @Test
    fun `F1_B01 multiline inline code prevented`() {
        val harness = createWysiwygHarness()
        harness.typeText("`line1\nline2`")
        harness.assertText("`line1\nline2`")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F1_B02 consecutive backticks do not crash or erroneously format`() {
        val harness = createWysiwygHarness()
        harness.typeText("````")
        harness.assertText("````")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F1_B03 empty backticks prevented`() {
        val harness = createWysiwygHarness()
        harness.typeText("``")
        harness.assertText("``")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F1_B04 special characters html tags and emojis in code`() {
        val harness = createWysiwygHarness()
        harness.typeText("`<div>🚀</div>`")
        harness.assertText("<div>🚀</div>")
        harness.assertInlineCode(range = 0..12)
    }

    @Test
    fun `F1_B05 whitespace enclosed backticks prevented`() {
        val harness = createWysiwygHarness()
        harness.typeText("` code `")
        harness.assertText("` code `")
        harness.assertNotInlineCode()
    }

    // =========================================================================
    // F2: Block Heading Boundary (R2)
    // =========================================================================

    @Test
    fun `F2_B01 heading level 4 not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("#### Level4")
        harness.assertText("#### Level4")
        harness.assertNotHeading()
    }

    @Test
    fun `F2_B02 no space after hash not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("#HeaderWithoutSpace")
        harness.assertText("#HeaderWithoutSpace")
        harness.assertNotHeading()
    }

    @Test
    fun `F2_B03 mid line hash space not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Text # NotHeader")
        harness.assertText("Text # NotHeader")
        harness.assertNotHeading()
    }

    @Test
    fun `F2_B04 empty line heading backspace reverts immediately`() {
        val harness = createWysiwygHarness()
        harness.typeText("# ")
        harness.assertHeading(HeadingLevel.H1, range = 0..0)
        harness.pressBackspace()
        harness.assertText("# ")
        harness.assertNotHeading()
    }

    @Test
    fun `F2_B05 consecutive hashes without space not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("####### SevenHashes")
        harness.assertText("####### SevenHashes")
        harness.assertNotHeading()
    }

    // =========================================================================
    // F3: Block Bullet List Boundary (R2)
    // =========================================================================

    @Test
    fun `F3_B01 no space after dash not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("-DashItem")
        harness.assertText("-DashItem")
        harness.assertNotBulletList()
    }

    @Test
    fun `F3_B02 mid line dash not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("left - right")
        harness.assertText("left - right")
        harness.assertNotBulletList()
    }

    @Test
    fun `F3_B03 horizontal rule dashes not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("--- HorizontalRule")
        harness.assertText("--- HorizontalRule")
        harness.assertNotBulletList()
    }

    @Test
    fun `F3_B04 empty bullet backspace reverts immediately`() {
        val harness = createWysiwygHarness()
        harness.typeText("- ")
        harness.assertBulletList(range = 0..0)
        harness.pressBackspace()
        harness.assertText("- ")
        harness.assertNotBulletList()
    }

    @Test
    fun `F3_B05 no space after asterisk not converted to list`() {
        val harness = createWysiwygHarness()
        harness.typeText("*AsteriskWord")
        harness.assertText("*AsteriskWord")
        harness.assertNotBulletList()
    }

    // =========================================================================
    // F4: Block Ordered List Boundary (R2)
    // =========================================================================

    @Test
    fun `F4_B01 digit other than 1 does not trigger initial ordered list`() {
        val harness = createWysiwygHarness()
        harness.typeText("2. SecondItem")
        harness.assertText("2. SecondItem")
        harness.assertNotOrderedList()
    }

    @Test
    fun `F4_B02 no space after digit and dot not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("1.Item")
        harness.assertText("1.Item")
        harness.assertNotOrderedList()
    }

    @Test
    fun `F4_B03 mid line ordered marker not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Version 1. Released")
        harness.assertText("Version 1. Released")
        harness.assertNotOrderedList()
    }

    @Test
    fun `F4_B04 alternative closing bracket not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("1) Parenthesis")
        harness.assertText("1) Parenthesis")
        harness.assertNotOrderedList()
    }

    @Test
    fun `F4_B05 empty ordered list backspace reverts immediately`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. ")
        harness.assertOrderedList(range = 0..0)
        harness.pressBackspace()
        harness.assertText("1. ")
        harness.assertNotOrderedList()
    }

    // =========================================================================
    // F5: Block Blockquote Boundary (R2)
    // =========================================================================

    @Test
    fun `F5_B01 no space after greater than not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText(">QuotedWord")
        harness.assertText(">QuotedWord")
        harness.assertNotBlockquote()
    }

    @Test
    fun `F5_B02 mid line greater than not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("A > B condition")
        harness.assertText("A > B condition")
        harness.assertNotBlockquote()
    }

    @Test
    fun `F5_B03 full width greater than symbol not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("＞ 全角引用")
        harness.assertText("＞ 全角引用")
        harness.assertNotBlockquote()
    }

    @Test
    fun `F5_B04 empty blockquote backspace reverts immediately`() {
        val harness = createWysiwygHarness()
        harness.typeText("> ")
        harness.assertBlockquote(range = 0..0)
        harness.pressBackspace()
        harness.assertText("> ")
        harness.assertNotBlockquote()
    }

    @Test
    fun `F5_B05 multiple greater than symbols without spaces`() {
        val harness = createWysiwygHarness()
        harness.typeText(">>> TripleGreater")
        harness.assertText(">>> TripleGreater")
        harness.assertNotBlockquote()
    }

    // =========================================================================
    // F6: Bold Boundary (R3)
    // =========================================================================

    @Test
    fun `F6_B01 escaped bold markers not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("\\*\\*escaped\\*\\*")
        harness.assertText("\\*\\*escaped\\*\\*")
        harness.assertNotBold()
    }

    @Test
    fun `F6_B02 whitespace left not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("** bold**")
        harness.assertText("** bold**")
        harness.assertNotBold()
    }

    @Test
    fun `F6_B03 whitespace right not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold **")
        harness.assertText("**bold **")
        harness.assertNotBold()
    }

    @Test
    fun `F6_B04 multiline bold not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("**line1\nline2**")
        harness.assertText("**line1\nline2**")
        harness.assertNotBold()
    }

    @Test
    fun `F6_B05 empty bold markers not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("****")
        harness.assertText("****")
        harness.assertNotBold()
    }

    // =========================================================================
    // F7: Italic Asterisk Boundary (R3)
    // =========================================================================

    @Test
    fun `F7_B01 math multiplication not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("2 * 3 * 4")
        harness.assertText("2 * 3 * 4")
        harness.assertNotItalic()
    }

    @Test
    fun `F7_B02 escaped asterisk not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("\\*escaped\\*")
        harness.assertText("\\*escaped\\*")
        harness.assertNotItalic()
    }

    @Test
    fun `F7_B03 whitespace left not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Prefix * italic*")
        harness.assertText("Prefix * italic*")
        harness.assertNotItalic()
    }

    @Test
    fun `F7_B04 whitespace right not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Prefix *italic *")
        harness.assertText("Prefix *italic *")
        harness.assertNotItalic()
    }

    @Test
    fun `F7_B05 multiline asterisk not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Prefix *line1\nline2*")
        harness.assertText("Prefix *line1\nline2*")
        harness.assertNotItalic()
    }

    // =========================================================================
    // F8: Italic Underscore Boundary (R3)
    // =========================================================================

    @Test
    fun `F8_B01 constant naming not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("val MAX_BUFFER_SIZE = 1024")
        harness.assertText("val MAX_BUFFER_SIZE = 1024")
        harness.assertNotItalic()
    }

    @Test
    fun `F8_B02 dunder names not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("def __init__(self):")
        harness.assertText("def __init__(self):")
        harness.assertNotItalic()
    }

    @Test
    fun `F8_B03 url underscores not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("https://example.com/api_v1_endpoint")
        harness.assertText("https://example.com/api_v1_endpoint")
        harness.assertNotItalic()
    }

    @Test
    fun `F8_B04 escaped underscore not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("\\_escaped\\_")
        harness.assertText("\\_escaped\\_")
        harness.assertNotItalic()
    }

    @Test
    fun `F8_B05 whitespace padded underscore not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Word _ spaced _ text")
        harness.assertText("Word _ spaced _ text")
        harness.assertNotItalic()
    }

    // =========================================================================
    // F9: Inline Code Boundary (R3)
    // =========================================================================

    @Test
    fun `F9_B01 escaped backtick not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("\\`escaped\\`")
        harness.assertText("\\`escaped\\`")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F9_B02 single backtick does not trigger`() {
        val harness = createWysiwygHarness()
        harness.typeText("A single ` backtick")
        harness.assertText("A single ` backtick")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F9_B03 directory path expressions not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("/usr/local/bin")
        harness.assertText("/usr/local/bin")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F9_B04 whitespace left in code not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("` code`")
        harness.assertText("` code`")
        harness.assertNotInlineCode()
    }

    @Test
    fun `F9_B05 whitespace right in code not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("`code `")
        harness.assertText("`code `")
        harness.assertNotInlineCode()
    }

    // =========================================================================
    // F10: Strikethrough Boundary (R3)
    // =========================================================================

    @Test
    fun `F10_B01 home directory path not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("~/documents/file.txt")
        harness.assertText("~/documents/file.txt")
        harness.assertNotStrikethrough()
    }

    @Test
    fun `F10_B02 approximation tilde not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("Discount is approx ~ 50%")
        harness.assertText("Discount is approx ~ 50%")
        harness.assertNotStrikethrough()
    }

    @Test
    fun `F10_B03 escaped tilde not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("\\~escaped\\~")
        harness.assertText("\\~escaped\\~")
        harness.assertNotStrikethrough()
    }

    @Test
    fun `F10_B04 whitespace padded tilde not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("~ spaced ~")
        harness.assertText("~ spaced ~")
        harness.assertNotStrikethrough()
    }

    @Test
    fun `F10_B05 empty tilde not converted`() {
        val harness = createWysiwygHarness()
        harness.typeText("~~")
        harness.assertText("~~")
        harness.assertNotStrikethrough()
    }

    // =========================================================================
    // F11: False Positive Prevention Boundary (R3)
    // =========================================================================

    @Test
    fun `F11_B01 only special characters does not crash`() {
        val harness = createWysiwygHarness()
        harness.typeText("***___~~~```")
        harness.assertText("***___~~~```")
    }

    @Test
    fun `F11_B02 japanese punctuation and symbols handled cleanly`() {
        val harness = createWysiwygHarness()
        harness.typeText("これは、**太字**です。そして*斜体*です！")
        harness.assertText("これは、太字です。そして斜体です！")
        harness.assertBold(range = 4..5)
        harness.assertItalic(range = 13..14)
    }

    @Test
    fun `F11_B03 surrogate pairs and emojis maintain index stability`() {
        val harness = createWysiwygHarness()
        harness.typeText("Hello 🌍 **World** 🚀")
        harness.assertText("Hello 🌍 World 🚀")
        harness.assertBold(range = 9..13)
    }

    @Test
    fun `F11_B04 rapid successive triggers handled cleanly`() {
        val harness = createWysiwygHarness()
        harness.typeText("**A****B****C**")
        harness.assertText("ABC")
        harness.assertBold(range = 0..0)
        harness.assertBold(range = 1..1)
        harness.assertBold(range = 2..2)
    }

    @Test
    fun `F11_B05 unbalanced nested delimiters prevented from corrupted formatting`() {
        val harness = createWysiwygHarness()
        harness.typeText("**_unbalanced*")
        harness.assertText("**_unbalanced*")
    }

    // =========================================================================
    // F12: Undo Boundary (R4)
    // =========================================================================

    @Test
    fun `F12_B01 undo after typing further reverts typing first`() {
        val harness = createWysiwygHarness()
        harness.typeText("# ")
        harness.typeText("Text")
        harness.assertText("Text")

        harness.undo() // reverts "Text" typing
        harness.assertText("")

        harness.undo() // reverts heading format to raw "# "
        harness.assertText("# ")
        harness.assertNotHeading()
    }

    @Test
    fun `F12_B02 multiple sequential undos rewind document history`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.typeText(" ")
        harness.typeText("*italic*")

        harness.undo() // reverts italic auto-format -> "bold *italic*"
        harness.assertText("bold *italic*")

        harness.undo() // reverts italic typing -> "bold "
        harness.assertText("bold ")

        harness.undo() // reverts space -> "bold"
        harness.assertText("bold")

        harness.undo() // reverts bold auto-format -> "**bold**"
        harness.assertText("**bold**")
    }

    @Test
    fun `F12_B03 undo when stack empty is safe no-op`() {
        val harness = createWysiwygHarness()
        harness.undo()
        harness.assertText("")
    }

    @Test
    fun `F12_B04 redo when stack empty is safe no-op`() {
        val harness = createWysiwygHarness()
        harness.redo()
        harness.assertText("")
    }

    @Test
    fun `F12_B05 retyping after undo triggers auto-formatting again`() {
        val harness = createWysiwygHarness()
        harness.typeText("# ")
        harness.undo()
        harness.assertText("# ")

        // delete space and re-type space
        harness.pressBackspace()
        harness.typeText(" ")
        harness.assertHeading(HeadingLevel.H1, range = 0..0)
    }

    // =========================================================================
    // F13: Backspace Boundary (R4)
    // =========================================================================

    @Test
    fun `F13_B01 backspace after cursor relocation deletes character normally`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.setCursor(2) // Move cursor inside "bold"
        harness.pressBackspace()
        harness.assertText("bld") // normal delete 'o'
    }

    @Test
    fun `F13_B02 backspace on selection deletes selection normally`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.setSelection(1, 3) // Select "ol"
        harness.pressBackspace()
        harness.assertText("bd")
    }

    @Test
    fun `F13_B03 retype space after backspace reversal triggers again`() {
        val harness = createWysiwygHarness()
        harness.typeText("> ")
        harness.pressBackspace() // Revert to "> "
        harness.assertText("> ")

        harness.pressBackspace() // deletes space -> ">"
        harness.typeText(" ") // re-type space -> triggers blockquote
        harness.assertText("")
        harness.assertBlockquote(range = 0..0)
    }

    @Test
    fun `F13_B04 continuous backspaces delete raw markers character by character`() {
        val harness = createWysiwygHarness()
        harness.typeText("### ")
        harness.pressBackspace() // revert to "### "
        harness.assertText("### ")

        harness.pressBackspace() // "###"
        harness.assertText("###")

        harness.pressBackspace() // "##"
        harness.assertText("##")

        harness.pressBackspace() // "#"
        harness.assertText("#")

        harness.pressBackspace() // ""
        harness.assertText("")
    }

    @Test
    fun `F13_B05 backspace at start of empty document is safe no-op`() {
        val harness = createWysiwygHarness()
        harness.pressBackspace()
        harness.assertText("")
    }

    // =========================================================================
    // F14: Cursor Ergonomics Boundary (R3, R4)
    // =========================================================================

    @Test
    fun `F14_B01 bold at start of line cursor positioned after word`() {
        val harness = createWysiwygHarness()
        harness.typeText("**abc**")
        harness.assertCursorAt(3)
    }

    @Test
    fun `F14_B02 bold at end of long text cursor positioned accurately`() {
        val harness = createWysiwygHarness()
        val longPrefix = "A".repeat(100)
        harness.typeText(longPrefix)
        harness.typeText(" **test**")
        harness.assertCursorAt(100 + 1 + 4) // 105
    }

    @Test
    fun `F14_B03 cursor relocation clears removed typing attribute lock`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**")
        harness.setCursor(0)
        harness.setCursor(4)
        harness.typeText("X")
        harness.assertText("boldX")
    }

    @Test
    fun `F14_B04 newline immediately after bold does not carry bold across newline`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold**\nplain")
        harness.assertText("bold\nplain")
        harness.assertBold(range = 0..3)
        harness.assertNotBold(range = 5..9)
    }

    @Test
    fun `F14_B05 continuous inline formats on same line retain proper spans`() {
        val harness = createWysiwygHarness()
        harness.typeText("**first** and *second*")
        harness.assertText("first and second")
        harness.assertBold(range = 0..4)
        harness.assertNotBold(range = 5..15)
        harness.assertItalic(range = 10..15)
    }

    // =========================================================================
    // F15: WysiwygEditor Boundary (R1)
    // =========================================================================

    @Test
    fun `F15_B01 disabled editor suppresses all auto formatting`() {
        val harness = createWysiwygHarness(readOnly = true)
        harness.typeText("# Header")
        harness.assertText("")
        harness.assertNotHeading()
    }

    @Test
    fun `F15_B02 large text typing stability`() {
        val harness = createWysiwygHarness()
        val baseText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(10)
        harness.typeText(baseText)
        harness.typeText("**bold**")
        harness.assertBold(range = baseText.length until (baseText.length + 4))
    }

    @Test
    fun `F15_B03 empty string initialization handled gracefully`() {
        val harness = createWysiwygHarness(initialText = "")
        harness.assertText("")
        harness.typeText("1. ")
        harness.assertOrderedList(range = 0..0)
    }

    @Test
    fun `F15_B04 multiple initial spans preserved`() {
        val initialSpans =
            listOf(
                RichSpan(range = 0..3, attributes = attributeContainerOf(BoldKey to Unit)),
                RichSpan(range = 5..9, attributes = attributeContainerOf(BoldKey to Unit)),
            )
        val harness = createWysiwygHarness(initialText = "bold bold", initialSpans = initialSpans)
        harness.assertBold(range = 0..3)
        harness.assertBold(range = 5..9)
    }

    @Test
    fun `F15_B05 external edit retains wysiwyg reactivity`() {
        val harness = createWysiwygHarness()
        harness.driver.rawState.edit {
            insert(0, "External ")
        }
        harness.typeText("**bold**")
        harness.assertText("External bold")
        harness.assertBold(range = 9..12)
    }

    // =========================================================================
    // F16: RichTextEditor Non-regression Boundary (R1)
    // =========================================================================

    @Test
    fun `F16_B01 rich text editor preserves complex markdown document raw`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        val markdownDoc = "# Title\n- Bullet\n1. Number\n> Quote\n**bold** `code`"
        harness.typeText(markdownDoc)
        harness.assertText(markdownDoc)
        harness.assertNotHeading()
        harness.assertNotBulletList()
        harness.assertNotOrderedList()
        harness.assertNotBlockquote()
        harness.assertNotBold()
        harness.assertNotInlineCode()
    }

    @Test
    fun `F16_B02 rich text editor backspace behaves as standard text deletion`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("# ")
        harness.pressBackspace()
        harness.assertText("#")
    }

    @Test
    fun `F16_B03 rich text editor undo operates on standard typing granularity`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("abc")
        harness.typeText(" ")
        harness.typeText("def")
        harness.undo()
        harness.assertText("abc ")
    }

    @Test
    fun `F16_B04 rich text editor selection deletion works normally`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("Hello World")
        harness.setSelection(5, 11)
        harness.pressBackspace()
        harness.assertText("Hello")
    }

    @Test
    fun `F16_B05 rich text editor read only suppresses text modification`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false, readOnly = true)
        harness.typeText("Attempt")
        harness.assertText("")
    }
}
