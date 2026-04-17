package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.HexColor
import dev.mkeeda.arranger.richtext.TextSize

/**
 * Converts a [HexColor] domain model to a Compose [Color].
 * Defaults to [Color.Unspecified] if the [HexColor] is unspecified or malformed.
 */
public fun HexColor.toColor(): Color {
    if (this == HexColor.Unspecified) return Color.Unspecified
    return try {
        Color(android.graphics.Color.parseColor(this.value))
    } catch (e: IllegalArgumentException) {
        Color.Unspecified
    }
}

/**
 * Converts a [TextSize] domain model to a Compose [TextUnit] using 'sp'.
 */
public fun TextSize.toTextUnit(): TextUnit {
    if (this == TextSize.Unspecified) return TextUnit.Unspecified
    return this.sp.sp
}
