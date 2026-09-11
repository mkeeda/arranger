package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mkeeda.arranger.richtext.editor.EditorSnapshot
import dev.mkeeda.arranger.richtext.editor.RichTextState

/**
 * WYSIWYG エディタの自動フォーマット直後の一時状態（Backspace復元用）を管理する。
 */
@Stable
public class WysiwygState {
    internal var lastAutoFormatEvent: AutoFormatEvent? by mutableStateOf(null)

    /**
     * 直前の自動変換が Backspace または Revert によって巻き戻し（State B 復元）可能かを判定する。
     * - 直前に自動変換が発生していること
     * - カーソル位置が自動変換直後の期待位置（postFormatCursor）と完全に一致していること
     * - 選択範囲が折りたたまれていること（collapsed）
     * - Undo が可能であること
     */
    public fun canRevert(state: RichTextState): Boolean {
        val event = lastAutoFormatEvent ?: return false
        val sel = state.selection
        return sel.collapsed && sel.start == event.postFormatCursor && state.undoState.canUndo
    }

    /**
     * 直前の自動変換を巻き戻し、State B（生記号テキスト）へ復元する。
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
    Code,
    Strikethrough,
}
