package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState

@Composable
fun UndoRedoSample(modifier: Modifier = Modifier) {
    val initialText = "Type something here and use Undo/Redo buttons."

    // 1. Initialize state
    val state =
        remember {
            RichTextState(initialText = RichString(text = initialText))
        }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Undo / Redo", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Add Undo and Redo buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { state.undoState.undo() },
                enabled = state.undoState.canUndo,
                modifier = Modifier.weight(1f),
            ) {
                Text("Undo")
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(
                onClick = { state.undoState.redo() },
                enabled = state.undoState.canRedo,
                modifier = Modifier.weight(1f),
            ) {
                Text("Redo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Render natively via Compose 1.7
        RichTextEditor(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}
