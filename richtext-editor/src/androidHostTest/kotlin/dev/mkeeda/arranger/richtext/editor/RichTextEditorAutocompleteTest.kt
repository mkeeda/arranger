package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.editor.wysiwyg.WysiwygEditor
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextEditorAutocompleteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `typing trigger prefix invokes onAutocompleteChange with match`() {
        val state = RichTextState(RichString(""))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Initially no match
        currentMatch.shouldBeNull()

        // Type "@"
        composeTestRule.onNodeWithText("").performTextInput("@")
        composeTestRule.waitForIdle()

        // Verify match
        currentMatch.shouldNotBeNull()
        currentMatch?.trigger?.prefix shouldBe "@"
        currentMatch?.query shouldBe ""
        currentMatch?.cursorRect.shouldNotBeNull()

        // Type "alice"
        composeTestRule.onNodeWithText("@").performTextInput("alice")
        composeTestRule.waitForIdle()

        // Verify query updated
        currentMatch.shouldNotBeNull()
        currentMatch?.query shouldBe "alice"
    }

    @Test
    fun `deleting trigger with backspace emits null match`() {
        val state = RichTextState(RichString(""))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Type "@"
        composeTestRule.onNodeWithText("").performTextInput("@")
        composeTestRule.waitForIdle()
        currentMatch.shouldNotBeNull()

        // Delete "@"
        composeTestRule.onNodeWithText("@").performTextInputSelection(TextRange(0, 1))
        composeTestRule.onNodeWithText("@").performTextInput("")
        composeTestRule.waitForIdle()

        currentMatch.shouldBeNull()
    }

    @Test
    fun `moving cursor away from trigger pattern emits null match`() {
        val initialText = "Hello @ali world"
        val state = RichTextState(RichString(initialText))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Position cursor at index 10 (immediately after "@ali")
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(10))
        composeTestRule.waitForIdle()

        currentMatch.shouldNotBeNull()
        currentMatch?.query shouldBe "ali"

        // Move cursor to index 0 ("Hello")
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(0))
        composeTestRule.waitForIdle()

        currentMatch.shouldBeNull()
    }

    @Test
    fun `non-collapsed selection emits null match`() {
        val initialText = "Hello @ali"
        val state = RichTextState(RichString(initialText))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Cursor collapsed at 10
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(10))
        composeTestRule.waitForIdle()
        currentMatch.shouldNotBeNull()

        // Select range 6..10
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(6, 10))
        composeTestRule.waitForIdle()

        currentMatch.shouldBeNull()
    }

    @Test
    fun `scroll offset adjusts cursorRect in AutocompleteMatch`() {
        val manyLines = "@trigger\n" + (1..50).joinToString("\n") { "Line $it" }
        val state = RichTextState(RichString(manyLines))
        val scrollState = ScrollState(0)
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                modifier = Modifier.height(100.dp),
                scrollState = scrollState,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Move cursor to end of "@trigger" (index 8, top line)
        composeTestRule.onNodeWithText(manyLines).performTextInputSelection(TextRange(8))
        composeTestRule.waitForIdle()

        val unscrollRect = currentMatch?.cursorRect
        unscrollRect.shouldNotBeNull()
        val initialScroll = scrollState.value

        // Scroll down by 50px
        runBlocking {
            scrollState.scrollTo(initialScroll + 50)
        }
        composeTestRule.waitForIdle()

        val scrolledRect = currentMatch?.cursorRect
        scrolledRect.shouldNotBeNull()

        // The top of cursorRect in viewport coordinates should decrease by exactly 50px
        (unscrollRect.top - scrolledRect.top) shouldBe 50f
    }

    @Test
    fun `WysiwygEditor delegates autocomplete triggers and notifications`() {
        val state = RichTextState(RichString(""))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            WysiwygEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "#")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Type "#arranger"
        composeTestRule.onNodeWithText("").performTextInput("#arranger")
        composeTestRule.waitForIdle()

        currentMatch.shouldNotBeNull()
        currentMatch?.trigger?.prefix shouldBe "#"
        currentMatch?.query shouldBe "arranger"
    }

    @Test
    fun `applying completion in editor updates text and clears autocomplete match`() {
        val state = RichTextState(RichString(""))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        composeTestRule.onNodeWithText("").performTextInput("@ali")
        composeTestRule.waitForIdle()

        val match = currentMatch
        match.shouldNotBeNull()

        // Apply completion programmatically like a suggestion popup click
        state.applyCompletion(match, "Alice ")
        composeTestRule.waitForIdle()

        // Editor state is updated
        state.richString.text shouldBe "Alice "
        // Trailing space after "Alice " terminates query (allowSpacesInQuery = false), so match becomes null
        currentMatch.shouldBeNull()
    }

    @Test
    fun `repositioning cursor between multiple lines reactivates corresponding trigger match`() {
        // Line 1 has "@alice", Line 2 has normal text
        val initialText = "Line1 @alice\nLine2 regular text"
        val state = RichTextState(RichString(initialText))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Cursor at end of Line 2 (index = text length): no active trigger
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))
        composeTestRule.waitForIdle()
        currentMatch.shouldBeNull()

        // Move cursor to end of "@alice" on Line 1 (index 12)
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(12))
        composeTestRule.waitForIdle()

        currentMatch.shouldNotBeNull()
        currentMatch?.query shouldBe "alice"

        // Move cursor back to Line 2
        composeTestRule.onNodeWithText(initialText).performTextInputSelection(TextRange(initialText.length))
        composeTestRule.waitForIdle()
        currentMatch.shouldBeNull()
    }

    @Test
    fun `typing Japanese full-width text triggers autocomplete match in editor`() {
        val state = RichTextState(RichString(""))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        composeTestRule.onNodeWithText("").performTextInput("@山田")
        composeTestRule.waitForIdle()

        currentMatch.shouldNotBeNull()
        currentMatch?.query shouldBe "山田"
    }

    @Test
    fun `switching between multiple triggers in editor emits respective trigger matches`() {
        val state = RichTextState(RichString(""))
        var currentMatch: AutocompleteMatch? = null

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                autocompleteTriggers =
                    listOf(
                        AutocompleteTrigger(prefix = "@"),
                        AutocompleteTrigger(prefix = "#"),
                    ),
                onAutocompleteChange = { currentMatch = it },
            )
        }

        // Type "@user"
        composeTestRule.onNodeWithText("").performTextInput("@user")
        composeTestRule.waitForIdle()
        currentMatch?.trigger?.prefix shouldBe "@"
        currentMatch?.query shouldBe "user"

        // Clear and type "#channel"
        composeTestRule.onNodeWithText("@user").performTextInputSelection(TextRange(0, 5))
        composeTestRule.onNodeWithText("@user").performTextInput("#channel")
        composeTestRule.waitForIdle()
        currentMatch?.trigger?.prefix shouldBe "#"
        currentMatch?.query shouldBe "channel"
    }
}
