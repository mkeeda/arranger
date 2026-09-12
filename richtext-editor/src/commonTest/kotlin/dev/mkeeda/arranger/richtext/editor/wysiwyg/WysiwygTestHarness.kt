package dev.mkeeda.arranger.richtext.editor.wysiwyg

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.RichTextInputTransformation
import dev.mkeeda.arranger.richtext.editor.RichTextState
import io.kotest.matchers.shouldBe

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
 * Production-driven WYSIWYG E2E Test Driver.
 * Directly operates on the real production engine:
 * - [RichTextState]
 * - [WysiwygState]
 * - [WysiwygInputTransformation]
 * - [handleWysiwygKey]
 * - [CodeKey]
 */
public class RealWysiwygTestDriver(
    public val isWysiwygEnabled: Boolean = true,
    public val readOnly: Boolean = false,
) : WysiwygTestDriver {
    private var _state: RichTextState = RichTextState()
    private var _wysiwygState: WysiwygState = WysiwygState()
    private var _transformation: InputTransformation =
        if (isWysiwygEnabled) WysiwygInputTransformation(_state, _wysiwygState) else RichTextInputTransformation(_state)

    override val rawState: RichTextState
        get() = _state

    override fun initialize(initialText: String, initialSpans: List<RichSpan>) {
        _state = RichTextState(initialText = RichString(text = initialText, spans = initialSpans))
        _wysiwygState = WysiwygState()
        _transformation =
            if (isWysiwygEnabled) {
                WysiwygInputTransformation(_state, _wysiwygState)
            } else {
                RichTextInputTransformation(_state)
            }
        _state.textFieldState.edit {
            selection = TextRange(initialText.length)
        }
    }

    override val currentText: String
        get() = _state.richString.text

    override val currentSelection: TextRange
        get() = _state.selection

    override val currentSpans: List<RichSpan>
        get() = _state.richString.spans

    override fun setCursor(offset: Int) {
        _state.textFieldState.edit {
            selection = TextRange(offset)
        }
    }

    override fun setSelection(start: Int, end: Int) {
        _state.textFieldState.edit {
            selection = TextRange(start, end)
        }
    }

    override fun typeText(text: String) {
        if (readOnly || text.isEmpty()) return
        for (char in text) {
            typeChar(char)
        }
    }

    override fun typeChar(char: Char) {
        if (readOnly) return
        _state.textFieldState.edit {
            val min = selection.min
            val max = selection.max
            replace(min, max, char.toString())
            selection = TextRange(min + 1)
            with(_transformation) {
                transformInput()
            }
        }
    }

    override fun pressBackspace() {
        if (readOnly) return
        val handled =
            handleWysiwygKey(
                isKeyDown = true,
                key = Key.Backspace,
                state = _state,
                wysiwygState = _wysiwygState,
            )
        if (!handled) {
            _state.textFieldState.edit {
                val sel = selection
                if (!sel.collapsed) {
                    val min = sel.min
                    val max = sel.max
                    delete(min, max)
                    selection = TextRange(min)
                } else if (sel.start > 0) {
                    val pos = sel.start
                    delete(pos - 1, pos)
                    selection = TextRange(pos - 1)
                }
                with(_transformation) {
                    transformInput()
                }
            }
            _wysiwygState.clearLastAutoFormat()
        }
    }

    override fun undo() {
        _wysiwygState.clearLastAutoFormat()
        _state.undoState.undo()
    }

    override fun redo() {
        _wysiwygState.clearLastAutoFormat()
        _state.undoState.redo()
    }

    override fun isBold(range: IntRange): Boolean =
        _state.richString.spans.any { it.attributes.containsKey(BoldKey) && it.range.overlapsWith(range) }

    override fun isItalic(range: IntRange): Boolean =
        _state.richString.spans.any { it.attributes.containsKey(ItalicKey) && it.range.overlapsWith(range) }

    override fun isStrikethrough(range: IntRange): Boolean =
        _state.richString.spans.any { it.attributes.containsKey(StrikethroughKey) && it.range.overlapsWith(range) }

    override fun isInlineCode(range: IntRange): Boolean =
        _state.richString.spans.any {
            (it.attributes.containsKey(CodeKey) || it.attributes.keys.any { k -> k.name == "code" }) &&
                it.range.overlapsWith(range)
        }

    override fun isHeading(level: HeadingLevel, range: IntRange): Boolean =
        _state.richString.spans.any {
            it.attributes[HeadingKey] == level && (it.range.overlapsWith(range) || currentText.isEmpty())
        } || (_state.typingAttributes?.get(HeadingKey) == level) ||
            (_state.currentAttributes[HeadingKey] == level)

    override fun isBulletList(range: IntRange): Boolean =
        _state.richString.spans.any {
            it.attributes.containsKey(BulletListKey) && (it.range.overlapsWith(range) || currentText.isEmpty())
        } || (_state.typingAttributes?.containsKey(BulletListKey) == true) ||
            _state.currentAttributes.containsKey(BulletListKey)

    override fun isOrderedList(range: IntRange): Boolean =
        _state.richString.spans.any {
            it.attributes.containsKey(OrderedListKey) && (it.range.overlapsWith(range) || currentText.isEmpty())
        } || (_state.typingAttributes?.containsKey(OrderedListKey) == true) ||
            _state.currentAttributes.containsKey(OrderedListKey)

    override fun isBlockquote(range: IntRange): Boolean =
        _state.richString.spans.any {
            it.attributes.containsKey(BlockquoteKey) && (it.range.overlapsWith(range) || currentText.isEmpty())
        } || (_state.typingAttributes?.containsKey(BlockquoteKey) == true) ||
            _state.currentAttributes.containsKey(BlockquoteKey)

    private fun IntRange.overlapsWith(other: IntRange): Boolean {
        return this.first <= other.last && other.first <= this.last
    }
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
            driver.currentSpans.none {
                it.attributes.containsKey(CodeKey) || it.attributes.keys.any { k -> k.name == "code" }
            } shouldBe true
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
    val driver = RealWysiwygTestDriver(isWysiwygEnabled = isWysiwygEnabled, readOnly = readOnly)
    driver.initialize(initialText = initialText, initialSpans = initialSpans)
    return WysiwygTestHarness(driver)
}
