package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.TestMonotonicFrameClock
import com.google.common.truth.Truth.assertThat
import dev.mkeeda.arranger.runtime.node.HeadingLevel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleEditorTest {

    private fun runEditorTest(block: suspend TestScope.(SimpleEditor) -> Unit) = runTest(UnconfinedTestDispatcher()) {
        val editor = SimpleEditor(clock = TestMonotonicFrameClock(this))
        block(editor)
        editor.dispose()
    }

    @Test
    fun outputSemanticText_static() = runEditorTest { editor ->
        editor.launch {
            Heading(level = HeadingLevel.H1, title = "My profile")
            Paragraph {
                Text(text = "Hello world. I am a arranger. My profile is ")
                Link(text = "this link", url = "https://example.com")
                Text(text = ".")
            }
        }

        assertThat(editor.currentText()).isEqualTo("""
            My profile
            Hello world. I am a arranger. My profile is this link.
        """.trimIndent())
    }

    @Test
    fun outputSemanticText_dynamic() = runEditorTest {editor ->
        var titleIsShown by mutableStateOf(false)
        editor.launch {
            if (titleIsShown) {
                Heading(level = HeadingLevel.H1, title = "My profile")
            }
            Paragraph {
                Text(text = "Hello world.")
            }
        }

        assertThat(editor.currentText()).isEqualTo("""
            Hello world.
        """.trimIndent())

        titleIsShown = true

        assertThat(editor.currentText()).isEqualTo("""
            My profile
            Hello world.
        """.trimIndent())
    }
}
