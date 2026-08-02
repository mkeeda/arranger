package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichTextStateFormatExtTest {
    @Test
    fun `toggleFormat SpanAttributeKey Unit applies attribute to selection when text is selected`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        state.toggleFormat(BoldKey)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().range shouldBe initialText.rangeOf("World")
        state.richString.spans.first().attributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `toggleFormat SpanAttributeKey Unit removes attribute from selection when already active`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        state.toggleFormat(BoldKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `toggleFormat SpanAttributeKey Unit sets typingAttribute when selection is collapsed and not active`() {
        val state = RichTextState(initialText = RichString("Test"))

        state.textFieldState.edit { selection = TextRange(4) }

        state.toggleFormat(BoldKey)

        state.currentAttributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `toggleFormat SpanAttributeKey Unit removes typingAttribute when selection is collapsed and active`() {
        val state = RichTextState(initialText = RichString("Test"))

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)

        state.toggleFormat(BoldKey)

        state.currentAttributes.containsKey(BoldKey) shouldBe false
    }

    @Test
    fun `toggleFormat ParagraphAttributeKey Unit applies paragraph attribute`() {
        val initialText = "Blockquote"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.textFieldState.edit { selection = TextRange(2) }

        state.toggleFormat(BlockquoteKey)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().attributes.containsKey(BlockquoteKey) shouldBe true
    }

    @Test
    fun `toggleFormat ParagraphAttributeKey Unit removes paragraph attribute when already active`() {
        val initialText = "Blockquote"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(BlockquoteKey, Unit, range = initialText.indices)
                    },
            )

        state.textFieldState.edit { selection = TextRange(2) }

        state.toggleFormat(BlockquoteKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `applyFormat SpanAttributeKey sets given value on selection`() {
        val initialText = "Hello World"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        val color = RgbaColor(0xFFFF0000)
        state.applyFormat(TextColorKey, color)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().attributes[TextColorKey] shouldBe color
    }

    @Test
    fun `applyFormat SpanAttributeKey sets typingAttribute when selection is collapsed`() {
        val state = RichTextState(initialText = RichString("Test"))

        state.textFieldState.edit { selection = TextRange(4) }

        val color = RgbaColor(0xFFFF0000)
        state.applyFormat(TextColorKey, color)

        state.currentAttributes[TextColorKey] shouldBe color
    }

    @Test
    fun `applyFormat ParagraphAttributeKey sets given value on paragraph`() {
        val initialText = "Heading"
        val state = RichTextState(initialText = RichString(text = initialText))

        state.textFieldState.edit { selection = TextRange(2) }

        state.applyFormat(HeadingKey, HeadingLevel.H1)

        state.richString.spans.size shouldBe 1
        state.richString.spans.first().attributes[HeadingKey] shouldBe HeadingLevel.H1
    }

    @Test
    fun `removeFormat removes the key from selection`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        state.removeFormat(BoldKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `removeFormat removes typingAttribute when selection is collapsed`() {
        val state = RichTextState(initialText = RichString("Test"))

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)

        state.removeFormat(BoldKey)

        state.currentAttributes.containsKey(BoldKey) shouldBe false
    }

    @Test
    fun `removeFormat removes paragraph attribute when selection is collapsed`() {
        val initialText = "Paragraph 1\nParagraph 2"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setParagraphAttribute(HeadingKey, HeadingLevel.H1, range = initialText.rangeOf("Paragraph 1"))
                    },
            )

        // Cursor is collapsed within "Paragraph 1"
        state.textFieldState.edit { selection = TextRange(5) }

        state.removeFormat(HeadingKey)

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `clearFormats clears all typing attributes`() {
        val state = RichTextState(initialText = RichString("Test"))

        state.textFieldState.edit { selection = TextRange(4) }
        state.setTypingAttribute(BoldKey, Unit)
        state.setTypingAttribute(UnderlineKey, Unit)

        state.clearFormats()

        state.currentAttributes.isEmpty() shouldBe true
    }

    @Test
    fun `clearFormats clears attributes on selection`() {
        val initialText = "Hello World"
        val state =
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(BoldKey, Unit, range = initialText.rangeOf("World"))
                        setSpanAttribute(UnderlineKey, Unit, range = initialText.rangeOf("World"))
                    },
            )

        state.textFieldState.edit {
            selection = TextRange(initialText.indexOf("World"), initialText.length)
        }

        state.clearFormats()

        state.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `toAnnotatedString creates LinkAnnotation for LinkKey`() {
        val text = "Click here for docs"
        val linkUrl = "https://example.com/docs"
        val state =
            RichTextState(
                initialText =
                    RichString(text = text).edit {
                        setSpanAttribute(LinkKey, linkUrl, range = text.rangeOf("here"))
                    },
            )

        val annotated = state.toAnnotatedString()
        val linkAnnotations = annotated.getLinkAnnotations(text.indexOf("here"), text.indexOf("here") + 4)
        linkAnnotations.size shouldBe 1
        (linkAnnotations.first().item as LinkAnnotation.Url).url shouldBe linkUrl
    }

    @Test
    fun `detectAndApplyLinks applies LinkKey to discovered URLs in RichTextState`() {
        val state = RichTextState(initialText = RichString("Check out https://example.com and www.test.org for info."))

        state.detectAndApplyLinks()

        val spans = state.richString.spans
        spans.size shouldBe 2

        val firstSpan = spans.first { it.attributes[LinkKey] == "https://example.com" }
        firstSpan.range shouldBe state.richString.text.rangeOf("https://example.com")

        val secondSpan = spans.first { it.attributes[LinkKey] == "https://www.test.org" }
        secondSpan.range shouldBe state.richString.text.rangeOf("www.test.org")
    }

    @Test
    fun `detectAndApplyLinks does not overwrite manually set links`() {
        val text = "Check out https://example.com"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        setSpanAttribute(LinkKey, "https://custom-tracking.com", range = text.rangeOf("https://example.com"))
                    },
            )

        state.detectAndApplyLinks()

        val spans = state.richString.spans
        spans.size shouldBe 1

        val linkSpan = spans.first()
        linkSpan.attributes[LinkKey] shouldBe "https://custom-tracking.com"
    }
}
