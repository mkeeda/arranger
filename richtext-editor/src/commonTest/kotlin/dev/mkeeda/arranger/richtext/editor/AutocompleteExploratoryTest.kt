package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Exploratory tests for Autocomplete hooks verifying:
 * 1. Multilingual, surrogate pairs, emojis, and full-width characters.
 * 2. Extreme boundary conditions, stress scenarios, and in-query edits.
 * 3. Complex span interactions with pre-existing styling and undo/redo.
 * 4. PopupPositionProvider boundary and extreme coordinates handling.
 */
class AutocompleteExploratoryTest {
    // region 1. Multilingual, Surrogate Pairs & Emojis

    @Test
    fun `detectAutocomplete recognizes trigger preceded by full-width ideographic space`() {
        // Arrange: full-width space (\u3000) followed by '@alice'
        val text = "こんにちは\u3000@alice"
        val triggers = listOf(AutocompleteTrigger(prefix = "@", requireLeadingWhitespace = true))

        // Act: Cursor at the end of the text
        val match = detectAutocomplete(text = text, cursorPosition = text.length, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@"
        match.query shouldBe "alice"
        match.range shouldBe 6..11
        match.queryRange shouldBe 7..11
    }

    @Test
    fun `detectAutocomplete rejects trigger immediately preceded by Japanese character without whitespace`() {
        // Arrange
        val text = "こんにちは@alice"
        val triggers = listOf(AutocompleteTrigger(prefix = "@", requireLeadingWhitespace = true))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = text.length, triggers = triggers)

        // Assert
        match.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete extracts Japanese query with full-width characters`() {
        // Arrange
        val text = "チームのみんな @山田太郎 さん"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))
        val cursorPosition = 13 // right after "山田太郎"

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = cursorPosition, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.query shouldBe "山田太郎"
        match.range shouldBe 8..12
        match.queryRange shouldBe 9..12
    }

    @Test
    fun `detectAutocomplete handles full-width space inside query based on allowSpacesInQuery flag`() {
        // Arrange: full-width space between last and first name
        val text = "@山田\u3000太郎"
        val cursorPosition = text.length

        // Case 1: allowSpacesInQuery = false -> should reject
        val disallowMatch =
            detectAutocomplete(
                text = text,
                cursorPosition = cursorPosition,
                triggers = listOf(AutocompleteTrigger(prefix = "@", allowSpacesInQuery = false)),
            )
        disallowMatch.shouldBeNull()

        // Case 2: allowSpacesInQuery = true -> should accept
        val allowMatch =
            detectAutocomplete(
                text = text,
                cursorPosition = cursorPosition,
                triggers = listOf(AutocompleteTrigger(prefix = "@", allowSpacesInQuery = true)),
            )
        allowMatch.shouldNotBeNull()
        allowMatch.query shouldBe "山田\u3000太郎"
    }

    @Test
    fun `detectAutocomplete handles surrogate pair emoji before and inside query`() {
        // Arrange: 🎉 is surrogate pair (\uD83C\uDF89)
        // Case A: Emoji immediately preceding prefix without space -> rejected if requireLeadingWhitespace
        val textWithoutSpace = "🎉@target"
        val matchWithoutSpace =
            detectAutocomplete(
                text = textWithoutSpace,
                cursorPosition = textWithoutSpace.length,
                triggers = listOf(AutocompleteTrigger("@", requireLeadingWhitespace = true)),
            )
        matchWithoutSpace.shouldBeNull()

        // Case B: Emoji followed by space, then trigger with emoji inside query
        val textWithEmoji = "🎉 @user\uD83D\uDE00end"
        // "🎉 " has length 3 (2 for emoji, 1 for space)
        val matchWithEmoji =
            detectAutocomplete(
                text = textWithEmoji,
                cursorPosition = textWithEmoji.length,
                triggers = listOf(AutocompleteTrigger("@")),
            )
        matchWithEmoji.shouldNotBeNull()
        matchWithEmoji.query shouldBe "user\uD83D\uDE00end"
        matchWithEmoji.range shouldBe 3..12
    }

    @Test
    fun `detectAutocomplete works with multi-character emoji trigger prefix`() {
        // Arrange: Trigger prefix is an emoji "🎉" (2 chars surrogate pair)
        val text = "Great news 🎉celebrate"
        val trigger = AutocompleteTrigger(prefix = "🎉")

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = text.length, triggers = listOf(trigger))

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "🎉"
        match.query shouldBe "celebrate"
        // "Great news " is length 11. "🎉" is index 11..12
        match.range shouldBe 11..21
        match.queryRange shouldBe 13..21
    }

    @Test
    fun `detectAutocomplete handles RTL text with Arabic script`() {
        // Arrange: Arabic greeting and mention
        val text = "مرحبا @علي"
        val trigger = AutocompleteTrigger(prefix = "@")

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = text.length, triggers = listOf(trigger))

        // Assert
        match.shouldNotBeNull()
        match.query shouldBe "علي"
        match.range shouldBe 6..9
    }

    // endregion

    // region 2. Stress, Edge Cases & In-Query Edits

    @Test
    fun `detectAutocomplete handles exact maxQueryLength boundary`() {
        val trigger = AutocompleteTrigger(prefix = "@", maxQueryLength = 5)

        // Exact boundary: 5 chars query -> matches
        val exactText = "@12345"
        val exactMatch = detectAutocomplete(exactText, exactText.length, listOf(trigger))
        exactMatch.shouldNotBeNull()
        exactMatch.query shouldBe "12345"

        // Exceeded boundary: 6 chars query -> null
        val exceededText = "@123456"
        val exceededMatch = detectAutocomplete(exceededText, exceededText.length, listOf(trigger))
        exceededMatch.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete handles consecutive blank lines and indentation`() {
        // Text with multiple newlines and indentation before trigger
        val text = "\n\n\n   @target"
        val trigger = AutocompleteTrigger(prefix = "@")

        val match = detectAutocomplete(text, text.length, listOf(trigger))
        match.shouldNotBeNull()
        match.query shouldBe "target"
        match.range shouldBe 6..12
    }

    @Test
    fun `detectAutocomplete does not detect trigger on prior line when cursor is on empty line`() {
        val text = "@target\n"
        val trigger = AutocompleteTrigger(prefix = "@")

        val match = detectAutocomplete(text, text.length, listOf(trigger))
        match.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete with overlapping triggers chooses correct prefix`() {
        // "@@" vs "@" triggers
        val triggerDouble = AutocompleteTrigger(prefix = "@@")
        val triggerSingle = AutocompleteTrigger(prefix = "@")
        val triggers = listOf(triggerDouble, triggerSingle)

        val text = "Hello @@admin"
        val match = detectAutocomplete(text, text.length, triggers)

        // Because "@@" starts after whitespace and requires leading whitespace,
        // single "@" at index 7 is preceded by '@' (not whitespace), so "@@" at index 6 wins!
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@@"
        match.query shouldBe "admin"
        match.range shouldBe 6..12
    }

    @Test
    fun `applyCompletion with cursor in middle of query replaces up to cursor preserving suffix`() {
        // Arrange: User typed "@developer", but cursor is at index 4 ("@dev|eloper")
        val state = RichTextState(RichString("@developer"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "dev",
                range = 0..3,
                queryRange = 1..3,
                cursorPosition = 4,
            )

        // Act: Apply replacement "coder"
        state.applyCompletion(match, replacement = "coder")

        // Assert: "@dev" (0..3) is replaced with "coder", "eloper" remains
        state.richString.text shouldBe "codereloper"
        state.selection.start shouldBe 5
        state.selection.end shouldBe 5
    }

    @Test
    fun `detectAutocomplete performs efficiently on very large text`() {
        // Stress test: 10,000 characters with multiple lines
        val padding = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n".repeat(150)
        val text = "$padding@quickquery"
        val trigger = AutocompleteTrigger(prefix = "@")

        val match = detectAutocomplete(text, text.length, listOf(trigger))
        match.shouldNotBeNull()
        match.query shouldBe "quickquery"
    }

    // endregion

    // region 3. Complex Span Interactions & Undo/Redo

    @Test
    fun `applyCompletion within existing styled span shifts subsequent spans properly`() {
        // Arrange: "Start [Bold Section] End" where Bold is 6..17
        val initial =
            RichString("Start Hello @ali End").edit {
                setSpanAttribute(BoldKey, Unit, 6..15) // covers "Hello @ali"
            }
        val state = RichTextState(initial)
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 12..15,
                queryRange = 13..15,
                cursorPosition = 16,
            )

        // Act: Replace "@ali" with "Alice LongName " (length 15 instead of 4, +11 chars)
        state.applyCompletion(match, replacement = "Alice LongName ")

        // Assert: Text updated
        state.richString.text shouldBe "Start Hello Alice LongName  End"
        // The original bold span originally spanning 6..15 should now be expanded or adjusted
        val boldSpans = state.richString.spans.filter { it.attributes.containsKey(BoldKey) }
        boldSpans.size shouldBe 1
        // Verify span correctly covers the expanded text
        boldSpans.first().range.first shouldBe 6
        boldSpans.first().range.last shouldBe 26
    }

    @Test
    fun `applyCompletion with RichString applies multiple attributes and maintains offsets`() {
        // Arrange
        val state = RichTextState(RichString("Mention: @target"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "target",
                range = 9..15,
                queryRange = 10..15,
                cursorPosition = 16,
            )

        // Replacement RichString with Bold for "@Alice" and Italic for "[Admin]"
        val replacement =
            RichString("@Alice [Admin]").edit {
                setSpanAttribute(BoldKey, Unit, 0..5) // "@Alice"
                setSpanAttribute(ItalicKey, Unit, 7..13) // "[Admin]"
            }

        // Act
        state.applyCompletion(match, replacement = replacement)

        // Assert
        state.richString.text shouldBe "Mention: @Alice [Admin]"
        state.selection.start shouldBe 23

        val boldSpan = state.richString.spans.first { it.attributes.containsKey(BoldKey) }
        boldSpan.range shouldBe 9..14 // 9 + 0..5

        val italicSpan = state.richString.spans.first { it.attributes.containsKey(ItalicKey) }
        italicSpan.range shouldBe 16..22 // 9 + 7..13
    }

    @Test
    fun `applyCompletion supports empty replacement string removing the matched trigger`() {
        // Arrange
        val state = RichTextState(RichString("Hello @tag world"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "tag",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
            )

        // Act: Delete trigger by replacing with empty string
        state.applyCompletion(match, replacement = "")

        // Assert
        state.richString.text shouldBe "Hello  world"
        state.selection.start shouldBe 6
    }

    @Test
    fun `applyCompletion with attributes can be undone and redone with complete style restoration`() {
        // Arrange
        val state = RichTextState(RichString("Check @dev please"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "dev",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
            )
        val color = RgbaColor(0xFFFF0000)

        // Act 1: Apply completion with TextColor and Bold
        state.applyCompletion(
            match = match,
            replacement = "@Alice ",
            attributes = attributeContainerOf(BoldKey to Unit, TextColorKey to color),
        )

        state.richString.text shouldBe "Check @Alice  please"
        state.richString.spans.any { it.attributes.containsKey(TextColorKey) }.shouldBeTrue()

        // Act 2: Undo
        state.undoState.undo()
        state.richString.text shouldBe "Check @dev please"
        state.richString.spans.any { it.attributes.containsKey(TextColorKey) }.shouldBeFalse()

        // Act 3: Redo
        state.undoState.redo()
        state.richString.text shouldBe "Check @Alice  please"
        val redSpans = state.richString.spans.filter { it.attributes[TextColorKey] == color }
        redSpans.size shouldBe 1
        redSpans.first().range shouldBe 6..12
    }

    // endregion

    // region 4. PopupPositionProvider Extreme Coordinates

    @Test
    fun `createPopupPositionProvider handles null cursorRect fallback gracefully`() {
        // When cursorRect is null, it should fall back to anchorBounds top-left and height
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "test",
                range = 0..4,
                queryRange = 1..4,
                cursorPosition = 5,
                cursorRect = null,
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(left = 50, top = 100, right = 400, bottom = 300)
        val windowSize = IntSize(width = 1000, height = 1000)
        val popupSize = IntSize(width = 200, height = 100)

        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // cursorLeft = 0, cursorBottom = anchorBounds.height (200)
        // windowCursorLeft = 50 + 0 = 50
        // windowCursorBottom = 100 + 200 = 300
        // spaceBelow = 1000 - 300 = 700 >= 100
        position.x shouldBe 50
        position.y shouldBe 300
    }

    @Test
    fun `createPopupPositionProvider clamps properly when popup is larger than window`() {
        // Extreme edge case: Popup content size exceeds entire window dimensions
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "overflow",
                range = 0..8,
                queryRange = 1..8,
                cursorPosition = 9,
                cursorRect = Rect(10f, 10f, 12f, 25f),
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(0, 0, 100, 100)
        val smallWindow = IntSize(width = 200, height = 200)
        val hugePopup = IntSize(width = 400, height = 400) // bigger than window

        val position = provider.calculatePosition(anchorBounds, smallWindow, LayoutDirection.Ltr, hugePopup)

        // maxX = (200 - 400).coerceAtLeast(0) = 0 -> clamped to 0
        // maxY = (200 - 400).coerceAtLeast(0) = 0 -> clamped to 0
        position.x shouldBe 0
        position.y shouldBe 0
    }

    @Test
    fun `createPopupPositionProvider clamps X to 0 when anchor is off-screen negative`() {
        // Anchor bounds partially scrolled off-screen left
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "offscreen",
                range = 0..9,
                queryRange = 1..9,
                cursorPosition = 10,
                cursorRect = Rect(5f, 10f, 7f, 25f),
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(left = -50, top = 20, right = 200, bottom = 200)
        val windowSize = IntSize(width = 800, height = 800)
        val popupSize = IntSize(width = 150, height = 100)

        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // windowCursorLeft = -50 + 5 = -45 -> clamped to 0
        position.x shouldBe 0
        position.y shouldBe 45 // 20 + 25 = 45
    }

    @Test
    fun `createPopupPositionProvider chooses side with more space when neither fits fully`() {
        // Space below is 50px, space above is 70px, popup requires 80px
        // Neither fits, but spaceAbove (70) > spaceBelow (50), so it flips above and clamps
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "tight",
                range = 0..5,
                queryRange = 1..5,
                cursorPosition = 6,
                cursorRect = Rect(10f, 70f, 12f, 90f),
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(0, 0, 200, 140)
        val windowSize = IntSize(width = 500, height = 140)
        val popupSize = IntSize(width = 100, height = 80)

        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // windowCursorTop = 70, windowCursorBottom = 90
        // spaceBelow = 140 - 90 = 50 < 80
        // spaceAbove = 70
        // spaceBelow (50) >= spaceAbove (70) is FALSE -> flip above: windowCursorTop (70) - popupHeight (80) = -10
        // maxY = 140 - 80 = 60
        // clampedY = (-10).coerceIn(0, 60) = 0
        position.y shouldBe 0
    }

    // endregion
}
