package dev.mkeeda.arranger.richtext.editor.wysiwyg

import dev.mkeeda.arranger.richtext.HeadingLevel
import kotlin.test.Test

/**
 * Tier 3: Cross-Feature Combinations Test Suite.
 * Pairwise and nesting tests covering interactions between block-level (F2-F5)
 * and inline-level (F6-F10) formatting features, along with Undo/Backspace ergonomics.
 */
class Tier3CrossFeatureCombinationTest {
    @Test
    fun `C01 bullet list containing bold span`() {
        val harness = createWysiwygHarness()
        harness.typeText("- Item with **bold** word")
        harness.assertText("Item with bold word")
        harness.assertBulletList(range = 0..18)
        harness.assertBold(range = 10..13)
        harness.assertNotBold(range = 0..9)
        harness.assertNotBold(range = 14..18)
    }

    @Test
    fun `C02 bullet list containing inline code`() {
        val harness = createWysiwygHarness()
        harness.typeText("- Execute `npm run test` script")
        harness.assertText("Execute npm run test script")
        harness.assertBulletList(range = 0..26)
        harness.assertInlineCode(range = 8..19)
        harness.assertNotInlineCode(range = 0..7)
    }

    @Test
    fun `C03 heading 1 containing inline code`() {
        val harness = createWysiwygHarness()
        harness.typeText("# API: `getUserById`")
        harness.assertText("API: getUserById")
        harness.assertHeading(HeadingLevel.H1, range = 0..15)
        harness.assertInlineCode(range = 5..15)
        harness.assertNotInlineCode(range = 0..4)
    }

    @Test
    fun `C04 heading 2 containing bold span`() {
        val harness = createWysiwygHarness()
        harness.typeText("## Chapter 1: **Getting Started**")
        harness.assertText("Chapter 1: Getting Started")
        harness.assertHeading(HeadingLevel.H2, range = 0..25)
        harness.assertBold(range = 11..25)
        harness.assertNotBold(range = 0..10)
    }

    @Test
    fun `C05 blockquote containing strikethrough span`() {
        val harness = createWysiwygHarness()
        harness.typeText("> Note: ~deprecated approach~")
        harness.assertText("Note: deprecated approach")
        harness.assertBlockquote(range = 0..24)
        harness.assertStrikethrough(range = 6..24)
        harness.assertNotStrikethrough(range = 0..5)
    }

    @Test
    fun `C06 blockquote containing both bold and italic spans`() {
        val harness = createWysiwygHarness()
        harness.typeText("> Be **bold** and *fearless*")
        harness.assertText("Be bold and fearless")
        harness.assertBlockquote(range = 0..19)
        harness.assertBold(range = 3..6)
        harness.assertItalic(range = 12..19)
    }

    @Test
    fun `C07 multiple inline formats on same line bold and italic and underscore`() {
        val harness = createWysiwygHarness()
        harness.typeText("**bold** and *italic* and _emphasis_")
        harness.assertText("bold and italic and emphasis")
        harness.assertBold(range = 0..3)
        harness.assertItalic(range = 9..14)
        harness.assertItalic(range = 20..27)
    }

    @Test
    fun `C08 ordered list containing inline code and bold`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. Run `gradlew` and **check** output")
        harness.assertText("Run gradlew and check output")
        harness.assertOrderedList(range = 0..27)
        harness.assertInlineCode(range = 4..10)
        harness.assertBold(range = 16..20)
    }

    @Test
    fun `C09 sequential block transforms across multiple lines`() {
        val harness = createWysiwygHarness()
        harness.typeText("# Section Title\n")
        harness.typeText("- Bullet item\n")
        harness.typeText("1. Ordered item\n")
        harness.typeText("> Blockquote text")

        harness.assertText("Section Title\nBullet item\nOrdered item\nBlockquote text")
        harness.assertHeading(HeadingLevel.H1, range = 0..12)
        harness.assertBulletList(range = 14..24)
        harness.assertOrderedList(range = 26..37)
        harness.assertBlockquote(range = 39..53)
    }

    @Test
    fun `C10 inline format followed by newline and heading transform`() {
        val harness = createWysiwygHarness()
        harness.typeText("Intro **bold**\n# New Chapter")
        harness.assertText("Intro bold\nNew Chapter")
        harness.assertBold(range = 6..9)
        harness.assertHeading(HeadingLevel.H1, range = 11..21)
    }

    @Test
    fun `C11 heading block transform followed by inline bold`() {
        val harness = createWysiwygHarness()
        harness.typeText("# Header **Bold**")
        harness.assertText("Header Bold")
        harness.assertHeading(HeadingLevel.H1, range = 0..10)
        harness.assertBold(range = 7..10)
    }

    @Test
    fun `C12 adjacent inline code and bold spans remain independent`() {
        val harness = createWysiwygHarness()
        harness.typeText("`code` **bold**")
        harness.assertText("code bold")
        harness.assertInlineCode(range = 0..3)
        harness.assertNotInlineCode(range = 4..8)
        harness.assertBold(range = 5..8)
        harness.assertNotBold(range = 0..4)
    }

    @Test
    fun `C13 undo inline decoration within bullet list preserves list block attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("- Item **bold**")
        harness.assertText("Item bold")
        harness.assertBulletList(range = 0..8)
        harness.assertBold(range = 5..8)

        harness.undo() // reverts inline bold to raw "**bold**"
        harness.assertText("Item **bold**")
        harness.assertBulletList(range = 0..12)
        harness.assertNotBold()
    }

    @Test
    fun `C14 backspace inline decoration within ordered list preserves list block attribute`() {
        val harness = createWysiwygHarness()
        harness.typeText("1. Step `run`")
        harness.assertText("Step run")
        harness.assertOrderedList(range = 0..7)
        harness.assertInlineCode(range = 5..7)

        harness.pressBackspace() // reverts `run` to raw backticks
        harness.assertText("Step `run`")
        harness.assertOrderedList(range = 0..9)
        harness.assertNotInlineCode()
    }

    @Test
    fun `C15 full document multi-action undo and redo chain`() {
        val harness = createWysiwygHarness()
        harness.typeText("# Title\n- Item **bold**")
        harness.assertText("Title\nItem bold")
        harness.assertHeading(HeadingLevel.H1, range = 0..4)
        harness.assertBulletList(range = 6..14)
        harness.assertBold(range = 11..14)

        // Undo bold auto-formatting
        harness.undo()
        harness.assertText("Title\nItem **bold**")
        harness.assertNotBold()

        // Redo bold auto-formatting
        harness.redo()
        harness.assertText("Title\nItem bold")
        harness.assertBold(range = 11..14)
    }
}
