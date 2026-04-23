package dev.mkeeda.arranger.richtext

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
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
        val throwingCharSequence =
            object : CharSequence {
                override val length: Int get() = throw RuntimeException("Evaluated!")

                override fun get(index: Int): Char = throw RuntimeException("Evaluated!")

                override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = throw RuntimeException("Evaluated!")
            }

        // Just calling rangesOf should not evaluate the sequence and thus not throw
        val sequence = throwingCharSequence.rangesOf("foo")

        // It should throw when the sequence is actually consumed
        shouldThrow<RuntimeException> {
            sequence.first()
        }
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
