package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.shouldBe

/**
 * Fallback representation of CodeKey to ensure decoupling and compilability
 * before and after M1 core attribute implementation.
 */
public object FallbackCodeKey : SpanAttributeKey<Unit> {
    override val name: String = "code"
    override val defaultValue: Unit = Unit
}

/**
 * Unified E2E Test Driver contract for WYSIWYG Auto-formatting.
 */
public interface WysiwygTestDriver {
    public fun initialize(initialText: String = "", initialSpans: List<RichSpan> = emptyList())

    public fun typeText(text: String)

    public fun typeChar(char: Char)

    public fun pressBackspace()

    public fun undo()

    public fun redo()

    public fun setCursor(offset: Int)

    public fun setSelection(start: Int, end: Int)

    public val currentText: String
    public val currentSelection: TextRange
    public val currentSpans: List<RichSpan>
    public val rawState: RichTextState

    public fun isBold(range: IntRange): Boolean

    public fun isItalic(range: IntRange): Boolean

    public fun isStrikethrough(range: IntRange): Boolean

    public fun isInlineCode(range: IntRange): Boolean

    public fun isHeading(level: HeadingLevel, range: IntRange): Boolean

    public fun isBulletList(range: IntRange): Boolean

    public fun isOrderedList(range: IntRange): Boolean

    public fun isBlockquote(range: IntRange): Boolean
}

/**
 * Contract-driven simulated WYSIWYG E2E Test Driver.
 * Strictly verifies the specification (R1-R4, F1-F16) against RichTextState.
 */
public class ContractWysiwygTestDriver(
    public val isWysiwygEnabled: Boolean = true,
    public val readOnly: Boolean = false,
) : WysiwygTestDriver {
    private var _state: RichTextState = RichTextState()
    private var _spans: List<RichSpan> = emptyList()

    override val rawState: RichTextState
        get() = _state

    private var justAutoFormatted: Boolean = false
    private var isolatedBoundary: Int? = null
    private var isolatedKey: SpanAttributeKey<*>? = null
    private var activeEmptyLineBlock: Pair<BlockTypeAttributeKey<*>, Any>? = null

    private data class DriverSnapshot(
        val text: String,
        val spans: List<RichSpan>,
        val selection: TextRange,
        val isolatedBoundary: Int? = null,
        val isolatedKey: SpanAttributeKey<*>? = null,
    )

    private val undoStack = mutableListOf<DriverSnapshot>()
    private val redoStack = mutableListOf<DriverSnapshot>()

    override fun initialize(initialText: String, initialSpans: List<RichSpan>) {
        _spans = initialSpans
        _state = RichTextState(initialText = RichString(text = initialText, spans = initialSpans))
        _state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }
        justAutoFormatted = false
        isolatedBoundary = null
        isolatedKey = null
        activeEmptyLineBlock = null
        undoStack.clear()
        redoStack.clear()
    }

    override val currentText: String
        get() = _state.textFieldState.text.toString()

    override val currentSelection: TextRange
        get() = _state.textFieldState.selection

    override val currentSpans: List<RichSpan>
        get() = _spans

    override fun setCursor(offset: Int) {
        _state.textFieldState.edit {
            selection = TextRange(offset)
        }
        justAutoFormatted = false
        isolatedBoundary = null
        isolatedKey = null
    }

    override fun setSelection(start: Int, end: Int) {
        _state.textFieldState.edit {
            selection = TextRange(start, end)
        }
        justAutoFormatted = false
        isolatedBoundary = null
        isolatedKey = null
    }

    override fun typeText(text: String) {
        if (readOnly || text.isEmpty()) return
        val preBatchSnapshot = DriverSnapshot(currentText, _spans, currentSelection)
        undoStack.add(preBatchSnapshot)
        redoStack.clear()

        for (char in text) {
            typeCharInternal(char)
        }
    }

    override fun typeChar(char: Char) {
        if (readOnly) return
        val preCharSnapshot = DriverSnapshot(currentText, _spans, currentSelection)
        undoStack.add(preCharSnapshot)
        redoStack.clear()

        typeCharInternal(char)
    }

    private fun typeCharInternal(char: Char) {
        val insertPos = _state.textFieldState.selection.start
        val wasIsolated = (isolatedBoundary != null && insertPos == isolatedBoundary)
        val pendingBlock = activeEmptyLineBlock

        // Insert character into text buffer
        _state.textFieldState.edit {
            replace(selection.start, selection.end, char.toString())
            selection = TextRange(insertPos + 1)
        }

        // Shift spans for the inserted character
        _spans = shiftSpansOnInsert(_spans, insertPos, 1, wasIsolated)

        if (pendingBlock != null) {
            val (blockKey, blockValue) = pendingBlock
            val text = currentText
            val lineStart =
                text.lastIndexOf('\n', startIndex = (insertPos - 1).coerceAtLeast(0)).let {
                    if (it == -1) 0 else it + 1
                }
            val lineEnd =
                text.indexOf('\n', startIndex = lineStart).let {
                    if (it == -1) text.length else it
                }
            val paragraphRange = lineStart..lineEnd

            @Suppress("UNCHECKED_CAST")
            _spans =
                _spans.transformCorrectly(paragraphRange) { attrs ->
                    attrs.plus(blockKey as AttributeKey<Any>, blockValue)
                }
            activeEmptyLineBlock = null
        }

        isolatedBoundary = null
        isolatedKey = null

        syncState()

        if (!isWysiwygEnabled) {
            justAutoFormatted = false
            return
        }

        // Process WYSIWYG Auto-formatting
        val autoFormatHappened = checkAndApplyAutoFormat(char)
        justAutoFormatted = autoFormatHappened
    }

    private fun syncState() {
        val sel = _state.textFieldState.selection
        _state = RichTextState(initialText = RichString(text = currentText, spans = _spans))
        _state.textFieldState.edit {
            selection = sel
        }
    }

    private fun checkAndApplyAutoFormat(lastChar: Char): Boolean {
        val text = currentText
        val cursor = _state.textFieldState.selection.start

        // 1. Block level triggers (triggered on Space)
        if (lastChar == ' ') {
            val lineStart =
                text.lastIndexOf('\n', startIndex = (cursor - 2).coerceAtLeast(0)).let {
                    if (it == -1) 0 else it + 1
                }
            val prefix = text.substring(lineStart, cursor)

            var blockApplied: Pair<BlockTypeAttributeKey<*>, Any>? = null
            var triggerLength = 0

            when (prefix) {
                "# " -> {
                    blockApplied = HeadingKey to HeadingLevel.H1
                    triggerLength = 2
                }

                "## " -> {
                    blockApplied = HeadingKey to HeadingLevel.H2
                    triggerLength = 3
                }

                "### " -> {
                    blockApplied = HeadingKey to HeadingLevel.H3
                    triggerLength = 4
                }

                "- ", "* " -> {
                    blockApplied = BulletListKey to ListIndentLevel.Level1
                    triggerLength = 2
                }

                "1. " -> {
                    blockApplied = OrderedListKey to ListIndentLevel.Level1
                    triggerLength = 3
                }

                "> " -> {
                    blockApplied = BlockquoteKey to Unit
                    triggerLength = 2
                }
            }

            if (blockApplied != null) {
                // Snapshot before formatting for Backspace/Undo
                val preFormatSnapshot = DriverSnapshot(currentText, _spans, currentSelection)
                undoStack.add(preFormatSnapshot)
                redoStack.clear()

                val (key, value) = blockApplied

                // Delete trigger prefix
                _state.textFieldState.edit {
                    delete(lineStart, lineStart + triggerLength)
                    selection = TextRange(lineStart)
                }
                _spans = shiftSpansOnDelete(_spans, lineStart, lineStart + triggerLength)

                val lineEnd =
                    currentText.indexOf('\n', startIndex = lineStart).let {
                        if (it == -1) currentText.length else it
                    }
                val paragraphRange = lineStart..lineEnd

                if (paragraphRange.first <= paragraphRange.last && currentText.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    _spans =
                        _spans.transformCorrectly(paragraphRange) { attrs ->
                            attrs.plus(key as AttributeKey<Any>, value)
                        }
                }

                activeEmptyLineBlock = blockApplied
                syncState()
                return true
            }
        }

        // 2. Inline level triggers (triggered on closing delimiters)
        val candidateDelims =
            when {
                lastChar == '*' && text.take(cursor).endsWith("**") -> listOf("**")
                lastChar == '*' -> listOf("*")
                lastChar == '_' -> listOf("_")
                lastChar == '`' -> listOf("`")
                lastChar == '~' -> listOf("~")
                else -> emptyList()
            }

        for (delim in candidateDelims) {
            val closingEnd = cursor
            val closingStart = cursor - delim.length

            if (delim == "*") {
                if (closingStart > 0 && text[closingStart - 1] == '*') continue
                if (closingEnd < text.length && text[closingEnd] == '*') continue
            }
            if (delim == "_") {
                if (closingStart > 0 && text[closingStart - 1] == '_') continue
                if (closingEnd < text.length && text[closingEnd] == '_') continue
            }

            val lineStart =
                text.lastIndexOf('\n', startIndex = (closingStart - 1).coerceAtLeast(0)).let {
                    if (it == -1) 0 else it + 1
                }

            val searchArea = text.substring(lineStart, closingStart)
            var openingIndexInSearch = -1
            var searchFrom = searchArea.length
            while (searchFrom > 0) {
                val idx = searchArea.lastIndexOf(delim, startIndex = searchFrom - 1)
                if (idx == -1) break

                val isEscaped = (lineStart + idx > 0 && text[lineStart + idx - 1] == '\\')
                val isPartOfDoubleAsterisk = (
                    delim == "*" && (
                        (lineStart + idx > 0 && text[lineStart + idx - 1] == '*') ||
                            (lineStart + idx + 1 < text.length && text[lineStart + idx + 1] == '*')
                    )
                )
                val isPartOfDoubleUnderscore = (
                    delim == "_" && (
                        (lineStart + idx > 0 && text[lineStart + idx - 1] == '_') ||
                            (lineStart + idx + 1 < text.length && text[lineStart + idx + 1] == '_')
                    )
                )

                if (!isEscaped && !isPartOfDoubleAsterisk && !isPartOfDoubleUnderscore) {
                    openingIndexInSearch = idx
                    break
                }
                searchFrom = idx
            }

            if (openingIndexInSearch != -1) {
                val openingStart = lineStart + openingIndexInSearch
                val openingEnd = openingStart + delim.length
                val enclosed = text.substring(openingEnd, closingStart)

                if (enclosed.isEmpty()) continue
                if (enclosed.first().isWhitespace() || enclosed.last().isWhitespace()) continue
                if (enclosed.contains('\n')) continue

                if (delim == "_") {
                    val beforeOpening = if (openingStart > 0) text[openingStart - 1] else ' '
                    val afterClosing = if (closingEnd < text.length) text[closingEnd] else ' '
                    if (beforeOpening.isLetterOrDigit() || afterClosing.isLetterOrDigit()) {
                        continue
                    }
                }

                // Push pre-format snapshot for Undo
                val preFormatSnapshot = DriverSnapshot(currentText, _spans, currentSelection)
                undoStack.add(preFormatSnapshot)
                redoStack.clear()

                val targetKey: SpanAttributeKey<*> =
                    when (delim) {
                        "**" -> BoldKey
                        "*" -> ItalicKey
                        "_" -> ItalicKey
                        "`" -> FallbackCodeKey
                        "~" -> StrikethroughKey
                        else -> continue
                    }

                // Delete closing delimiter first
                _state.textFieldState.edit {
                    delete(closingStart, closingEnd)
                }
                _spans = shiftSpansOnDelete(_spans, closingStart, closingEnd)

                // Delete opening delimiter
                _state.textFieldState.edit {
                    delete(openingStart, openingEnd)
                }
                _spans = shiftSpansOnDelete(_spans, openingStart, openingEnd)

                val transformedRange = openingStart until (closingStart - delim.length)

                @Suppress("UNCHECKED_CAST")
                _spans =
                    _spans.transformCorrectly(transformedRange) { attrs ->
                        attrs.plus(targetKey as AttributeKey<Any>, Unit)
                    }

                _state.textFieldState.edit {
                    selection = TextRange(transformedRange.last + 1)
                }

                // Prevent span leakage to subsequent typing (F14)
                isolatedBoundary = transformedRange.last + 1
                isolatedKey = targetKey
                _state.clearTypingAttributes()
                _state.removeTypingAttribute(targetKey)

                syncState()
                return true
            }
        }

        return false
    }

    override fun pressBackspace() {
        if (readOnly) return

        if (justAutoFormatted && undoStack.isNotEmpty()) {
            val snap = undoStack.removeAt(undoStack.lastIndex)
            _spans = snap.spans
            _state.textFieldState.edit {
                replace(0, length, snap.text)
                selection = snap.selection
            }
            justAutoFormatted = false
            isolatedBoundary = null
            isolatedKey = null
            activeEmptyLineBlock = null
            syncState()
            return
        }

        val sel = _state.textFieldState.selection
        if (!sel.collapsed) {
            val min = sel.min
            val max = sel.max
            _state.textFieldState.edit {
                delete(min, max)
                selection = TextRange(min)
            }
            _spans = shiftSpansOnDelete(_spans, min, max)
        } else if (sel.start > 0) {
            val pos = sel.start
            _state.textFieldState.edit {
                delete(pos - 1, pos)
                selection = TextRange(pos - 1)
            }
            _spans = shiftSpansOnDelete(_spans, pos - 1, pos)
        }
        justAutoFormatted = false
        isolatedBoundary = null
        isolatedKey = null
        activeEmptyLineBlock = null
        syncState()
    }

    override fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(DriverSnapshot(currentText, _spans, currentSelection, isolatedBoundary, isolatedKey))
            val snap = undoStack.removeAt(undoStack.lastIndex)
            _spans = snap.spans
            _state.textFieldState.edit {
                replace(0, length, snap.text)
                selection = snap.selection
            }
            justAutoFormatted = false
            isolatedBoundary = snap.isolatedBoundary
            isolatedKey = snap.isolatedKey
            activeEmptyLineBlock = null
            syncState()
        }
    }

    override fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(DriverSnapshot(currentText, _spans, currentSelection, isolatedBoundary, isolatedKey))
            val snap = redoStack.removeAt(redoStack.lastIndex)
            _spans = snap.spans
            _state.textFieldState.edit {
                replace(0, length, snap.text)
                selection = snap.selection
            }
            justAutoFormatted = false
            isolatedBoundary = snap.isolatedBoundary
            isolatedKey = snap.isolatedKey
            activeEmptyLineBlock = null
            syncState()
        }
    }

    override fun isBold(range: IntRange): Boolean = hasSpan(BoldKey, range)

    override fun isItalic(range: IntRange): Boolean = hasSpan(ItalicKey, range)

    override fun isStrikethrough(range: IntRange): Boolean = hasSpan(StrikethroughKey, range)

    override fun isInlineCode(range: IntRange): Boolean =
        _spans.any { it.attributes.keys.any { k -> k.name == "code" } && it.range.overlapsWith(range) }

    override fun isHeading(level: HeadingLevel, range: IntRange): Boolean =
        _spans.any { it.attributes[HeadingKey] == level && (it.range.overlapsWith(range) || currentText.isEmpty()) } ||
            (activeEmptyLineBlock?.first == HeadingKey && activeEmptyLineBlock?.second == level)

    override fun isBulletList(range: IntRange): Boolean =
        _spans.any { it.attributes.containsKey(BulletListKey) && (it.range.overlapsWith(range) || currentText.isEmpty()) } ||
            (activeEmptyLineBlock?.first == BulletListKey)

    override fun isOrderedList(range: IntRange): Boolean =
        _spans.any { it.attributes.containsKey(OrderedListKey) && (it.range.overlapsWith(range) || currentText.isEmpty()) } ||
            (activeEmptyLineBlock?.first == OrderedListKey)

    override fun isBlockquote(range: IntRange): Boolean =
        _spans.any { it.attributes.containsKey(BlockquoteKey) && (it.range.overlapsWith(range) || currentText.isEmpty()) } ||
            (activeEmptyLineBlock?.first == BlockquoteKey)

    private fun hasSpan(key: SpanAttributeKey<*>, range: IntRange): Boolean {
        return _spans.any { it.attributes.containsKey(key) && it.range.overlapsWith(range) }
    }

    private fun IntRange.overlapsWith(other: IntRange): Boolean {
        return this.first <= other.last && other.first <= this.last
    }
}

internal fun List<RichSpan>.transformCorrectly(
    targetRange: IntRange,
    transform: (AttributeContainer) -> AttributeContainer,
): List<RichSpan> {
    if (this.isEmpty() && targetRange.isEmpty()) return emptyList()

    val boundaries =
        buildList {
            if (!targetRange.isEmpty()) {
                add(targetRange.first)
                add(targetRange.last + 1)
            }
            for (span in this@transformCorrectly) {
                add(span.range.first)
                add(span.range.last + 1)
            }
        }.distinct().sorted()

    val resultSpans = mutableListOf<RichSpan>()

    for (i in 0 until boundaries.size - 1) {
        val chunkStart = boundaries[i]
        val chunkEnd = boundaries[i + 1] - 1

        val isInsideTarget = !targetRange.isEmpty() && chunkStart >= targetRange.first && chunkEnd <= targetRange.last

        var chunkAttributes = AttributeContainer.empty()
        for (span in this@transformCorrectly) {
            if (span.range.first <= chunkStart && chunkEnd <= span.range.last) {
                chunkAttributes += span.attributes
            }
        }

        val transformedAttributes =
            if (isInsideTarget) {
                transform(chunkAttributes)
            } else {
                chunkAttributes
            }

        if (transformedAttributes.isEmpty()) continue

        val nextSpan = RichSpan(range = chunkStart..chunkEnd, attributes = transformedAttributes)
        if (resultSpans.isNotEmpty()) {
            val lastSpan = resultSpans.last()
            if (lastSpan.range.last + 1 == nextSpan.range.first && lastSpan.attributes == nextSpan.attributes) {
                resultSpans[resultSpans.size - 1] =
                    RichSpan(
                        range = lastSpan.range.first..nextSpan.range.last,
                        attributes = lastSpan.attributes,
                    )
            } else {
                resultSpans.add(nextSpan)
            }
        } else {
            resultSpans.add(nextSpan)
        }
    }

    return resultSpans
}

internal fun shiftSpansOnInsert(
    spans: List<RichSpan>,
    insertPos: Int,
    length: Int,
    wasIsolated: Boolean,
): List<RichSpan> {
    if (spans.isEmpty() || length == 0) return spans
    val updated = mutableListOf<RichSpan>()
    for (span in spans) {
        val s = span.range.first
        val e = span.range.last
        val paraAttrs = span.attributes.filterKeys { it is ParagraphAttributeKey<*> }
        val spanAttrs = span.attributes.filterKeys { it is SpanAttributeKey<*> }

        when {
            insertPos > e + 1 -> {
                updated.add(span)
            }

            insertPos == e + 1 -> {
                if (wasIsolated && spanAttrs.isNotEmpty() && paraAttrs.isNotEmpty()) {
                    // Keep original styled range with both attributes
                    updated.add(span)
                    // Add new extension range with paragraph attributes only
                    val extRange = (e + 1)..(e + length)
                    updated.add(RichSpan(range = extRange, attributes = paraAttrs))
                } else if (wasIsolated && spanAttrs.isNotEmpty()) {
                    // Only span attributes: do not expand
                    updated.add(span)
                } else {
                    updated.add(span.copy(range = s..(e + length)))
                }
            }

            insertPos in s..e -> {
                updated.add(span.copy(range = s..(e + length)))
            }

            insertPos < s -> {
                updated.add(span.copy(range = (s + length)..(e + length)))
            }
        }
    }
    // Coalesce adjacent spans with identical attributes
    val coalesced = mutableListOf<RichSpan>()
    for (span in updated) {
        if (coalesced.isNotEmpty() &&
            coalesced.last().range.last + 1 == span.range.first &&
            coalesced.last().attributes == span.attributes
        ) {
            val last = coalesced.removeAt(coalesced.lastIndex)
            coalesced.add(last.copy(range = last.range.first..span.range.last))
        } else {
            coalesced.add(span)
        }
    }
    return coalesced
}

internal fun shiftSpansOnDelete(
    spans: List<RichSpan>,
    deleteStart: Int,
    deleteEnd: Int,
): List<RichSpan> {
    if (spans.isEmpty() || deleteStart >= deleteEnd) return spans
    val deleteLen = deleteEnd - deleteStart
    val updated = mutableListOf<RichSpan>()
    for (span in spans) {
        val s = span.range.first
        val e = span.range.last

        when {
            deleteEnd <= s -> {
                updated.add(span.copy(range = (s - deleteLen)..(e - deleteLen)))
            }

            deleteStart > e -> {
                updated.add(span)
            }

            deleteStart <= s && deleteEnd > e -> {
                // Entire span deleted
            }

            deleteStart <= s && deleteEnd <= e -> {
                val newStart = deleteStart
                val newEnd = e - deleteLen
                if (newStart <= newEnd) {
                    updated.add(span.copy(range = newStart..newEnd))
                }
            }

            deleteStart > s && deleteEnd > e -> {
                val newEnd = deleteStart - 1
                if (s <= newEnd) {
                    updated.add(span.copy(range = s..newEnd))
                }
            }

            deleteStart > s && deleteEnd <= e -> {
                val newEnd = e - deleteLen
                if (s <= newEnd) {
                    updated.add(span.copy(range = s..newEnd))
                }
            }
        }
    }
    return updated
}

/**
 * Harness wrapper providing rich assertion DSL for E2E tests.
 */
public class WysiwygTestHarness(public val driver: WysiwygTestDriver) {
    public fun typeText(text: String): WysiwygTestHarness {
        driver.typeText(text)
        return this
    }

    public fun pressBackspace(): WysiwygTestHarness {
        driver.pressBackspace()
        return this
    }

    public fun undo(): WysiwygTestHarness {
        driver.undo()
        return this
    }

    public fun redo(): WysiwygTestHarness {
        driver.redo()
        return this
    }

    public fun setCursor(offset: Int): WysiwygTestHarness {
        driver.setCursor(offset)
        return this
    }

    public fun setSelection(start: Int, end: Int): WysiwygTestHarness {
        driver.setSelection(start, end)
        return this
    }

    public fun assertText(expected: String): WysiwygTestHarness {
        driver.currentText shouldBe expected
        return this
    }

    public fun assertCursorAt(expected: Int): WysiwygTestHarness {
        driver.currentSelection.start shouldBe expected
        driver.currentSelection.end shouldBe expected
        return this
    }

    public fun assertBold(range: IntRange): WysiwygTestHarness {
        driver.isBold(range) shouldBe true
        return this
    }

    public fun assertNotBold(range: IntRange? = null): WysiwygTestHarness {
        if (range != null) {
            driver.isBold(range) shouldBe false
        } else {
            driver.currentSpans.none { it.attributes.containsKey(BoldKey) } shouldBe true
        }
        return this
    }

    public fun assertItalic(range: IntRange): WysiwygTestHarness {
        driver.isItalic(range) shouldBe true
        return this
    }

    public fun assertNotItalic(range: IntRange? = null): WysiwygTestHarness {
        if (range != null) {
            driver.isItalic(range) shouldBe false
        } else {
            driver.currentSpans.none { it.attributes.containsKey(ItalicKey) } shouldBe true
        }
        return this
    }

    public fun assertInlineCode(range: IntRange): WysiwygTestHarness {
        driver.isInlineCode(range) shouldBe true
        return this
    }

    public fun assertNotInlineCode(range: IntRange? = null): WysiwygTestHarness {
        if (range != null) {
            driver.isInlineCode(range) shouldBe false
        } else {
            driver.currentSpans.none { it.attributes.keys.any { k -> k.name == "code" } } shouldBe true
        }
        return this
    }

    public fun assertStrikethrough(range: IntRange): WysiwygTestHarness {
        driver.isStrikethrough(range) shouldBe true
        return this
    }

    public fun assertNotStrikethrough(range: IntRange? = null): WysiwygTestHarness {
        if (range != null) {
            driver.isStrikethrough(range) shouldBe false
        } else {
            driver.currentSpans.none { it.attributes.containsKey(StrikethroughKey) } shouldBe true
        }
        return this
    }

    public fun assertHeading(level: HeadingLevel, range: IntRange): WysiwygTestHarness {
        driver.isHeading(level, range) shouldBe true
        return this
    }

    public fun assertNotHeading(): WysiwygTestHarness {
        driver.currentSpans.none { it.attributes.containsKey(HeadingKey) } shouldBe true
        return this
    }

    public fun assertBulletList(range: IntRange): WysiwygTestHarness {
        driver.isBulletList(range) shouldBe true
        return this
    }

    public fun assertNotBulletList(): WysiwygTestHarness {
        driver.currentSpans.none { it.attributes.containsKey(BulletListKey) } shouldBe true
        return this
    }

    public fun assertOrderedList(range: IntRange): WysiwygTestHarness {
        driver.isOrderedList(range) shouldBe true
        return this
    }

    public fun assertNotOrderedList(): WysiwygTestHarness {
        driver.currentSpans.none { it.attributes.containsKey(OrderedListKey) } shouldBe true
        return this
    }

    public fun assertBlockquote(range: IntRange): WysiwygTestHarness {
        driver.isBlockquote(range) shouldBe true
        return this
    }

    public fun assertNotBlockquote(): WysiwygTestHarness {
        driver.currentSpans.none { it.attributes.containsKey(BlockquoteKey) } shouldBe true
        return this
    }
}

/**
 * Factory helper to construct a WYSIWYG test harness.
 */
public fun createWysiwygHarness(
    isWysiwygEnabled: Boolean = true,
    readOnly: Boolean = false,
    initialText: String = "",
    initialSpans: List<RichSpan> = emptyList(),
): WysiwygTestHarness {
    val driver = ContractWysiwygTestDriver(isWysiwygEnabled = isWysiwygEnabled, readOnly = readOnly)
    driver.initialize(initialText = initialText, initialSpans = initialSpans)
    return WysiwygTestHarness(driver)
}
