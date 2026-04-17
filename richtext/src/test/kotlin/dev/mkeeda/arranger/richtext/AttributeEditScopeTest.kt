package dev.mkeeda.arranger.richtext

import io.kotest.matchers.shouldBe
import org.junit.Test

class AttributeEditScopeTest {
    @Test
    fun `editAttributes can apply multiple attributes simultaneously to the specified range`() {
        val str =
            RichString("Hello World").edit {
                editAttributes(range = 0..4) {
                    textColor(HexColor("#FF0000"))
                    backgroundColor(HexColor("#00FF00"))
                    fontSize(TextSize(16f))
                    bold()
                }
            }

        val spans = str.getSpans()
        spans.size shouldBe 1
        val attrs = spans.first().attributes
        attrs.getOrNull(TextColorKey) shouldBe HexColor("#FF0000")
        attrs.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FF00")
        attrs.getOrNull(FontSizeKey) shouldBe TextSize(16f)
        attrs.getOrNull(BoldKey) shouldBe Unit
    }

    @Test
    fun `editAttributes can clear multiple attributes simultaneously from the specified range`() {
        val initialContainer =
            attributeContainerOf(
                TextColorKey to HexColor("#FF0000"),
                BoldKey to Unit,
            )
        val initialStr = RichString("Hello World", initialContainer)

        val str =
            initialStr.edit {
                editAttributes(range = 0..4) {
                    clearTextColor()
                    clearBold()
                }
            }

        val spans = str.getSpans()
        spans.size shouldBe 1
        val secondSpan = spans.first()

        // 5..10 attrs should remain
        secondSpan.range shouldBe 5..10
        secondSpan.attributes.getOrNull(TextColorKey) shouldBe HexColor("#FF0000")
        secondSpan.attributes.getOrNull(BoldKey) shouldBe Unit
    }

    @Test
    fun `editAttributes applies attributes to the entire text when range is omitted`() {
        val str =
            RichString("Hello").edit {
                editAttributes {
                    bold()
                }
            }
        val spans = str.getSpans()
        spans.size shouldBe 1
        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(BoldKey) shouldBe Unit
    }

    @Test
    fun `textColor sets and clears properly`() {
        var str =
            RichString("Test").edit {
                editAttributes { textColor(HexColor("#FF0000")) }
            }
        str.getSpans()[0].attributes.getOrNull(TextColorKey) shouldBe HexColor("#FF0000")

        // Passing Unspecified should clear it
        str =
            str.edit {
                editAttributes { textColor(HexColor.Unspecified) }
            }
        str.getSpans().isEmpty() shouldBe true

        // clearTextColor() should clear it
        str =
            str.edit {
                editAttributes { textColor(HexColor("#00FF00")) }
            }
        str =
            str.edit {
                editAttributes { clearTextColor() }
            }
        str.getSpans().isEmpty() shouldBe true
    }

    @Test
    fun `backgroundColor sets and clears properly`() {
        var str =
            RichString("Test").edit {
                editAttributes { backgroundColor(HexColor("#00FF00")) }
            }
        str.getSpans()[0].attributes.getOrNull(BackgroundColorKey) shouldBe HexColor("#00FF00")

        str =
            str.edit {
                editAttributes { backgroundColor(HexColor.Unspecified) }
            }
        str.getSpans().isEmpty() shouldBe true

        str =
            str.edit {
                editAttributes { backgroundColor(HexColor("#00FF00")) }
            }
        str =
            str.edit {
                editAttributes { clearBackgroundColor() }
            }
        str.getSpans().isEmpty() shouldBe true
    }

    @Test
    fun `fontSize sets and clears properly`() {
        var str =
            RichString("Test").edit {
                editAttributes { fontSize(TextSize(16f)) }
            }
        str.getSpans()[0].attributes.getOrNull(FontSizeKey) shouldBe TextSize(16f)

        str =
            str.edit {
                editAttributes { fontSize(TextSize.Unspecified) }
            }
        str.getSpans().isEmpty() shouldBe true

        str =
            str.edit {
                editAttributes { fontSize(TextSize(16f)) }
            }
        str =
            str.edit {
                editAttributes { clearFontSize() }
            }
        str.getSpans().isEmpty() shouldBe true
    }

    @Test
    fun `bold sets and clears properly`() {
        var str =
            RichString("Test").edit {
                editAttributes { bold() }
            }
        str.getSpans()[0].attributes.getOrNull(BoldKey) shouldBe Unit

        str =
            str.edit {
                editAttributes { clearBold() }
            }
        str.getSpans().isEmpty() shouldBe true
    }
}
