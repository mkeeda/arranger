package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.TextSize

/**
 * Converts an [RgbaColor] domain model to a Compose [Color].
 * Defaults to [Color.Unspecified] if the [RgbaColor] is unspecified.
 */
public fun RgbaColor.toColor(): Color {
    if (this == RgbaColor.Unspecified) return Color.Unspecified
    return Color(color = this.value and 0xFFFFFFFFL) // Ensuring just 32bit raw ARGB is fed
}

/**
 * Converts a Compose [Color] to an [RgbaColor] domain model.
 * Defaults to [RgbaColor.Unspecified] if the [Color] is unspecified.
 */

/**
 * Converts a [TextSize] domain model to a Compose [TextUnit] using 'sp'.
 */
public fun TextSize.toTextUnit(): TextUnit {
    if (this == TextSize.Unspecified) return TextUnit.Unspecified
    return this.sp.sp
}

/**
 * Converts a Compose [TextUnit] using 'sp' to a [TextSize] domain model.
 * Defaults to [TextSize.Unspecified] if the [TextUnit] is unspecified.
 * @throws IllegalArgumentException if the [TextUnit] is not specified in SP.
 */
    return TextSize(this.value)
}
