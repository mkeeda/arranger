package dev.mkeeda.arranger.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class EditorTest {
    @Test
    fun addText() = runComposeUiTest {
        var state by mutableStateOf(EditorState())
        setContent {
            Editor(
                editorState = state,
                onChangeEditorState = { state = it }
            )
        }

        onNodeWithText("")
        onNode(hasSetTextAction()).performTextInput("abc")
        onNodeWithText("abc")
    }
}
