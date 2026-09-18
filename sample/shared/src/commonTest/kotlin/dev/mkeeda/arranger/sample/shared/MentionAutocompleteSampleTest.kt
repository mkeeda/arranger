package dev.mkeeda.arranger.sample.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.mkeeda.arranger.sample.shared.theme.ArrangerTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MentionAutocompleteSampleTest {
    @Test
    fun `typing at-symbol displays user suggestion popup and filters by name`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            // Type "@" to trigger mention suggestions
            onNodeWithText("").performTextInput("@")

            // Verify initial suggestion list contains sample users
            onNodeWithText("Alice").assertIsDisplayed()
            onNodeWithText("Bob").assertIsDisplayed()
            onNodeWithText("Charlie").assertIsDisplayed()

            // Filter by name "Ali"
            onNodeWithText("@").performTextInput("Ali")
            onNodeWithText("Alice").assertIsDisplayed()
            onAllNodesWithText("Bob").assertCountEquals(0)
        }

    @Test
    fun `filtering users supports searching by role`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            // Type "@" and query by role keyword "Engineer"
            // Both "Android Engineer" (Bob) and "QA Engineer" (Diana) match
            onNodeWithText("").performTextInput("@Engineer")

            onNodeWithText("Bob").assertIsDisplayed()
            onNodeWithText("Diana").assertIsDisplayed()
            onAllNodesWithText("Alice").assertCountEquals(0)
        }

    @Test
    fun `shows no users found when mention query has no match`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            onNodeWithText("").performTextInput("@NonExistentUser")

            onNodeWithText("No users found").assertIsDisplayed()
        }

    @Test
    fun `clicking user suggestion completes mention into editor and dismisses popup`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            onNodeWithText("").performTextInput("@Ali")
            onNodeWithText("Alice").assertIsDisplayed()

            // Click user item
            onNodeWithText("Alice").performClick()

            // Popup should be dismissed
            onAllNodesWithText("No users found").assertCountEquals(0)
            onAllNodesWithText("Product Designer").assertCountEquals(0)

            // Editor text is replaced with "@Alice "
            onNodeWithText("@Alice ").assertIsDisplayed()
        }

    @Test
    fun `typing hash-symbol displays channels and completes selection`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            onNodeWithText("").performTextInput("#arran")

            onNodeWithText("arranger-dev").assertIsDisplayed()
            onNodeWithText("arranger-dev").performClick()

            // Completed into editor as "#arranger-dev "
            onNodeWithText("#arranger-dev ").assertIsDisplayed()
        }

    @Test
    fun `sending a message adds it to timeline and clears the editor`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            val messageText = "Hello team, welcome to Arranger!"
            onNodeWithText("").performTextInput(messageText)

            // Click Send
            onNodeWithText("Send").performClick()

            // Message should appear in timeline
            onNodeWithText(messageText).assertIsDisplayed()
            onNodeWithText("You").assertIsDisplayed()

            // Editor should be cleared (placeholder text visible again)
            onNodeWithText("Type '@' for mention, '#' for channel...").assertIsDisplayed()
        }

    @Test
    fun `empty or whitespace-only messages are not sent`() =
        runComposeUiTest {
            setContent {
                ArrangerTheme {
                    MentionAutocompleteSample()
                }
            }

            // Initially one bot welcome message exists
            onAllNodesWithText("You").assertCountEquals(0)

            // Try sending empty text
            onNodeWithText("Send").performClick()
            onAllNodesWithText("You").assertCountEquals(0)

            // Try sending whitespace only
            onNodeWithText("").performTextInput("   ")
            onNodeWithText("Send").performClick()
            onAllNodesWithText("You").assertCountEquals(0)
        }
}
