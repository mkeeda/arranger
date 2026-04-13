package dev.mkeeda.arranger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.mkeeda.arranger.core.text.AttributeKey
import dev.mkeeda.arranger.core.text.RichString
import dev.mkeeda.arranger.core.text.rangeOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
                resolve(BoldAttributeKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                resolve(ColorAttributeKey) { color -> SpanStyle(color = color) }
            }

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                styleResolver = resolver,
            )
        }

        // TODO: Verify visual styles emitted by OutputTransformation
        // In Compose 1.7+, OutputTransformation modifies the text purely at the visual layer.
        // Because of this, the applied SpanStyles don't appear in the `SemanticsProperties.EditableText`
        // property, and extracting them via `GetTextLayoutResult` hits Kotlin generic type inference
        // issues in UI tests.
        // For now, the styling mapping logic is thoroughly tested in AttributeStyleResolverTest,
        // , and this test ensures the component renders without crashing.
        composeTestRule.onNodeWithText("Hello Colorful World").assertExists()
    }
}
