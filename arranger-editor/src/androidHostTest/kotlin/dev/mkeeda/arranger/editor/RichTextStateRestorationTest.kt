package dev.mkeeda.arranger.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class RichTextStateRestorationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `rememberRichTextState restores text formatting and selection across state restoration`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        lateinit var stateInstance: RichTextState

        restorationTester.setContent {
            val state =
                rememberRichTextState(
                    initialText =
                        RichString(
                            text = "Hello world",
                            spans =
                                listOf(
                                    RichSpan(
                                        range = 0..4,
                                        attributes = attributeContainerOf(BoldKey to Unit),
                                    ),
                                ),
                        ),
                )
            stateInstance = state
            RichTextEditor(state = state)
        }

        // Type additional text and select part of it
        composeTestRule.onNodeWithText("Hello world").performTextInputSelection(TextRange(index = 11))
        composeTestRule.onNodeWithText("Hello world").performTextInput("!")
        composeTestRule.onNodeWithText("Hello world!").performTextInputSelection(TextRange(start = 2, end = 5))

        stateInstance.richString.text shouldBe "Hello world!"
        stateInstance.selection shouldBe TextRange(start = 2, end = 5)

        // Trigger recreation simulating configuration change or process restoration
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify that the restored instance retains content and selection
        stateInstance.shouldNotBeNull()
        stateInstance.richString.text shouldBe "Hello world!"
        stateInstance.selection shouldBe TextRange(start = 2, end = 5)
        val boldSpan = stateInstance.richString.spans.firstOrNull { it.attributes.containsKey(BoldKey) }
        boldSpan.shouldNotBeNull()
        boldSpan.range shouldBe (0..4)
    }

    @Test
    fun `rememberRichTextState with custom saver restores custom attributes across state restoration`() {
        val restorationTester = StateRestorationTester(composeTestRule)

        data class Note(val content: String)

        val noteKey =
            object : SpanAttributeKey<Note> {
                override val name: String = "note"
                override val defaultValue: Note = Note("")
            }

        val noteSerializer =
            attributeSerializer(
                key = noteKey,
                save = { note -> note.content },
                restore = { saved -> Note(content = saved as String) },
            )

        val customSaver = RichTextState.saver(customSerializers = listOf(noteSerializer))
        lateinit var stateInstance: RichTextState

        restorationTester.setContent {
            val state =
                rememberRichTextState(
                    initialText =
                        RichString(
                            text = "Annotated text",
                            spans =
                                listOf(
                                    RichSpan(
                                        range = 0..8,
                                        attributes = attributeContainerOf(noteKey to Note(content = "Test note")),
                                    ),
                                ),
                        ),
                    saver = customSaver,
                )
            stateInstance = state
            RichTextEditor(state = state)
        }

        stateInstance.richString.spans.first().attributes.get(noteKey) shouldBe Note(content = "Test note")

        // Trigger restoration
        restorationTester.emulateSavedInstanceStateRestore()

        stateInstance.shouldNotBeNull()
        stateInstance.richString.text shouldBe "Annotated text"
        val restoredNote = stateInstance.richString.spans.first().attributes.get(noteKey)
        restoredNote shouldBe Note(content = "Test note")
    }

    @Test
    fun `rememberRichTextState preserves multi-paragraph document with lists and headings across state restoration`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        lateinit var stateInstance: RichTextState

        val docText = "Header Title\nList item 1\nList item 2"
        val initialSpans =
            listOf(
                RichSpan(
                    range = 0..12,
                    attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1),
                ),
                RichSpan(
                    range = 13..24,
                    attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1),
                ),
            )

        restorationTester.setContent {
            val state =
                rememberRichTextState(
                    initialText = RichString(text = docText, spans = initialSpans),
                )
            stateInstance = state
            RichTextEditor(state = state)
        }

        stateInstance.richString.text shouldBe docText
        stateInstance.richString.spans.size shouldBe 2

        restorationTester.emulateSavedInstanceStateRestore()

        stateInstance.shouldNotBeNull()
        stateInstance.richString.text shouldBe docText
        val headingSpan = stateInstance.richString.spans.firstOrNull { it.attributes.containsKey(HeadingKey) }
        headingSpan.shouldNotBeNull()
        headingSpan.attributes.get(HeadingKey) shouldBe HeadingLevel.H1

        val listSpan = stateInstance.richString.spans.firstOrNull { it.attributes.containsKey(BulletListKey) }
        listSpan.shouldNotBeNull()
        listSpan.attributes.get(BulletListKey) shouldBe ListIndentLevel.Level1
    }

    @Test
    fun `rememberRichTextState preserves reversed selection across state restoration`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        lateinit var stateInstance: RichTextState

        restorationTester.setContent {
            val state =
                rememberRichTextState(
                    initialText = RichString(text = "Hello Reversed Selection"),
                )
            stateInstance = state
            RichTextEditor(state = state)
        }

        // Set reversed selection (start = 14, end = 6)
        composeTestRule.runOnIdle {
            stateInstance.textFieldState.edit {
                selection = TextRange(start = 14, end = 6)
            }
        }

        stateInstance.selection shouldBe TextRange(start = 14, end = 6)
        stateInstance.selection.reversed shouldBe true

        restorationTester.emulateSavedInstanceStateRestore()

        stateInstance.shouldNotBeNull()
        stateInstance.selection shouldBe TextRange(start = 14, end = 6)
        stateInstance.selection.reversed shouldBe true
    }
}
