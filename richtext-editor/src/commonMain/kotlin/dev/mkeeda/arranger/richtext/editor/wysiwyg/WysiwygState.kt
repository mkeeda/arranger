package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mkeeda.arranger.richtext.editor.EditorSnapshot
import dev.mkeeda.arranger.richtext.editor.RichTextState

/**
 * Manages the transient state immediately following an auto-formatting event in the WYSIWYG editor
 * (used for Backspace restoration).
 */
@Stable
public class WysiwygState {
    internal var lastAutoFormatEvent: AutoFormatEvent? by mutableStateOf(null)

    /**
     * Determines whether the most recent auto-formatting event can be reverted (restoring State B)
     * via Backspace or explicit revert action.
     * - An auto-formatting event must have occurred immediately prior
     * - The cursor must be exactly at the expected post-format position
     * - The selection must be collapsed
     * - Undo must be available
     */
    public fun canRevert(state: RichTextState): Boolean {
        val event = lastAutoFormatEvent ?: return false
        val sel = state.selection
        return sel.collapsed && sel.start == event.postFormatCursor && state.undoState.canUndo
    }

    /**
     * Reverts the most recent auto-formatting event, restoring State B (raw symbol text).
     */
    public fun revert(state: RichTextState): Boolean {
        if (!canRevert(state)) return false
        state.undoState.undo()
        clearLastAutoFormat()
        return true
    }

    public fun clearLastAutoFormat() {
        lastAutoFormatEvent = null
    }

    internal fun recordAutoFormat(event: AutoFormatEvent) {
        lastAutoFormatEvent = event
    }
}

internal data class AutoFormatEvent(
    val type: AutoFormatType,
    val preFormatSnapshot: EditorSnapshot,
    val postFormatCursor: Int,
)

internal enum class AutoFormatType {
    Heading,
    BulletList,
    OrderedList,
    Blockquote,
    Bold,
    Italic,
    InlineCode,
    Strikethrough,
}
