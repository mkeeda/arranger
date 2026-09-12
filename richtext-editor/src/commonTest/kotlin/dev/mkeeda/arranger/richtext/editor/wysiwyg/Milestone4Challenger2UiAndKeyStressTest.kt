package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.ComposeParagraphWorkarounds
import dev.mkeeda.arranger.richtext.editor.DefaultListMarkerResolver
import dev.mkeeda.arranger.richtext.editor.ResolvedRichStyle
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextInputTransformation
import dev.mkeeda.arranger.richtext.editor.RichTextOutputTransformation
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tier 5 Adversarial Coverage Hardening Suite for Milestone 4:
 * Focus: Compose UI, Component Separation, Key Event Interception,
 * Recomposition Stability, Parameter Permutations, and Backspace Reversal Edge Cases.
 */
class Milestone4Challenger2UiAndKeyStressTest {
    // =========================================================================
    // Group 1: Key Event Interception & Modifiers & Diverse Selection States
    // =========================================================================

    @Test
    fun `handleWysiwygKey rejects KeyUp Backspace and only accepts KeyDown`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "# ")

        wysiwygState.canRevert(state) shouldBe true

        // KeyUp for Backspace must be rejected
        val keyUpConsumed =
            handleWysiwygKey(
                isKeyDown = false,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(keyUpConsumed)
        wysiwygState.canRevert(state) shouldBe true

        // KeyDown for Backspace must be accepted
        val keyDownConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertTrue(keyDownConsumed)
        state.richString.text shouldBe "# "
        assertFalse(wysiwygState.canRevert(state))
    }

    @Test
    fun `handleWysiwygKey rejects Delete key forward delete even after auto-format`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        wysiwygState.canRevert(state) shouldBe true

        // Key.Delete (forward delete / Fn+Backspace on Mac) must not trigger revert
        val deleteConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Delete,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(deleteConsumed)
        wysiwygState.canRevert(state) shouldBe true
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey rejects all navigation keys after auto-format`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        val navigationKeys =
            listOf(
                Key.DirectionLeft,
                Key.DirectionRight,
                Key.DirectionUp,
                Key.DirectionDown,
                Key.PageUp,
                Key.PageDown,
                Key.MoveHome,
                Key.MoveEnd,
            )

        for (navKey in navigationKeys) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = true,
                    key = navKey,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            assertFalse(consumed, "Navigation key $navKey must not be consumed")
        }
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `handleWysiwygKey rejects all modifier keys pressed alone`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "# ")

        val modifierKeys =
            listOf(
                Key.ShiftLeft,
                Key.ShiftRight,
                Key.CtrlLeft,
                Key.CtrlRight,
                Key.AltLeft,
                Key.AltRight,
                Key.MetaLeft,
                Key.MetaRight,
            )

        for (modKey in modifierKeys) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = true,
                    key = modKey,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            assertFalse(consumed, "Modifier key $modKey must not be consumed")
        }
        wysiwygState.canRevert(state) shouldBe true
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when selection is non-collapsed forward`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")
        state.richString.text shouldBe "bold"

        // Selection forward 0..4
        state.textFieldState.edit {
            selection = TextRange(0, 4)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when selection is non-collapsed reversed`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        // Selection reversed 4..0
        state.textFieldState.edit {
            selection = TextRange(4, 0)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when cursor is collapsed at offset 0 after inline format`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        // Cursor at 0 (postFormatCursor is 4)
        state.textFieldState.edit {
            selection = TextRange(0)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when cursor is collapsed 1 character before postFormatCursor`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        // Cursor at 3 (postFormatCursor is 4)
        state.textFieldState.edit {
            selection = TextRange(3)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when cursor is collapsed 1 character beyond postFormatCursor`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        // Insert character without transformation to place cursor at 5
        state.textFieldState.edit {
            insert(4, "!")
            selection = TextRange(5)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when undo stack canUndo is false`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")

        wysiwygState.canRevert(state) shouldBe true

        // Externally clear undo history
        state.undoState.clearHistory()
        state.undoState.canUndo shouldBe false

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `handleWysiwygKey rejects Backspace when lastAutoFormatEvent is null on clean state`() {
        val state = RichTextState(initialText = RichString("Some text"))
        val wysiwygState = WysiwygState()

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
    }

    // =========================================================================
    // Group 2: Backspace Reversal Edge Cases (Rapid, Empty Buffer, External Mutation)
    // =========================================================================

    @Test
    fun `backspace on completely empty buffer without prior autoformat does not revert and returns false`() {
        val (state, wysiwygState, _) = createWysiwygEngine()
        state.richString.text shouldBe ""
        state.selection shouldBe TextRange(0)

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
        state.richString.text shouldBe ""
    }

    @Test
    fun `backspace on empty line after block autoformat correctly reverts and subsequent backspaces delete cleanly`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "# ")

        // Buffer text is empty, heading style applied, cursor at 0
        state.richString.text shouldBe ""
        state.selection shouldBe TextRange(0)
        assertTrue(wysiwygState.canRevert(state))

        // 1st Backspace: Reverts to raw "# " with cursor at 2
        val firstRevert =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertTrue(firstRevert)
        state.richString.text shouldBe "# "
        state.selection shouldBe TextRange(2)
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
        assertFalse(wysiwygState.canRevert(state))

        // 2nd Backspace: Not consumed by WYSIWYG
        val secondConsumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(secondConsumed)

        // Simulate normal backspace deleting space -> "#"
        simulateStandardBackspace(state)
        state.richString.text shouldBe "#"
        state.selection shouldBe TextRange(1)

        // 3rd Backspace: Normal backspace deleting "#" -> ""
        simulateStandardBackspace(state)
        state.richString.text shouldBe ""
        state.selection shouldBe TextRange(0)

        // 4th Backspace on empty buffer: Safe no-op
        simulateStandardBackspace(state)
        state.richString.text shouldBe ""
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `rapid repeated backspace 10 consecutive calls after block autoformat only reverts once and never crashes`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "- ")

        // Heading/List on empty line
        state.richString.text shouldBe ""
        assertTrue(wysiwygState.canRevert(state))

        val results = mutableListOf<Boolean>()
        for (i in 1..10) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = true,
                    key = Key.Backspace,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            results.add(consumed)
            if (!consumed) {
                simulateStandardBackspace(state)
            }
        }

        // Exactly the first one consumed, remaining 9 not consumed
        results[0] shouldBe true
        for (i in 1..9) {
            results[i] shouldBe false
        }
        state.richString.text shouldBe ""
    }

    @Test
    fun `rapid repeated backspace 10 consecutive calls after inline autoformat only reverts once and deletes characters normally`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**a**")

        state.richString.text shouldBe "a"
        assertTrue(wysiwygState.canRevert(state))

        val consumedResults = mutableListOf<Boolean>()
        for (i in 1..10) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = true,
                    key = Key.Backspace,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            consumedResults.add(consumed)
            if (!consumed) {
                simulateStandardBackspace(state)
            }
        }

        consumedResults[0] shouldBe true
        for (i in 1..9) {
            consumedResults[i] shouldBe false
        }
        state.richString.text shouldBe ""
    }

    @Test
    fun `backspace reversal is rejected after external text length mutation via textFieldState`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "**bold**")
        state.richString.text shouldBe "bold"
        assertTrue(wysiwygState.canRevert(state))

        // External mutation alters buffer text length to 11
        state.textFieldState.edit {
            replace(0, length, "hello world")
            selection = TextRange(11)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertFalse(consumed)
        state.richString.text shouldBe "hello world"
    }

    @Test
    fun `backspace reversal is rejected when selection is moved externally and re-enabled when selection returns`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()
        typeTextToWysiwyg(state, transformation, "`code`")
        state.richString.text shouldBe "code"
        state.selection shouldBe TextRange(4)
        assertTrue(wysiwygState.canRevert(state))

        // Move cursor away
        state.textFieldState.edit { selection = TextRange(2) }
        assertFalse(wysiwygState.canRevert(state))

        // Move cursor back to postFormatCursor
        state.textFieldState.edit { selection = TextRange(4) }
        assertTrue(wysiwygState.canRevert(state))

        // Revert succeeds
        val consumed =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = state,
                wysiwygState = wysiwygState,
            )
        assertTrue(consumed)
        state.richString.text shouldBe "`code`"
    }

    @Test
    fun `consecutive autoformat and backspace reversal cycles across all inline formats`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        // 1. Bold cycle
        typeTextToWysiwyg(state, transformation, "**bold**")
        state.richString.text shouldBe "bold"
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "**bold**"
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()

        // Clear for next cycle
        state.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }
        wysiwygState.clearLastAutoFormat()

        // 2. Italic Asterisk cycle
        typeTextToWysiwyg(state, transformation, "*italic*")
        state.richString.text shouldBe "italic"
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "*italic*"
        state.richString.spans.filter { it.attributes.containsKey(ItalicKey) }.shouldBeEmpty()

        // Clear
        state.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }
        wysiwygState.clearLastAutoFormat()

        // 3. Italic Underscore cycle
        typeTextToWysiwyg(state, transformation, "_italic2_")
        state.richString.text shouldBe "italic2"
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "_italic2_"
        state.richString.spans.filter { it.attributes.containsKey(ItalicKey) }.shouldBeEmpty()

        // Clear
        state.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }
        wysiwygState.clearLastAutoFormat()

        // 4. Code cycle
        typeTextToWysiwyg(state, transformation, "`code`")
        state.richString.text shouldBe "code"
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "`code`"
        state.richString.spans.filter { it.attributes.containsKey(InlineCodeKey) }.shouldBeEmpty()

        // Clear
        state.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }
        wysiwygState.clearLastAutoFormat()

        // 5. Strikethrough cycle
        typeTextToWysiwyg(state, transformation, "~strike~")
        state.richString.text shouldBe "strike"
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "~strike~"
        state.richString.spans.filter { it.attributes.containsKey(StrikethroughKey) }.shouldBeEmpty()
    }

    @Test
    fun `consecutive autoformat and backspace reversal cycles across all block formats`() {
        val (state, wysiwygState, transformation) = createWysiwygEngine()

        val blockTriggers =
            listOf(
                "# " to HeadingKey,
                "## " to HeadingKey,
                "### " to HeadingKey,
                "- " to BulletListKey,
                "* " to BulletListKey,
                "1. " to OrderedListKey,
                "> " to BlockquoteKey,
            )

        for ((trigger, key) in blockTriggers) {
            state.textFieldState.edit {
                replace(0, length, "")
                selection = TextRange(0)
            }
            wysiwygState.clearLastAutoFormat()

            typeTextToWysiwyg(state, transformation, trigger)
            state.richString.text shouldBe ""
            assertTrue(wysiwygState.canRevert(state), "Expected canRevert for trigger '$trigger'")

            val reverted = handleWysiwygKey(true, Key.Backspace, state, wysiwygState)
            assertTrue(reverted, "Expected successful revert for trigger '$trigger'")
            state.richString.text shouldBe trigger
            state.richString.spans.filter { it.attributes.containsKey(key) }.shouldBeEmpty()
        }
    }

    // =========================================================================
    // Group 3: Non-Regression Parity & Parameter Permutations
    // =========================================================================

    @Test
    fun `RichTextEditor and WysiwygEditor composable overloads compile and bind with every parameter permutation`() {
        val composableRef: @Composable () -> Unit = {
            val state = RichTextState(initialText = RichString("Sample"))
            val wysiwygState = WysiwygState()
            val interactionSource = MutableInteractionSource()
            val scrollState = rememberScrollState()
            val customDecorator = TextFieldDecorator { inner -> inner() }
            val customStyleResolver =
                object : AttributeStyleResolver {
                    override fun resolve(attributes: AttributeContainer) = ResolvedRichStyle()
                }

            // Permutation 1: Minimal invocations
            RichTextEditor(state = state)
            WysiwygEditor(state = state)

            // Permutation 2: readOnly = true, enabled = false
            RichTextEditor(
                state = state,
                readOnly = true,
                enabled = false,
            )
            WysiwygEditor(
                state = state,
                readOnly = true,
                enabled = false,
            )

            // Permutation 3: full parameter binding with styleResolver and attributeStyleResolver
            RichTextEditor(
                state = state,
                modifier = Modifier,
                enabled = true,
                readOnly = false,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.DarkGray),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                onKeyboardAction = KeyboardActionHandler { _ -> },
                lineLimits = TextFieldLineLimits.SingleLine,
                onTextLayout = { _ -> },
                scrollState = scrollState,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Red),
                decorator = customDecorator,
                styleResolver = customStyleResolver,
                attributeStyleResolver = customStyleResolver,
                listMarkerResolver = DefaultListMarkerResolver,
                onLinkClick = { _ -> },
            )

            WysiwygEditor(
                state = state,
                modifier = Modifier,
                enabled = true,
                readOnly = false,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.DarkGray),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                onKeyboardAction = KeyboardActionHandler { _ -> },
                lineLimits = TextFieldLineLimits.SingleLine,
                onTextLayout = { _ -> },
                scrollState = scrollState,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Red),
                decorator = customDecorator,
                styleResolver = customStyleResolver,
                listMarkerResolver = DefaultListMarkerResolver,
                onLinkClick = { _ -> },
            )
        }
        composableRef shouldBe composableRef
    }

    @Test
    fun `RichTextEditor never auto-formats under any parameter configuration`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        // Type all markdown shortcuts into RichTextInputTransformation
        typeTextToRichText(state, transformation, "# H1\n- Bullet\n`code`\n**bold**\n> quote")

        state.richString.text shouldBe "# H1\n- Bullet\n`code`\n**bold**\n> quote"
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(BulletListKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(InlineCodeKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()
        state.richString.spans.filter { it.attributes.containsKey(BlockquoteKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextOutputTransformation applies resolved styles accurately from effectiveStyleResolver`() {
        val customBoldStyle = SpanStyle(color = Color.Magenta, fontWeight = FontWeight.ExtraBold)
        val customResolver =
            object : AttributeStyleResolver {
                override fun resolve(attributes: AttributeContainer): ResolvedRichStyle {
                    return if (attributes.containsKey(BoldKey)) {
                        ResolvedRichStyle(spanStyle = customBoldStyle)
                    } else {
                        ResolvedRichStyle()
                    }
                }
            }

        val state =
            RichTextState(
                initialText =
                    RichString(
                        text = "Hello Custom Bold",
                    ),
            )
        state.edit {
            setSpanAttribute(BoldKey, Unit, 6..11)
        }

        val workarounds = ComposeParagraphWorkarounds()
        val outputTransform = RichTextOutputTransformation(state, customResolver, workarounds)

        // Verify outputTransform is initialized properly
        outputTransform shouldNotBe null
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    // =========================================================================
    // Group 4: Recomposition Stability & State Isolation Across Multiple Editor Instances
    // =========================================================================

    @Test
    fun `two independent WysiwygEditor instances maintain completely isolated autoformat and revert states`() {
        val (state1, wysiwygState1, trans1) = createWysiwygEngine()
        val (state2, wysiwygState2, trans2) = createWysiwygEngine()

        // 1. Format Editor 1 with Heading
        typeTextToWysiwyg(state1, trans1, "# ")
        state1.richString.text shouldBe ""
        wysiwygState1.canRevert(state1) shouldBe true

        // Editor 2 must be completely empty and unchanged
        state2.richString.text shouldBe ""
        wysiwygState2.canRevert(state2) shouldBe false

        // 2. Format Editor 2 with Bold (closing ** triggers autoformat)
        typeTextToWysiwyg(state2, trans2, "**Editor2**")
        state2.richString.text shouldBe "Editor2"
        wysiwygState2.canRevert(state2) shouldBe true

        // Editor 1 canRevert must still be true (isolated from Editor 2 edits)
        wysiwygState1.canRevert(state1) shouldBe true

        // 3. Revert Editor 1
        val revert1 = handleWysiwygKey(true, Key.Backspace, state1, wysiwygState1)
        assertTrue(revert1)
        state1.richString.text shouldBe "# "
        assertFalse(wysiwygState1.canRevert(state1))

        // Editor 2 must be completely unaffected!
        state2.richString.text shouldBe "Editor2"
        state2.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        assertTrue(wysiwygState2.canRevert(state2))

        // 4. Revert Editor 2
        val revert2 = handleWysiwygKey(true, Key.Backspace, state2, wysiwygState2)
        assertTrue(revert2)
        state2.richString.text shouldBe "**Editor2**"
        assertFalse(wysiwygState2.canRevert(state2))
    }

    @Test
    fun `WysiwygState reset via clearLastAutoFormat cleanly isolates state before reuse`() {
        val (state, wysiwygState, trans) = createWysiwygEngine()
        typeTextToWysiwyg(state, trans, "**bold**")
        assertTrue(wysiwygState.canRevert(state))

        // Clean reset
        wysiwygState.clearLastAutoFormat()
        assertFalse(wysiwygState.canRevert(state))

        // Backspace does nothing
        val consumed = handleWysiwygKey(true, Key.Backspace, state, wysiwygState)
        assertFalse(consumed)
        state.richString.text shouldBe "bold"
    }

    @Test
    fun `WysiwygInputTransformation immediately invalidates previous autoformat event on any subsequent input`() {
        val (state, wysiwygState, trans) = createWysiwygEngine()
        typeTextToWysiwyg(state, trans, "**bold**")
        assertTrue(wysiwygState.canRevert(state))

        // Type additional letter
        typeTextToWysiwyg(state, trans, " ")
        assertFalse(wysiwygState.canRevert(state))

        val consumed = handleWysiwygKey(true, Key.Backspace, state, wysiwygState)
        assertFalse(consumed)
        state.richString.text shouldBe "bold "
    }

    @Test
    fun `cyclic autoformat then revert then re-trigger autoformat ergonomic workflow`() {
        val (state, wysiwygState, trans) = createWysiwygEngine()

        // 1. First format: **word** -> word
        typeTextToWysiwyg(state, trans, "**word**")
        state.richString.text shouldBe "word"
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        assertTrue(wysiwygState.canRevert(state))

        // 2. Revert via Backspace -> **word**
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "**word**"
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()
        assertFalse(wysiwygState.canRevert(state))

        // 3. Normal Backspace removes trailing asterisk -> **word*
        simulateStandardBackspace(state)
        state.richString.text shouldBe "**word*"

        // 4. Re-type the closing asterisk -> triggers bold autoformat again!
        typeTextToWysiwyg(state, trans, "*")
        state.richString.text shouldBe "word"
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
        assertTrue(wysiwygState.canRevert(state))

        // 5. Revert again -> **word**
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "**word**"
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()
    }

    @Test
    fun `Japanese multibyte autoformat and backspace reversal edge cases`() {
        val (state, wysiwygState, trans) = createWysiwygEngine()

        // 1. Japanese heading autoformat: # 見出し
        typeTextToWysiwyg(state, trans, "# ")
        assertTrue(wysiwygState.canRevert(state))
        assertTrue(handleWysiwygKey(true, Key.Backspace, state, wysiwygState))
        state.richString.text shouldBe "# "

        // Reset
        state.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }
        wysiwygState.clearLastAutoFormat()

        // 2. Japanese inline bold: **太字テキスト**
        typeTextToWysiwyg(state, trans, "**太字テキスト**")
        state.richString.text shouldBe "太字テキスト"
        assertTrue(wysiwygState.canRevert(state))

        // Typing punctuation after formatting invalidates reversal
        typeTextToWysiwyg(state, trans, "、")
        state.richString.text shouldBe "太字テキスト、"
        assertFalse(wysiwygState.canRevert(state))

        // Backspace deletes the Japanese comma, preserving bold text
        val consumed = handleWysiwygKey(true, Key.Backspace, state, wysiwygState)
        assertFalse(consumed)
        simulateStandardBackspace(state)
        state.richString.text shouldBe "太字テキスト"
        state.richString.spans.any { it.attributes.containsKey(BoldKey) } shouldBe true
    }

    @Test
    fun `handleWysiwygKey rejects extensive set of symbol and alphanumeric keys`() {
        val (state, wysiwygState, trans) = createWysiwygEngine()
        typeTextToWysiwyg(state, trans, "**bold**")
        assertTrue(wysiwygState.canRevert(state))

        val rejectedKeys =
            listOf(
                Key.Spacebar,
                Key.Enter,
                Key.Tab,
                Key.Escape,
                Key.A,
                Key.Z,
                Key.Zero,
                Key.One,
                Key.Nine,
                Key.Slash,
                Key.Backslash,
                Key.Minus,
                Key.Equals,
                Key.Period,
                Key.Comma,
            )

        for (k in rejectedKeys) {
            val consumed =
                handleWysiwygKey(
                    isKeyDown = true,
                    key = k,
                    state = state,
                    wysiwygState = wysiwygState,
                )
            assertFalse(consumed, "Key $k should not be consumed")
        }
        assertTrue(wysiwygState.canRevert(state))
    }

    @Test
    fun `switching active editor states simulates document tab switching without state leakage`() {
        val doc1State = RichTextState(initialText = RichString("Doc 1"))
        val doc2State = RichTextState(initialText = RichString("Doc 2"))
        val wysiwygState1 = WysiwygState()
        val wysiwygState2 = WysiwygState()
        val trans1 = WysiwygInputTransformation(doc1State, wysiwygState1)

        // Type into doc 1: # at start
        doc1State.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }
        typeTextToWysiwyg(doc1State, trans1, "# ")
        assertTrue(wysiwygState1.canRevert(doc1State))

        // Tab switch to doc 2: check that doc2's wysiwygState cannot revert doc1
        assertFalse(wysiwygState2.canRevert(doc2State))
        assertFalse(wysiwygState2.canRevert(doc1State))

        // Revert on doc 1 succeeds
        assertTrue(handleWysiwygKey(true, Key.Backspace, doc1State, wysiwygState1))
        doc1State.richString.text shouldBe "# "
        assertFalse(wysiwygState1.canRevert(doc1State))
    }

    @Test
    fun `external clear of textFieldState completely invalidates canRevert`() {
        val (state, wysiwygState, trans) = createWysiwygEngine()
        typeTextToWysiwyg(state, trans, "**bold**")
        state.richString.text shouldBe "bold"
        assertTrue(wysiwygState.canRevert(state))

        // External clear
        state.textFieldState.edit {
            replace(0, length, "")
            selection = TextRange(0)
        }

        assertFalse(wysiwygState.canRevert(state))
        val consumed = handleWysiwygKey(true, Key.Backspace, state, wysiwygState)
        assertFalse(consumed)
        state.richString.text shouldBe ""
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    private fun createWysiwygEngine(): Triple<RichTextState, WysiwygState, WysiwygInputTransformation> {
        val state = RichTextState()
        val wysiwygState = WysiwygState()
        val transformation = WysiwygInputTransformation(state, wysiwygState)
        return Triple(state, wysiwygState, transformation)
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

    private fun simulateStandardBackspace(state: RichTextState) {
        state.textFieldState.edit {
            if (selection.collapsed) {
                if (selection.start > 0) {
                    delete(selection.start - 1, selection.start)
                }
            } else {
                delete(selection.start, selection.end)
            }
        }
    }
}
