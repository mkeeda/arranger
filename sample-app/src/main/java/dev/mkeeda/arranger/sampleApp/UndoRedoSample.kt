package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.editAttributes
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.rangeOf

@Composable
fun UndoRedoSample(modifier: Modifier = Modifier) {
    val initialText = "Type something here, make changes, and use Undo/Redo buttons."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        val range = initialText.rangeOf("Undo/Redo")
                        editAttributes(range = range) {
                            bold()
                            textColor(Color(0xFF1976D2)) // Blue
                        }
                    },
            )
        }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Undo / Redo", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { state.undoState.undo() },
                enabled = state.undoState.canUndo,
            ) {
                Text("Undo")
            }
            Button(
                onClick = { state.undoState.redo() },
                enabled = state.undoState.canRedo,
            ) {
                Text("Redo")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    state.edit {
                        editAttributes(range = 0 until textLength) { bold() }
                    }
                },
            ) {
                Text("Make All Bold")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RichTextEditor(
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}
