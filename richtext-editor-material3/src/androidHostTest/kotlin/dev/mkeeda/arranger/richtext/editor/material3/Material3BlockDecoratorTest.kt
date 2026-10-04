package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.mkeeda.arranger.richtext.VisualBlock
import dev.mkeeda.arranger.richtext.editor.BlockDecorationContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Material3BlockDecoratorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleContext =
        BlockDecorationContext(
            lineCount = 2,
            modifier = Modifier.fillMaxSize().testTag("block_container"),
        )

    @Test
    fun `Material3BlockDecorator renders code block with language header in light theme`() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val decorator = rememberMaterial3BlockDecorator()
                decorator.Decoration(
                    block = VisualBlock.CodeBlock(range = 0..10, language = "kotlin"),
                    context = sampleContext,
                )
            }
        }

        composeTestRule.onNodeWithTag("block_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("code_block_background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("KOTLIN").assertIsDisplayed()
    }

    @Test
    fun `Material3BlockDecorator renders code block with language header in dark theme`() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val decorator = rememberMaterial3BlockDecorator()
                decorator.Decoration(
                    block = VisualBlock.CodeBlock(range = 0..10, language = "rust"),
                    context = sampleContext,
                )
            }
        }

        composeTestRule.onNodeWithTag("block_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("code_block_background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("RUST").assertIsDisplayed()
    }

    @Test
    fun `Material3BlockDecorator renders blockquote correctly`() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val decorator = rememberMaterial3BlockDecorator()
                decorator.Decoration(
                    block = VisualBlock.Blockquote(range = 0..10),
                    context = sampleContext,
                )
            }
        }

        composeTestRule.onNodeWithTag("block_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blockquote_bar").assertIsDisplayed()
    }

    @Test
    fun `Material3BlockDecorator renders code block without language badge when language is null`() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val decorator = rememberMaterial3BlockDecorator()
                decorator.Decoration(
                    block = VisualBlock.CodeBlock(range = 0..10, language = null),
                    context = sampleContext,
                )
            }
        }

        composeTestRule.onNodeWithTag("block_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("code_block_background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_badge").assertDoesNotExist()
    }
}
