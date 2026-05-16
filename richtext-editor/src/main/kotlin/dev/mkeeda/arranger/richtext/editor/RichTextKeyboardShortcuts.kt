package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

internal fun Modifier.richTextKeyboardShortcuts(state: RichTextState): Modifier =
    this.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
            val isCommandOrCtrl = event.isCtrlPressed || event.isMetaPressed
            if (isCommandOrCtrl) {
                when {
                    event.key == Key.Z && event.isShiftPressed -> {
                        if (state.undoState.canRedo) state.undoState.redo()
                        true
                    }

                    event.key == Key.Z -> {
                        if (state.undoState.canUndo) state.undoState.undo()
                        true
                    }

                    else -> {
                        false
                    }
                }
            } else {
                false
            }
        } else {
            false
        }
    }
