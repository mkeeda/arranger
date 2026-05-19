package dev.mkeeda.arranger.richtext

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichStringTest {
    @Test
    fun `creates a RichString from plain text`() {
        val richString = RichString(text = "Hello, World!")

        richString.text shouldBe "Hello, World!"
    }
}
