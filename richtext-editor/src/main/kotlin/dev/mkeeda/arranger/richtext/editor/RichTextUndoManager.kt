package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class RichTextUndoManager(
    private val maxSize: Int = 100
) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    var canUndo: Boolean by mutableStateOf(false)
        private set

    var canRedo: Boolean by mutableStateOf(false)
        private set

    fun pushSnapshot(snapshot: EditorSnapshot, mergePolicy: UndoMergePolicy) {
        when (mergePolicy) {
            UndoMergePolicy.MERGE -> {
                if (undoStack.isNotEmpty()) {
                    undoStack[undoStack.lastIndex] = snapshot
                } else {
                    undoStack.addLast(snapshot)
                }
            }
            UndoMergePolicy.SEPARATE -> {
                undoStack.addLast(snapshot)
            }
        }
        
        if (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
        
        redoStack.clear()
        updateFlags()
    }

    fun undo(currentSnapshot: EditorSnapshot): EditorSnapshot? {
        if (undoStack.isEmpty()) return null
        val previousSnapshot = undoStack.removeLast()
        redoStack.addLast(currentSnapshot)
        updateFlags()
        return previousSnapshot
    }

    fun redo(currentSnapshot: EditorSnapshot): EditorSnapshot? {
        if (redoStack.isEmpty()) return null
        val nextSnapshot = redoStack.removeLast()
        undoStack.addLast(currentSnapshot)
        updateFlags()
        return nextSnapshot
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateFlags()
    }

    private fun updateFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}
