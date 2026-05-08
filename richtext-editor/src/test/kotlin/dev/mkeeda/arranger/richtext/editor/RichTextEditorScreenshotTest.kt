package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboGif
import com.github.takahirom.roborazzi.captureRoboImage
import dev.mkeeda.arranger.richtext.BulletListItem
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListItem
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.backgroundColor
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.italic
import dev.mkeeda.arranger.richtext.orderedList
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.richtext.strikethrough
import dev.mkeeda.arranger.richtext.textAlignment
import dev.mkeeda.arranger.richtext.textColor
import dev.mkeeda.arranger.richtext.underline
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextEditorScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `render plain text`() {
        val text =
            "The quick brown fox jumps over the lazy dog.\n" +
                "Pack my box with five dozen liquor jugs.\n" +
                "How vexingly quick daft zebras jump."
        val state = RichTextState(initialText = RichString(text))

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White),
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render inline styles`() {
        val text = "Bold Italic Strikethrough Underline Colored Highlighted"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.rangeOf("Bold")) { bold() }
                        editAttributes(text.rangeOf("Italic")) { italic() }
                        editAttributes(text.rangeOf("Strikethrough")) { strikethrough() }
                        editAttributes(text.rangeOf("Underline")) { underline() }
                        editAttributes(text.rangeOf("Colored")) { textColor(Color.Red) }
                        editAttributes(text.rangeOf("Highlighted")) { backgroundColor(Color.Yellow) }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White),
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render paragraph styles`() {
        val text = "Heading 1\nBody text under H1.\nHeading 3\nBlockquote text\nCenter aligned text"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.rangeOf("Heading 1")) { headingLevel(HeadingLevel.H1) }
                        editAttributes(text.rangeOf("Heading 3")) { headingLevel(HeadingLevel.H3) }
                        editAttributes(text.rangeOf("Blockquote text")) { blockquote() }
                        editAttributes(text.rangeOf("Center aligned text")) { textAlignment(TextAlignment.Center) }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White),
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render multi level lists`() {
        val state = createMultiLevelListState()

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White),
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render lists with custom marker`() {
        val state = createMultiLevelListState()

        val customMarkerResolver =
            ListMarkerResolver { item ->
                when (item) {
                    is BulletListItem -> "★"
                    is OrderedListItem -> "(${item.index})"
                }
            }

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White),
                listMarkerResolver = customMarkerResolver,
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `render paragraph spacing correctly`() {
        val text = "First paragraph with bold text.\n\nSecond paragraph plain.\nThird paragraph with italic.\n\nFourth paragraph plain."
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.rangeOf("bold")) { bold() }
                        editAttributes(text.rangeOf("italic")) { italic() }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White),
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `clip scrolled list markers`() {
        val state = createMultiLevelListState()
        val scrollState = ScrollState(150)

        composeTestRule.setContent {
            Box(modifier = Modifier.padding(50.dp).background(Color.LightGray)) {
                RichTextEditor(
                    state = state,
                    modifier = Modifier.width(400.dp).height(100.dp).background(Color.White),
                    scrollState = scrollState,
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
    @Test
    fun `type newline on list item generates marker and indents cursor`() {
        val text = "Bullet item 1"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.rangeOf("Bullet item 1")) { bulletList(ListIndentLevel.Level1) }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.width(400.dp).background(Color.White).testTag("editor"),
            )
        }

        composeTestRule.onNode(hasTestTag("editor")).captureRoboGif(
            composeTestRule,
        ) {
            // Move cursor to the end
            state.textFieldState.edit {
                selection = androidx.compose.ui.text.TextRange(text.length)
            }
            composeTestRule.mainClock.advanceTimeByFrame()

            // Type newline
            composeTestRule.onNode(hasTestTag("editor")).performTextInput("\n")

            // Advance multiple frames to capture cursor blinking
            for (i in 0..10) {
                composeTestRule.mainClock.advanceTimeByFrame()
            }
        }
    }

    private fun createMultiLevelListState(): RichTextState {
        val text =
            "Bullet item 1\nBullet item 2\nNested bullet\nDeep nested bullet\n" +
                "Ordered item 1\nOrdered item 2\nNested ordered\nDeep nested ordered"
        return RichTextState(
            initialText =
                RichString(text).edit {
                    editAttributes(text.rangeOf("Bullet item 1")) { bulletList(ListIndentLevel.Level1) }
                    editAttributes(text.rangeOf("Bullet item 2")) { bulletList(ListIndentLevel.Level1) }
                    editAttributes(text.rangeOf("Nested bullet")) { bulletList(ListIndentLevel.Level2) }
                    editAttributes(text.rangeOf("Deep nested bullet")) { bulletList(ListIndentLevel.Level3) }
                    editAttributes(text.rangeOf("Ordered item 1")) { orderedList(ListIndentLevel.Level1) }
                    editAttributes(text.rangeOf("Ordered item 2")) { orderedList(ListIndentLevel.Level1) }
                    editAttributes(text.rangeOf("Nested ordered")) { orderedList(ListIndentLevel.Level2) }
                    editAttributes(text.rangeOf("Deep nested ordered")) { orderedList(ListIndentLevel.Level3) }
                },
        )
    }
}
