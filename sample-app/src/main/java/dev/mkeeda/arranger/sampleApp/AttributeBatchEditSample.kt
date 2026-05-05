package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun AttributeBatchEditSample(modifier: Modifier = Modifier) {
    val initialText =
        "This text has some bold words.\n" +
            "We can find all bold parts and change their color at once."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        editAttributes(range = initialText.rangeOf("bold words")) {
                            bold()
                        }
                        editAttributes(range = initialText.rangeOf("bold parts")) {
                            bold()
                        }
                    },
            )
        }

    Column(modifier = modifier) {
        Button(
            onClick = {
                // Find all runs that have the BoldKey
                val boldRuns = state.richString.runs(BoldKey)

                // Batch edit those specific runs
                state.edit {
                    editAll(boldRuns) {
                        textColor(Color(0xFFD32F2F)) // Red
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Highlight Bold Text in Red")
        }

        Spacer(modifier = Modifier.height(16.dp))

        RichTextEditor(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AttributeBatchEditSamplePreview() {
    ArrangerTheme {
        AttributeBatchEditSample()
    }
}
