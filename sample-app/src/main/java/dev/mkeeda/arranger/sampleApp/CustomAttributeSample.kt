package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

// 1. Define Custom Attribute Key
public object HighlightKey : SpanAttributeKey<Unit> {
    override val name: String = "Highlight"
    override val defaultValue: Unit = Unit
}

// 2. Create a custom AttributeStyleResolver inheriting from DefaultAttributeStyleResolver
private val customResolver =
    AttributeStyleResolver(base = DefaultAttributeStyleResolver) {
        spanStyle(HighlightKey) {
            SpanStyle(
                background = Color(0xFFFFF59D), // Light Yellow
                color = Color(0xFFE65100), // Orange Text
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }

@Composable
fun CustomAttributeSample(modifier: Modifier = Modifier) {
    val initialText = "Arranger also supports Custom Attributes.\nThis text is highlighted using a custom resolver!"

    // 3. Initialize RichTextState with the custom attribute
    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        val range = initialText.rangeOf("highlighted")
                        setSpanAttribute(HighlightKey, Unit, range)
                    },
            )
        }

    // 4. Pass the custom resolver to RichTextEditor
    RichTextEditor(
        state = state,
        styleResolver = customResolver,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
fun CustomAttributeSamplePreview() {
    ArrangerTheme {
        CustomAttributeSample()
    }
}
