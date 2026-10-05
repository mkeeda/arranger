package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichTextCodeBlockEditTest {
    @Test
    fun `codeBlock applies CodeBlockKey snapping to paragraph boundaries`() {
        val text = "Line 1\nLine 2\nLine 3"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.rangeOf("e 2")) {
                    codeBlock(language = "kotlin")
                }
            }

        val codeBlockSpans = richString.spans.filter { it.attributes.containsKey(CodeBlockKey) }
        codeBlockSpans shouldHaveSize 1
        codeBlockSpans.first().range shouldBe text.rangeOf("Line 2\n")
        codeBlockSpans.first().attributes[CodeBlockKey] shouldBe "kotlin"
    }

    @Test
    fun `codeBlock without language sets null language`() {
        val text = "val a = 1"
        val richString =
            RichString(text = text).edit {
                editAttributes(range = text.indices) {
                    codeBlock()
                }
            }

        val codeBlockSpans = richString.spans.filter { it.attributes.containsKey(CodeBlockKey) }
        codeBlockSpans shouldHaveSize 1
        codeBlockSpans.first().attributes[CodeBlockKey].shouldBeNull()
    }

    @Test
    fun `clearCodeBlock removes CodeBlockKey from paragraph`() {
        val text = "val a = 1"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.indices) {
                        codeBlock(language = "kotlin")
                    }
                }
                .edit {
                    editAttributes(range = text.indices) {
                        clearCodeBlock()
                    }
                }

        val codeBlockSpans = richString.spans.filter { it.attributes.containsKey(CodeBlockKey) }
        codeBlockSpans.shouldBeEmpty()
    }

    @Test
    fun `codeBlock clears conflicting BlockTypeAttributeKey like heading`() {
        val text = "Heading 1"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.indices) {
                        headingLevel(level = HeadingLevel.H1)
                    }
                }
                .edit {
                    editAttributes(range = text.indices) {
                        codeBlock(language = "kotlin")
                    }
                }

        val headingSpans = richString.spans.filter { it.attributes.containsKey(HeadingKey) }
        val codeBlockSpans = richString.spans.filter { it.attributes.containsKey(CodeBlockKey) }

        headingSpans.shouldBeEmpty()
        codeBlockSpans shouldHaveSize 1
        codeBlockSpans.first().attributes[CodeBlockKey] shouldBe "kotlin"
    }

    @Test
    fun `codeBlock clears conflicting BlockTypeAttributeKey like blockquote`() {
        val text = "Quote"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.indices) {
                        blockquote()
                    }
                }
                .edit {
                    editAttributes(range = text.indices) {
                        codeBlock()
                    }
                }

        val quoteSpans = richString.spans.filter { it.attributes.containsKey(BlockquoteKey) }
        val codeBlockSpans = richString.spans.filter { it.attributes.containsKey(CodeBlockKey) }

        quoteSpans.shouldBeEmpty()
        codeBlockSpans shouldHaveSize 1
    }

    @Test
    fun `codeBlock clears conflicting BlockTypeAttributeKey like bullet list`() {
        val text = "List item"
        val richString =
            RichString(text = text)
                .edit {
                    editAttributes(range = text.indices) {
                        bulletList(level = ListIndentLevel.Level1)
                    }
                }
                .edit {
                    editAttributes(range = text.indices) {
                        codeBlock(language = "java")
                    }
                }

        val listSpans = richString.spans.filter { it.attributes.containsKey(BulletListKey) }
        val codeBlockSpans = richString.spans.filter { it.attributes.containsKey(CodeBlockKey) }

        listSpans.shouldBeEmpty()
        codeBlockSpans shouldHaveSize 1
        codeBlockSpans.first().attributes[CodeBlockKey] shouldBe "java"
    }
}
