package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bold
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

        // Set typing attribute
        state.setTypingAttribute(BoldKey, Unit)
        state.currentAttributes.containsKey(BoldKey) shouldBe true

        // Move cursor to the beginning
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(0))

        // Typing attributes should be cleared
        state.currentAttributes.containsKey(BoldKey) shouldBe false
        state.currentAttributes.isEmpty() shouldBe true
    }
}
