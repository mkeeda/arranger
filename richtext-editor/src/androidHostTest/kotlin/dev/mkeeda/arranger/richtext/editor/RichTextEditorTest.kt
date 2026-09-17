package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.wysiwyg.WysiwygEditor
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextEditorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `spans shift synchronously when user edits text within RichTextEditor`() {
        val initialText = "Welcome to Arranger!"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        // "Arranger!" is length 9, at index 11
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Arranger!"))
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                styleResolver =
                    AttributeStyleResolver {
                        spanStyle(BoldKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                    },
            )
        }

        // Test editing: Replace "to " (length 3, indices 8..11) with "a" (length 1)
        // Original: "Welcome to Arranger!"
        // New     : "Welcome aArranger!"
        // Net change: -2 characters. "Arranger!" shifts from 11..19 to 9..17
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(8, 11))
        composeTestRule.onNodeWithText(initialText).performTextInput("a")

        val expectedNewText = "Welcome aArranger!"
        state.richString.text shouldBe expectedNewText

        val newSpans = state.richString.spans
        newSpans.size shouldBe 1

        // Assert the span accurately shifted
        newSpans.first().range shouldBe expectedNewText.rangeOf("Arranger!")
    }

    @Test
    fun `selection is exposed from TextFieldState`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Initial state should be no selection at the start of the text
        state.selection shouldBe TextRange(initialText.length)

        // Select "World" (indices 6 to 11)
        val selectionRange = TextRange(6, 11)
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(selectionRange)

        // The state should now reflect the selection
        state.selection shouldBe selectionRange
    }

    @Test
    fun `editAttributes correctly handles reversed selection range`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        val reversedSelection = TextRange(11, 6)

        // Apply formatting using editAttributes with the reversed selection
        state.edit {
            editAttributes(reversedSelection) {
                bold()
            }
        }

        // The formatting should be correctly applied to the min/max range (6..11)
        val newSpans = state.richString.spans
        newSpans.size shouldBe 1
        newSpans.first().range shouldBe (6 until 11)
    }

    @Test
    fun `programmatic edit and user edit produce identical spans`() {
        val initialText = "Hello World"
        val stateProgrammatic = RichTextState(initialText = RichString(initialText).edit { editAttributes { bold() } })
        val stateUser = RichTextState(initialText = RichString(initialText).edit { editAttributes { bold() } })

        // 1. Programmatic Edit
        stateProgrammatic.edit {
            replace(0..4, "Beautiful")
        }

        // 2. User Edit
        composeTestRule.setContent {
            RichTextEditor(state = stateUser)
        }
        // Select "Hello" and type "Beautiful"
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(0, 5))
        composeTestRule.onNodeWithText(initialText).performTextInput("Beautiful")

        // 3. Verify
        stateProgrammatic.richString.text shouldBe stateUser.richString.text

        // Assert that spans and paragraph spans are identical.
        // This guarantees `RichTextBuffer` shift logic is identical to `updateRichString`
        stateProgrammatic.richString.spans shouldBe stateUser.richString.spans
    }

    @Test
    fun `typing attributes are applied when text is entered`() {
        val initialText = "Hello "
        val state = RichTextState(initialText = RichString(text = initialText))

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Set cursor at the end
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Set typing attribute
        state.setTypingAttribute(BoldKey, Unit)

        // Type "World"
        composeTestRule.onNodeWithText(initialText).performTextInput("World")

        val expectedNewText = "Hello World"
        state.richString.text shouldBe expectedNewText

        // Assert that the newly typed text has the Bold attribute
        val newSpans = state.richString.spans
        newSpans.size shouldBe 1
        newSpans.first().range shouldBe expectedNewText.rangeOf("World")
        newSpans.first().attributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `typing attributes are cleared on cursor movement`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Set cursor at the end
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))
        composeTestRule.waitForIdle()

        // Set typing attribute
        state.setTypingAttribute(BoldKey, Unit)
        state.currentAttributes.containsKey(BoldKey) shouldBe true

        // Move cursor to the beginning
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(0))
        composeTestRule.waitForIdle()

        // Typing attributes should be cleared
        state.currentAttributes.containsKey(BoldKey) shouldBe false
        state.currentAttributes.isEmpty() shouldBe true
    }

    @Test
    fun `turned off attributes are not inherited when typing at the end of styled text`() {
        val initialText = "Hello"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.indices)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to the end of "Hello"
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // At this point, currentAttributes should have BoldKey due to inheritance
        state.currentAttributes.containsKey(BoldKey) shouldBe true

        // Remove the inherited BoldKey
        state.removeTypingAttribute(BoldKey)

        // currentAttributes should no longer have BoldKey
        state.currentAttributes.containsKey(BoldKey) shouldBe false

        // Type new text
        composeTestRule.onNodeWithText(initialText).performTextInput(" World")

        // Verify that the new text does NOT have BoldKey
        val newSpans = state.richString.spans
        val boldSpans = newSpans.filter { it.attributes.containsKey(BoldKey) }

        // Bold should only cover "Hello" (0..4)
        boldSpans.size shouldBe 1
        boldSpans[0].range shouldBe 0..4
    }

    @Test
    fun `turned off attributes are not inherited when typing inside styled text`() {
        val initialText = "Hello"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.indices)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to between 'l' and 'l' (index 3)
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(3))

        // Remove inherited BoldKey
        state.removeTypingAttribute(BoldKey)

        // Type new text
        composeTestRule.onNodeWithText(initialText).performTextInput("x")

        // The text is now "Helxlo"
        // Bold should cover "Hel" (0..2) and "lo" (4..5), but NOT "x" (3..3)
        val newSpans = state.richString.spans
        val boldSpans = newSpans.filter { it.attributes.containsKey(BoldKey) }

        boldSpans.size shouldBe 2
        boldSpans[0].range shouldBe 0..2
        boldSpans[1].range shouldBe 4..5
    }

    @Test
    fun `typing newline in heading clears heading attribute for the new paragraph`() {
        val initialText = "Heading"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setParagraphAttribute(HeadingKey, HeadingLevel.H1, initialText.indices)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to the end
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Type a newline and some text on the new paragraph sequentially
        composeTestRule.onNodeWithText(initialText).performTextInput("\n")
        composeTestRule.onNodeWithText("$initialText\n").performTextInput("New Paragraph")

        val expectedText = "Heading\nNew Paragraph"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        // The Heading attribute should NOT cover "New Paragraph" (index 8 onwards)
        spans.first().range shouldBe expectedText.rangeOf("Heading\n")
        spans.first().attributes shouldBe attributeContainerOf(HeadingKey to HeadingLevel.H1)
    }

    @Test
    fun `typing newline in list item inherits list attribute for the new paragraph`() {
        val initialText = "Item 1"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, initialText.indices)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to the end
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Type a newline and some text on the new paragraph sequentially
        composeTestRule.onNodeWithText(initialText).performTextInput("\n")
        composeTestRule.onNodeWithText("$initialText\n").performTextInput("Item 2")

        val expectedText = "Item 1\nItem 2"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe expectedText.indices
        spans.first().attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
    }

    @Test
    fun `typing newline in an empty list item outdents the list level`() {
        val initialText = "List\n"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level2, 0..initialText.length)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to the end (at the empty paragraph)
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Type a newline
        composeTestRule.onNodeWithText(initialText).performTextInput("\n")

        // The newline should be consumed by the outdent operation
        val expectedText = "List\n"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 2
        spans[0].range shouldBe expectedText.rangeOf("List\n")
        spans[0].attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level2)
        spans[1].range shouldBe (5..5)
        spans[1].attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
    }

    @Test
    fun `typing newline in an empty level 1 list item clears the list attribute`() {
        val initialText = "List\n"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..initialText.length)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to the end (at the empty paragraph)
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Type a newline
        composeTestRule.onNodeWithText(initialText).performTextInput("\n")

        val expectedText = "List\n"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 1
        spans.first().range shouldBe (0..4)
        spans.first().attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
    }

    @Test
    fun `deleting empty outdented list item with backspace preserves previous item level`() {
        val state =
            RichTextState(
                initialText =
                    RichString(text = "Item 1\nItem 2").edit {
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..6)
                        setParagraphAttribute(BulletListKey, ListIndentLevel.Level2, 7..12)
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        val initialText = "Item 1\nItem 2"
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Type a newline to create Level 2 empty item
        composeTestRule.onNodeWithText(initialText).performTextInput("\n")

        // Type a newline again to outdent the empty item to Level 1
        // (Wait, typing \n at empty item consumes the \n and outdents it. So text is still "Item 1\nItem 2\n")
        val textWithOneEnter = "Item 1\nItem 2\n"
        composeTestRule.onNodeWithText(textWithOneEnter).performTextInput("\n")

        // Now the text is "Item 1\nItem 2\n".
        // The span for "Item 2\n" (index 7..13) should be Level 2.
        // The span for the empty line at index 14..14 should be Level 1.
        val textAfterEnters = "Item 1\nItem 2\n"

        // Select the last newline character
        composeTestRule.onNodeWithText(
            textAfterEnters,
        ).performTextInputSelection(TextRange(textAfterEnters.length - 1, textAfterEnters.length))

        // Replace it with empty string (simulating Backspace).
        // Note: We use performTextInput("") instead of performKeyInput { pressKey(Key.Backspace) }
        // because performKeyInput is an ExperimentalTestApi and can be unreliable for triggering
        // exact TextFieldBuffer IME deletions in Robolectric environments.
        composeTestRule.onNodeWithText(textAfterEnters).performTextInput("")

        // Expected text: "Item 1\nItem 2"
        val expectedText = "Item 1\nItem 2"
        state.richString.text shouldBe expectedText

        val spans = state.richString.spans
        spans.size shouldBe 2
        spans[0].range shouldBe (0..6)
        spans[0].attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level1)
        spans[1].range shouldBe (7..12)
        spans[1].attributes shouldBe attributeContainerOf(BulletListKey to ListIndentLevel.Level2)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `undo and redo via keyboard shortcuts`() {
        val initialText = "Hello"
        val state = RichTextState(initialText = RichString(text = initialText))

        composeTestRule.setContent {
            RichTextEditor(state = state)
        }

        // Move cursor to the end
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Type " World"
        composeTestRule.onNodeWithText(initialText).performTextInput(" World")
        composeTestRule.waitForIdle()

        val expectedNewText = "Hello World"
        state.richString.text shouldBe expectedNewText

        // Simulate Ctrl+Z (Undo)
        composeTestRule.onNodeWithText(expectedNewText).performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.Z)
            keyUp(Key.Z)
            keyUp(Key.CtrlLeft)
        }
        composeTestRule.waitForIdle()

        // `performTextInput(" World")` is delivered as a single bulk operation, creating one undo entry.
        // A single Ctrl+Z should therefore revert the entire " World" addition.
        state.richString.text shouldBe "Hello"

        // Simulate Ctrl+Shift+Z (Redo)
        composeTestRule.onNodeWithText("Hello").performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            keyDown(Key.Z)
            keyUp(Key.Z)
            keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
        composeTestRule.waitForIdle()

        state.richString.text shouldBe "Hello World"
    }

    @Test
    fun `tapping on custom span triggers onSpanClick with accurate RichSpan and consumes event when consume is called`() {
        val initialText = "Hello @mkeeda welcome"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap plain text character 'H' (index 0) first to position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)
        clickedSpan shouldBe null

        // Tap on '@' (index 6) inside the mention span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        clickedSpan?.range shouldBe initialText.rangeOf("@mkeeda")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        // Cursor placement must be suppressed because consume() was called
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `onSpanClick without calling consume on custom span allows normal cursor placement`() {
        val initialText = "Hello @mkeeda welcome"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    // Note: event.consume() is intentionally NOT called
                },
            )
        }

        // Tap on plain text 'H' (index 0) to position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // Tap on '@' (index 6) inside the mention span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        clickedSpan?.range shouldBe initialText.rangeOf("@mkeeda")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        // Since consume() was not called, cursor should be placed normally at index 6
        state.selection shouldBe TextRange(6)
    }

    @Test
    fun `tapping on hyperlink triggers onSpanClick allowing user to open uri and consume event`() {
        val initialText = "Click here"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", initialText.rangeOf("here"))
                    },
            )

        var openedUri: String? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    val url = event.span.attributes[LinkKey]
                    if (!url.isNullOrEmpty()) {
                        openedUri = url
                        event.consume()
                    }
                },
            )
        }

        // Tap on plain text 'C' (index 0)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        openedUri shouldBe null
        state.selection shouldBe TextRange(0)

        // Tap on hyperlink "here" (index 6)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        openedUri shouldBe "https://example.com"
        // Cursor placement must be suppressed by event.consume()
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `tapping on hyperlink with onSpanClick null does not consume event and moves cursor`() {
        val initialText = "Click here"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", initialText.rangeOf("here"))
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = null,
            )
        }

        // Initially position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // Tap on hyperlink "here" (index 6) with no handler
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        // Cursor should move to index 6 without being intercepted
        state.selection shouldBe TextRange(6)
    }

    @Test
    fun `tapping on plain text does not trigger onSpanClick and allows cursor placement`() {
        val initialText = "Hello @mkeeda welcome"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var onSpanClickCalled = false
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = {
                    onSpanClickCalled = true
                    it.consume()
                },
            )
        }

        // Tap on plain text 'H' (index 0)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)

        onSpanClickCalled shouldBe false
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `WysiwygEditor correctly propagates onSpanClick`() {
        val initialText = "Check @mkeeda text"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            WysiwygEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)
        clickedSpan shouldBe null

        // Tap on '@' (index 6) inside the mention span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        clickedSpan?.range shouldBe initialText.rangeOf("@mkeeda")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        // Cursor placement suppressed due to consume()
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `tapping on exact start and end characters of a span triggers onSpanClick, but immediate surrounding characters do not`() {
        val initialText = "ABC Hello XYZ"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@Hello", initialText.rangeOf("Hello"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on character 'C' (index 2) - plain text before span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(2)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(2)
        clickedSpan shouldBe null

        // Tap on space (index 3) - immediate predecessor to span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(3)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(3)
        clickedSpan shouldBe null

        // Tap on 'H' (index 4) - exact start of span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(4)
        composeTestRule.waitForIdle()
        clickedSpan?.range shouldBe initialText.rangeOf("Hello")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@Hello"
        state.selection shouldBe TextRange(3) // suppressed by consume()

        clickedSpan = null

        // Tap on 'o' (index 8) - exact end of span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(8)
        composeTestRule.waitForIdle()
        clickedSpan?.range shouldBe initialText.rangeOf("Hello")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@Hello"
        state.selection shouldBe TextRange(3) // suppressed by consume()

        clickedSpan = null

        // Tap on space (index 9) - immediate successor to span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(9)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(9)
        clickedSpan shouldBe null

        // Tap on 'X' (index 10) - plain text after span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(10)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(10)
        clickedSpan shouldBe null
    }

    @Test
    fun `tapping consecutive adjacent spans with different attributes triggers onSpanClick with exact corresponding span`() {
        val initialText = "@alice#tech"
        val aliceRange = initialText.rangeOf("@alice")
        val techRange = initialText.rangeOf("#tech")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@alice", aliceRange)
                        setSpanAttribute(HashtagKey, "#tech", techRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on 'e' (index 5) - last character of @alice
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(5)
        clickedSpan?.range shouldBe aliceRange
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@alice"
        clickedSpan?.attributes?.containsKey(HashtagKey) shouldBe false

        clickedSpan = null

        // Tap on '#' (index 6) - first character of #tech
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)
        clickedSpan?.range shouldBe techRange
        clickedSpan?.attributes?.get(HashtagKey) shouldBe "#tech"
        clickedSpan?.attributes?.containsKey(MentionKey) shouldBe false
    }

    @Test
    fun `tapping span with multiple overlapping attributes provides complete attribute container in SpanClickEvent`() {
        val initialText = "Visit Google today"
        val googleRange = initialText.rangeOf("Google")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(BoldKey, Unit, initialText.rangeOf("Visit Google"))
                        setSpanAttribute(LinkKey, "https://google.com", googleRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on 'G' (index 6) of Google
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)
        clickedSpan?.range shouldBe googleRange
        clickedSpan?.attributes?.get(LinkKey) shouldBe "https://google.com"
        clickedSpan?.attributes?.containsKey(BoldKey) shouldBe true

        clickedSpan = null

        // Tap on 'V' (index 0) of Visit - Bold only (no link, but it's a span!)
        // Wait: Visit has Bold attribute, which is a span attribute!
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        // Since onSpanClick receives ANY RichSpan, Visit is also a RichSpan (with BoldKey)!
        clickedSpan?.range shouldBe initialText.rangeOf("Visit ")
        clickedSpan?.attributes?.containsKey(BoldKey) shouldBe true
        clickedSpan?.attributes?.containsKey(LinkKey) shouldBe false
    }

    @Test
    fun `tapping across multiple lines within a single span triggers onSpanClick on all lines`() {
        val initialText = "Line One\nLine Two\nLine Three"
        val multilineRange = 0..initialText.rangeOf("Line Two").last
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", multilineRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on 'n' in "One" (index 6, line 1)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)
        clickedSpan?.range shouldBe multilineRange
        clickedSpan?.attributes?.get(LinkKey) shouldBe "https://example.com"

        clickedSpan = null

        // Tap on 'T' in "Two" (index 14, line 2)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(14)
        clickedSpan?.range shouldBe multilineRange
        clickedSpan?.attributes?.get(LinkKey) shouldBe "https://example.com"

        clickedSpan = null

        // Tap on 'T' in "Three" (index 23, line 3, outside span)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(23)
        clickedSpan shouldBe null
        state.selection shouldBe TextRange(23)
    }

    @Test
    fun `repeated taps properly isolate SpanClickEvent state and avoid stale consumption`() {
        val initialText = "Click @one then @two here"
        val oneRange = initialText.rangeOf("@one")
        val twoRange = initialText.rangeOf("@two")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@one", oneRange)
                        setSpanAttribute(MentionKey, "@two", twoRange)
                    },
            )

        var lastEvent: SpanClickEvent? = null
        var shouldConsume = true
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    lastEvent = event
                    if (shouldConsume) {
                        event.consume()
                    }
                },
            )
        }

        // Tap plain text first
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // 1. Tap @one and consume
        shouldConsume = true
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(oneRange.first)
        lastEvent?.span?.range shouldBe oneRange
        lastEvent?.isConsumed shouldBe true
        state.selection shouldBe TextRange(0) // unchanged because consumed

        // 2. Tap @two and do NOT consume
        shouldConsume = false
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(twoRange.first)
        lastEvent?.span?.range shouldBe twoRange
        lastEvent?.isConsumed shouldBe false
        state.selection shouldBe TextRange(twoRange.first) // cursor moved

        // 3. Tap plain text "here" (index 22)
        lastEvent = null
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(22)
        lastEvent shouldBe null
        state.selection shouldBe TextRange(22) // cursor moved
    }

    @Test
    fun `tapping on span in readOnly editor triggers onSpanClick and suppresses cursor move when consumed`() {
        val initialText = "Read only @mkeeda text"
        val mentionRange = initialText.rangeOf("@mkeeda")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", mentionRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                readOnly = true,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap plain text character 'R' (index 0)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // Tap on mention in readOnly mode
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(mentionRange.first)
        clickedSpan?.range shouldBe mentionRange
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        state.selection shouldBe TextRange(0) // suppressed by consume()
    }

    @Test
    fun `tapping on span in disabled editor does not trigger onSpanClick`() {
        val initialText = "Disabled @mkeeda text"
        val mentionRange = initialText.rangeOf("@mkeeda")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", mentionRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                enabled = false,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on mention in disabled mode
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(mentionRange.first)

        // When editor is disabled, user interaction should be completely disabled!
        clickedSpan shouldBe null
    }
}

private object MentionKey : SpanAttributeKey<String> {
    override val name: String = "mention"
    override val defaultValue: String = ""
}

private object HashtagKey : SpanAttributeKey<String> {
    override val name: String = "hashtag"
    override val defaultValue: String = ""
}

private fun SemanticsNodeInteraction.clickOnCharacter(index: Int) {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(textLayoutResults)
    val layoutResult = textLayoutResults.first()
    val boundingBox = layoutResult.getBoundingBox(index)
    performTouchInput {
        advanceEventTime(1000)
        click(boundingBox.center)
    }
}
