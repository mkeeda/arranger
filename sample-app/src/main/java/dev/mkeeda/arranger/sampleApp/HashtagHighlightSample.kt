package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.clearTextColor
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.rangesOf
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun HashtagHighlightSample(modifier: Modifier = Modifier) {
    val initialText = "Type some #hashtags here!\nFor example: #Compose is #awesome"

    val state =
        remember {
            RichTextState(
                initialText = RichString(text = initialText),
            )
        }

    LaunchedEffect(state) {
        snapshotFlow { state.richString.text }.collect { text ->
            state.edit {
                // Clear existing colors first
                editAttributes(range = text.indices) {
                    clearTextColor()
                }

                // Find all hashtags and highlight them in blue
                val hashtagRanges = text.rangesOf(Regex("#\\w+"))
                editAll(hashtagRanges) {
                    textColor(Color(0xFF1976D2)) // Blue
                }
            }
        }
    }

    RichTextEditor(
        state = state,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
fun HashtagHighlightSamplePreview() {
    ArrangerTheme {
        HashtagHighlightSample()
    }
}
