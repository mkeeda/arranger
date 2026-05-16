package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Material3AttributeStyleResolverTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `heading maps to display and headline typography from MaterialTheme`() {
        var resolver: AttributeStyleResolver? = null

        composeTestRule.setContent {
            MaterialTheme {
                resolver = rememberMaterial3AttributeStyleResolver()
            }
        }

        // H1 -> displayLarge
        var resolved = resolver!!.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H1))
        resolved.spanStyle?.fontSize shouldBe 57.sp

        // H3 -> headlineLarge
        resolved = resolver!!.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H3))
        resolved.spanStyle?.fontSize shouldBe 32.sp

        // H6 -> titleMedium
        resolved = resolver!!.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H6))
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Medium
    }

    @Test
    fun `blockquote maps to bodyMedium style with onSurfaceVariant color`() {
        var resolver: AttributeStyleResolver? = null

        composeTestRule.setContent {
            MaterialTheme {
                resolver = rememberMaterial3AttributeStyleResolver()
            }
        }

        val resolved = resolver!!.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))

        resolved.spanStyle?.fontSize shouldBe 14.sp // default bodyMedium
        // The default light onSurfaceVariant is roughly Color(0xFF49454F)
        resolved.spanStyle?.color shouldBe Color(0xFF49454F)
    }

    @Test
    fun `resolver updates dynamically when MaterialTheme ColorScheme changes`() {
        var resolver: AttributeStyleResolver? = null
        val darkColorScheme =
            darkColorScheme(
                onSurfaceVariant = Color.Red,
            )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme) {
                resolver = rememberMaterial3AttributeStyleResolver()
            }
        }

        val resolved = resolver!!.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))
        resolved.spanStyle?.color shouldBe Color.Red
    }
}
