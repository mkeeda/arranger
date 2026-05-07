package dev.mkeeda.arranger.richtext

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class RichStringEditSpanTest {
    @Test
    fun `applies an attribute to the entire text and returns a new immutable instance`() {
        val original = RichString(text = "Hello")
        val styled = original.edit { setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000)) }

        // A new instance is returned (immutability)
        styled shouldNotBe original

        // The original remains unchanged
        original.text shouldBe "Hello"
        original.spans.shouldBeEmpty()

        // The styled instance has the attribute applied to the full range
        styled.text shouldBe "Hello"
        val spans = styled.spans
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `applies an attribute to a specific range`() {
        val richString =
            RichString(text = "Hello, World!")
                .edit { setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 0..4) }

        richString.text shouldBe "Hello, World!"
        val spans = richString.spans
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `applies multiple different attributes to different ranges`() {
        val richString =
            RichString(text = "Hello, World!")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FF00), range = 7..11)
                }

        richString.text shouldBe "Hello, World!"
        val spans = richString.spans
        spans shouldHaveSize 2

        val colorSpan =
            spans.first {
                it.attributes.getOrNull(TextColorKey) != null
            }
        colorSpan.range shouldBe 0..4
        colorSpan.attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)

        val mentionSpan =
            spans.first {
                it.attributes.getOrNull(BackgroundColorKey) != null
            }
        mentionSpan.range shouldBe 7..11
        mentionSpan.attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
    }

    @Test
    fun `throws IllegalArgumentException when applying to a negative range`() {
        val richString = RichString(text = "Hello, World!")
        val exception =
            shouldThrow<IllegalArgumentException> {
                richString.edit { setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = -1..3) }
            }
        exception.message shouldBe "Range start must not be negative: -1"
    }

    @Test
    fun `throws IllegalArgumentException when range exceeds text length`() {
        val richString = RichString(text = "Hello")
        val exception =
            shouldThrow<IllegalArgumentException> {
                richString.edit { setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..5) }
            }
        exception.message shouldBe "Range end must be within text bounds: 5 >= 5"
    }

    @Test
    fun `safely ignores when applying to a reversed or empty range`() {
        val original = RichString(text = "Hello, World!")
        val richString = original.edit { setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 5..3) }

        richString.spans.shouldBeEmpty()
        richString.text shouldBe original.text
    }

    @Test
    fun `successfully applies an attribute to a 1-character range`() {
        val richString =
            RichString(text = "Hello")
                .edit { setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 3..3) }

        val spans = richString.spans
        spans shouldHaveSize 1
        spans[0].range shouldBe 3..3
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `resolves partial overlap by splitting and merging spans`() {
        // e.g. [0..10] Color=Red, then [5..15] Mention=@user
        // Result:
        // [0..4] Color=Red
        // [5..10] Color=Red, Mention=@user
        // [11..15] Mention=@user
        val richString =
            RichString("1234567890123456")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..10)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FF00), range = 5..15)
                }

        val spans = richString.spans
        spans shouldHaveSize 3

        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[0].attributes.getOrNull(BackgroundColorKey) shouldBe null

        spans[1].range shouldBe 5..10
        spans[1].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[1].attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)

        spans[2].range shouldBe 11..15
        spans[2].attributes.getOrNull(TextColorKey) shouldBe null
        spans[2].attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
    }

    @Test
    fun `resolves complete containment by splitting and merging spans`() {
        // e.g. [0..10] Color=Red, then [3..7] Mention=@user
        // Result:
        // [0..2] Color=Red
        // [3..7] Color=Red, Mention=@user
        // [8..10] Color=Red
        val richString =
            RichString("12345678901")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..10)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FF00), range = 3..7)
                }

        val spans = richString.spans
        spans shouldHaveSize 3

        spans[0].range shouldBe 0..2
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[0].attributes.getOrNull(BackgroundColorKey) shouldBe null

        spans[1].range shouldBe 3..7
        spans[1].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[1].attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)

        spans[2].range shouldBe 8..10
        spans[2].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[2].attributes.getOrNull(BackgroundColorKey) shouldBe null
    }

    @Test
    fun `overwrites completely when same attribute key is applied over existing span`() {
        val richString =
            RichString("12345678901")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..10)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 0..10)
                }

        // It should perfectly replace the attribute and optimize back to 1 span
        val spans = richString.spans
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..10
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `applies attribute across multiple existing spans`() {
        // [0..4] Color=Red
        // [8..12] Color=Red
        // new: [2..10] Mention=@user
        // Expected:
        // [0..1] Color=Red
        // [2..4] Color=Red, Mention=@user
        // [5..7] Mention=@user
        // [8..10] Color=Red, Mention=@user
        // [11..12] Color=Red
        val richString =
            RichString("1234567890123")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 8..12)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FF00), range = 2..10)
                }

        val spans = richString.spans
        spans shouldHaveSize 5

        spans[0].range shouldBe 0..1
        spans[1].range shouldBe 2..4
        spans[2].range shouldBe 5..7
        spans[3].range shouldBe 8..10
        spans[4].range shouldBe 11..12

        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[1].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[1].attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
        spans[2].attributes.getOrNull(TextColorKey) shouldBe null
        spans[2].attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
        spans[3].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
        spans[3].attributes.getOrNull(BackgroundColorKey) shouldBe RgbaColor(0xFF00FF00)
        spans[4].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `merges adjacent spans when attributes are perfectly identical`() {
        val richString =
            RichString("12345678901")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 5..10)
                }

        val spans = richString.spans
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..10
        spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `edit block allows building new RichString with multiple operations safely`() {
        val original =
            RichString("12345678901234567890")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..9)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 10..19)
                }

        val edited =
            original.edit {
                // Apply mention to the middle
                setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FFFF), range = 5..14)
                // Remove the color over a sub-range
                removeSpanAttribute(TextColorKey, range = 8..11)
                // Add a completely new attribute spanning across everything
                setSpanAttribute(TextColorKey, RgbaColor.Unspecified, range = 2..17)
            }

        // Original remains completely unchanged
        original.spans shouldHaveSize 2
        original.spans[0].attributes.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)

        // Edited contains the new attributes
        edited.runs(BackgroundColorKey).toList()[0].range shouldBe 5..14

        // Assert color spans in the edited version
        val colorRuns = edited.runs(TextColorKey).toList()

        // Color runs:
        // [0..1] Red
        // [2..17] Unspecified (from the last set operation override)
        // [18..19] Blue
        colorRuns shouldHaveSize 3
        colorRuns[0].range shouldBe 0..1
        colorRuns[0].value shouldBe RgbaColor(0xFFFF0000)
        colorRuns[1].range shouldBe 2..17
        colorRuns[1].value shouldBe RgbaColor.Unspecified
        colorRuns[2].range shouldBe 18..19
        colorRuns[2].value shouldBe RgbaColor(0xFF0000FF)
    }

    @Test
    fun `edit block handles empty spans and no-op correctly`() {
        val original = RichString("hello", attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000)))
        val edited =
            original.edit {
                // Do nothing
            }

        edited.text shouldBe original.text
        edited.spans shouldBe original.spans
    }
}
