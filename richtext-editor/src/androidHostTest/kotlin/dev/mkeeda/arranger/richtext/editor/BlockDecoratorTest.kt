package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.VisualBlock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlockDecoratorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleContext =
        BlockDecorationContext(
            bounds = Rect(0f, 0f, 400f, 100f),
            textBounds = Rect(16f, 0f, 380f, 100f),
            lineCount = 2,
            modifier = Modifier.fillMaxSize(),
        )

    @Test
    fun `DefaultBlockDecorator renders Blockquote without error`() {
        composeTestRule.setContent {
            DefaultBlockDecorator.Decoration(
                block = VisualBlock.Blockquote(range = 0..10),
                context = sampleContext,
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `DefaultBlockDecorator renders CodeBlock without error`() {
        composeTestRule.setContent {
            DefaultBlockDecorator.Decoration(
                block = VisualBlock.CodeBlock(range = 0..10, language = "kotlin"),
                context = sampleContext,
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `BlockContainer renders background, leading, and header slots correctly`() {
        composeTestRule.setContent {
            BlockContainer(
                context = sampleContext,
                background = {
                    Box(modifier = Modifier.fillMaxSize().testTag("bg_slot"))
                },
                leading = {
                    Box(modifier = Modifier.width(10.dp).fillMaxHeight().testTag("leading_slot"))
                },
                header = {
                    BasicText(text = "KOTLIN", modifier = Modifier.testTag("header_slot"))
                },
            )
        }

        composeTestRule.onNodeWithTag("bg_slot").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leading_slot").assertIsDisplayed()
        composeTestRule.onNodeWithText("KOTLIN").assertIsDisplayed()
    }
}
