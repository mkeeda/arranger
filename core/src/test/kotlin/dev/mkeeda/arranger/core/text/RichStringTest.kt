package dev.mkeeda.arranger.core.text

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class RichStringTest {
    @Test
    fun `creates a RichString from plain text`() {
        val richString = RichString(text = "Hello, World!")

        richString.text shouldBe "Hello, World!"
    }

    @Test
    fun `applies an attribute to the entire text and returns a new immutable instance`() {
        val original = RichString(text = "Hello")
        val styled = original.with(ColorAttributeKey, TextColor.Red)

        // A new instance is returned (immutability)
        styled shouldNotBe original

        // The original remains unchanged
        original.text shouldBe "Hello"
        original.getSpans().shouldBeEmpty()

        // The styled instance has the attribute applied to the full range
        styled.text shouldBe "Hello"
        val spans = styled.getSpans()
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
    }

    @Test
    fun `applies an attribute to a specific range`() {
        val richString =
            RichString(text = "Hello, World!")
                .with(ColorAttributeKey, TextColor.Blue, range = 0..4)

        richString.text shouldBe "Hello, World!"
        val spans = richString.getSpans()
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Blue
    }

    @Test
    fun `applies multiple different attributes to different ranges`() {
        val richString =
            RichString(text = "Hello, World!")
                .with(ColorAttributeKey, TextColor.Red, range = 0..4)
                .with(MentionAttributeKey, "@user", range = 7..11)

        richString.text shouldBe "Hello, World!"
        val spans = richString.getSpans()
        spans shouldHaveSize 2

        val colorSpan =
            spans.first {
                it.attributes.getOrNull(ColorAttributeKey) != null
            }
        colorSpan.range shouldBe 0..4
        colorSpan.attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red

        val mentionSpan =
            spans.first {
                it.attributes.getOrNull(MentionAttributeKey) != null
            }
        mentionSpan.range shouldBe 7..11
        mentionSpan.attributes.getOrNull(MentionAttributeKey) shouldBe "@user"
    }

    @Test
    fun `throws IllegalArgumentException when applying to a negative range`() {
        val richString = RichString(text = "Hello, World!")
        val exception =
            shouldThrow<IllegalArgumentException> {
                richString.with(ColorAttributeKey, TextColor.Red, range = -1..3)
            }
        exception.message shouldBe "Range start must not be negative: -1"
    }

    @Test
    fun `throws IllegalArgumentException when range exceeds text length`() {
        val richString = RichString(text = "Hello")
        val exception =
            shouldThrow<IllegalArgumentException> {
                richString.with(ColorAttributeKey, TextColor.Red, range = 0..5)
            }
        exception.message shouldBe "Range end must be within text bounds: 5 >= 5"
    }

    @Test
    fun `throws IllegalArgumentException when range is reversed or empty`() {
        val richString = RichString(text = "Hello, World!")
        val exception =
            shouldThrow<IllegalArgumentException> {
                richString.with(ColorAttributeKey, TextColor.Red, range = 5..3)
            }
        exception.message shouldBe "Range must not be empty: 5..3"
    }

    @Test
    fun `successfully applies an attribute to a 1-character range`() {
        val richString =
            RichString(text = "Hello")
                .with(ColorAttributeKey, TextColor.Blue, range = 3..3)

        val spans = richString.getSpans()
        spans shouldHaveSize 1
        spans[0].range shouldBe 3..3
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Blue
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
                .with(ColorAttributeKey, TextColor.Red, range = 0..10)
                .with(MentionAttributeKey, "@user", range = 5..15)

        val spans = richString.getSpans()
        spans shouldHaveSize 3

        spans[0].range shouldBe 0..4
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[0].attributes.getOrNull(MentionAttributeKey) shouldBe null

        spans[1].range shouldBe 5..10
        spans[1].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[1].attributes.getOrNull(MentionAttributeKey) shouldBe "@user"

        spans[2].range shouldBe 11..15
        spans[2].attributes.getOrNull(ColorAttributeKey) shouldBe null
        spans[2].attributes.getOrNull(MentionAttributeKey) shouldBe "@user"
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
                .with(ColorAttributeKey, TextColor.Red, range = 0..10)
                .with(MentionAttributeKey, "@user", range = 3..7)

        val spans = richString.getSpans()
        spans shouldHaveSize 3

        spans[0].range shouldBe 0..2
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[0].attributes.getOrNull(MentionAttributeKey) shouldBe null

        spans[1].range shouldBe 3..7
        spans[1].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[1].attributes.getOrNull(MentionAttributeKey) shouldBe "@user"

        spans[2].range shouldBe 8..10
        spans[2].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[2].attributes.getOrNull(MentionAttributeKey) shouldBe null
    }

    @Test
    fun `overwrites completely when same attribute key is applied over existing span`() {
        val richString =
            RichString("12345678901")
                .with(ColorAttributeKey, TextColor.Red, range = 0..10)
                .with(ColorAttributeKey, TextColor.Blue, range = 0..10)

        // It should perfectly replace the attribute and optimize back to 1 span
        val spans = richString.getSpans()
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..10
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Blue
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
                .with(ColorAttributeKey, TextColor.Red, range = 0..4)
                .with(ColorAttributeKey, TextColor.Red, range = 8..12)
                .with(MentionAttributeKey, "@user", range = 2..10)

        val spans = richString.getSpans()
        spans shouldHaveSize 5

        spans[0].range shouldBe 0..1
        spans[1].range shouldBe 2..4
        spans[2].range shouldBe 5..7
        spans[3].range shouldBe 8..10
        spans[4].range shouldBe 11..12

        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[1].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[1].attributes.getOrNull(MentionAttributeKey) shouldBe "@user"
        spans[2].attributes.getOrNull(ColorAttributeKey) shouldBe null
        spans[2].attributes.getOrNull(MentionAttributeKey) shouldBe "@user"
        spans[3].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
        spans[3].attributes.getOrNull(MentionAttributeKey) shouldBe "@user"
        spans[4].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
    }

    @Test
    fun `merges adjacent spans when attributes are perfectly identical`() {
        val richString =
            RichString("12345678901")
                .with(ColorAttributeKey, TextColor.Red, range = 0..4)
                .with(ColorAttributeKey, TextColor.Red, range = 5..10)

        val spans = richString.getSpans()
        spans shouldHaveSize 1
        spans[0].range shouldBe 0..10
        spans[0].attributes.getOrNull(ColorAttributeKey) shouldBe TextColor.Red
    }
}
