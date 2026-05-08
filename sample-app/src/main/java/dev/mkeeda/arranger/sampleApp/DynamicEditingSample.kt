package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun DynamicEditingSample(modifier: Modifier = Modifier) {
    val initialText = "Edit this styled text to see the magic."

    // 1. Initialize state with formatting
    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        editAttributes(range = initialText.rangeOf("styled text")) {
                            bold()
                            textColor(Color(0xFF6200EA)) // Purple
                        }
                    },
            )
        }

    // 2. Render natively via Compose 1.7
    // Try typing in the middle of "styled text"!
    // Arranger automatically tracks and shifts the span indices in the background.
    RichTextEditor(
        state = state,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
fun DynamicEditingSamplePreview() {
    ArrangerTheme {
        DynamicEditingSample()
    }
}
