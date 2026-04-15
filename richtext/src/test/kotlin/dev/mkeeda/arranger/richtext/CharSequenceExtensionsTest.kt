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
}
