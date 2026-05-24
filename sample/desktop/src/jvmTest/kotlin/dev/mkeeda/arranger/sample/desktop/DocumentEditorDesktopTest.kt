package dev.mkeeda.arranger.sample.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.sample.shared.DocumentEditorSample
import dev.mkeeda.arranger.sample.shared.theme.ArrangerTheme
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DocumentEditorDesktopTest {

    @Test
    fun `toolbar buttons apply format to selected text on desktop`() = runComposeUiTest {
        setContent {
            ArrangerTheme {
                DocumentEditorSample()
            }
        }

        val textInputNode = onNodeWithTag("DocumentEditor")
        textInputNode.performTextInput("Desktop Test")

        // Select "Desktop Test"
        textInputNode.performTextInputSelection(TextRange(0, 12))
        textInputNode.assertIsFocused()

        // Assert initial state (Bold is OFF)
        onNodeWithContentDescription("Bold").assertIsOff()

        // Toggle Bold on
        onNodeWithContentDescription("Bold").performClick()

        // Assert Bold is applied (button remains toggled ON)
        onNodeWithContentDescription("Bold").assertIsOn()
        textInputNode.assertIsFocused()
    }
}
