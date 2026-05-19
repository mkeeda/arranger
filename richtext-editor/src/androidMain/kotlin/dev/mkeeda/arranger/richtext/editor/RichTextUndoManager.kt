package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.mutableStateListOf

internal class RichTextUndoManager(
    private val maxSize: Int = 100,
) {
    private val undoStack = mutableStateListOf<EditorSnapshot>()
    private val redoStack = mutableStateListOf<EditorSnapshot>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    private var lastMergePolicy: UndoMergePolicy? = null

    fun pushSnapshot(snapshot: EditorSnapshot, mergePolicy: UndoMergePolicy) {
        when (mergePolicy) {
            UndoMergePolicy.Merge -> {
                if (lastMergePolicy != UndoMergePolicy.Merge || undoStack.isEmpty()) {
                    undoStack.add(snapshot)
                }
            }

            UndoMergePolicy.Separate -> {
                undoStack.add(snapshot)
            }
        }

        lastMergePolicy = mergePolicy

        if (undoStack.size > maxSize) {
            undoStack.removeAt(0)
        }

        redoStack.clear()
    }

    fun undo(currentSnapshot: EditorSnapshot): EditorSnapshot? {
        if (undoStack.isEmpty()) return null
        val previousSnapshot = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(currentSnapshot)
        lastMergePolicy = null // Reset merging after undo
        return previousSnapshot
    }

    fun redo(currentSnapshot: EditorSnapshot): EditorSnapshot? {
        if (redoStack.isEmpty()) return null
        val nextSnapshot = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(currentSnapshot)
        lastMergePolicy = null // Reset merging after redo
        return nextSnapshot
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastMergePolicy = null
    }
}
