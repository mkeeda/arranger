# Autocomplete & Mentions

Arranger provides a built-in autocomplete engine designed specifically for rich text editors. It enables effortless implementation of:

- **@mentions** for team members and users
- **#hashtags** and channels
- **:emoji:** shortcodes
- Custom slash-commands (such as `/h1`, `/bullet`, or `/code`)

The engine provides full trigger detection, query extraction, atomic completion replacement, and **zero-calculation cursor popup positioning** that accounts for scrolling, keyboard insets, and window constraints.

---

## Visual Demos

### Mention Autocomplete

Typing `@` followed by a user's name displays an elevated popup positioned directly below the active cursor. Selecting a user inserts a stylized mention badge.

![Mention Autocomplete](../images/mention-autocomplete.png){ width="500" }

### Hashtag Trigger

Hashtag detection works in real time, highlighting matching tags and channels as you type.

![Hashtag Highlight](../images/hashtag-highlight.gif){ width="500" }

---

## AutocompleteTrigger Configuration

Define triggers using `AutocompleteTrigger`:

```kotlin
package dev.mkeeda.arranger.richtext.editor

@Immutable
public data class AutocompleteTrigger(
    val prefix: String,
    val id: String = prefix,
    val requireLeadingWhitespace: Boolean = true,
    val allowSpacesInQuery: Boolean = false,
    val maxQueryLength: Int = 50,
) {
    public constructor(
        character: Char,
        id: String = character.toString(),
        requireLeadingWhitespace: Boolean = true,
        allowSpacesInQuery: Boolean = false,
        maxQueryLength: Int = 50,
    )
}
```

### Trigger Parameters

- `prefix`: The triggering character or string (e.g. `"@"` or `"#"`).
- `id`: Optional unique identifier when multiple triggers share characters.
- `requireLeadingWhitespace`: When `true` (default), prevents false triggers inside words (for example, `user@example.com` will **not** trigger mention autocomplete).
- `allowSpacesInQuery`: When `false` (default), typing a space dismisses the autocomplete popup. Set to `true` for queries with spaces, such as full names (`@John Doe`).
- `maxQueryLength`: Upper limit on query length (default `50`). Typing beyond this limit automatically dismisses the popup.

---

## AutocompleteMatch & Detection

Arranger continuously analyzes the cursor position via `detectAutocomplete`:

```kotlin
public fun detectAutocomplete(
    text: CharSequence,
    cursorPosition: Int,
    triggers: List<AutocompleteTrigger>,
): AutocompleteMatch?
```

When an active trigger matches, Arranger produces an `AutocompleteMatch`:

```kotlin
@Immutable
public data class AutocompleteMatch(
    val trigger: AutocompleteTrigger,
    val query: String,
    val range: IntRange,        // Includes prefix and query: e.g. 5..10 for "@alice"
    val queryRange: IntRange,   // Query only: e.g. 6..10 for "alice"
    val cursorPosition: Int,
    val cursorRect: Rect? = null, // Viewport-relative cursor bounding box
)
```

---

## Zero-Calculation Cursor Popup Positioning

Positioning popups near an editor cursor usually requires tedious calculation of scroll offsets, window boundaries, and flip logic.

Arranger handles all of this automatically via `createPopupPositionProvider()`:

```kotlin
public fun AutocompleteMatch.createPopupPositionProvider(
    offset: IntOffset = IntOffset.Zero,
): PopupPositionProvider
```

### How It Works Under the Hood

1. Reads `match.cursorRect` obtained from Compose text layout.
2. Automatically offsets against editor scroll state.
3. Automatically flips the popup above the cursor if there is insufficient vertical space below.
4. Clamps the horizontal coordinate to prevent overflowing the left or right edges of the window.

```kotlin
match?.let { currentMatch ->
    Popup(
        popupPositionProvider = currentMatch.createPopupPositionProvider(
            offset = IntOffset(x = 0, y = 8) // Optional extra margin
        ),
        onDismissRequest = { autocompleteMatch = null },
    ) {
        SuggestionPopupContent(match = currentMatch)
    }
}
```

---

## Applying Completions

When the user selects a suggestion, apply the completion atomically via `RichTextState.applyCompletion`:

### 1. Plain Text Replacement

Replaces the trigger and query range with plain text and advances the cursor:

```kotlin
state.applyCompletion(match, replacement = "@Alice ")
```

### 2. Formatted Replacement with Attributes

Replaces the trigger with styled text (for example, a bold and colored mention badge):

```kotlin
state.applyCompletion(
    match = match,
    replacement = "@${user.name} ",
    attributes = attributeContainerOf(
        BoldKey to Unit,
        TextColorKey to user.color.toRgbaColor(),
    )
)
```

### 3. RichString Replacement

Replaces the match with a full `RichString` structure:

```kotlin
state.applyCompletion(match, replacement = richMentionString)
```

Each completion automatically records an isolated Undo step (`UndoMergePolicy.Separate`), allowing users to revert the completion with a single Undo command.

---

## Complete Working Autocomplete Example

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.editor.AutocompleteMatch
import dev.mkeeda.arranger.richtext.editor.AutocompleteTrigger
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.applyCompletion
import dev.mkeeda.arranger.richtext.editor.createPopupPositionProvider
import dev.mkeeda.arranger.richtext.editor.toRgbaColor

@Composable
fun ChatEditorWithMentions() {
    val state = remember { RichTextState() }
    var activeMatch by remember { mutableStateOf<AutocompleteMatch?>(null) }

    val triggers = remember {
        listOf(
            AutocompleteTrigger(prefix = "@"),
            AutocompleteTrigger(prefix = "#"),
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        RichTextEditor(
            state = state,
            autocompleteTriggers = triggers,
            onAutocompleteChange = { match -> activeMatch = match },
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )

        activeMatch?.let { match ->
            Popup(
                popupPositionProvider = match.createPopupPositionProvider(
                    offset = IntOffset(x = 0, y = 4)
                ),
                onDismissRequest = { activeMatch = null }
            ) {
                ElevatedCard(
                    modifier = Modifier.width(240.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    if (match.trigger.prefix == "@") {
                        val candidates = listOf("Alice", "Bob", "Charlie")
                            .filter { it.contains(match.query, ignoreCase = true) }

                        LazyColumn {
                            items(candidates) { name ->
                                Text(
                                    text = "@$name",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            state.applyCompletion(
                                                match = match,
                                                replacement = "@$name ",
                                                attributes = attributeContainerOf(
                                                    BoldKey to Unit,
                                                    TextColorKey to Color(0xFF1976D2).toRgbaColor()
                                                )
                                            )
                                            activeMatch = null
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

---

## Summary

- Use `AutocompleteTrigger` to define prefixes like `@` and `#`.
- Receive real-time matches via `onAutocompleteChange`.
- Use `match.createPopupPositionProvider()` for zero-math cursor popup placement with automatic flip and edge clamping.
- Apply completions with styling via `state.applyCompletion(...)`.
