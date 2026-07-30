package dev.mkeeda.arranger.richtext

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichStringTest {
    @Test
    fun `creates a RichString from plain text`() {
        val richString = RichString(text = "Hello, World!")

        richString.text shouldBe "Hello, World!"
    }

    @Test
    fun `returns true when isEmpty is called on empty RichString`() {
        // Arrange
        val richString = RichString(text = "")

        // Act
        val result = richString.isEmpty()

        // Assert
        result shouldBe true
    }

    @Test
    fun `returns false when isEmpty is called on non-empty RichString`() {
        // Arrange
        val richString = RichString(text = "Hello")

        // Act
        val result = richString.isEmpty()

        // Assert
        result shouldBe false
    }

    @Test
    fun `returns true when isBlank is called on blank RichString`() {
        // Arrange
        val richString = RichString(text = "   ")

        // Act
        val result = richString.isBlank()

        // Assert
        result shouldBe true
    }

    @Test
    fun `returns false when isBlank is called on non-blank RichString`() {
        // Arrange
        val richString = RichString(text = "  Hello  ")

        // Act
        val result = richString.isBlank()

        // Assert
        result shouldBe false
    }
}
