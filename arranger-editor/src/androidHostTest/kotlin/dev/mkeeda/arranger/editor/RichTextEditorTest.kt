package dev.mkeeda.arranger.editor

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import dev.mkeeda.arranger.editor.wysiwyg.WysiwygEditor
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.VisualBlock
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.codeBlock
import dev.mkeeda.arranger.richtext.extractVisualBlocks
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
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
    fun `cursorBrush automatically follows textStyle color when unspecified`() {
        val style = TextStyle(color = Color.White)
        val resolved =
            resolveEffectiveCursorBrush(
                cursorBrush = SolidColor(Color.Unspecified),
                textStyle = style,
            )
        resolved shouldBe SolidColor(Color.White)
    }

    @Test
    fun `cursorBrush falls back to black when both cursorBrush and textStyle color are unspecified`() {
        val style = TextStyle.Default
        val resolved =
            resolveEffectiveCursorBrush(
                cursorBrush = SolidColor(Color.Unspecified),
                textStyle = style,
            )
        resolved shouldBe SolidColor(Color.Black)
    }

    @Test
    fun `explicit cursorBrush takes precedence`() {
        val customBrush = SolidColor(Color.Red)
        val style = TextStyle(color = Color.White)
        val resolved =
            resolveEffectiveCursorBrush(
                cursorBrush = customBrush,
                textStyle = style,
            )
        resolved shouldBe customBrush
    }

    @Test
    fun `custom gradient cursorBrush is preserved`() {
        val gradient = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val style = TextStyle(color = Color.White)
        val resolved =
            resolveEffectiveCursorBrush(
                cursorBrush = gradient,
                textStyle = style,
            )
        resolved shouldBe gradient
    }

    @Test
    fun `transparent cursorBrush is preserved when explicitly set`() {
        val transparentBrush = SolidColor(Color.Transparent)
        val style = TextStyle(color = Color.White)
        val resolved =
            resolveEffectiveCursorBrush(
                cursorBrush = transparentBrush,
                textStyle = style,
            )
        resolved shouldBe transparentBrush
    }

    @Test
    fun `cursorBrush follows semi-transparent textStyle color`() {
        val semiTransparent = Color(0x80FFFFFF)
        val style = TextStyle(color = semiTransparent)
        val resolved =
            resolveEffectiveCursorBrush(
                cursorBrush = SolidColor(Color.Unspecified),
                textStyle = style,
            )
        resolved shouldBe SolidColor(semiTransparent)
    }

    @Test
    fun `partial selection formatting applies custom colors accurately in dark mode`() {
        val text = "Arranger Dark Mode"
        val state = RichTextState(initialText = RichString(text))

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                textStyle = TextStyle(color = Color.White),
            )
        }

        // Select "Dark" (indices 9..13)
        composeTestRule.onNodeWithText(text).performTextInputSelection(TextRange(9, 13))
        state.applyFormat(TextColorKey, Color.Yellow.toRgbaColor())
        state.applyFormat(BackgroundColorKey, Color.DarkGray.toRgbaColor())

        val darkSpan = state.richString.spans.firstOrNull { it.range == 9..12 }
        darkSpan.shouldNotBeNull()
        darkSpan.attributes[TextColorKey] shouldBe Color.Yellow.toRgbaColor()
        darkSpan.attributes[BackgroundColorKey] shouldBe Color.DarkGray.toRgbaColor()
    }

    @Test
    fun `WysiwygEditor renders and accepts typing in dark theme with dynamic cursor brush`() {
        val state = RichTextState(initialText = RichString("Dark theme editor"))

        composeTestRule.setContent {
            WysiwygEditor(
                state = state,
                textStyle = TextStyle(color = Color.White),
            )
        }

        composeTestRule.onNodeWithText("Dark theme editor").performTextInput(" works")
        composeTestRule.waitForIdle()

        state.richString.text shouldBe "Dark theme editor works"
    }

    @Test
    fun `code block multiline editing preserves CodeBlockKey on non-empty line and exits on empty line`() {
        val initialText = "val a = 1"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        editAttributes(range = initialText.indices) {
                            codeBlock(language = "kotlin")
                        }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                blockDecorator = DefaultBlockDecorator,
            )
        }

        // Place cursor at end of first line
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))

        // Press Enter to create new line
        composeTestRule.onNodeWithText(initialText).performTextInput("\n")
        composeTestRule.waitForIdle()

        // Type code on the second line
        composeTestRule.onNodeWithText("val a = 1\n").performTextInput("val b = 2")
        composeTestRule.waitForIdle()

        state.richString.text shouldBe "val a = 1\nval b = 2"
        val blocks = state.richString.extractVisualBlocks()
        blocks shouldHaveSize 1
        blocks.first() shouldBe VisualBlock.CodeBlock(range = 0..18, language = "kotlin")

        // Press Enter after second line
        composeTestRule.onNodeWithText("val a = 1\nval b = 2").performTextInputSelection(TextRange(19))
        composeTestRule.onNodeWithText("val a = 1\nval b = 2").performTextInput("\n")
        composeTestRule.waitForIdle()

        // Press Enter again on the empty third line to exit code block
        composeTestRule.onNodeWithText("val a = 1\nval b = 2\n").performTextInput("\n")
        composeTestRule.waitForIdle()

        // Type normal text outside the code block
        composeTestRule.onNodeWithText("val a = 1\nval b = 2\n").performTextInput("val c = 3")
        composeTestRule.waitForIdle()

        state.richString.text shouldBe "val a = 1\nval b = 2\nval c = 3"
        val blocksAfterExit = state.richString.extractVisualBlocks()
        blocksAfterExit shouldHaveSize 1
        blocksAfterExit.first() shouldBe VisualBlock.CodeBlock(range = 0..19, language = "kotlin")
    }

    @Test
    fun `WysiwygEditor triggers code block with language on typing triple backtick and reverts on backspace`() {
        val state = RichTextState(initialText = RichString(""))

        composeTestRule.setContent {
            WysiwygEditor(
                state = state,
                blockDecorator = DefaultBlockDecorator,
            )
        }

        // Type "```kotlin " to trigger code block
        composeTestRule.onNodeWithText("").performTextInput("```kotlin ")
        composeTestRule.waitForIdle()

        state.richString.text shouldBe ""
        state.typingAttributes?.containsKey(CodeBlockKey) shouldBe true
        state.typingAttributes?.get(CodeBlockKey) shouldBe "kotlin"

        // Press Backspace to revert auto-format
        composeTestRule.onNodeWithText("").performKeyInput {
            keyDown(Key.Backspace)
            keyUp(Key.Backspace)
        }
        composeTestRule.waitForIdle()

        state.richString.text shouldBe "```kotlin "
        (state.typingAttributes?.containsKey(CodeBlockKey) ?: false) shouldBe false
    }
}
