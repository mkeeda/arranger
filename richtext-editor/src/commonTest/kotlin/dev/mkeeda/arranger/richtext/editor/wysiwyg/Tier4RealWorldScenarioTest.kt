package dev.mkeeda.arranger.richtext.editor.wysiwyg

import dev.mkeeda.arranger.richtext.HeadingLevel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tier 4: Real-World Application Scenarios Test Suite.
 * Comprehensive end-to-end workflows testing realistic authoring sessions,
 * complex mixed structures, backspace/undo error corrections, and syntax boundary safety.
 */
class Tier4RealWorldScenarioTest {
    @Test
    fun `S01 technical blog article authoring workflow`() {
        val harness = createWysiwygHarness()

        // 1. Title Heading 1
        harness.typeText("# Getting Started with Arranger\n")
        val titleText = "Getting Started with Arranger"
        val titleIdx = harness.driver.currentText.indexOf(titleText)
        harness.assertHeading(HeadingLevel.H1, range = titleIdx until (titleIdx + titleText.length))

        // 2. Blockquote Overview
        harness.typeText("> Modern rich text editing for Compose Multiplatform\n")
        val quoteText = "Modern rich text editing for Compose Multiplatform"
        val quoteIdx = harness.driver.currentText.indexOf(quoteText)
        harness.assertBlockquote(range = quoteIdx until (quoteIdx + quoteText.length))

        // 3. Section Heading 2
        harness.typeText("## Key Features\n")
        val h2Text = "Key Features"
        val h2Idx = harness.driver.currentText.indexOf(h2Text)
        harness.assertHeading(HeadingLevel.H2, range = h2Idx until (h2Idx + h2Text.length))

        // 4. Bullet List with Bold emphasis
        harness.typeText("- Fast **WYSIWYG** auto formatting\n")
        val item1Text = "Fast WYSIWYG auto formatting"
        val item1Idx = harness.driver.currentText.indexOf(item1Text)
        harness.assertBulletList(range = item1Idx until (item1Idx + item1Text.length))
        val boldIdx = harness.driver.currentText.indexOf("WYSIWYG")
        harness.assertBold(range = boldIdx until (boldIdx + "WYSIWYG".length))
        harness.assertNotBold(range = item1Idx until boldIdx)
        harness.assertNotBold(range = (boldIdx + "WYSIWYG".length) until (item1Idx + item1Text.length))

        // 5. Bullet List with Inline Code
        harness.typeText("- Lightweight `arranger-core` module\n")
        val item2Text = "Lightweight arranger-core module"
        val item2Idx = harness.driver.currentText.indexOf(item2Text)
        harness.assertBulletList(range = item2Idx until (item2Idx + item2Text.length))
        val codeIdx = harness.driver.currentText.indexOf("arranger-core")
        harness.assertInlineCode(range = codeIdx until (codeIdx + "arranger-core".length))

        // 6. Section Heading 2 & Ordered List
        harness.typeText("## Installation\n")
        harness.typeText("1. Add dependencies to `build.gradle.kts`\n")
        val order1Text = "Add dependencies to build.gradle.kts"
        val order1Idx = harness.driver.currentText.indexOf(order1Text)
        harness.assertOrderedList(range = order1Idx until (order1Idx + order1Text.length))
        val gradleIdx = harness.driver.currentText.indexOf("build.gradle.kts")
        harness.assertInlineCode(range = gradleIdx until (gradleIdx + "build.gradle.kts".length))

        harness.typeText("1. Run `gradle build` to compile\n")
        val order2Text = "Run gradle build to compile"
        val order2Idx = harness.driver.currentText.indexOf(order2Text)
        harness.assertOrderedList(range = order2Idx until (order2Idx + order2Text.length))
        val buildIdx = harness.driver.currentText.indexOf("gradle build")
        harness.assertInlineCode(range = buildIdx until (buildIdx + "gradle build".length))

        // 7. Concluding Blockquote
        harness.typeText("> Enjoy seamless editing!")
        val endQuoteText = "Enjoy seamless editing!"
        val endQuoteIdx = harness.driver.currentText.indexOf(endQuoteText)
        harness.assertBlockquote(range = endQuoteIdx until (endQuoteIdx + endQuoteText.length))

        // Verify document structure contains all parts
        assertTrue(harness.driver.currentText.contains("Getting Started with Arranger"))
        assertTrue(harness.driver.currentText.contains("WYSIWYG"))
        assertTrue(harness.driver.currentText.contains("arranger-core"))
        assertTrue(harness.driver.currentText.contains("build.gradle.kts"))
    }

    @Test
    fun `S02 meeting notes and sprint planning workflow`() {
        val harness = createWysiwygHarness()

        // 1. Meeting Title
        harness.typeText("## Sprint Planning 2026\n")
        val title = "Sprint Planning 2026"
        val titleIdx = harness.driver.currentText.indexOf(title)
        harness.assertHeading(HeadingLevel.H2, range = titleIdx until (titleIdx + title.length))

        // 2. Sprint Goal with Bold
        harness.typeText("> Goal: Deliver **WYSIWYG** release on time\n")
        val goal = "Goal: Deliver WYSIWYG release on time"
        val goalIdx = harness.driver.currentText.indexOf(goal)
        harness.assertBlockquote(range = goalIdx until (goalIdx + goal.length))
        val boldGoalIdx = harness.driver.currentText.indexOf("WYSIWYG")
        harness.assertBold(range = boldGoalIdx until (boldGoalIdx + "WYSIWYG".length))

        // 3. Attendees with Bold and Italic
        harness.typeText("### Attendees\n")
        harness.typeText("- **Alice** (Lead)\n")
        val aliceIdx = harness.driver.currentText.indexOf("Alice")
        harness.assertBold(range = aliceIdx until (aliceIdx + "Alice".length))

        harness.typeText("- _Bob_ (Reviewer)\n")
        val bobIdx = harness.driver.currentText.indexOf("Bob")
        harness.assertItalic(range = bobIdx until (bobIdx + "Bob".length))

        // 4. Action Items with Code and Strikethrough
        harness.typeText("### Action Items\n")
        harness.typeText("1. Review `PROJECT.md` specification\n")
        val specIdx = harness.driver.currentText.indexOf("PROJECT.md")
        harness.assertInlineCode(range = specIdx until (specIdx + "PROJECT.md".length))

        harness.typeText("1. Complete ~deprecated task~ items\n")
        val strikeIdx = harness.driver.currentText.indexOf("deprecated task")
        harness.assertStrikethrough(range = strikeIdx until (strikeIdx + "deprecated task".length))

        harness.typeText("1. Deploy preview artifacts")
        val deployText = "Deploy preview artifacts"
        val deployIdx = harness.driver.currentText.indexOf(deployText)
        harness.assertOrderedList(range = deployIdx until (deployIdx + deployText.length))
    }

    @Test
    fun `S03 developer release checklist with toggles and backspace`() {
        val harness = createWysiwygHarness()

        // 1. Release Header
        harness.typeText("# Release v1.0.0 Checklist\n")
        val h1 = "Release v1.0.0 Checklist"
        val h1Idx = harness.driver.currentText.indexOf(h1)
        harness.assertHeading(HeadingLevel.H1, range = h1Idx until (h1Idx + h1.length))

        // 2. Checklist Items
        harness.typeText("- Code freeze **passed**\n")
        val passedIdx = harness.driver.currentText.indexOf("passed")
        harness.assertBold(range = passedIdx until (passedIdx + "passed".length))

        harness.typeText("- ~Unit tests 100% pass~\n")
        val unitTestIdx = harness.driver.currentText.indexOf("Unit tests 100% pass")
        harness.assertStrikethrough(range = unitTestIdx until (unitTestIdx + "Unit tests 100% pass".length))

        harness.typeText("- ~Spotless check clean~\n")
        val spotlessIdx = harness.driver.currentText.indexOf("Spotless check clean")
        harness.assertStrikethrough(range = spotlessIdx until (spotlessIdx + "Spotless check clean".length))

        harness.typeText("- Tag version `v1.0.0` in git\n")
        val tagIdx = harness.driver.currentText.lastIndexOf("v1.0.0")
        harness.assertInlineCode(range = tagIdx until (tagIdx + "v1.0.0".length))

        // 3. Accidental format followed by immediate Backspace revert
        harness.typeText("## ")
        // Backspace immediately after auto-format reverts H2 back to raw '## '
        harness.pressBackspace()
        val rawH2Idx = harness.driver.currentText.lastIndexOf("## ")
        assertFalse(harness.driver.isHeading(HeadingLevel.H2, rawH2Idx..(rawH2Idx + 2)))
        harness.assertText(
            "Release v1.0.0 Checklist\nCode freeze passed\nUnit tests 100% pass\nSpotless check clean\nTag version v1.0.0 in git\n## ",
        )
    }

    @Test
    fun `S04 research paper summary and technical documentation`() {
        val harness = createWysiwygHarness()

        // 1. Paper Title
        harness.typeText("# Scalable WYSIWYG Architecture\n")
        val title = "Scalable WYSIWYG Architecture"
        val titleIdx = harness.driver.currentText.indexOf(title)
        harness.assertHeading(HeadingLevel.H1, range = titleIdx until (titleIdx + title.length))

        // 2. Abstract Quote
        harness.typeText("> Abstract: Analysis of text formatting pipelines in Compose.\n")
        val abstract = "Abstract: Analysis of text formatting pipelines in Compose."
        val abstractIdx = harness.driver.currentText.indexOf(abstract)
        harness.assertBlockquote(range = abstractIdx until (abstractIdx + abstract.length))

        // 3. Syntax Section
        harness.typeText("## Syntax Isolation Rules\n")

        // 4. Snake_case and constants should NOT trigger italic
        harness.typeText("Variables like my_variable_name and MAX_BUFFER are protected.\n")
        harness.assertNotItalic()

        // 5. Spaced math expression should NOT trigger italic
        harness.typeText("Formula 2 * 3 * 4 preserves spacing verbatim.\n")
        harness.assertNotItalic()

        // 6. Benchmark Section with Ordered List, Inline Code and Bold
        harness.typeText("### Benchmark Results\n")
        harness.typeText("1. Achieved throughput of `10000` ops/sec with **zero lag**.")

        val numIdx = harness.driver.currentText.indexOf("10000")
        harness.assertInlineCode(range = numIdx until (numIdx + "10000".length))

        val lagIdx = harness.driver.currentText.indexOf("zero lag")
        harness.assertBold(range = lagIdx until (lagIdx + "zero lag".length))
        harness.assertNotBold(range = 0 until lagIdx)
    }

    @Test
    fun `S05 trial-and-error editing with multi-level undo redo`() {
        val harness = createWysiwygHarness()

        // 1. Type header shortcut
        harness.typeText("# ")
        harness.assertHeading(HeadingLevel.H1, range = 0..0)

        // 2. Backspace immediately reverts to raw trigger
        harness.pressBackspace()
        harness.assertText("# ")
        harness.assertNotHeading()

        // 3. Clear line and create bullet list
        harness.typeText("Draft\n- First item with **bold text**")
        val boldIdx = harness.driver.currentText.indexOf("bold text")
        harness.assertBold(range = boldIdx until (boldIdx + "bold text".length))

        // 4. Undo the bold auto-format
        harness.undo()
        harness.assertNotBold()
        assertTrue(harness.driver.currentText.contains("**bold text**"))

        // 5. Redo restores the bold formatting
        harness.redo()
        val boldIdxAfterRedo = harness.driver.currentText.indexOf("bold text")
        harness.assertBold(range = boldIdxAfterRedo until (boldIdxAfterRedo + "bold text".length))

        // 6. Continue typing after bold to verify F14 style boundary isolation
        harness.typeText(" and `code` inline")
        val codeIdx = harness.driver.currentText.indexOf("code")
        harness.assertInlineCode(range = codeIdx until (codeIdx + "code".length))

        // Plain text between bold and code must remain plain
        val andIdx = harness.driver.currentText.indexOf(" and ")
        harness.assertNotBold(range = andIdx until (andIdx + 5))
        harness.assertNotInlineCode(range = andIdx until (andIdx + 5))
    }
}
