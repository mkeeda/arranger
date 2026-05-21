package dev.mkeeda.arranger.sampleApp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `Navigate to all sample screens`() {
        SampleDestination.entries.forEach { destination ->
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(destination.title))
            composeTestRule.onNodeWithText(destination.title).performClick()

            val backButton = composeTestRule.onNodeWithContentDescription("back")
            backButton.assertIsDisplayed()
            backButton.performClick()
        }
    }
}
