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
                        setAttribute(BoldKey, Unit, range = initialText.rangeOf("Arranger!"))
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
}
