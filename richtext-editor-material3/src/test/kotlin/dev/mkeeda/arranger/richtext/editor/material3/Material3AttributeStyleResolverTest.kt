package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
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
        var typography: Typography? = null

        composeTestRule.setContent {
            MaterialTheme {
                typography = MaterialTheme.typography
                resolver = rememberMaterial3AttributeStyleResolver()
            }
        }

        // H1 -> displayLarge
        var resolved = resolver!!.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H1))
        resolved.spanStyle?.fontSize shouldBe typography!!.displayLarge.fontSize

        // H3 -> headlineLarge
        resolved = resolver!!.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H3))
        resolved.spanStyle?.fontSize shouldBe typography!!.headlineLarge.fontSize

        // H6 -> titleMedium
        resolved = resolver!!.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H6))
        resolved.spanStyle?.fontWeight shouldBe typography!!.titleMedium.fontWeight
    }

    @Test
    fun `blockquote maps to bodyMedium style with onSurfaceVariant color`() {
        var resolver: AttributeStyleResolver? = null
        var typography: Typography? = null
        var colorScheme: ColorScheme? = null

        composeTestRule.setContent {
            MaterialTheme {
                typography = MaterialTheme.typography
                colorScheme = MaterialTheme.colorScheme
                resolver = rememberMaterial3AttributeStyleResolver()
            }
        }

        val resolved = resolver!!.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))

        resolved.spanStyle?.fontSize shouldBe typography!!.bodyMedium.fontSize
        resolved.spanStyle?.color shouldBe colorScheme!!.onSurfaceVariant
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
