package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import dev.mkeeda.arranger.richtext.AttributeEditScope
import dev.mkeeda.arranger.richtext.backgroundColor
import dev.mkeeda.arranger.richtext.fontSize
import dev.mkeeda.arranger.richtext.textColor

/**
 * Convenience function to set the text color using a Compose [Color].
 */
public fun AttributeEditScope.textColor(color: Color) {
    textColor(color.toRgbaColor())
}

/**
 * Convenience function to set the background color using a Compose [Color].
 */
public fun AttributeEditScope.backgroundColor(color: Color) {
    backgroundColor(color.toRgbaColor())
}

/**
 * Convenience function to set the font size using a Compose [TextUnit].
 */
public fun AttributeEditScope.fontSize(size: TextUnit) {
    fontSize(size.toTextSize())
}
