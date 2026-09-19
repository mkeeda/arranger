# Module :richtext-editor API Reference

The `:richtext-editor` module delivers the Compose Multiplatform user interface layer for Arranger. Built on Compose Foundation (`BasicTextField` and `TextFieldState`), it provides full-featured rich text editing canvases, robust bidirectional state synchronization, typing attribute reservations, atomic undo/redo history, dynamic autocompletion menus, span click dispatching, and toolbar convenience APIs.

---

## Interactive Dokka KDoc

For detailed method signatures, default parameters, and composable configurations, view the interactive Dokka documentation:

👉 **[Module :richtext-editor Dokka KDoc](https://mkeeda.github.io/arranger/api/dokka/richtext-editor/)**

---

## Coordinates & Dependency

```kotlin
// build.gradle.kts (commonMain)
implementation("dev.mkeeda.arranger:arranger-richtext-editor:0.4.0-alpha03")
```

---

## Composable Editors

Arranger provides two high-level editor composables sharing identical parameter surfaces:

### `RichTextEditor`
The standard, highly customizable rich text canvas. Formatting is driven programmatically via toolbar buttons, shortcuts, or external events.

```kotlin
@Composable
fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null,
    styleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
    listMarkerResolver: ListMarkerResolver = DefaultListMarkerResolver,
    onSpanClick: ((SpanClickEvent) -> Unit)? = null,
    autocompleteTriggers: List<AutocompleteTrigger> = emptyList(),
    onAutocompleteChange: ((AutocompleteMatch?) -> Unit)? = null,
)
```

### `WysiwygEditor`
An auto-formatting editor that intercepts keystrokes to convert standard Markdown shortcuts on-the-fly:
- Headings: typing `# ` through `###### ` at the start of a paragraph transforms the line into a heading.
- Lists: `- `, `* `, or `1. ` transforms the line into bullet or ordered lists.
- Blockquotes: `> ` transforms the line into a quotation block.
- Inline styles: typing `**bold**`, `*italic*`, `~~strike~~`, or `` `code` `` automatically styles the enclosed text.
- **Immediate Backspace Reversion**: Pressing Backspace immediately after an automatic conversion reverts the formatted text back into its raw Markdown characters.

---

## State Management (`RichTextState`)

`RichTextState` is the single source of truth for Arranger editor instances. It is annotated with `@Stable` and manages immutable `RichString` snapshots, cursor selection, typing reservation, and the undo/redo stack.

### Key Properties

- **`val richString: RichString`**: The current immutable text model with all associated formatting spans.
- **`val typingAttributes: AttributeContainer?`**: Attributes reserved for characters to be typed next. When the cursor is collapsed and a formatting button (e.g. Bold) is toggled, the format is staged here. Moving the cursor to a new location without typing automatically clears these reservations.
- **`val currentAttributes: AttributeContainer`**: Returns the attributes active at the current selection. For collapsed cursors, this reflects the formatting at the cursor. For non-collapsed selections (text ranges), this computes the **mathematical intersection** of attributes—only attributes present across 100% of the selected characters are included.
- **`val selection: TextRange`**: The current cursor index or selection range.
- **`val undoState: RichTextUndoState`**: Controls atomic undo/redo history (`canUndo`, `canRedo`, `undo()`, `redo()`, `clearHistory()`). Consecutive typing of single characters is coalesced into single undo snapshots, while deletes, pastes, and formatting changes create distinct boundary snapshots.

### Primary Methods

```kotlin
// Batch editing via mutable buffer
state.edit {
    insert(0, "Header\n")
    editAttributes(0..5) { headingLevel(HeadingLevel.H1) }
}

// Resetting the editor content
state.setRichString(newRichString) // Resets undo stack, clamps selection, clears typing attrs
```

---

## High-Level Formatting APIs (`RichTextStateFormatExt`)

Instead of manually computing ranges inside `state.edit { }`, use the extension functions in `RichTextStateFormatExt.kt` to build toolbars in seconds:

```kotlin
// Toggles boolean formatting (applies to selection if selected, or typingAttributes if collapsed)
state.toggleFormat(BoldKey)
state.toggleFormat(ItalicKey)

// Applies or removes value-bearing formats
state.applyFormat(HeadingKey, HeadingLevel.H2)
state.applyFormat(TextColorKey, Color.Red.toRgbaColor())
state.removeFormat(HeadingKey)

// Clears all formatting across selection or typing attributes
state.clearFormats()

// Scans text for URLs and applies LinkKey automatically
state.detectAndApplyLinks()
```

!!! tip "Preventing Focus Loss on Toolbar Clicks"
    Always apply `Modifier.focusProperties { canFocus = false }` to toolbar buttons and icons. This prevents Compose from transferring focus away from the editor text field when buttons are tapped:
    ```kotlin
    IconButton(
        onClick = { state.toggleFormat(BoldKey) },
        modifier = Modifier.focusProperties { canFocus = false }
    ) {
        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
    }
    ```

---

## Interactive Features

### Autocomplete (`AutocompleteTrigger` & `AutocompleteMatch`)
Supports `@mention`, `#tag`, or custom trigger autocompletion:

```kotlin
val mentionTrigger = AutocompleteTrigger(
    character = '@',
    requireLeadingWhitespace = true,
    allowSpacesInQuery = false,
    maxQueryLength = 50,
)
```

- **`detectAutocomplete(text, cursor, triggers)`**: Scans for active trigger matches.
- **`createPopupPositionProvider(offset)`**: Returns a `PopupPositionProvider` that positions suggestion menus directly adjacent to the cursor, with automatic boundary clamping and upward flipping if screen space below is insufficient.
- **`state.applyCompletion(match, replacement, attributes)`**: Atomically replaces the query range with the completed text and optional styling.

### Span Clicks (`SpanClickEvent`)
Intercepts pointer taps on formatted spans (e.g. hyperlinks or hashtags):

```kotlin
RichTextEditor(
    state = state,
    onSpanClick = { event ->
        val url = event.span.attributes[LinkKey]
        if (url != null) {
            event.consume() // Prevents cursor repositioning and keyboard focus shifts
            uriHandler.openUri(url)
        }
    }
)
```

---

## Style & List Resolvers

- **`AttributeStyleResolver`**: Functional interface mapping an `AttributeContainer` to Compose styles:
  ```kotlin
  fun interface AttributeStyleResolver {
      fun resolve(attributes: AttributeContainer): ResolvedRichStyle
  }
  ```
- **`DefaultAttributeStyleResolver`**: Provides standard rendering for bold, italic, underline, strikethrough, monospace inline code, colors, font sizes, headings, and blockquotes.
- **`ListMarkerResolver`**: Resolves visual bullet characters or numeric prefixes (`1.`, `2.`) for list items:
  ```kotlin
  fun interface ListMarkerResolver {
      fun resolve(item: ListItem): String
  }
  ```
  `DefaultListMarkerResolver` renders `•` for Level 1, `○` for Level 2, `▪` for Level 3+, and `${index}.` for ordered lists.
