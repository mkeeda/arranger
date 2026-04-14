package dev.mkeeda.arranger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.mkeeda.arranger.core.text.AttributeKey
import dev.mkeeda.arranger.core.text.RichString
import dev.mkeeda.arranger.core.text.rangeOf
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextEditorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private object BoldAttributeKey : AttributeKey<Unit> {
        override val name: String = "Bold"
        override val defaultValue: Unit = Unit
    }

    private object ColorAttributeKey : AttributeKey<Color> {
        override val name: String = "Color"
        override val defaultValue: Color = Color.Unspecified
    }

    @Test
    fun `render rich text with mapped attributes via semantics`() {
        val initialText = "Hello Colorful World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setAttribute(BoldAttributeKey, Unit, range = initialText.rangeOf("Colorful"))
                        setAttribute(ColorAttributeKey, Color.Red, range = initialText.rangeOf("Colorful World"))
                    },
            )

        val resolver =
            AttributeStyleResolver {
                spanStyle(BoldAttributeKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                spanStyle(ColorAttributeKey) { color -> SpanStyle(color = color) }
            }

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                styleResolver = resolver,
            )
        }
    }

    @Test
    fun `spans shift synchronously when user edits text within RichTextEditor`() {
        val initialText = "Welcome to Arranger!"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        // "Arranger!" is length 9, at index 11
                        setAttribute(BoldAttributeKey, Unit, range = initialText.rangeOf("Arranger!"))
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                styleResolver =
                    AttributeStyleResolver {
                        spanStyle(BoldAttributeKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                    },
            )
        }

        // Test editing: Replace "to " (length 3, indices 8..11) with "a" (length 1)
        // Original: "Welcome to Arranger!"
        // New     : "Welcome aArranger!"
        // Net change: -2 characters. "Arranger!" shifts from 11..19 to 9..17
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(androidx.compose.ui.text.TextRange(8, 11))
        composeTestRule.onNodeWithText(initialText).performTextInput("a")

        val newSpans = state.richString.getSpans()
        newSpans.size shouldBe 1

        // Assert the span accurately shifted
        newSpans.first().range.first shouldBe 9
        newSpans.first().range.last shouldBe 17
    }
}
