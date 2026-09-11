package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import dev.mkeeda.arranger.richtext.CodeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFalse

class WysiwygEditorTest {
    @Test
    fun `WysiwygEditor composable contract compiles and binds properly with all parameter overloads`() {
        val composableRef: @Composable () -> Unit = {
            val state = RichTextState(initialText = RichString(text = "Sample"))
            val wysiwygState = WysiwygState()
            val interactionSource = MutableInteractionSource()

            // Call with default parameters
            WysiwygEditor(state = state)

            // Call with attributeStyleResolver and onLinkClick
            WysiwygEditor(
                state = state,
                modifier = Modifier,
                readOnly = false,
                textStyle = TextStyle.Default,
                attributeStyleResolver = DefaultAttributeStyleResolver,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Black),
                onLinkClick = { _ -> },
                wysiwygState = wysiwygState,
            )
        }
        composableRef shouldBe composableRef
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for heading 1`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "# Heading 1")

        state.richString.text shouldBe "Heading 1"
        val headingSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(HeadingKey) }
        headingSpan shouldBe state.richString.spans.first()
        headingSpan?.attributes?.get(HeadingKey) shouldBe HeadingLevel.H1
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for heading 2`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "## Heading 2")

        state.richString.text shouldBe "Heading 2"
        val headingSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(HeadingKey) }
        headingSpan?.attributes?.get(HeadingKey) shouldBe HeadingLevel.H2
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for heading 3`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "### Heading 3")

        state.richString.text shouldBe "Heading 3"
        val headingSpan = state.richString.spans.firstOrNull { it.attributes.containsKey(HeadingKey) }
        headingSpan?.attributes?.get(HeadingKey) shouldBe HeadingLevel.H3
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for bullet list with dash`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "- Item")

        state.richString.text shouldBe "Item"
        state.richString.spans.any { it.attributes.containsKey(BulletListKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for bullet list with asterisk`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "* Item")

        state.richString.text shouldBe "Item"
        state.richString.spans.any { it.attributes.containsKey(BulletListKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for ordered list`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "1. Item")

        state.richString.text shouldBe "Item"
        state.richString.spans.any { it.attributes.containsKey(OrderedListKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers block auto-formatting for blockquote`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "> Quote")

        state.richString.text shouldBe "Quote"
        state.richString.spans.any { it.attributes.containsKey(BlockquoteKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers inline auto-formatting for bold`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "**bold**")

        state.richString.text shouldBe "bold"
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers inline auto-formatting for italic asterisk`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "*italic*")

        state.richString.text shouldBe "italic"
        state.richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers inline auto-formatting for italic underscore`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "_italic_")

        state.richString.text shouldBe "italic"
        state.richString.spans.any { it.attributes.containsKey(ItalicKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers inline auto-formatting for inline code`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "`code`")

        state.richString.text shouldBe "code"
        state.richString.spans.any { it.attributes.containsKey(CodeKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor triggers inline auto-formatting for strikethrough`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "~strike~")

        state.richString.text shouldBe "strike"
        state.richString.spans.any { it.attributes.containsKey(StrikethroughKey) } shouldBe true
    }

    @Test
    fun `WysiwygEditor prevents false positive formatting for words with underscores`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "foo_bar_baz")

        state.richString.text shouldBe "foo_bar_baz"
        state.richString.spans.shouldBeEmpty()
    }

    @Test
    fun `handleWysiwygKey intercepts backspace immediately after block auto-formatting and reverts`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "# ")

        // Heading format was applied to empty line
        wysiwygState.canRevert(state) shouldBe true

        // Press Backspace key
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )

        consumed shouldBe true
        state.richString.text shouldBe "# "
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun `handleWysiwygKey intercepts backspace immediately after inline auto-formatting and reverts`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "**test**")

        state.richString.text shouldBe "test"
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        wysiwygState.canRevert(state) shouldBe true

        // Press Backspace key
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )

        consumed shouldBe true
        state.richString.text shouldBe "**test**"
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()
        wysiwygState.canRevert(state) shouldBe false
    }

    @Test
    fun `handleWysiwygKey ignores backspace when no auto-format occurred`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "Normal text")

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
    fun `handleWysiwygKey ignores non-backspace keys and key up events`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "# ")

        // Key Up Backspace should not consume
        assertFalse(
            handleWysiwygKey(
                isKeyDown = false,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            ),
        )

        // Key Down Enter should not consume
        assertFalse(
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Enter,
                state = state,
                wysiwygState = wysiwygState,
            ),
        )
    }

    @Test
    fun `handleWysiwygKey repeated backspace consumes only first event and allows subsequent standard backspace`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "# ")

        // First Backspace consumes and reverts
        val firstConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        firstConsumed shouldBe true
        state.richString.text shouldBe "# "
        wysiwygState.canRevert(state) shouldBe false

        // Second Backspace does NOT consume, allowing normal deletion
        val secondConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        secondConsumed shouldBe false

        // Third Backspace does NOT consume
        val thirdConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        thirdConsumed shouldBe false
    }

    @Test
    fun `handleWysiwygKey repeated backspace for inline formatting consumes only first event`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "**bold**")

        val firstConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        firstConsumed shouldBe true
        state.richString.text shouldBe "**bold**"
        wysiwygState.canRevert(state) shouldBe false

        val secondConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        secondConsumed shouldBe false
    }

    @Test
    fun `handleWysiwygKey does not consume backspace after typing additional text following auto-format`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "# ")
        typeText(state, wysiwygState, transformation, "Title")

        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        consumed shouldBe false
        state.richString.text shouldBe "Title"
    }

    @Test
    fun `handleWysiwygKey does not consume backspace when cursor moves away after auto-format`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "**bold**")
        state.richString.text shouldBe "bold"

        // Move cursor away from post-format position
        state.textFieldState.edit {
            selection = TextRange(2)
        }

        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        consumed shouldBe false
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey does not consume backspace when text is selected`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "**bold**")
        state.richString.text shouldBe "bold"

        // Non-collapsed selection
        state.textFieldState.edit {
            selection = TextRange(0, 4)
        }

        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        consumed shouldBe false
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey rejects diverse non-backspace keys even after auto-format`() {
        val (state, wysiwygState, transformation) = setupEditor()
        typeText(state, wysiwygState, transformation, "# ")

        val nonBackspaceKeys =
            listOf(
                Key.Delete,
                Key.Spacebar,
                Key.A,
                Key.Escape,
                Key.Tab,
                Key.DirectionLeft,
                Key.DirectionRight,
            )

        for (testKey in nonBackspaceKeys) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = true,
                    key = testKey,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            consumed shouldBe false
        }

        // Key-up for all those keys must also not be consumed
        for (testKey in nonBackspaceKeys + Key.Backspace) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = false,
                    key = testKey,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            consumed shouldBe false
        }
    }

    @Test
    fun `side by side comparison WysiwygEditor auto-formats while RichTextEditor does not`() {
        val wysiwygHarness = createWysiwygHarness(isWysiwygEnabled = true)
        val richTextHarness = createWysiwygHarness(isWysiwygEnabled = false)

        val input = "# Title\n- List Item\n**Bold Content**"

        wysiwygHarness.typeText(input)
        richTextHarness.typeText(input)

        // WysiwygEditor formatted:
        wysiwygHarness.assertHeading(HeadingLevel.H1, range = 0..5)
        wysiwygHarness.assertBulletList(range = 6..15)
        wysiwygHarness.assertBold(range = 16..27)

        // RichTextEditor kept raw text:
        richTextHarness.assertText(input)
        richTextHarness.assertNotHeading()
        richTextHarness.assertNotBulletList()
        richTextHarness.assertNotBold()
    }

    private data class EditorFixture(
        val state: RichTextState,
        val wysiwygState: WysiwygState,
        val transformation: WysiwygInputTransformation,
    )

    private fun setupEditor(): EditorFixture {
        val state = RichTextState()
        val wysiwygState = WysiwygState()
        val transformation = WysiwygInputTransformation(state, wysiwygState)
        return EditorFixture(state, wysiwygState, transformation)
    }

    private fun typeText(
        state: RichTextState,
        wysiwygState: WysiwygState,
        transformation: WysiwygInputTransformation,
        text: String,
    ) {
        for (char in text) {
            val insertIndex = state.selection.start
            state.textFieldState.edit {
                replace(insertIndex, insertIndex, char.toString())
                selection = TextRange(insertIndex + 1)
                val buffer = this
                with(transformation) {
                    buffer.transformInput()
                }
            }
        }
    }
}
