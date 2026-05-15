package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import dev.mkeeda.arranger.richtext.RichSpan

/**
 * Manages the undo and redo history for a [RichTextState].
 *
 * This state allows you to programmatically trigger [undo], [redo], and [clearHistory] operations,
 * as well as observe the availability of these operations via [canUndo] and [canRedo].
 *
 * ### Snapshot Granularity
 * The undo history is recorded automatically based on the following rules:
 * - Consecutive single-character typing (e.g., typing "abc") is merged into a single undo step.
 * - Whitespace, newlines, text deletions (e.g., Backspace, Delete), and pasting multiple
 *   characters always create a separate undo step.
 */
@Stable
public class RichTextUndoState internal constructor(
    private val textFieldState: TextFieldState,
    private val getSpans: () -> List<RichSpan>,
    private val setSpans: (List<RichSpan>) -> Unit,
    private val clearTypingAttributes: () -> Unit,
) {
    internal val undoManager = RichTextUndoManager()

    /**
     * Whether an undo operation is available.
     * Returns `true` if there is at least one action that can be undone.
     */
    public val canUndo: Boolean
        get() = undoManager.canUndo

    /**
     * Whether a redo operation is available.
     * Returns `true` if there is at least one action that can be redone.
     */
    public val canRedo: Boolean
        get() = undoManager.canRedo

    /**
     * Reverts the most recent change made to the text or rich text spans.
     * If [canUndo] is `false`, this operation does nothing.
     */
    @OptIn(ExperimentalFoundationApi::class)
    public fun undo() {
        val undoneSnapshot = undoManager.undo(takeSnapshot()) ?: return
        restoreSnapshot(undoneSnapshot)
    }

    /**
     * Reapplies the most recent change that was undone.
     * If [canRedo] is `false`, this operation does nothing.
     */
    @OptIn(ExperimentalFoundationApi::class)
    public fun redo() {
        val redoneSnapshot = undoManager.redo(takeSnapshot()) ?: return
        restoreSnapshot(redoneSnapshot)
    }

    /**
     * Clears both the undo and redo history.
     * This resets [canUndo] and [canRedo] to `false`.
     */
    @OptIn(ExperimentalFoundationApi::class)
    public fun clearHistory() {
        undoManager.clear()
        textFieldState.undoState.clearHistory()
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun captureSnapshotBeforeChange(buffer: TextFieldBuffer) {
        val snapshotBefore = takeSnapshot()
        val mergePolicy = resolveMergePolicy(buffer)
        undoManager.pushSnapshot(snapshotBefore, mergePolicy)
    }

    private fun takeSnapshot(): EditorSnapshot {
        return EditorSnapshot(
            text = textFieldState.text.toString(),
            spans = getSpans(),
            selection = textFieldState.selection,
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun restoreSnapshot(snapshot: EditorSnapshot) {
        textFieldState.edit {
            replace(0, length, snapshot.text)
            selection = snapshot.selection
        }
        textFieldState.undoState.clearHistory()
        setSpans(snapshot.spans)
        clearTypingAttributes()
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun resolveMergePolicy(buffer: TextFieldBuffer): UndoMergePolicy {
    if (buffer.changes.changeCount != 1) return UndoMergePolicy.Separate

    val range = buffer.changes.getRange(0)
    val originalRange = buffer.changes.getOriginalRange(0)

    val insertedText = buffer.asCharSequence().substring(range.min, range.max)
    val deletedLength = originalRange.length

    // Deletion always creates a new undo entry
    if (deletedLength > 0) return UndoMergePolicy.Separate

    // Paste (multiple characters) always creates a new undo entry
    if (insertedText.length > 1) return UndoMergePolicy.Separate

    // Space or newline creates a new undo entry
    if (insertedText.any { it.isWhitespace() }) return UndoMergePolicy.Separate

    return UndoMergePolicy.Merge
}
