package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.codeBlock
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.inlineCode
import dev.mkeeda.arranger.richtext.link
import dev.mkeeda.arranger.richtext.rangeOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Material3RichTextEditorScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRichTextState(): RichTextState {
        val text =
            "Arranger Material 3\n" +
                "Explore the documentation and inspect the code sample.\n" +
                "Quoted callout section\n" +
                "First item\n" +
                "Second item"
        return RichTextState(
            initialText =
                RichString(text).edit {
                    editAttributes(text.rangeOf("Arranger Material 3")) { headingLevel(HeadingLevel.H1) }
                    editAttributes(text.rangeOf("documentation")) { link("https://example.com") }
                    editAttributes(text.rangeOf("code sample")) { inlineCode() }
                    editAttributes(text.rangeOf("Quoted callout section")) { blockquote() }
                    editAttributes(text.rangeOf("First item")) { bulletList(ListIndentLevel.Level1) }
                    editAttributes(text.rangeOf("Second item")) { bulletList(ListIndentLevel.Level1) }
                },
        )
    }

    @Test
    fun `render rich text in light theme`() {
        val state = createRichTextState()

        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(
                    modifier = Modifier.width(400.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    RichTextEditor(
                        state = state,
                        styleResolver = rememberMaterial3AttributeStyleResolver(),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render rich text in dark theme`() {
        val state = createRichTextState()

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.width(400.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    RichTextEditor(
                        state = state,
                        styleResolver = rememberMaterial3AttributeStyleResolver(),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    private fun createVisualBlockState(): RichTextState {
        val text =
            "Blockquote section with accent bar\n\n" +
                "Visual Block Decorations & Code Blocks\nprintln(\"Hello\")\n\n" +
                "plain code block"
        return RichTextState(
            initialText =
                RichString(text).edit {
                    editAttributes(text.rangeOf("Blockquote section with accent bar")) { blockquote() }
                    editAttributes(text.rangeOf("Visual Block Decorations & Code Blocks\nprintln(\"Hello\")")) { codeBlock("kotlin") }
                    editAttributes(text.rangeOf("plain code block")) { codeBlock() }
                },
        )
    }

    @Test
    fun `render visual blocks in light theme`() {
        val state = createVisualBlockState()

        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(
                    modifier = Modifier.width(400.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    RichTextEditor(
                        state = state,
                        styleResolver = rememberMaterial3AttributeStyleResolver(),
                        blockDecorator = rememberMaterial3BlockDecorator(),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render visual blocks in dark theme`() {
        val state = createVisualBlockState()

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.width(400.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    RichTextEditor(
                        state = state,
                        styleResolver = rememberMaterial3AttributeStyleResolver(),
                        blockDecorator = rememberMaterial3BlockDecorator(),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
