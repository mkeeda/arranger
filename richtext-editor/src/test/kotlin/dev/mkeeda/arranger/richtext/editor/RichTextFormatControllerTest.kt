package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichTextFormatControllerTest {
    @Test
    fun `isActive returns true when cursor is inside a span with the given key`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )
        val controller = RichTextFormatController(state)

        // Cursor inside "World"
        state.textFieldState.edit { selection = TextRange(initialText.indexOf("World") + 2) }

        controller.isActive(BoldKey) shouldBe true
        controller.isActive(UnderlineKey) shouldBe false
    }

    @Test
    fun `isActive returns false when cursor is outside any span with the given key`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello"))
                    },
            )
        val controller = RichTextFormatController(state)

        // Cursor inside "World"
        state.textFieldState.edit { selection = TextRange(initialText.indexOf("World") + 2) }

        controller.isActive(BoldKey) shouldBe false
    }

    @Test
    fun `isActive reflects typingAttributes when selection is collapsed`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)

        controller.isActive(BoldKey) shouldBe true
    }

    @Test
    fun `isActive returns true when entire selection range has the key applied`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("Hello World"))
                    },
            )
        val controller = RichTextFormatController(state)

        // Select "llo Wor"
        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("llo"), initialText.indexOf("rld"))
        }

        controller.isActive(BoldKey) shouldBe true
    }

    @Test
    fun `isActive returns false when only part of the selection has the key`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )
        val controller = RichTextFormatController(state)

        // Select "lo Wor" (partially bold)
        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("lo"), initialText.indexOf("rld"))
        }

        controller.isActive(BoldKey) shouldBe false
    }

    @Test
    fun `isActive returns true regardless of value`() {
        val initialText = "Heading"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(HeadingKey, HeadingLevel.H2, range = initialText.indices)
                    },
            )
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(2) }

        // It should be active because HeadingKey exists, even if we don't pass H2
        controller.isActive(HeadingKey) shouldBe true
    }

    @Test
    fun `toggle SpanAttributeKey Unit applies attribute to selection when text is selected`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        controller.toggle(BoldKey)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().range shouldBe initialText.rangeOf("World")
        state.richString.spans.first().attributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `toggle SpanAttributeKey Unit removes attribute from selection when already active`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )
        val controller = RichTextFormatController(state)

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        controller.toggle(BoldKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `toggle SpanAttributeKey Unit sets typingAttribute when selection is collapsed and not active`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(4) }

        controller.toggle(BoldKey)

        state.currentAttributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `toggle SpanAttributeKey Unit removes typingAttribute when selection is collapsed and active`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)

        controller.toggle(BoldKey)

        state.currentAttributes.containsKey(BoldKey) shouldBe false
    }

    @Test
    fun `toggle ParagraphAttributeKey Unit applies paragraph attribute`() {
        val initialText = "Blockquote"
        val state = RichTextState(initialText = RichString(text = initialText))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(2) }

        controller.toggle(BlockquoteKey)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().attributes.containsKey(BlockquoteKey) shouldBe true
    }

    @Test
    fun `toggle ParagraphAttributeKey Unit removes paragraph attribute when already active`() {
        val initialText = "Blockquote"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(BlockquoteKey, Unit, range = initialText.indices)
                    },
            )
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(2) }

        controller.toggle(BlockquoteKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `apply SpanAttributeKey sets given value on selection`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        val color = RgbaColor(0xFFFF0000)
        controller.apply(TextColorKey, color)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().attributes[TextColorKey] shouldBe color
    }

    @Test
    fun `apply SpanAttributeKey sets typingAttribute when selection is collapsed`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(4) }

        val color = RgbaColor(0xFFFF0000)
        controller.apply(TextColorKey, color)

        state.currentAttributes[TextColorKey] shouldBe color
    }

    @Test
    fun `apply ParagraphAttributeKey sets given value on paragraph`() {
        val initialText = "Heading"
        val state = RichTextState(initialText = RichString(text = initialText))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(2) }

        controller.apply(HeadingKey, HeadingLevel.H1)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().attributes[HeadingKey] shouldBe HeadingLevel.H1
    }

    @Test
    fun `getCurrentValue returns current value of the attribute at cursor`() {
        val initialText = "Heading"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(HeadingKey, HeadingLevel.H2, range = initialText.indices)
                    },
            )
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(2) }

        controller.getCurrentValue(HeadingKey) shouldBe HeadingLevel.H2
    }

    @Test
    fun `getCurrentValue returns null when key is not set`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        controller.getCurrentValue(HeadingKey) shouldBe null
    }

    @Test
    fun `remove removes the key from selection`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )
        val controller = RichTextFormatController(state)

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        controller.remove(BoldKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `remove removes typingAttribute when selection is collapsed`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)

        controller.remove(BoldKey)

        state.currentAttributes.containsKey(BoldKey) shouldBe false
    }

    @Test
    fun `clearAll clears all typing attributes`() {
        val state = RichTextState(initialText = RichString("Test"))
        val controller = RichTextFormatController(state)

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)
        state.setTypingAttribute(UnderlineKey, Unit)

        controller.clearAll()

        state.currentAttributes.isEmpty() shouldBe true
    }

    @Test
    fun `clearAll clears attributes on selection`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                        setSpanAttribute(UnderlineKey, Unit, range = initialText.rangeOf("World"))
                    },
            )
        val controller = RichTextFormatController(state)

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        controller.clearAll()

        state.richString.spans.isEmpty() shouldBe true
    }
}
