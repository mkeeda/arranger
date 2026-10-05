package dev.mkeeda.arranger.editor

import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.extractVisualBlocks
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichTextStateSaverTest {
    private val testSaverScope = SaverScope { true }

    @Test
    fun `restores plain text and cursor selection correctly`() {
        val originalState =
            RichTextState(
                initialText = RichString(text = "Hello, world!"),
                initialSelection = TextRange(start = 2, end = 5),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe "Hello, world!"
        restoredState.selection shouldBe TextRange(start = 2, end = 5)
        restoredState.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `restores all built-in span and paragraph attributes`() {
        val richString =
            RichString(
                text = "Line 1\nLine 2\nLine 3",
                spans =
                    listOf(
                        RichSpan(
                            range = 0..5,
                            attributes =
                                attributeContainerOf(
                                    BoldKey to Unit,
                                    ItalicKey to Unit,
                                    UnderlineKey to Unit,
                                    StrikethroughKey to Unit,
                                    InlineCodeKey to Unit,
                                    TextColorKey to RgbaColor(value = 0xFF123456L),
                                    BackgroundColorKey to RgbaColor(value = 0xFFABCDEFL),
                                    FontSizeKey to TextSize(sp = 18f),
                                    LinkKey to "https://example.com",
                                ),
                        ),
                        RichSpan(
                            range = 0..6,
                            attributes =
                                attributeContainerOf(
                                    HeadingKey to HeadingLevel.H2,
                                    TextAlignmentKey to TextAlignment.Center,
                                ),
                        ),
                        RichSpan(
                            range = 7..13,
                            attributes =
                                attributeContainerOf(
                                    BlockquoteKey to Unit,
                                    BulletListKey to ListIndentLevel.Level2,
                                ),
                        ),
                        RichSpan(
                            range = 14..19,
                            attributes =
                                attributeContainerOf(
                                    OrderedListKey to ListIndentLevel.Level1,
                                ),
                        ),
                    ),
            )

        val originalState =
            RichTextState(
                initialText = richString,
                initialSelection = TextRange(index = 7),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe richString.text
        restoredState.richString.spans shouldBe originalState.richString.spans
        restoredState.selection shouldBe TextRange(index = 7)
    }

    @Test
    fun `restores pending typing attributes and removed typing attributes`() {
        val originalState =
            RichTextState(
                initialText = RichString(text = "Hello"),
                initialSelection = TextRange(index = 5),
            )
        originalState.setTypingAttribute(key = BoldKey, value = Unit)
        originalState.setTypingAttribute(key = TextColorKey, value = RgbaColor(value = 0xFF998877L))
        originalState.removeTypingAttribute(key = ItalicKey)

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        val typing = restoredState.typingAttributes
        typing.shouldNotBeNull()
        typing.get(BoldKey) shouldBe Unit
        typing.get(TextColorKey) shouldBe RgbaColor(value = 0xFF998877L)

        // Type a character to verify removedTypingAttributes is maintained
        restoredState.textFieldState.edit {
            replace(start = length, end = length, text = "!")
            restoredState.updateRichString(this)
        }
        val lastCharSpan = restoredState.richString.spans.firstOrNull { 5 in it.range }
        lastCharSpan.shouldNotBeNull()
        lastCharSpan.attributes.containsKey(BoldKey) shouldBe true
        lastCharSpan.attributes.containsKey(TextColorKey) shouldBe true
        lastCharSpan.attributes.containsKey(ItalicKey) shouldBe false
    }

    @Test
    fun `excludes undo history to prevent bundle explosion and resets safely`() {
        val originalState = RichTextState(initialText = RichString(text = "Initial"))
        originalState.textFieldState.edit {
            replace(start = length, end = length, text = " edit")
            originalState.updateRichString(this)
        }
        originalState.undoState.canUndo shouldBe true

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe "Initial edit"
        restoredState.undoState.canUndo shouldBe false
        restoredState.undoState.canRedo shouldBe false
    }

    @Test
    fun `supports user-defined custom attribute keys through custom serializers`() {
        data class Highlight(val note: String)

        val customHighlightKey =
            object : SpanAttributeKey<Highlight> {
                override val name: String = "customHighlight"
                override val defaultValue: Highlight = Highlight(note = "")
            }

        val highlightSerializer =
            attributeSerializer(
                key = customHighlightKey,
                save = { highlight -> highlight.note },
                restore = { savedNote -> Highlight(note = savedNote as String) },
            )

        val customSaver =
            RichTextState.saver(
                customSerializers = listOf(highlightSerializer),
            )

        val richString =
            RichString(
                text = "Custom highlight test",
                spans =
                    listOf(
                        RichSpan(
                            range = 0..5,
                            attributes =
                                attributeContainerOf(
                                    customHighlightKey to Highlight(note = "important"),
                                    BoldKey to Unit,
                                ),
                        ),
                    ),
            )

        val originalState = RichTextState(initialText = richString)

        val saved = with(customSaver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = customSaver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe originalState.richString.text
        val firstSpan = restoredState.richString.spans.first()
        firstSpan.attributes.get(customHighlightKey) shouldBe Highlight(note = "important")
        firstSpan.attributes.containsKey(BoldKey) shouldBe true
    }

    @Test
    fun `gracefully ignores unregistered custom attribute keys and preserves known attributes`() {
        val unknownKey =
            object : SpanAttributeKey<String> {
                override val name: String = "unknownKey"
                override val defaultValue: String = ""
            }

        val originalState =
            RichTextState(
                initialText =
                    RichString(
                        text = "Ignored custom key",
                        spans =
                            listOf(
                                RichSpan(
                                    range = 0..6,
                                    attributes =
                                        attributeContainerOf(
                                            unknownKey to "secret",
                                            BoldKey to Unit,
                                        ),
                                ),
                            ),
                    ),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        val span = restoredState.richString.spans.first()
        span.attributes.containsKey(BoldKey) shouldBe true
        span.attributes.containsKey(unknownKey) shouldBe false
    }

    @Test
    fun `restores empty state without exceptions`() {
        val originalState =
            RichTextState(
                initialText = RichString(text = ""),
                initialSelection = TextRange.Zero,
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe ""
        restoredState.selection shouldBe TextRange.Zero
        restoredState.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `restores state via SaveableStateRegistry provider mechanism`() {
        val originalState =
            RichTextState(
                initialText = RichString(text = "Registry Test"),
                initialSelection = TextRange(start = 2, end = 6),
            )

        val registry1 =
            SaveableStateRegistry(
                restoredValues = emptyMap(),
                canBeSaved = { true },
            )
        registry1.registerProvider("editorState") {
            with(RichTextState.Saver) { testSaverScope.save(originalState) }
        }
        val savedValues = registry1.performSave()

        val registry2 =
            SaveableStateRegistry(
                restoredValues = savedValues,
                canBeSaved = { true },
            )
        val restoredRaw = registry2.consumeRestored("editorState")
        restoredRaw.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(restoredRaw)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe "Registry Test"
        restoredState.selection shouldBe TextRange(start = 2, end = 6)
    }

    @Test
    fun `restores reversed cursor selection accurately`() {
        val originalState =
            RichTextState(
                initialText = RichString(text = "Compose Multiplatform"),
                initialSelection = TextRange(start = 15, end = 5),
            )

        originalState.selection.reversed shouldBe true
        originalState.selection.start shouldBe 15
        originalState.selection.end shouldBe 5

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.selection shouldBe TextRange(start = 15, end = 5)
        restoredState.selection.reversed shouldBe true
        restoredState.selection.min shouldBe 5
        restoredState.selection.max shouldBe 15
    }

    @Test
    fun `restores cursor positions at extreme boundaries`() {
        val stateAtStart =
            RichTextState(
                initialText = RichString(text = "Start and End"),
                initialSelection = TextRange.Zero,
            )
        val savedStart = with(RichTextState.Saver) { testSaverScope.save(stateAtStart) }
        val restoredStart = RichTextState.Saver.restore(savedStart.shouldNotBeNull())
        restoredStart.shouldNotBeNull()
        restoredStart.selection shouldBe TextRange.Zero

        val stateAtEnd =
            RichTextState(
                initialText = RichString(text = "Start and End"),
                initialSelection = TextRange(index = 13),
            )
        val savedEnd = with(RichTextState.Saver) { testSaverScope.save(stateAtEnd) }
        val restoredEnd = RichTextState.Saver.restore(savedEnd.shouldNotBeNull())
        restoredEnd.shouldNotBeNull()
        restoredEnd.selection shouldBe TextRange(index = 13)
    }

    @Test
    fun `restores intersecting overlapping and nested spans correctly`() {
        val text = "The quick brown fox jumps over the lazy dog"
        val richString =
            RichString(
                text = text,
                spans =
                    listOf(
                        RichSpan(
                            range = 4..14,
                            attributes = attributeContainerOf(BoldKey to Unit),
                        ),
                        RichSpan(
                            range = 10..18,
                            attributes = attributeContainerOf(ItalicKey to Unit),
                        ),
                        RichSpan(
                            range = 16..24,
                            attributes = attributeContainerOf(LinkKey to "https://example.com"),
                        ),
                        RichSpan(
                            range = 35..42,
                            attributes =
                                attributeContainerOf(
                                    InlineCodeKey to Unit,
                                    TextColorKey to RgbaColor(value = 0xFF112233L),
                                ),
                        ),
                        RichSpan(
                            range = 35..38,
                            attributes = attributeContainerOf(UnderlineKey to Unit),
                        ),
                    ),
            )

        val originalState =
            RichTextState(
                initialText = richString,
                initialSelection = TextRange(start = 10, end = 20),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe text
        restoredState.richString.spans.size shouldBe 5
        restoredState.richString.spans shouldBe originalState.richString.spans
        restoredState.selection shouldBe TextRange(start = 10, end = 20)
    }

    @Test
    fun `restores multi-paragraph document with varied headings alignments and lists`() {
        val docText = "Title\nSection\nFirst item\nSecond item\nQuoted text\nFinal paragraph"
        val originalState = RichTextState(initialText = RichString(text = docText))

        originalState.setParagraphAttributeDirectly(
            key = HeadingKey,
            value = HeadingLevel.H1,
            range = 0..4,
            currentText = docText,
        )
        originalState.setParagraphAttributeDirectly(
            key = TextAlignmentKey,
            value = TextAlignment.Center,
            range = 0..4,
            currentText = docText,
        )
        originalState.setParagraphAttributeDirectly(
            key = HeadingKey,
            value = HeadingLevel.H3,
            range = 6..12,
            currentText = docText,
        )
        originalState.setParagraphAttributeDirectly(
            key = BulletListKey,
            value = ListIndentLevel.Level1,
            range = 14..23,
            currentText = docText,
        )
        originalState.setParagraphAttributeDirectly(
            key = OrderedListKey,
            value = ListIndentLevel.Level2,
            range = 25..35,
            currentText = docText,
        )
        originalState.setParagraphAttributeDirectly(
            key = BlockquoteKey,
            value = Unit,
            range = 37..47,
            currentText = docText,
        )
        originalState.setSpanAttributeDirectly(
            key = BoldKey,
            value = Unit,
            range = 49..53,
            currentText = docText,
        )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe docText
        restoredState.richString.spans shouldBe originalState.richString.spans
    }

    @Test
    fun `handles consecutive newlines and empty styled lines without crashing`() {
        val text = "Line 1\n\n\nLine 2\n"
        val originalState = RichTextState(initialText = RichString(text = text))
        originalState.setParagraphAttributeDirectly(
            key = HeadingKey,
            value = HeadingLevel.H2,
            range = 0..5,
            currentText = text,
        )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe text
        restoredState.richString.spans shouldBe originalState.richString.spans
    }

    @Test
    fun `preserves idempotency across repeated save and restore cycles`() {
        val richString =
            RichString(
                text = "Cycle Test Paragraph 1\nCycle Test Paragraph 2",
                spans =
                    listOf(
                        RichSpan(
                            range = 0..21,
                            attributes = attributeContainerOf(HeadingKey to HeadingLevel.H1),
                        ),
                        RichSpan(
                            range = 6..10,
                            attributes = attributeContainerOf(BoldKey to Unit, ItalicKey to Unit),
                        ),
                        RichSpan(
                            range = 23..44,
                            attributes = attributeContainerOf(BulletListKey to ListIndentLevel.Level1),
                        ),
                    ),
            )

        val state0 =
            RichTextState(
                initialText = richString,
                initialSelection = TextRange(start = 6, end = 10),
            )
        state0.setTypingAttribute(key = UnderlineKey, value = Unit)
        state0.removeTypingAttribute(key = StrikethroughKey)

        val saved0 = with(RichTextState.Saver) { testSaverScope.save(state0) }
        val state1 = RichTextState.Saver.restore(saved0.shouldNotBeNull()).shouldNotBeNull()

        val saved1 = with(RichTextState.Saver) { testSaverScope.save(state1) }
        val state2 = RichTextState.Saver.restore(saved1.shouldNotBeNull()).shouldNotBeNull()

        val saved2 = with(RichTextState.Saver) { testSaverScope.save(state2) }
        val state3 = RichTextState.Saver.restore(saved2.shouldNotBeNull()).shouldNotBeNull()

        state3.richString.text shouldBe state0.richString.text
        state3.richString.spans shouldBe state0.richString.spans
        state3.selection shouldBe state0.selection
        state3.typingAttributes shouldBe state0.typingAttributes
        state3.removedTypingAttributes shouldBe state0.removedTypingAttributes
    }

    @Test
    fun `supports continuation of typing and editing after restoration`() {
        val originalState =
            RichTextState(
                initialText = RichString(text = "Hello "),
                initialSelection = TextRange(index = 6),
            )
        originalState.setTypingAttribute(key = BoldKey, value = Unit)

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        val restoredState = RichTextState.Saver.restore(saved.shouldNotBeNull()).shouldNotBeNull()

        restoredState.textFieldState.edit {
            replace(start = length, end = length, text = "World")
            restoredState.updateRichString(this)
        }

        restoredState.richString.text shouldBe "Hello World"
        val worldSpan = restoredState.richString.spans.firstOrNull { 6 in it.range && 10 in it.range }
        worldSpan.shouldNotBeNull()
        worldSpan.attributes.containsKey(BoldKey) shouldBe true

        restoredState.typingAttributes shouldBe null
    }

    @Test
    fun `restores pending removed typing attributes and suppresses them on subsequent typing`() {
        val originalState =
            RichTextState(
                initialText =
                    RichString(
                        text = "Bold Text",
                        spans = listOf(RichSpan(range = 0..8, attributes = attributeContainerOf(BoldKey to Unit))),
                    ),
                initialSelection = TextRange(index = 9),
            )
        originalState.removeTypingAttribute(key = BoldKey)

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        val restoredState = RichTextState.Saver.restore(saved.shouldNotBeNull()).shouldNotBeNull()

        restoredState.textFieldState.edit {
            replace(start = length, end = length, text = " Plain")
            restoredState.updateRichString(this)
        }

        restoredState.richString.text shouldBe "Bold Text Plain"
        val plainSpan = restoredState.richString.spans.firstOrNull { 10 in it.range }
        if (plainSpan != null) {
            plainSpan.attributes.containsKey(BoldKey) shouldBe false
        }
    }

    @Test
    fun `handles large text and numerous spans under stress`() {
        val paragraphCount = 100
        val textBuilder = StringBuilder()
        val spans = mutableListOf<RichSpan>()

        for (i in 0 until paragraphCount) {
            val start = textBuilder.length
            val line = "Paragraph $i: This is a stress test line with some formatting."
            textBuilder.append(line).append("\n")
            val end = textBuilder.length - 2

            if (i % 3 == 0) {
                spans.add(
                    RichSpan(
                        range = start..end,
                        attributes = attributeContainerOf(HeadingKey to HeadingLevel.H4),
                    ),
                )
            }
            spans.add(
                RichSpan(
                    range = start..(start + 11),
                    attributes = attributeContainerOf(BoldKey to Unit),
                ),
            )
        }

        val largeRichString = RichString(text = textBuilder.toString(), spans = spans)
        val originalState = RichTextState(initialText = largeRichString, initialSelection = TextRange(index = 500))

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restoredState = RichTextState.Saver.restore(saved)
        restoredState.shouldNotBeNull()

        restoredState.richString.text shouldBe largeRichString.text
        restoredState.richString.spans shouldBe originalState.richString.spans
        restoredState.selection shouldBe TextRange(index = 500)
    }

    @Test
    fun `handles multiple custom attribute keys in single document`() {
        data class Author(val id: String)

        data class Comment(val commentId: Long, val text: String)

        val authorKey =
            object : SpanAttributeKey<Author> {
                override val name: String = "author"
                override val defaultValue: Author = Author("")
            }
        val commentKey =
            object : SpanAttributeKey<Comment> {
                override val name: String = "comment"
                override val defaultValue: Comment = Comment(0L, "")
            }

        val authorSerializer =
            attributeSerializer(
                key = authorKey,
                save = { author -> author.id },
                restore = { saved -> Author(id = saved as String) },
            )
        val commentSerializer =
            attributeSerializer(
                key = commentKey,
                save = { comment -> listOf(comment.commentId, comment.text) },
                restore = { saved ->
                    val list = saved as List<*>
                    Comment(commentId = (list[0] as Number).toLong(), text = list[1] as String)
                },
            )

        val customSaver =
            RichTextState.saver(
                customSerializers = listOf(authorSerializer, commentSerializer),
            )

        val richString =
            RichString(
                text = "Document by Alice with Comment",
                spans =
                    listOf(
                        RichSpan(
                            range = 0..16,
                            attributes = attributeContainerOf(authorKey to Author("alice-123"), BoldKey to Unit),
                        ),
                        RichSpan(
                            range = 23..29,
                            attributes =
                                attributeContainerOf(
                                    commentKey to Comment(42L, "Looks good"),
                                    ItalicKey to Unit,
                                ),
                        ),
                    ),
            )

        val originalState = RichTextState(initialText = richString)
        val saved = with(customSaver) { testSaverScope.save(originalState) }
        val restoredState = customSaver.restore(saved.shouldNotBeNull()).shouldNotBeNull()

        restoredState.richString.text shouldBe originalState.richString.text
        val firstSpan = restoredState.richString.spans.first { 0 in it.range }
        firstSpan.attributes.get(authorKey) shouldBe Author("alice-123")
        firstSpan.attributes.containsKey(BoldKey) shouldBe true

        val secondSpan = restoredState.richString.spans.first { 25 in it.range }
        secondSpan.attributes.get(commentKey) shouldBe Comment(42L, "Looks good")
        secondSpan.attributes.containsKey(ItalicKey) shouldBe true
    }

    @Test
    fun `gracefully handles corrupted or malformed saved payloads`() {
        RichTextState.Saver.restore("not-a-list") shouldBe null
        RichTextState.Saver.restore(listOf("text", 0, 0)) shouldBe null
        RichTextState.Saver.restore(listOf(12345, 0, 0, emptyList<Any>(), null, null)) shouldBe null

        val restoredOutOfBounds =
            RichTextState.Saver.restore(
                listOf(
                    "Hello",
                    -10,
                    9999,
                    emptyList<Any>(),
                    null,
                    null,
                ),
            )
        restoredOutOfBounds.shouldNotBeNull()
        restoredOutOfBounds.selection shouldBe TextRange(start = 0, end = 5)

        val restoredMalformedSpans =
            RichTextState.Saver.restore(
                listOf(
                    "Sample",
                    0,
                    0,
                    listOf(
                        "invalid-span-element",
                        listOf(0),
                        listOf("not-a-number", 3, emptyList<Any>()),
                        listOf(0, 3, "not-a-list-of-attrs"),
                    ),
                    null,
                    null,
                ),
            )
        restoredMalformedSpans.shouldNotBeNull()
        restoredMalformedSpans.richString.spans.isEmpty() shouldBe true
    }

    @Test
    fun `restores code block attribute with language`() {
        val originalState =
            RichTextState(
                initialText =
                    RichString(
                        text = "println(42)",
                        spans =
                            listOf(
                                RichSpan(
                                    range = 0..10,
                                    attributes = attributeContainerOf(CodeBlockKey to "kotlin"),
                                ),
                            ),
                    ),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restored = RichTextState.Saver.restore(saved)
        restored.shouldNotBeNull()
        restored.richString.spans shouldHaveSize 1
        restored.richString.spans.first().attributes[CodeBlockKey] shouldBe "kotlin"
    }

    @Test
    fun `restores code block attribute without language`() {
        val originalState =
            RichTextState(
                initialText =
                    RichString(
                        text = "echo hi",
                        spans =
                            listOf(
                                RichSpan(
                                    range = 0..6,
                                    attributes = attributeContainerOf(CodeBlockKey to null),
                                ),
                            ),
                    ),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restored = RichTextState.Saver.restore(saved)
        restored.shouldNotBeNull()
        restored.richString.spans shouldHaveSize 1
        restored.richString.spans.first().attributes.containsKey(CodeBlockKey) shouldBe true
        restored.richString.spans.first().attributes[CodeBlockKey] shouldBe null
    }

    @Test
    fun `restores state preserving code block visual blocks and cursor position`() {
        val originalState =
            RichTextState(
                initialText =
                    RichString(
                        text = "fun main() {\n    println(42)\n}",
                        spans =
                            listOf(
                                RichSpan(
                                    range = 0..30,
                                    attributes = attributeContainerOf(CodeBlockKey to "kotlin"),
                                ),
                            ),
                    ),
                initialSelection = TextRange(15),
            )

        val saved = with(RichTextState.Saver) { testSaverScope.save(originalState) }
        saved.shouldNotBeNull()

        val restored = RichTextState.Saver.restore(saved)
        restored.shouldNotBeNull()
        restored.selection shouldBe TextRange(15)
        restored.richString.extractVisualBlocks() shouldBe originalState.richString.extractVisualBlocks()
    }
}
