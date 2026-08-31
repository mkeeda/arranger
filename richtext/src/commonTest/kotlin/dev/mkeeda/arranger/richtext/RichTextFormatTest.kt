package dev.mkeeda.arranger.richtext

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichTextFormatTest {
    private val sampleRichString = RichString("Hello World")

    private val dummyFormat =
        object : RichTextFormat<String> {
            override fun export(richString: RichString): String = "EXPORTED:${richString.text}"

            override fun import(input: String): RichString = RichString(input.removePrefix("EXPORTED:"))
        }

    @Test
    fun `exporting a RichString with an exporter delegates correctly`() {
        val result = sampleRichString.export(dummyFormat)
        result shouldBe "EXPORTED:Hello World"
    }

    @Test
    fun `importing into RichString with an importer delegates correctly`() {
        val imported = RichString.import("EXPORTED:Hello World", dummyFormat)
        imported.text shouldBe "Hello World"
    }
}
