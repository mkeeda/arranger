package dev.mkeeda.arranger.runtime

import androidx.compose.ui.test.TestMonotonicFrameClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleEditorTest {
    @Test
    fun insertTest() = runTest(UnconfinedTestDispatcher()) {
        val hello = "Hello"
        val editor = SimpleEditor(clock = TestMonotonicFrameClock(this))
        editor.launch(initialText = hello)

        assertThat(editor.currentText()).isEqualTo(hello)

        editor.insert(text = " World", index = hello.length)
        assertThat(editor.currentText()).isEqualTo("$hello World")

        editor.dispose()
    }
}
