package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AutocompleteTest {
    // region detectAutocomplete tests

    @Test
    fun `detectAutocomplete matches trigger at beginning of document`() {
        // Arrange
        val text = "@"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 1, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@"
        match.query shouldBe ""
        match.range shouldBe 0..0
        match.queryRange shouldBe 1..0
        match.cursorPosition shouldBe 1
    }

    @Test
    fun `detectAutocomplete matches trigger at line start`() {
        // Arrange
        val text = "Hello\n@"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 7, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@"
        match.query shouldBe ""
        match.range shouldBe 6..6
        match.queryRange shouldBe 7..6
        match.cursorPosition shouldBe 7
    }

    @Test
    fun `detectAutocomplete matches trigger after whitespace`() {
        // Arrange
        val text = "Hello @"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 7, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@"
        match.query shouldBe ""
        match.range shouldBe 6..6
        match.queryRange shouldBe 7..6
        match.cursorPosition shouldBe 7
    }

    @Test
    fun `detectAutocomplete fails in middle of word when requireLeadingWhitespace is true`() {
        // Arrange
        val text = "email@test.com"
        val triggers = listOf(AutocompleteTrigger(prefix = "@", requireLeadingWhitespace = true))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 6, triggers = triggers)

        // Assert
        match.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete succeeds in middle of word when requireLeadingWhitespace is false`() {
        // Arrange
        val text = "email@test.com"
        val triggers = listOf(AutocompleteTrigger(prefix = "@", requireLeadingWhitespace = false))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 10, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@"
        match.query shouldBe "test"
        match.range shouldBe 5..9
        match.queryRange shouldBe 6..9
    }

    @Test
    fun `detectAutocomplete matches query during typing`() {
        // Arrange
        val text = "Hello @ali"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 10, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "@"
        match.query shouldBe "ali"
        match.range shouldBe 6..9
        match.queryRange shouldBe 7..9
        match.cursorPosition shouldBe 10
    }

    @Test
    fun `detectAutocomplete terminates when space is typed and allowSpacesInQuery is false`() {
        // Arrange
        val text = "Hello @ali "
        val triggers = listOf(AutocompleteTrigger(prefix = "@", allowSpacesInQuery = false))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 11, triggers = triggers)

        // Assert
        match.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete allows spaces when allowSpacesInQuery is true`() {
        // Arrange
        val text = "Hello @ali "
        val triggers = listOf(AutocompleteTrigger(prefix = "@", allowSpacesInQuery = true))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 11, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.query shouldBe "ali "
        match.range shouldBe 6..10
        match.queryRange shouldBe 7..10
    }

    @Test
    fun `detectAutocomplete prioritizes the nearest trigger before cursor`() {
        // Arrange
        val text = "@alice #tag"
        val triggers =
            listOf(
                AutocompleteTrigger(prefix = "@"),
                AutocompleteTrigger(prefix = "#"),
            )

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 11, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.trigger.prefix shouldBe "#"
        match.query shouldBe "tag"
        match.range shouldBe 7..10
        match.queryRange shouldBe 8..10
    }

    @Test
    fun `detectAutocomplete returns null when query exceeds maxQueryLength`() {
        // Arrange
        val text = "@" + "a".repeat(11)
        val triggers = listOf(AutocompleteTrigger(prefix = "@", maxQueryLength = 10))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = text.length, triggers = triggers)

        // Assert
        match.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete does not match across newline`() {
        // Arrange
        val text = "@alice\nbob"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))

        // Act
        val match = detectAutocomplete(text = text, cursorPosition = 10, triggers = triggers)

        // Assert
        match.shouldBeNull()
    }

    @Test
    fun `detectAutocomplete reflects cursor repositioning inside query`() {
        // Arrange
        val text = "@alice"
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))

        // Act: Cursor at index 3 ("@al|ice")
        val match = detectAutocomplete(text = text, cursorPosition = 3, triggers = triggers)

        // Assert
        match.shouldNotBeNull()
        match.query shouldBe "al"
        match.range shouldBe 0..2
        match.queryRange shouldBe 1..2
    }

    @Test
    fun `detectAutocomplete returns null for invalid cursor positions or empty triggers`() {
        val triggers = listOf(AutocompleteTrigger(prefix = "@"))
        detectAutocomplete("Hello", 0, triggers).shouldBeNull()
        detectAutocomplete("Hello", -1, triggers).shouldBeNull()
        detectAutocomplete("Hello", 10, triggers).shouldBeNull()
        detectAutocomplete("@test", 2, emptyList()).shouldBeNull()
    }

    // endregion

    // region applyCompletion tests

    @Test
    fun `applyCompletion with plain text replaces range and sets cursor at end`() {
        // Arrange
        val state = RichTextState(RichString("Hello @ali, welcome"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
            )

        // Act
        state.applyCompletion(match, replacement = "Alice Smith ")

        // Assert
        state.richString.text shouldBe "Hello Alice Smith , welcome"
        state.selection.start shouldBe 18
        state.selection.end shouldBe 18
    }

    @Test
    fun `applyCompletion with RichString applies styling and shifts spans`() {
        // Arrange
        val state = RichTextState(RichString("Hello @ali"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
            )
        val replacement =
            RichString("@Alice Smith").edit {
                setSpanAttribute(BoldKey, Unit, 0..11)
            }

        // Act
        state.applyCompletion(match, replacement = replacement)

        // Assert
        state.richString.text shouldBe "Hello @Alice Smith"
        state.selection.start shouldBe 18
        val boldSpans = state.richString.spans.filter { it.attributes.containsKey(BoldKey) }
        boldSpans.size shouldBe 1
        boldSpans.first().range shouldBe 6..17
    }

    @Test
    fun `applyCompletion with attributes applies attribute container to replacement`() {
        // Arrange
        val state = RichTextState(RichString("Hello @ali"))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
            )

        // Act
        state.applyCompletion(match, replacement = "@Alice", attributes = attributeContainerOf(BoldKey to Unit))

        // Assert
        state.richString.text shouldBe "Hello @Alice"
        state.selection.start shouldBe 12
        val boldSpans = state.richString.spans.filter { it.attributes.containsKey(BoldKey) }
        boldSpans.size shouldBe 1
        boldSpans.first().range shouldBe 6..11
    }

    @Test
    fun `applyCompletion supports Undo and Redo operations`() {
        // Arrange
        val initialText = "Hello @ali"
        val state = RichTextState(RichString(initialText))
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
            )

        // Act: Apply completion
        state.applyCompletion(match, replacement = "Alice ")
        state.richString.text shouldBe "Hello Alice "

        // Act: Undo
        state.undoState.canUndo.shouldBeTrue()
        state.undoState.undo()

        // Assert: Reverted back to initial state
        state.richString.text shouldBe initialText
        state.selection.start shouldBe 10

        // Act: Redo
        state.undoState.canRedo.shouldBeTrue()
        state.undoState.redo()

        // Assert: Restored to completed state
        state.richString.text shouldBe "Hello Alice "
        state.selection.start shouldBe 12
    }

    // endregion

    // region createPopupPositionProvider tests

    @Test
    fun `createPopupPositionProvider positions popup below cursor when space is sufficient`() {
        // Arrange
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
                cursorRect = Rect(left = 50f, top = 100f, right = 52f, bottom = 120f),
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(left = 10, top = 20, right = 400, bottom = 500)
        val windowSize = IntSize(width = 1000, height = 1000)
        val popupSize = IntSize(width = 200, height = 150)

        // Act
        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // Assert
        // windowCursorLeft = 10 + 50 = 60
        // windowCursorBottom = 20 + 120 = 140
        // spaceBelow = 1000 - 140 = 860 >= 150 -> places below cursor at 140
        position.x shouldBe 60
        position.y shouldBe 140
    }

    @Test
    fun `createPopupPositionProvider flips popup above cursor when space below is insufficient`() {
        // Arrange
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
                cursorRect = Rect(left = 50f, top = 800f, right = 52f, bottom = 820f),
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(left = 0, top = 0, right = 500, bottom = 900)
        val windowSize = IntSize(width = 1000, height = 900)
        val popupSize = IntSize(width = 200, height = 150)

        // Act
        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // Assert
        // windowCursorBottom = 0 + 820 = 820
        // spaceBelow = 900 - 820 = 80 < 150
        // spaceAbove = 800 >= 150
        // Placed above: windowCursorTop (800) - popupHeight (150) = 650
        position.x shouldBe 50
        position.y shouldBe 650
    }

    @Test
    fun `createPopupPositionProvider clamps X to window bounds when overflowing right`() {
        // Arrange
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
                cursorRect = Rect(left = 750f, top = 100f, right = 752f, bottom = 120f),
            )
        val provider = match.createPopupPositionProvider()
        val anchorBounds = IntRect(left = 100, top = 0, right = 900, bottom = 500)
        val windowSize = IntSize(width = 800, height = 600)
        val popupSize = IntSize(width = 200, height = 100)

        // Act
        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // Assert
        // windowCursorLeft = 100 + 750 = 850
        // maxX = 800 - 200 = 600 -> clamped to 600
        position.x shouldBe 600
    }

    @Test
    fun `createPopupPositionProvider applies extra offset`() {
        // Arrange
        val match =
            AutocompleteMatch(
                trigger = AutocompleteTrigger("@"),
                query = "ali",
                range = 6..9,
                queryRange = 7..9,
                cursorPosition = 10,
                cursorRect = Rect(left = 50f, top = 100f, right = 52f, bottom = 120f),
            )
        val provider = match.createPopupPositionProvider(offset = IntOffset(x = 10, y = 15))
        val anchorBounds = IntRect(left = 0, top = 0, right = 500, bottom = 500)
        val windowSize = IntSize(width = 1000, height = 1000)
        val popupSize = IntSize(width = 200, height = 100)

        // Act
        val position = provider.calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        // Assert
        // x = 50 + 10 = 60
        // y = 120 + 15 = 135
        position.x shouldBe 60
        position.y shouldBe 135
    }

    // endregion
}
