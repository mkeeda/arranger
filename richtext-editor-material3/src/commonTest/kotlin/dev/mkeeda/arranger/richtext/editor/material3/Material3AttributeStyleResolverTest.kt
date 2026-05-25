package dev.mkeeda.arranger.richtext.editor.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class Material3AttributeStyleResolverTest {
    @Test
    fun `heading maps to display and headline typography from MaterialTheme`() =
        runComposeUiTest {
            var resolver: AttributeStyleResolver? = null
            var typography: Typography? = null

            setContent {
                MaterialTheme {
                    typography = MaterialTheme.typography
                    resolver = rememberMaterial3AttributeStyleResolver()
                }
            }

            val currentResolver = requireNotNull(resolver)
            val currentTypography = requireNotNull(typography)

            // H1 -> displayLarge
            var resolved = currentResolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H1))
            resolved.spanStyle?.fontSize shouldBe currentTypography.displayLarge.fontSize

            // H3 -> headlineLarge
            resolved = currentResolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H3))
            resolved.spanStyle?.fontSize shouldBe currentTypography.headlineLarge.fontSize

            // H6 -> titleMedium
            resolved = currentResolver.resolve(AttributeContainer.empty() + (HeadingKey to HeadingLevel.H6))
            resolved.spanStyle?.fontWeight shouldBe currentTypography.titleMedium.fontWeight
        }

    @Test
    fun `blockquote maps to bodyMedium style with onSurfaceVariant color`() =
        runComposeUiTest {
            var resolver: AttributeStyleResolver? = null
            var typography: Typography? = null
            var colorScheme: ColorScheme? = null

            setContent {
                MaterialTheme {
                    typography = MaterialTheme.typography
                    colorScheme = MaterialTheme.colorScheme
                    resolver = rememberMaterial3AttributeStyleResolver()
                }
            }

            val currentResolver = requireNotNull(resolver)
            val currentTypography = requireNotNull(typography)
            val currentColor = requireNotNull(colorScheme)

            val resolved = currentResolver.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))

            resolved.spanStyle?.fontSize shouldBe currentTypography.bodyMedium.fontSize
            resolved.spanStyle?.color shouldBe currentColor.onSurfaceVariant
        }

    @Test
    fun `resolver updates dynamically when MaterialTheme ColorScheme changes`() =
        runComposeUiTest {
            var resolver: AttributeStyleResolver? = null
            val darkColorScheme =
                darkColorScheme(
                    onSurfaceVariant = Color.Red,
                )

            setContent {
                MaterialTheme(colorScheme = darkColorScheme) {
                    resolver = rememberMaterial3AttributeStyleResolver()
                }
            }

            val currentResolver = requireNotNull(resolver)
            val resolved = currentResolver.resolve(AttributeContainer.empty() + (BlockquoteKey to Unit))
            resolved.spanStyle?.color shouldBe Color.Red
        }
}
