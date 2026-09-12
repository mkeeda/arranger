package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.DefaultListMarkerResolver
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextInputTransformation
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Empirical challenger stress test suite for Milestone 3 (Component Separation: F15 vs F16).
 *
 * Verifies that:
 * 1. RichTextEditor NEVER triggers Markdown auto-formatting under any conditions (F16).
 * 2. WysiwygEditor triggers all Markdown auto-formatting correctly (F15).
 * 3. Component signatures and parameter bindings maintain 100% backward compatibility and parity.
 */
class Milestone3Challenger1StressTest {
    @Test
    fun `RichTextEditor composable contract compiles and binds properly across all overloads`() {
        val composableRef: @Composable () -> Unit = {
            val state = RichTextState(initialText = RichString("Sample"))
            val interactionSource = MutableInteractionSource()
            val scrollState = rememberScrollState()

            // 1. Minimum invocation
            RichTextEditor(state = state)

            // 2. Full overload invocation with styleResolver and attributeStyleResolver
            RichTextEditor(
                state = state,
                modifier = Modifier,
                enabled = true,
                readOnly = false,
                textStyle = TextStyle.Default,
                scrollState = scrollState,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Black),
                styleResolver = DefaultAttributeStyleResolver,
                attributeStyleResolver = DefaultAttributeStyleResolver,
                listMarkerResolver = DefaultListMarkerResolver,
                onLinkClick = { _ -> },
            )
        }
        composableRef shouldBe composableRef
    }

    @Test
    fun `WysiwygEditor composable contract compiles and binds properly across all overloads`() {
        val composableRef: @Composable () -> Unit = {
            val state = RichTextState(initialText = RichString("Sample"))
            val wysiwygState = WysiwygState()
            val interactionSource = MutableInteractionSource()
            val scrollState = rememberScrollState()

            // 1. Minimum invocation
            WysiwygEditor(state = state)

            // 2. Full overload invocation with onLinkClick and resolvers
            WysiwygEditor(
                state = state,
                modifier = Modifier,
                enabled = true,
                readOnly = false,
                textStyle = TextStyle.Default,
                scrollState = scrollState,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Black),
                styleResolver = DefaultAttributeStyleResolver,
                listMarkerResolver = DefaultListMarkerResolver,
                onLinkClick = { _ -> },
            )
        }
        composableRef shouldBe composableRef
    }

    @Test
    fun `RichTextEditor never triggers block formatting for headings H1 H2 H3 and H4`() {
        val (state, transformation) = createRichTextEngine()

        typeTextToRichText(state, transformation, "# Heading 1\n## Heading 2\n### Heading 3\n#### Heading 4")

        state.richString.text shouldBe "# Heading 1\n## Heading 2\n### Heading 3\n#### Heading 4"
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextEditor never triggers block formatting for bullet lists ordered lists and blockquotes`() {
        val (state, transformation) = createRichTextEngine()

        typeTextToRichText(state, transformation, "- Dash list\n* Asterisk list\n1. Ordered list\n> Blockquote text")

        state.richString.text shouldBe "- Dash list\n* Asterisk list\n1. Ordered list\n> Blockquote text"
        state.richString.spans.filter { it.attributes.containsKey(BulletListKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(OrderedListKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(BlockquoteKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextEditor never triggers inline formatting for bold italic code and strikethrough`() {
        val (state, transformation) = createRichTextEngine()

        typeTextToRichText(
            state,
            transformation,
            "**bold** and *italic* and _underscore_ and `code` and ~strike~",
        )

        state.richString.text shouldBe "**bold** and *italic* and _underscore_ and `code` and ~strike~"
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(ItalicKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(InlineCodeKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(StrikethroughKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextEditor backspace performs standard deletion without any autoformat revert logic`() {
        val (state, transformation) = createRichTextEngine()

        typeTextToRichText(state, transformation, "# ")
        state.richString.text shouldBe "# "

        // Normal backspace simulation (delete 1 character)
        state.textFieldState.edit {
            delete(selection.start - 1, selection.start)
            with(transformation) {
                transformInput()
            }
        }

        state.richString.text shouldBe "#"
        state.richString.spans.shouldBeEmpty()
    }

    @Test
    fun `WysiwygEditor triggers all block level auto-formatting correctly`() {
        // H1
        val (s1, w1, t1) = createWysiwygEngine()
        typeTextToWysiwyg(s1, t1, "# H1")
        s1.richString.text shouldBe "H1"
        s1.richString.spans.any { it.attributes[HeadingKey] == HeadingLevel.H1 } shouldBe true

        // H2
        val (s2, w2, t2) = createWysiwygEngine()
        typeTextToWysiwyg(s2, t2, "## H2")
        s2.richString.text shouldBe "H2"
        s2.richString.spans.any { it.attributes[HeadingKey] == HeadingLevel.H2 } shouldBe true

        // H3
        val (s3, w3, t3) = createWysiwygEngine()
        typeTextToWysiwyg(s3, t3, "### H3")
        s3.richString.text shouldBe "H3"
        s3.richString.spans.any { it.attributes[HeadingKey] == HeadingLevel.H3 } shouldBe true

        // Bullet Dash
        val (s4, w4, t4) = createWysiwygEngine()
        typeTextToWysiwyg(s4, t4, "- Item")
        s4.richString.text shouldBe "Item"
        s4.richString.spans.any { it.attributes.containsKey(BulletListKey) } shouldBe true

        // Bullet Asterisk
        val (s5, w5, t5) = createWysiwygEngine()
        typeTextToWysiwyg(s5, t5, "* Item")
        s5.richString.text shouldBe "Item"
        s5.richString.spans.any { it.attributes.containsKey(BulletListKey) } shouldBe true

        // Ordered List
        val (s6, w6, t6) = createWysiwygEngine()
        typeTextToWysiwyg(s6, t6, "1. Item")
        s6.richString.text shouldBe "Item"
        s6.richString.spans.any { it.attributes.containsKey(OrderedListKey) } shouldBe true

        // Blockquote
        val (s7, w7, t7) = createWysiwygEngine()
        typeTextToWysiwyg(s7, t7, "> Quote")
        s7.richString.text shouldBe "Quote"
        s7.richString.spans.any { it.attributes.containsKey(BlockquoteKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers all inline auto-formatting correctly`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        typeTextToWysiwyg(state, transformation, "**bold** ")
        state.richString.text shouldBe "bold "
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true

        typeTextToWysiwyg(state, transformation, "*italic* ")
        state.richString.text shouldBe "bold italic "
        state.richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true

        typeTextToWysiwyg(state, transformation, "_italic2_ ")
        state.richString.text shouldBe "bold italic italic2 "

        typeTextToWysiwyg(state, transformation, "`code` ")
        state.richString.text shouldBe "bold italic italic2 code "
        state.richString.spans.any { it.attributes.containsKey(InlineCodeKey) } shouldBe true

        typeTextToWysiwyg(state, transformation, "~strike~")
        state.richString.text shouldBe "bold italic italic2 code strike"
        state.richString.spans.any { it.attributes.containsKey(StrikethroughKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor avoids false positive formatting on snake case arithmetic and paths`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        typeTextToWysiwyg(state, transformation, "val snake case_variable = 2 * 3 * 4; cd ~/docs; ****")

        state.richString.text shouldBe "val snake case_variable = 2 * 3 * 4; cd ~/docs; ****"
        state.richString.spans.shouldBeEmpty()
    }

    @Test
    fun `WysiwygEditor handleWysiwygKey intercepts backspace to revert block auto-format`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        typeTextToWysiwyg(state, transformation, "## ")
        state.richString.text shouldBe ""
        (
            state.typingAttributes?.containsKey(HeadingKey) == true ||
                state.richString.spans.any {
                    it.attributes.containsKey(HeadingKey)
                }
        ) shouldBe true
        wysiwygState.canRevert(state) shouldBe true

        // Handle backspace key
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )

        consumed shouldBe true
        state.richString.text shouldBe "## "
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun `WysiwygEditor handleWysiwygKey intercepts backspace to revert inline auto-format`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        typeTextToWysiwyg(state, transformation, "`fun main()`")
        state.richString.text shouldBe "fun main()"
        state.richString.spans.any { it.attributes.containsKey(InlineCodeKey) } shouldBe true
        wysiwygState.canRevert(state) shouldBe true

        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )

        consumed shouldBe true
        state.richString.text shouldBe "`fun main()`"
        state.richString.spans.filter { it.attributes.containsKey(InlineCodeKey) }.shouldBeEmpty()
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun `WysiwygEditor does not revert auto-formatting after subsequent character entry`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        typeTextToWysiwyg(state, transformation, "# ")
        typeTextToWysiwyg(state, transformation, "A")

        wysiwygState.canRevert(state) shouldBe false

        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )

        consumed shouldBe false
    }

    @Test
    fun `Adversarial parity test verifies RichTextEditor preserves raw text while WysiwygEditor formats`() {
        val (richState, richTransform) = createRichTextEngine()
        val (wysiwygState, _, wysiwygTransform) = createWysiwygEngine()

        val complexScript =
            listOf(
                "# Heading\n",
                "- Item 1\n",
                "> Quote block\n",
                "**Bold** text\n",
                "`code` block",
            )

        for (chunk in complexScript) {
            typeTextToRichText(richState, richTransform, chunk)
            typeTextToWysiwyg(wysiwygState, wysiwygTransform, chunk)
        }

        // RichTextEditor: raw symbols completely intact, spans empty
        richState.richString.text shouldBe complexScript.joinToString("")
        richState.richString.spans.shouldBeEmpty()

        // WysiwygEditor: triggers converted into rich attributes
        wysiwygState.richString.spans.any { it.attributes.containsKey(HeadingKey) } shouldBe true
        wysiwygState.richString.spans.any { it.attributes.containsKey(BulletListKey) } shouldBe true
        wysiwygState.richString.spans.any { it.attributes.containsKey(BlockquoteKey) } shouldBe true
        wysiwygState.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        wysiwygState.richString.spans.any { it.attributes.containsKey(InlineCodeKey) } shouldBe true
    }

    // --- Helpers ---

    private fun createRichTextEngine(): Pair<RichTextState, RichTextInputTransformation> {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)
        return Pair(state, transformation)
    }

    private fun createWysiwygEngine(): Triple<RichTextState, WysiwygState, WysiwygInputTransformation> {
        val state = RichTextState()
        val wysiwygState = WysiwygState()
        val transformation = WysiwygInputTransformation(state, wysiwygState)
        return Triple(state, wysiwygState, transformation)
    }

    private fun typeTextToRichText(
        state: RichTextState,
        transformation: RichTextInputTransformation,
        text: String,
    ) {
        for (char in text) {
            val insertIndex = state.selection.start
            state.textFieldState.edit {
                insert(insertIndex, char.toString())
                selection = TextRange(insertIndex + 1)
                val buffer = this
                with(transformation) {
                    buffer.transformInput()
                }
            }
        }
    }

    private fun typeTextToWysiwyg(
        state: RichTextState,
        transformation: WysiwygInputTransformation,
        text: String,
    ) {
        for (char in text) {
            val insertIndex = state.selection.start
            state.textFieldState.edit {
                insert(insertIndex, char.toString())
                selection = TextRange(insertIndex + 1)
                val buffer = this
                with(transformation) {
                    buffer.transformInput()
                }
            }
        }
    }
}
