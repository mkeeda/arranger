package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun BasicUsageSample(modifier: Modifier = Modifier) {
    val initialText = "Welcome to Arranger!\nEnjoy building RichText in Compose programmatically."

    // 1. Initialize state with minimal attributes using the declarative DSL
    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        editAttributes(range = initialText.rangeOf("Arranger!")) {
                            bold()
                            textColor(Color(0xFF6200EA)) // Purple
                        }
                    },
            )
        }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Basic Usage", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Render editor
        RichTextEditor(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BasicUsageSamplePreview() {
    ArrangerTheme {
        BasicUsageSample()
    }
}
