package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichStringRunsTest {

    @Test
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

    @Test
    fun `runs with predicate merges adjacent spans with identical attributes`() {
        val fragmentedString = RichString(
            text = "0123456789",
            spans = listOf(
                RichSpan(0..4, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000))),
                RichSpan(5..9, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000)))
            )
        )

        val redRuns = fragmentedString.runs { it.getOrNull(TextColorKey) == RgbaColor(0xFFFF0000) }.toList()

        redRuns shouldHaveSize 1
        redRuns[0].range shouldBe 0..9
        redRuns[0].value.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
    }

    @Test
    fun `runs with predicate splits adjacent spans with different attributes`() {
        val richString =
            RichString("01234567890123456789")
                .edit {
                    // [0..4] Red, Bold
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4)
                    setSpanAttribute(BoldKey, Unit, range = 0..4)

                    // [5..9] Red, Italic
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 5..9)
                    setSpanAttribute(ItalicKey, Unit, range = 5..9)

                    // [10..14] Blue, Bold
                    setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 10..14)
                    setSpanAttribute(BoldKey, Unit, range = 10..14)

                    // [15..19] Red, Bold
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 15..19)
                    setSpanAttribute(BoldKey, Unit, range = 15..19)
                }

        // Predicate: TextColor is Red
        val redRuns = richString.runs { it.getOrNull(TextColorKey) == RgbaColor(0xFFFF0000) }.toList()

        redRuns shouldHaveSize 3

        // 0..4 and 5..9 are both Red, but they have different attributes (Bold vs Italic), so they are NOT merged.
        redRuns[0].range shouldBe 0..4
        redRuns[0].value.getOrNull(BoldKey) shouldBe Unit

        redRuns[1].range shouldBe 5..9
        redRuns[1].value.getOrNull(ItalicKey) shouldBe Unit

        // 10..14 is Blue, so it's skipped.
        // 15..19 is Red, Bold.
        redRuns[2].range shouldBe 15..19
        redRuns[2].value.getOrNull(BoldKey) shouldBe Unit
    }

    @Test
    fun `runs with predicate is evaluated lazily`() {
        var evaluatedCount = 0
        val fragmentedString = RichString(
            text = "01234567890123456789",
            spans = listOf(
                RichSpan(0..4, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000))),
                RichSpan(5..9, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000))),
                RichSpan(10..14, attributeContainerOf(TextColorKey to RgbaColor(0xFF0000FF))),
                RichSpan(15..19, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000)))
            )
        )

        val sequence = fragmentedString.runs {
            evaluatedCount++
            it.getOrNull(TextColorKey) == RgbaColor(0xFFFF0000)
        }

        evaluatedCount shouldBe 0 // Not evaluated yet

        val firstRun = sequence.first()

        firstRun.range shouldBe 0..9
        // Evaluating first() should process up to index 2 (10..14) to know that 5..9 can't be merged further.
        // It shouldn't evaluate index 3 (15..19).
        evaluatedCount shouldBe 3
    }

    @Test
    fun `runs with predicate returns empty sequence for empty spans`() {
        val richString = RichString("empty", spans = emptyList())
        val runs = richString.runs { true }.toList()
        runs shouldHaveSize 0
    }

    @Test
    fun `runs with predicate skips spans not matching condition`() {
        val richString =
            RichString("0123456789")
                .edit {
                    setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4)
                    setSpanAttribute(TextColorKey, RgbaColor(0xFF0000FF), range = 5..9)
                }

        val blueRuns = richString.runs { it.getOrNull(TextColorKey) == RgbaColor(0xFF0000FF) }.toList()

        blueRuns shouldHaveSize 1
        blueRuns[0].range shouldBe 5..9
    }
}
