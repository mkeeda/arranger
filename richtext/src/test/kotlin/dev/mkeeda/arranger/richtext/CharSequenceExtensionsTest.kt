package dev.mkeeda.arranger.richtext

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test

class CharSequenceExtensionsTest {
    @Test
    fun `rangeOf returns correct index range for existing substring`() {
        val text = "Hello Kotlin World"

        text.rangeOf("Hello") shouldBe 0..4
        text.rangeOf("Kotlin") shouldBe 6..11
        text.rangeOf("World") shouldBe 13..17
        text.rangeOf(" ") shouldBe 5..5
    }

    @Test
    fun `rangeOf throws exception for missing substring`() {
        val text = "Hello Kotlin World"

        val exception =
            shouldThrow<IllegalArgumentException> {
                text.rangeOf("Java")
            }
        exception.message shouldBe "Substring 'Java' not found in 'Hello Kotlin World'"
    }

    @Test
    fun `rangeOf works with CharSequence boundaries`() {
        val text: CharSequence = "Arranger"

        text.rangeOf("A") shouldBe 0..0
        text.rangeOf("r") shouldBe 1..1 // Finds the first occurrence
        text.rangeOf("ger") shouldBe 5..7
    }

    @Test
    fun `rangesOf returns all matching ranges for a repeated word`() {
        val text = "foo bar foo baz foo"
        val ranges = text.rangesOf("foo").toList()
        ranges shouldBe listOf(0..2, 8..10, 16..18)
    }

    @Test
    fun `rangesOf with ignoreCase matches case-insensitively`() {
        val text = "Hello hello HELLO"
        val ranges = text.rangesOf("hello", ignoreCase = true).toList()
        ranges shouldBe listOf(0..4, 6..10, 12..16)
    }

    @Test
    fun `rangesOf returns empty sequence when no match found`() {
        val text = "Hello Kotlin World"
        val ranges = text.rangesOf("Java").toList()
        ranges.shouldBeEmpty()
    }

    @Test
    fun `rangesOf is lazily evaluated`() {
        val text = "foo bar foo baz foo"
        var evaluationCount = 0
        val sequence = text.rangesOf("foo").map {
            evaluationCount++
            it
        }

        evaluationCount shouldBe 0 // Not evaluated yet

        sequence.first() shouldBe 0..2
        evaluationCount shouldBe 1 // Evaluated only the first match
    }

    @Test
    fun `rangesOf with regex returns all matching ranges`() {
        val text = "#kotlin is great #android #compose"
        val regex = Regex("#\\w+")
        val ranges = text.rangesOf(regex).toList()

        ranges shouldBe listOf(0..6, 17..24, 26..33)
    }

    @Test
    fun `rangesOf with regex returns empty sequence when no match`() {
        val text = "hello world"
        val regex = Regex("#\\w+")
        val ranges = text.rangesOf(regex).toList()

        ranges.shouldBeEmpty()
    }
}
