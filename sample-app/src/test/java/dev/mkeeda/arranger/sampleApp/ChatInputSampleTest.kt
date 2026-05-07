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
    fun `toolbar buttons toggle formatting on selection when clicked`() {
        composeTestRule.setContent {
            ArrangerTheme {
                ChatInputSample()
            }
        }

        val textInputNode = composeTestRule.onNodeWithTag("ChatInputEditor")
        textInputNode.performTextInput("Hello World")

        // Select "Hello"
        textInputNode.performTextInputSelection(TextRange(0, 5))

        // Toggle Bold on
        composeTestRule.onNodeWithContentDescription("Bold").performClick()
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOn()

        // Toggle Bold off
        composeTestRule.onNodeWithContentDescription("Bold").performClick()
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOff()
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

        // Turn OFF Bold at the current position
        composeTestRule.onNodeWithContentDescription("Bold").performClick()
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOff()

        // Type more text
        textInputNode.performTextInput("Normal")

        // Cursor is now at the end of "Normal", which should NOT be bold
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOff()

        // Move cursor to beginning (index 0)
        textInputNode.performTextInputSelection(TextRange(0))

        // Button should be OFF because at index 0, there is no inherited attribute and no typing attribute
        composeTestRule.onNodeWithContentDescription("Bold").assertIsOff()
    }
}
