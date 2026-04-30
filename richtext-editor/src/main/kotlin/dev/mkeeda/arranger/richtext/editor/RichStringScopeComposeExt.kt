package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.AttributeEditScope
import dev.mkeeda.arranger.richtext.RichStringScope

/**
 * Applies a set of attribute mutations to the range specified by the given [TextRange].
 * This is a convenience overload that converts [TextRange] to [IntRange].
 *
 * If the selection is collapsed (cursor with no selection), the resulting range is empty
 * and no attributes are modified.
 */
public fun RichStringScope.editAttributes(
    selection: TextRange,
    editAction: AttributeEditScope.() -> Unit,
) {
    editAttributes(range = selection.min until selection.max, editAction = editAction)
}

/**
 * Applies a set of attribute mutations to the range specified by the given [TextRange].
 * This is a convenience overload that converts [TextRange] to [IntRange].
 *
 * If the selection is collapsed (cursor with no selection), the resulting range is empty
 * and no attributes are modified.
 */
public fun RichTextBuffer.editAttributes(
    selection: TextRange,
    editAction: AttributeEditScope.() -> Unit,
) {
    editAttributes(range = selection.min until selection.max, editAction = editAction)
}
