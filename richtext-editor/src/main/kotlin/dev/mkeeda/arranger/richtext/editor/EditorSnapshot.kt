package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.RichSpan

internal data class EditorSnapshot(
    val text: String,
    val spans: List<RichSpan>,
    val selection: TextRange,
)
