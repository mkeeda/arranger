package dev.mkeeda.arranger.sampleApp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

        // Check if toolbar buttons exist
        composeTestRule.onNodeWithContentDescription("Bold").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Italic").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Strikethrough").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Clear Formatting").assertIsDisplayed()

        // Check if the editor exists by tag
        composeTestRule.onNodeWithTag("ChatInputEditor").assertIsDisplayed()

        // Check if placeholder is displayed
        composeTestRule.onNodeWithText("Type a message...").assertIsDisplayed()

        // Buttons should be disabled initially (no selection)
        composeTestRule.onNodeWithContentDescription("Bold").assertIsNotEnabled()
    }

    @Test
    fun `buttons are enabled only when text is selected`() {
        composeTestRule.setContent {
            ArrangerTheme {
                ChatInputSample()
            }
        }

        val textInputNode = composeTestRule.onNodeWithTag("ChatInputEditor")
        textInputNode.performTextInput("Hello World")

        // Still disabled because no text is selected (selection is just a cursor)
        composeTestRule.onNodeWithContentDescription("Bold").assertIsNotEnabled()

        // Select some text
        textInputNode.performTextInputSelection(TextRange(0, 5))

        // Now buttons should be enabled
        composeTestRule.onNodeWithContentDescription("Bold").assertIsEnabled()
    }
}
