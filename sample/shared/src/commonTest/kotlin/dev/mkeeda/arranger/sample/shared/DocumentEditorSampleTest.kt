package dev.mkeeda.arranger.sample.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.sample.shared.theme.ArrangerTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DocumentEditorSampleTest {

    @Test
    fun `toolbar buttons toggle formatting on selection when clicked`() = runComposeUiTest {
        setContent {
            ArrangerTheme {
                DocumentEditorSample()
            }
        }

        val textInputNode = onNodeWithTag("DocumentEditor")
        textInputNode.performTextInput("Hello World")

        // Select "Hello"
        textInputNode.performTextInputSelection(TextRange(0, 5))

        // Toggle Bold on
        onNodeWithContentDescription("Bold").performClick()
        onNodeWithContentDescription("Bold").assertIsOn()

        // Toggle Bold off
        onNodeWithContentDescription("Bold").performClick()
        onNodeWithContentDescription("Bold").assertIsOff()
    }

    @Test
    fun `toolbar buttons sync with cursor position attributes`() = runComposeUiTest {
        setContent {
            ArrangerTheme {
                DocumentEditorSample()
            }
        }

        val textInputNode = onNodeWithTag("DocumentEditor")
        textInputNode.performTextInput("Hello World")

        // Select "Hello" and apply Bold
        textInputNode.performTextInputSelection(TextRange(0, 5))
        onNodeWithContentDescription("Bold").performClick()

        // Move cursor to "Hello" (index 3)
        textInputNode.performTextInputSelection(TextRange(3))

        // Bold button should be toggled ON
        onNodeWithContentDescription("Bold").assertIsOn()

        // Move cursor to "World" (index 8)
        textInputNode.performTextInputSelection(TextRange(8))

        // Bold button should be toggled OFF
        onNodeWithContentDescription("Bold").assertIsOff()
    }

    @Test
    fun `typing attributes toggle on collapsed selection`() = runComposeUiTest {
        setContent {
            ArrangerTheme {
                DocumentEditorSample()
            }
        }

        val textInputNode = onNodeWithTag("DocumentEditor")

        // Tap Bold button when empty
        onNodeWithContentDescription("Bold").performClick()

        // Button should be toggled ON
        onNodeWithContentDescription("Bold").assertIsOn()

        // Type text
        textInputNode.performTextInput("BoldText")

        // Cursor is now at the end of "BoldText", which is bold, so button should remain ON
        onNodeWithContentDescription("Bold").assertIsOn()

        // Turn OFF Bold at the current position
        onNodeWithContentDescription("Bold").performClick()
        onNodeWithContentDescription("Bold").assertIsOff()

        // Type more text
        textInputNode.performTextInput("Normal")

        // Cursor is now at the end of "Normal", which should NOT be bold
        onNodeWithContentDescription("Bold").assertIsOff()

        // Move cursor to beginning (index 0)
        textInputNode.performTextInputSelection(TextRange(0))

        // Button should be OFF because at index 0, there is no inherited attribute and no typing attribute
        onNodeWithContentDescription("Bold").assertIsOff()
    }
}
