package dev.mkeeda.arranger.sampleApp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatInputSampleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `initial state shows empty editor and formatting buttons`() {
        composeTestRule.setContent {
            ArrangerTheme {
                ChatInputSample()
            }
        }

        // Check if toolbar buttons exist in the composition (some may be scrolled off-screen)
        composeTestRule.onNodeWithContentDescription("Bold").assertExists()
        composeTestRule.onNodeWithContentDescription("Italic").assertExists()
        composeTestRule.onNodeWithContentDescription("Strikethrough").assertExists()
        composeTestRule.onNodeWithContentDescription("Underline").assertExists()
        composeTestRule.onNodeWithContentDescription("Text Color Red").assertExists()
        composeTestRule.onNodeWithContentDescription("Background Color Yellow").assertExists()
        composeTestRule.onNodeWithContentDescription("Large Font Size").assertExists()
        composeTestRule.onNodeWithContentDescription("Heading 1").assertExists()
        composeTestRule.onNodeWithContentDescription("Align Center").assertExists()
        composeTestRule.onNodeWithContentDescription("Blockquote").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear Formatting").assertExists()

        // Check if the editor exists by tag
        composeTestRule.onNodeWithTag("ChatInputEditor").assertIsDisplayed()

        // Check if placeholder is displayed
        composeTestRule.onNodeWithText("Type a message...").assertIsDisplayed()

        // Buttons should be enabled initially (even with no selection)
        composeTestRule.onNodeWithContentDescription("Bold").assertIsEnabled()
    }

    @Test
    fun `buttons are always enabled regardless of selection`() {
        composeTestRule.setContent {
            ArrangerTheme {
                ChatInputSample()
            }
        }

        val textInputNode = composeTestRule.onNodeWithTag("ChatInputEditor")
        textInputNode.performTextInput("Hello World")

        // Enabled even with no selection (cursor only)
        composeTestRule.onNodeWithContentDescription("Bold").assertIsEnabled()

        // Select some text
        textInputNode.performTextInputSelection(TextRange(0, 5))

        // Still enabled
        composeTestRule.onNodeWithContentDescription("Bold").assertIsEnabled()
    }

    @Test
    fun `toolbar buttons sync with cursor position attributes`() {
        composeTestRule.setContent {
            ArrangerTheme {
                ChatInputSample()
            }
        }

        val textInputNode = composeTestRule.onNodeWithTag("ChatInputEditor")
        textInputNode.performTextInput("Hello World")

        // Select "Hello" and apply Bold
        textInputNode.performTextInputSelection(TextRange(0, 5))
        composeTestRule.onNodeWithContentDescription("Bold").performClick()

        // Move cursor to "Hello" (index 3)
        textInputNode.performTextInputSelection(TextRange(3))

        // Bold button should be toggled ON
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOn()

        // Move cursor to "World" (index 8)
        textInputNode.performTextInputSelection(TextRange(8))

        // Bold button should be toggled OFF
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOff()
    }

    @Test
    fun `typing attributes toggle on collapsed selection`() {
        composeTestRule.setContent {
            ArrangerTheme {
                ChatInputSample()
            }
        }

        val textInputNode = composeTestRule.onNodeWithTag("ChatInputEditor")

        // Tap Bold button when empty
        composeTestRule.onNodeWithContentDescription("Bold").performClick()

        // Button should be toggled ON
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOn()

        // Type text
        textInputNode.performTextInput("BoldText")

        // Cursor is now at the end of "BoldText", which is bold, so button should remain ON
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOn()

        // Move cursor to beginning (index 0)
        textInputNode.performTextInputSelection(TextRange(0))

        // Button should be OFF because at index 0, there is no inherited attribute and no typing attribute
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOff()
    }
}
