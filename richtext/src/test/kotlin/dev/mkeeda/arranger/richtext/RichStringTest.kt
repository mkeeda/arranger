package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichStringTest {
    @Test
    fun `creates a RichString from plain text`() {
        val richString = RichString(text = "Hello, World!")

        richString.text shouldBe "Hello, World!"
    }

    fun `runs returns continuous chunks of the same queried attribute value, ignoring other attributes`() {
        // [0..4] Color=Red
        // [5..9] Mention=@user
        // [10..15] Mention=@user, Color=Blue
        // [16..19] Mention=@other_user
        val richString =
            RichString("12345678901234567890")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FF00), range = 5..9)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFF00FF00), range = 10..15)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 10..15)
                    setSpanAttribute(BackgroundColorKey, RgbaColor(0xFFFF00FF), range = 16..19)
                }

        // There should be 4 internal spans: [0..4], [5..9], [10..15], [16..19]
        richString.spans shouldHaveSize 4

        val mentionRuns = richString.runs(BackgroundColorKey)

        // However, the runs API should merge [5..9] and [10..15]
        // because both have Mention=@user, ignoring the color change.
        mentionRuns shouldHaveSize 2

        mentionRuns[0].range shouldBe 5..15
        mentionRuns[0].value shouldBe RgbaColor(0xFF00FF00)
        mentionRuns[0].text shouldBe "67890123456"

        mentionRuns[1].range shouldBe 16..19
        mentionRuns[1].value shouldBe RgbaColor(0xFFFF00FF)
        mentionRuns[1].text shouldBe "7890"
    }

    @Test
    fun `runs handles gaps correctly and only returns regions with the attribute`() {
        val richString =
            RichString("01234567890123456789") // length 20
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 2..4)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 8..10)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 15..19)
                }

        val colorRuns = richString.runs(TextColorKey)

        colorRuns shouldHaveSize 3

        colorRuns[0].range shouldBe 2..4
        colorRuns[0].value shouldBe RgbaColor(0xFFFF0000)

        colorRuns[1].range shouldBe 8..10
        colorRuns[1].value shouldBe RgbaColor(0xFF0000FF)

        colorRuns[2].range shouldBe 15..19
        colorRuns[2].value shouldBe RgbaColor(0xFFFF0000)
    }
}
