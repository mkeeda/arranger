package dev.mkeeda.arranger.richtext.editor

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.ParagraphStyle

internal class ComposeParagraphWorkarounds {
    private var emptyParagraphIndices: List<Int> = emptyList()

    /**
     * Maps an original character index to its new index in the transformed text buffer.
     * This is used when targeting a specific character that may have been shifted by insertions.
     */
    fun mapCharacterIndex(originalIndex: Int): Int {
        return originalIndex + emptyParagraphIndices.count { it <= originalIndex }
    }

    /**
     * Maps a style boundary offset to its new position in the transformed text buffer.
     * This is used for ranges where we want the style to cover newly inserted workaround characters.
     */
    fun mapStyleOffset(originalOffset: Int): Int {
        return originalOffset + emptyParagraphIndices.count { it < originalOffset }
    }

    fun apply(
        buffer: TextFieldBuffer,
        getParagraphStyleAt: (Int) -> ParagraphStyle?,
    ) {
        // Reset at the start so callers always see a consistent state in case of mid-frame reads
        emptyParagraphIndices = emptyList()

        val originalText = buffer.asCharSequence().toString()
        val originalLength = originalText.length

        // Workaround 1: Compose TextLayoutResult ignores ParagraphStyle for completely empty paragraphs.
        // This breaks cursor positioning (e.g. list indentation) when typing a newline at the end of a list item.
        // We find all empty paragraphs and insert a Zero-Width Space (\u200B) to force the style application.
        // We iterate backwards to avoid index shifting during insertion.
        val emptyIndices = mutableListOf<Int>()
        for (i in originalLength downTo 0) {
            val isLineEmpty = (i == originalLength || originalText[i] == '\n') && (i == 0 || originalText[i - 1] == '\n')
            if (isLineEmpty && getParagraphStyleAt(i) != null) {
                emptyIndices.add(i)
                buffer.replace(i, i, "\u200B")
            }
        }
        emptyParagraphIndices = emptyIndices

        // Workaround 2: Compose interprets `\n` within or at the end of a `ParagraphStyle` span as a hard paragraph separator.
        // When two adjacent lines have different `ParagraphStyle`s, keeping the `\n` between them causes Compose
        // to render an unintended extra empty line (double spacing).
        // By replacing the boundary `\n` with a zero-width non-breaking space (`\uFEFF`) right before rendering,
        // we eliminate the explicit newline character while letting the style change handle the visual line break.
        var searchStartIndex = 0
        while (true) {
            val i = originalText.indexOf('\n', searchStartIndex)
            if (i == -1) break

            val styleAtI = getParagraphStyleAt(i)
            val styleAtNext = getParagraphStyleAt(i + 1)

            val isBoundary = styleAtI != styleAtNext
            // If the `\n` is the very last character in the text, replacing it would completely remove the trailing
            // empty line (since there is no text after it to break to).
            // We only replace it if there is a `ParagraphStyle` spanning the empty region after the `\n`,
            // because that empty style block itself will force Compose to render the trailing empty line.
            val isSafeToReplace =
                if (i == originalLength - 1) {
                    styleAtNext != null
                } else {
                    true
                }

            if (isBoundary && isSafeToReplace) {
                val mappedI = mapCharacterIndex(i)
                buffer.replace(mappedI, mappedI + 1, "\uFEFF")
            }

            searchStartIndex = i + 1
        }
    }
}
