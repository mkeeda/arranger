package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.EditorSnapshot
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.UndoMergePolicy

internal object WysiwygAutoFormatter {
    internal fun formatBlockIfMatched(
        buffer: TextFieldBuffer,
        state: RichTextState,
        lastInsertedChar: Char?,
    ): AutoFormatEvent? {
        val cursor = buffer.selection.start
        if (!buffer.selection.collapsed || cursor < 2) return null

        val text = buffer.asCharSequence()
        if (text[cursor - 1] != ' ') return null
        if (lastInsertedChar != null && lastInsertedChar != ' ') return null

        val lastNewline = text.lastIndexOf('\n', startIndex = (cursor - 2).coerceAtLeast(0))
        val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
        val prefix = text.subSequence(lineStart, cursor).toString()

        val (type, triggerLength, applyAction) =
            when (prefix) {
                "# " -> {
                    Triple(AutoFormatType.Heading, 2) { r: IntRange, txt: String ->
                        state.setParagraphAttributeDirectly(HeadingKey, HeadingLevel.H1, r, txt)
                    }
                }

                "## " -> {
                    Triple(AutoFormatType.Heading, 3) { r: IntRange, txt: String ->
                        state.setParagraphAttributeDirectly(HeadingKey, HeadingLevel.H2, r, txt)
                    }
                }

                "### " -> {
                    Triple(AutoFormatType.Heading, 4) { r: IntRange, txt: String ->
                        state.setParagraphAttributeDirectly(HeadingKey, HeadingLevel.H3, r, txt)
                    }
                }

                "- ", "* " -> {
                    Triple(AutoFormatType.BulletList, 2) { r: IntRange, txt: String ->
                        state.setParagraphAttributeDirectly(BulletListKey, ListIndentLevel.Level1, r, txt)
                    }
                }

                "1. " -> {
                    Triple(AutoFormatType.OrderedList, 3) { r: IntRange, txt: String ->
                        state.setParagraphAttributeDirectly(OrderedListKey, ListIndentLevel.Level1, r, txt)
                    }
                }

                "> " -> {
                    Triple(AutoFormatType.Blockquote, 2) { r: IntRange, txt: String ->
                        state.setParagraphAttributeDirectly(BlockquoteKey, Unit, r, txt)
                    }
                }

                else -> {
                    return null
                }
            }

        // --- 3-state model: push State B snapshot to UndoManager ---
        val stateBSnapshot =
            EditorSnapshot(
                text = buffer.toString(),
                spans = state.richString.spans,
                selection = buffer.selection,
            )
        state.undoState.undoManager.pushSnapshot(stateBSnapshot, UndoMergePolicy.Separate)

        // --- Transform to State C ---
        val deletedText = prefix
        buffer.delete(lineStart, lineStart + triggerLength)
        buffer.selection = TextRange(lineStart)
        state.shiftSpansDirectly(
            editStart = lineStart,
            editEnd = lineStart + triggerLength,
            newLength = 0,
            offsetDiff = -triggerLength,
            deletedText = deletedText,
        )

        val updatedText = buffer.toString()
        val rawLineEnd = updatedText.indexOf('\n', startIndex = lineStart)
        val lineEnd = if (rawLineEnd == -1) updatedText.length else rawLineEnd
        val isLineEmpty = lineStart == lineEnd
        val paragraphRange =
            if (rawLineEnd == -1) {
                lineStart..(lineEnd - 1).coerceAtLeast(lineStart)
            } else {
                lineStart..rawLineEnd
            }

        applyAction(paragraphRange, updatedText)

        // Prevent block-level attribute collision: remove existing block attributes from typing attributes
        state.removeTypingAttribute(HeadingKey)
        state.removeTypingAttribute(BulletListKey)
        state.removeTypingAttribute(OrderedListKey)
        state.removeTypingAttribute(BlockquoteKey)

        // Synchronize typing attributes if line is empty
        if (isLineEmpty) {
            when (type) {
                AutoFormatType.Heading -> {
                    state.setTypingAttribute(
                        HeadingKey,
                        when (prefix) {
                            "# " -> HeadingLevel.H1
                            "## " -> HeadingLevel.H2
                            else -> HeadingLevel.H3
                        },
                    )
                }

                AutoFormatType.BulletList -> {
                    state.setTypingAttribute(BulletListKey, ListIndentLevel.Level1)
                }

                AutoFormatType.OrderedList -> {
                    state.setTypingAttribute(OrderedListKey, ListIndentLevel.Level1)
                }

                AutoFormatType.Blockquote -> {
                    state.setTypingAttribute(BlockquoteKey, Unit)
                }

                else -> {}
            }
        }

        return AutoFormatEvent(
            type = type,
            preFormatSnapshot = stateBSnapshot,
            postFormatCursor = lineStart,
        )
    }

    internal fun formatInlineIfMatched(
        buffer: TextFieldBuffer,
        state: RichTextState,
        lastInsertedChar: Char?,
    ): AutoFormatEvent? {
        val cursor = buffer.selection.start
        if (!buffer.selection.collapsed || cursor <= 0) return null
        val text = buffer.asCharSequence()

        val lastChar = text[cursor - 1]
        val candidateDelims =
            when {
                (lastInsertedChar == '*' || lastChar == '*') && text.take(cursor).endsWith("**") -> listOf("**")
                (lastInsertedChar == '*' || lastChar == '*') -> listOf("*")
                (lastInsertedChar == '_' || lastChar == '_') -> listOf("_")
                (lastInsertedChar == '`' || lastChar == '`') -> listOf("`")
                (lastInsertedChar == '~' || lastChar == '~') -> listOf("~")
                else -> return null
            }

        for (delim in candidateDelims) {
            val closingEnd = cursor
            val closingStart = cursor - delim.length

            if (closingStart > 0 && isEscaped(text, closingStart)) continue

            if (delim == "*") {
                if (closingStart > 0 && text[closingStart - 1] == '*') continue
                if (closingEnd < text.length && text[closingEnd] == '*') continue
            }
            if (delim == "_") {
                if (closingStart > 0 && text[closingStart - 1] == '_') continue
                if (closingEnd < text.length && text[closingEnd] == '_') continue
            }

            // Closing delimiter cannot be preceded by whitespace
            if (closingStart > 0 && text[closingStart - 1].isWhitespace()) continue

            val lineStart =
                text.lastIndexOf('\n', startIndex = (closingStart - 1).coerceAtLeast(0)).let {
                    if (it == -1) 0 else it + 1
                }

            val searchArea = text.subSequence(lineStart, closingStart).toString()
            var openingIndexInSearch = -1
            var searchFrom = searchArea.length
            while (searchFrom > 0) {
                val idx = searchArea.lastIndexOf(delim, startIndex = searchFrom - 1)
                if (idx == -1) break

                val absOpeningStart = lineStart + idx
                val absOpeningEnd = absOpeningStart + delim.length
                val isEscaped = isEscaped(text, absOpeningStart)
                val isPartOfDouble =
                    when (delim) {
                        "*" -> {
                            (absOpeningStart > 0 && text[absOpeningStart - 1] == '*') ||
                                (absOpeningEnd < text.length && text[absOpeningEnd] == '*')
                        }

                        "_" -> {
                            (absOpeningStart > 0 && text[absOpeningStart - 1] == '_') ||
                                (absOpeningEnd < text.length && text[absOpeningEnd] == '_')
                        }

                        else -> {
                            false
                        }
                    }

                if (!isEscaped && !isPartOfDouble) {
                    openingIndexInSearch = idx
                    break
                }
                searchFrom = idx
            }

            if (openingIndexInSearch != -1) {
                val openingStart = lineStart + openingIndexInSearch
                val openingEnd = openingStart + delim.length
                val enclosed = text.subSequence(openingEnd, closingStart).toString()

                if (enclosed.isEmpty()) continue
                if (enclosed.first().isWhitespace() || enclosed.last().isWhitespace()) continue
                if (enclosed.contains('\n')) continue

                if (delim == "_") {
                    val beforeOpening = if (openingStart > 0) text[openingStart - 1] else ' '
                    val afterClosing = if (closingEnd < text.length) text[closingEnd] else ' '
                    if (beforeOpening.isLetterOrDigit() || afterClosing.isLetterOrDigit()) continue
                }

                // --- 3-state model: push State B snapshot to UndoManager ---
                val stateBSnapshot =
                    EditorSnapshot(
                        text = buffer.toString(),
                        spans = state.richString.spans,
                        selection = buffer.selection,
                    )
                state.undoState.undoManager.pushSnapshot(stateBSnapshot, UndoMergePolicy.Separate)

                // --- Transform to State C ---
                val (type, targetKey: SpanAttributeKey<Unit>) =
                    when (delim) {
                        "**" -> AutoFormatType.Bold to BoldKey
                        "*" -> AutoFormatType.Italic to ItalicKey
                        "_" -> AutoFormatType.Italic to ItalicKey
                        "`" -> AutoFormatType.InlineCode to InlineCodeKey
                        "~" -> AutoFormatType.Strikethrough to StrikethroughKey
                        else -> continue
                    }

                val transformedRange = openingStart until (closingStart - delim.length)

                buffer.delete(closingStart, closingEnd)
                buffer.delete(openingStart, openingEnd)
                state.shiftSpansDirectly(
                    editStart = closingStart,
                    editEnd = closingEnd,
                    newLength = 0,
                    offsetDiff = -(closingEnd - closingStart),
                    deletedText = delim,
                )
                state.shiftSpansDirectly(
                    editStart = openingStart,
                    editEnd = openingEnd,
                    newLength = 0,
                    offsetDiff = -(openingEnd - openingStart),
                    deletedText = delim,
                )

                state.setSpanAttributeDirectly(targetKey, Unit, transformedRange, buffer.toString())

                val postCursor = transformedRange.last + 1
                buffer.selection = TextRange(postCursor)

                // F14: Prevent attribute leakage to subsequent typed characters
                state.clearTypingAttributes()
                state.removeTypingAttribute(targetKey)

                return AutoFormatEvent(
                    type = type,
                    preFormatSnapshot = stateBSnapshot,
                    postFormatCursor = postCursor,
                )
            }
        }
        return null
    }

    private fun isEscaped(text: CharSequence, index: Int): Boolean {
        var backslashCount = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') {
            backslashCount++
            i--
        }
        return backslashCount % 2 != 0
    }
}
