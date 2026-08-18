package dev.mkeeda.arranger.sample.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.applyFormat
import dev.mkeeda.arranger.richtext.editor.clearFormats
import dev.mkeeda.arranger.richtext.editor.detectAndApplyLinks
import dev.mkeeda.arranger.richtext.editor.removeFormat

@Composable
public fun HyperlinkSample(modifier: Modifier = Modifier) {
    val initialText = "Check out https://kotlinlang.org and www.google.com for more info."

    val state = remember { RichTextState(initialText = RichString(initialText)) }
    var inputUrl by remember { mutableStateOf("https://example.com") }

    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            Text("Hyperlink Support Sample")
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("Link URL") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = { state.applyFormat(LinkKey, inputUrl) }) {
                    Text("Apply Link")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { state.removeFormat(LinkKey) }) {
                    Text("Remove Link")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    state.clearFormats()
                    state.detectAndApplyLinks()
                }) {
                    Text("Auto Detect")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            RichTextEditor(
                state = state,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
            )
        }
    }
}
