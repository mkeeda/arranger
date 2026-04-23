package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class RichStringBatchEditTest {
    @Test
    fun `editAll applies attributes to all specified ranges`() {
        val original = RichString("12345678901234567890")

        val ranges =
            sequenceOf(
                0..4,
                10..14,
            )

        val edited =
            original.edit {
                editAll(ranges) {
                    textColor(RgbaColor(0xFFFF0000))
                }
            }

        val spans = edited.spans
        spans shouldBe
            listOf(
                RichSpan(0..4, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000))),
                RichSpan(10..14, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000))),
            )
    }

    @Test
    fun `editAll with empty ranges is a no-op`() {
        val original = RichString("1234567890")

        val edited =
            original.edit {
                editAll(emptySequence<IntRange>()) {
                    textColor(RgbaColor(0xFFFF0000))
                }
            }

        edited.spans.shouldBeEmpty()
        edited.text shouldBe original.text
    }

    @Test
    fun `editAll with runs applies attributes based on original run values`() {
        val original =
            RichString("01234567890123456789").edit {
                // Setup original runs
                setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 0..4) // Red
                setSpanAttribute(TextColorKey, RgbaColor(0xFF00FF00), range = 5..9) // Green
                setSpanAttribute(TextColorKey, RgbaColor(0xFFFF0000), range = 10..14) // Red
            }

        // Query runs that are red
        val redRuns = original.runs { it.getOrNull(TextColorKey) == RgbaColor(0xFFFF0000) }

        // Change red runs to blue
        val edited =
            original.edit {
                editAll(redRuns) { run ->
                    // Ensure run has the expected original value
                    run.value.getOrNull(TextColorKey) shouldBe RgbaColor(0xFFFF0000)
                    // Overwrite with Blue
                    textColor(RgbaColor(0xFF0000FF))
                }
            }

        val spans = edited.spans
        spans shouldBe
            listOf(
                RichSpan(0..4, attributeContainerOf(TextColorKey to RgbaColor(0xFF0000FF))),
                RichSpan(5..9, attributeContainerOf(TextColorKey to RgbaColor(0xFF00FF00))),
                RichSpan(10..14, attributeContainerOf(TextColorKey to RgbaColor(0xFF0000FF))),
            )
    }

    @Test
    fun `editAll combined with rangesOf highlights all keyword occurrences`() {
        val original = RichString("Error: Invalid input. Error: Null pointer. Error: Timeout.")

        // Search
        val errorRanges = original.text.rangesOf("Error:")

        // Batch edit
        val edited =
            original.edit {
                editAll(errorRanges) {
                    textColor(RgbaColor(0xFFFF0000)) // Highlight in red
                    bold()
                }
            }

        val spans = edited.spans
        spans shouldBe
            listOf(
                RichSpan(0..5, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000), BoldKey to Unit)),
                RichSpan(22..27, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000), BoldKey to Unit)),
                RichSpan(43..48, attributeContainerOf(TextColorKey to RgbaColor(0xFFFF0000), BoldKey to Unit)),
            )
    }
}
