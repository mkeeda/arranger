package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextEditorSpanTapTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping on custom span triggers onSpanClick with accurate RichSpan and consumes event when consume is called`() {
        val initialText = "Hello @mkeeda welcome"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap plain text character 'H' (index 0) first to position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)
        clickedSpan shouldBe null

        // Tap on '@' (index 6) inside the mention span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        clickedSpan?.range shouldBe initialText.rangeOf("@mkeeda")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        // Cursor placement must be suppressed because consume() was called
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `onSpanClick without calling consume on custom span allows normal cursor placement`() {
        val initialText = "Hello @mkeeda welcome"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    // Note: event.consume() is intentionally NOT called
                },
            )
        }

        // Tap on plain text 'H' (index 0) to position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // Tap on '@' (index 6) inside the mention span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        clickedSpan?.range shouldBe initialText.rangeOf("@mkeeda")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        // Since consume() was not called, cursor should be placed normally at index 6
        state.selection shouldBe TextRange(6)
    }

    @Test
    fun `tapping on hyperlink triggers onSpanClick allowing user to open uri and consume event`() {
        val initialText = "Click here"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", initialText.rangeOf("here"))
                    },
            )

        var openedUri: String? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    val url = event.span.attributes[LinkKey]
                    if (!url.isNullOrEmpty()) {
                        openedUri = url
                        event.consume()
                    }
                },
            )
        }

        // Tap on plain text 'C' (index 0)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        openedUri shouldBe null
        state.selection shouldBe TextRange(0)

        // Tap on hyperlink "here" (index 6)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        openedUri shouldBe "https://example.com"
        // Cursor placement must be suppressed by event.consume()
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `tapping on hyperlink with onSpanClick null does not consume event and moves cursor`() {
        val initialText = "Click here"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", initialText.rangeOf("here"))
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = null,
            )
        }

        // Initially position cursor at 0
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // Tap on hyperlink "here" (index 6) with no handler
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)

        // Cursor should move to index 6 without being intercepted
        state.selection shouldBe TextRange(6)
    }

    @Test
    fun `tapping on plain text does not trigger onSpanClick and allows cursor placement`() {
        val initialText = "Hello @mkeeda welcome"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", initialText.rangeOf("@mkeeda"))
                    },
            )

        var onSpanClickCalled = false
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = {
                    onSpanClickCalled = true
                    it.consume()
                },
            )
        }

        // Tap on plain text 'H' (index 0)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)

        onSpanClickCalled shouldBe false
        state.selection shouldBe TextRange(0)
    }

    @Test
    fun `tapping on exact start and end characters of a span triggers onSpanClick, but immediate surrounding characters do not`() {
        val initialText = "ABC Hello XYZ"
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@Hello", initialText.rangeOf("Hello"))
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on character 'C' (index 2) - plain text before span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(2)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(2)
        clickedSpan shouldBe null

        // Tap on space (index 3) - immediate predecessor to span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(3)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(3)
        clickedSpan shouldBe null

        // Tap on 'H' (index 4) - exact start of span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(4)
        composeTestRule.waitForIdle()
        clickedSpan?.range shouldBe initialText.rangeOf("Hello")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@Hello"
        state.selection shouldBe TextRange(3) // suppressed by consume()

        clickedSpan = null

        // Tap on 'o' (index 8) - exact end of span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(8)
        composeTestRule.waitForIdle()
        clickedSpan?.range shouldBe initialText.rangeOf("Hello")
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@Hello"
        state.selection shouldBe TextRange(3) // suppressed by consume()

        clickedSpan = null

        // Tap on space (index 9) - immediate successor to span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(9)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(9)
        clickedSpan shouldBe null

        // Tap on 'X' (index 10) - plain text after span
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(10)
        composeTestRule.waitForIdle()
        state.selection shouldBe TextRange(10)
        clickedSpan shouldBe null
    }

    @Test
    fun `tapping consecutive adjacent spans with different attributes triggers onSpanClick with exact corresponding span`() {
        val initialText = "@alice#tech"
        val aliceRange = initialText.rangeOf("@alice")
        val techRange = initialText.rangeOf("#tech")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@alice", aliceRange)
                        setSpanAttribute(HashtagKey, "#tech", techRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on 'e' (index 5) - last character of @alice
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(5)
        clickedSpan?.range shouldBe aliceRange
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@alice"
        clickedSpan?.attributes?.containsKey(HashtagKey) shouldBe false

        clickedSpan = null

        // Tap on '#' (index 6) - first character of #tech
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)
        clickedSpan?.range shouldBe techRange
        clickedSpan?.attributes?.get(HashtagKey) shouldBe "#tech"
        clickedSpan?.attributes?.containsKey(MentionKey) shouldBe false
    }

    @Test
    fun `tapping span with multiple overlapping attributes provides complete attribute container in SpanClickEvent`() {
        val initialText = "Visit Google today"
        val googleRange = initialText.rangeOf("Google")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(BoldKey, Unit, initialText.rangeOf("Visit Google"))
                        setSpanAttribute(LinkKey, "https://google.com", googleRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on 'G' (index 6) of Google
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)
        clickedSpan?.range shouldBe googleRange
        clickedSpan?.attributes?.get(LinkKey) shouldBe "https://google.com"
        clickedSpan?.attributes?.containsKey(BoldKey) shouldBe true

        clickedSpan = null

        // Tap on 'V' (index 0) of Visit - Bold only (no link, but it's a span!)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        clickedSpan?.range shouldBe initialText.rangeOf("Visit ")
        clickedSpan?.attributes?.containsKey(BoldKey) shouldBe true
        clickedSpan?.attributes?.containsKey(LinkKey) shouldBe false
    }

    @Test
    fun `tapping across multiple lines within a single span triggers onSpanClick on all lines`() {
        val initialText = "Line One\nLine Two\nLine Three"
        val multilineRange = 0..initialText.rangeOf("Line Two").last
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(LinkKey, "https://example.com", multilineRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on 'n' in "One" (index 6, line 1)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(6)
        clickedSpan?.range shouldBe multilineRange
        clickedSpan?.attributes?.get(LinkKey) shouldBe "https://example.com"

        clickedSpan = null

        // Tap on 'T' in "Two" (index 14, line 2)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(14)
        clickedSpan?.range shouldBe multilineRange
        clickedSpan?.attributes?.get(LinkKey) shouldBe "https://example.com"

        clickedSpan = null

        // Tap on 'T' in "Three" (index 23, line 3, outside span)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(23)
        clickedSpan shouldBe null
        state.selection shouldBe TextRange(23)
    }

    @Test
    fun `repeated taps properly isolate SpanClickEvent state and avoid stale consumption`() {
        val initialText = "Click @one then @two here"
        val oneRange = initialText.rangeOf("@one")
        val twoRange = initialText.rangeOf("@two")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@one", oneRange)
                        setSpanAttribute(MentionKey, "@two", twoRange)
                    },
            )

        var lastEvent: SpanClickEvent? = null
        var shouldConsume = true
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                onSpanClick = { event ->
                    lastEvent = event
                    if (shouldConsume) {
                        event.consume()
                    }
                },
            )
        }

        // Tap plain text first
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // 1. Tap @one and consume
        shouldConsume = true
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(oneRange.first)
        lastEvent?.span?.range shouldBe oneRange
        lastEvent?.isConsumed shouldBe true
        state.selection shouldBe TextRange(0) // unchanged because consumed

        // 2. Tap @two and do NOT consume
        shouldConsume = false
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(twoRange.first)
        lastEvent?.span?.range shouldBe twoRange
        lastEvent?.isConsumed shouldBe false
        state.selection shouldBe TextRange(twoRange.first) // cursor moved

        // 3. Tap plain text "here" (index 22)
        lastEvent = null
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(22)
        lastEvent shouldBe null
        state.selection shouldBe TextRange(22) // cursor moved
    }

    @Test
    fun `tapping on span in readOnly editor triggers onSpanClick and suppresses cursor move when consumed`() {
        val initialText = "Read only @mkeeda text"
        val mentionRange = initialText.rangeOf("@mkeeda")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", mentionRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                readOnly = true,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap plain text character 'R' (index 0)
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(0)
        state.selection shouldBe TextRange(0)

        // Tap on mention in readOnly mode
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(mentionRange.first)
        clickedSpan?.range shouldBe mentionRange
        clickedSpan?.attributes?.get(MentionKey) shouldBe "@mkeeda"
        state.selection shouldBe TextRange(0) // suppressed by consume()
    }

    @Test
    fun `tapping on span in disabled editor does not trigger onSpanClick`() {
        val initialText = "Disabled @mkeeda text"
        val mentionRange = initialText.rangeOf("@mkeeda")
        val state =
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        setSpanAttribute(MentionKey, "@mkeeda", mentionRange)
                    },
            )

        var clickedSpan: RichSpan? = null
        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                enabled = false,
                onSpanClick = { event ->
                    clickedSpan = event.span
                    event.consume()
                },
            )
        }

        // Tap on mention in disabled mode
        composeTestRule.onNodeWithText(initialText).clickOnCharacter(mentionRange.first)

        // When editor is disabled, user interaction should be completely disabled!
        clickedSpan shouldBe null
    }
}

private object MentionKey : SpanAttributeKey<String> {
    override val name: String = "mention"
    override val defaultValue: String = ""
}

private object HashtagKey : SpanAttributeKey<String> {
    override val name: String = "hashtag"
    override val defaultValue: String = ""
}
