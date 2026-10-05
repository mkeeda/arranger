package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichTextSerializationTest {
    private val fakeUppercaseExporter =
        object : RichTextExporter<String> {
            override fun export(richString: RichString): String = richString.text.uppercase()
        }

    private val fakeLowercaseImporter =
        object : RichTextImporter<String> {
            override fun import(input: String): RichString = RichString(text = input.lowercase())
        }

    private class TaggedTextExporter : RichTextExporter<String> {
        override fun export(richString: RichString): String {
            val boldSpans = richString.spans.filter { it.attributes.containsKey(BoldKey) }
            val builder = StringBuilder()
            var lastIndex = 0
            for (span in boldSpans) {
                builder.append(richString.text.substring(lastIndex, span.range.first))
                builder.append("[b]")
                builder.append(richString.text.substring(span.range.first, span.range.last + 1))
                builder.append("[/b]")
                lastIndex = span.range.last + 1
            }
            builder.append(richString.text.substring(lastIndex))
            return builder.toString()
        }
    }

    private class TaggedTextImporter : RichTextImporter<String> {
        override fun import(input: String): RichString {
            val openTag = "[b]"
            val closeTag = "[/b]"
            var working = input
            val spans = mutableListOf<RichSpan>()
            val textBuilder = StringBuilder()

            while (working.isNotEmpty()) {
                val openIdx = working.indexOf(openTag)
                if (openIdx == -1) {
                    textBuilder.append(working)
                    break
                }
                textBuilder.append(working.substring(0, openIdx))
                val afterOpen = working.substring(openIdx + openTag.length)
                val closeIdx = afterOpen.indexOf(closeTag)
                if (closeIdx == -1) {
                    textBuilder.append(afterOpen)
                    break
                }
                val boldContent = afterOpen.substring(0, closeIdx)
                val start = textBuilder.length
                textBuilder.append(boldContent)
                val end = textBuilder.length
                spans.add(RichSpan(range = start until end, attributes = attributeContainerOf(BoldKey to Unit)))
                working = afterOpen.substring(closeIdx + closeTag.length)
            }

            return RichString(text = textBuilder.toString(), spans = spans)
        }
    }

    @Test
    fun `exporting RichString using independent exporter transforms text`() {
        val richString = RichString(text = "hello world")
        val result = richString.export(exporter = fakeUppercaseExporter)

        result shouldBe "HELLO WORLD"
    }

    @Test
    fun `importing input using independent importer builds RichString`() {
        val input = "HELLO WORLD"
        val richString = RichString.import(input = input, importer = fakeLowercaseImporter)

        richString.text shouldBe "hello world"
    }

    @Test
    fun `exporting and importing with custom exporter and importer preserves rich text structure`() {
        val exporter = TaggedTextExporter()
        val importer = TaggedTextImporter()

        val original =
            RichString(text = "Hello world").edit {
                setSpanAttribute(key = BoldKey, value = Unit, range = 0..4)
            }

        val exported = original.export(exporter = exporter)
        exported shouldBe "[b]Hello[/b] world"

        val restored = RichString.import(input = exported, importer = importer)
        restored.text shouldBe "Hello world"
        restored.spans.shouldContainExactly(
            RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
        )
    }

    @Test
    fun `exporting and importing with non-string custom DTO preserves rich text fidelity`() {
        data class SpanDto(val start: Int, val end: Int, val isBold: Boolean)

        data class RichTextDto(val content: String, val spans: List<SpanDto>)

        val dtoExporter =
            object : RichTextExporter<RichTextDto> {
                override fun export(richString: RichString): RichTextDto {
                    val spanDtos =
                        richString.spans.map { span ->
                            SpanDto(
                                start = span.range.first,
                                end = span.range.last,
                                isBold = span.attributes.containsKey(BoldKey),
                            )
                        }
                    return RichTextDto(content = richString.text, spans = spanDtos)
                }
            }

        val dtoImporter =
            object : RichTextImporter<RichTextDto> {
                override fun import(input: RichTextDto): RichString {
                    return RichString(input.content).edit {
                        for (span in input.spans) {
                            if (span.isBold) {
                                setSpanAttribute(BoldKey, Unit, span.start..span.end)
                            }
                        }
                    }
                }
            }

        val original =
            RichString("Custom DTO Test").edit {
                setSpanAttribute(BoldKey, Unit, 0..5)
            }

        val exportedDto = original.export(dtoExporter)
        exportedDto.content shouldBe "Custom DTO Test"
        exportedDto.spans shouldContainExactly listOf(SpanDto(start = 0, end = 5, isBold = true))

        val restored = RichString.import(exportedDto, dtoImporter)
        restored.text shouldBe original.text
        restored.spans shouldContainExactly original.spans
    }

    @Test
    fun `exporting empty RichString with custom exporter handles empty text correctly`() {
        val empty = RichString("")
        val exported = empty.export(TaggedTextExporter())
        exported shouldBe ""

        val restored = RichString.import(exported, TaggedTextImporter())
        restored.text shouldBe ""
        restored.spans.shouldBeEmpty()
    }

    @Test
    fun `single RichString can be exported to multiple targets independently without mutating original`() {
        val original =
            RichString("Multi Target").edit {
                setSpanAttribute(BoldKey, Unit, 0..4)
            }

        val taggedResult = original.export(TaggedTextExporter())
        val upperResult = original.export(fakeUppercaseExporter)

        taggedResult shouldBe "[b]Multi[/b] Target"
        upperResult shouldBe "MULTI TARGET"

        // Original rich string remains intact
        original.text shouldBe "Multi Target"
        original.spans.shouldContainExactly(
            RichSpan(range = 0..4, attributes = attributeContainerOf(BoldKey to Unit)),
        )
    }
}
