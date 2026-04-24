package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.AttributeEditScope
import dev.mkeeda.arranger.richtext.RichStringBuffer

/**
 * Applies a set of attribute mutations to the range specified by the given [TextRange].
 * This is a convenience overload that converts [TextRange] to [IntRange].
 */
public fun RichStringBuffer.editAttributes(
    selection: TextRange,
    editAction: AttributeEditScope.() -> Unit,
) {
    editAttributes(range = selection.min until selection.max, editAction = editAction)
}
