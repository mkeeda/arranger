package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.clickOnCharacter
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WysiwygEditorTapTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `WysiwygEditor correctly propagates onSpanClick`() {
        val initialText = "Click here for more"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", 6..9)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            WysiwygEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        clickedSpan shouldBe
            RichSpan(
                range = 6..9,
                attributes = attributeContainerOf(LinkKey to "https://example.com"),
            )
        state.selection.collapsed shouldBe true
    }
}
