# State Management & History

State management in Arranger is centered around `@Stable class RichTextState`, strictly adhering to Compose state hoisting principles. It serves as the Single Source of Truth (SSOT), encapsulating text content, attribute spans, cursor selection, undo/redo history, and pending typing attributes.

---

## Core Properties of RichTextState

```kotlin
@Stable
class RichTextState(initialText: RichString = RichString("")) {
    // Immutable data structure holding the current text and all formatted spans
    val richString: RichString

    // Current selection range (cursor position or dragged selection range)
    val selection: TextRange

    // Pending attributes reserved for subsequent keystrokes
    val typingAttributes: AttributeContainer?

    // Effective attributes over the current selection, calculated via logical intersection
    val currentAttributes: AttributeContainer

    // Undo / Redo history management object
    val undoState: RichTextUndoState
}
```

---

## Typing Attributes Lifecycle

In word processors and rich text editors, a standard interaction is tapping the **Bold** button with no text selected, followed by typing characters that appear bold. Arranger models this behavior via `typingAttributes`.

### 1. Reservation Flow

```mermaid
sequenceDiagram
    participant User as User
    participant Toolbar as Toolbar (Bold Button)
    participant State as RichTextState
    participant Editor as Editor (BasicTextField)

    User->>Toolbar: Tap Bold button (collapsed selection)
    Toolbar->>State: toggleFormat(BoldKey)
    Note over State: Reserve BoldKey in typingAttributes
    User->>Editor: Type character 'A'
    Editor->>State: Text mutation event
    Note over State: Automatically attach BoldKey span to inserted 'A'<br/>Preserve typingAttributes
```

### 2. Attribute Exclusion Reservation (`removedTypingAttributes`)

When toggling bold OFF at the trailing boundary of an existing bold span, `removedTypingAttributes` records the exclusion reservation so subsequent keystrokes revert to unstyled regular text instead of inheriting previous styling.

### 3. Automatic Discard on Caret Relocation

!!! warning "Crucial Lifecycle Behavior"
    If the user moves the cursor (via arrow keys or tapping elsewhere on the screen) after reserving typing attributes without typing any character, **the pending typing attributes are automatically discarded**.

    The effective attributes at the new cursor position (regular text if unstyled, bold if inside a bold span) are immediately re-evaluated into `currentAttributes`.

---

## Current Attributes & Logical Intersection Logic

Determining whether toolbar buttons (such as Bold or Italic) should appear active depends on the user's current selection.

Arranger calculates `state.currentAttributes` using a strict logical intersection algorithm:

1. **Collapsed selection (`selection.collapsed == true`)**:
    - Returns `typingAttributes` if present.
    - Otherwise, returns attributes at the immediate cursor boundary.
2. **Non-collapsed selection (`!selection.collapsed`)**:
    - Returns **only attributes present across 100% of the selected characters**.

### Example Walkthrough

Consider the following formatted string:

> <span style="font-weight: bold; color: blue;">Hello</span> <span style="font-weight: bold;">World</span>

- **Selecting "Hello"**: Both `BoldKey` and `TextColorKey(Blue)` are present across all selected characters and included in `currentAttributes`.
- **Selecting "Hello World"**: Only `BoldKey` is shared across every selected character; `TextColorKey` is excluded because it does not cover "World".
- **Selecting "o W" (boundary)**: Because the intervening space has no formatting, `currentAttributes` is an empty set.

This logical intersection model ensures toolbar buttons can simply query `state.currentAttributes.containsKey(BoldKey)` without manual calculation.

---

## Atomic Batch Mutations with RichTextBuffer

To mutate text content and styling simultaneously, use `state.edit { ... }`. The scoped `RichTextBuffer` provides rich editing operations:

```kotlin
state.edit {
    // 1. Insert text with styles
    insert(index = 0, text = "Title\n") {
        bold()
        headingLevel(HeadingLevel.H1)
    }

    // 2. Replace text range
    replace(range = 10..15, text = "replacement text") {
        italic()
    }

    // 3. Delete text (spans are automatically trimmed/shifted)
    delete(range = 20..25)

    // 4. Apply attributes across existing range
    editAttributes(range = 0 until textLength) {
        textColor(Color.DarkGray)
    }
}
```

Upon exiting the `edit` block, all mutations are committed in a single snapshot, triggering Compose recomposition atomically.

---

## Undo & Redo History (RichTextUndoState)

Arranger includes a built-in undo/redo engine that tracks both plain text modifications and attribute span mutations.

<div align="center" markdown>

![Undo Redo Demo](../images/undo-redo.gif){ width="380" }

</div>

### Primary APIs

- `state.undoState.canUndo`: Indicates whether undoable history exists (bind to button `enabled` state).
- `state.undoState.canRedo`: Indicates whether redoable history exists.
- `state.undoState.undo()`: Reverts the most recent operation.
- `state.undoState.redo()`: Reapplies the previously reverted operation.
- `state.undoState.clearHistory()`: Clears the undo/redo history stacks.

### Wiring Toolbar Buttons

```kotlin
Row {
    IconButton(
        onClick = { state.undoState.undo() },
        enabled = state.undoState.canUndo,
        modifier = Modifier.focusProperties { canFocus = false },
    ) {
        Icon(Icons.Default.Undo, contentDescription = "Undo")
    }

    IconButton(
        onClick = { state.undoState.redo() },
        enabled = state.undoState.canRedo,
        modifier = Modifier.focusProperties { canFocus = false },
    ) {
        Icon(Icons.Default.Redo, contentDescription = "Redo")
    }
}
```

### Keyboard Shortcuts

Native keyboard shortcuts (<kbd>Cmd/Ctrl</kbd> + <kbd>Z</kbd> and <kbd>Shift</kbd> + <kbd>Cmd/Ctrl</kbd> + <kbd>Z</kbd>) are automatically handled by the editor component.

### Intelligent Mutation Coalescing

Creating a separate undo entry for every single keystroke forces users to press undo dozens of times just to revert a single word. Arranger coalesces mutations automatically:

- **Continuous alphanumeric typing**: Coalesced into a single undo step.
- **Whitespace, newlines (Enter), Backspace, and paste events**: Discrete operation boundaries that split coalescing batches.
- **History stack capacity**: Retains the 100 most recent operations to bound memory usage.

---

## Loading External Documents (`setRichString`)

To replace the entire editor content when loading documents or creating new files, use `setRichString`.

```kotlin
val newContent = RichString("New document loaded from external source")
state.setRichString(newContent)
```

Calling `setRichString` safely executes the following operations:

1. Replaces the text and all spans with the new content.
2. Clears pending `typingAttributes`.
3. Resets the undo/redo history stack.
4. Automatically clamps the cursor position to the end of the new text or within valid bounds.

---

## State Preservation Across Configuration Changes & Process Death

Mobile and multiplatform environments frequently recreate UI components due to screen rotation, split-screen resizing (Configuration Changes), or operating system memory reclamation (Process Death).

### 1. Composable Scope: `rememberRichTextState`

For standard text editing, use `rememberRichTextState()`. It automatically registers with Compose's `rememberSaveable` and uses `RichTextState.Saver` to persist and restore editor state out of the box:

```kotlin
@Composable
fun NoteEditorScreen() {
    // Automatically survives configuration changes and process death
    val state = rememberRichTextState(
        initialText = RichString(text = "Hello Arranger!"),
    )

    RichTextEditor(state = state)
}
```

#### What is Saved vs. Excluded

| Data Item | Preserved | Rationale / Behavior |
| :--- | :---: | :--- |
| `text` | :white_check_mark: | Raw text string is completely restored. |
| `selection` | :white_check_mark: | Cursor position and range selection are restored. |
| `spans` | :white_check_mark: | All built-in attributes (bold, italic, colors, headings, lists, links, etc.) are serialized. Custom attributes use registered serializers. |
| `typingAttributes` | :white_check_mark: | Pending keyboard styles reserved at cursor are maintained. |
| `undoState` | :x: | **Intentionally excluded**. Storing dozens of full document snapshots causes Android Bundle size explosions (`TransactionTooLargeException`). Restored instances reset with a clean, safe history (matching standard `EditText` and Compose `rememberTextFieldState`). |

---

### 2. Large Document Scope: ViewModel Hoisting Pattern

For complex editors, long-form documents, or applications where **undo/redo history must survive configuration changes**, hoist `RichTextState` directly in a `ViewModel`:

```kotlin
class EditorViewModel : ViewModel() {
    // Hoisted in memory: surviving configuration changes with 100% of undo history intact
    val state = RichTextState(
        initialText = RichString(text = "Long document content..."),
    )

    fun onSaveDocument() {
        val currentContent = state.richString
        // Persist to local database, file storage, or cloud API
    }
}

@Composable
fun DocumentEditorScreen(viewModel: EditorViewModel = viewModel()) {
    // UI simply observes and controls the ViewModel-held state
    RichTextEditor(state = viewModel.state)
}
```

!!! tip "Handling Process Death for Long Documents"
    Android Bundles are limited to ~1MB across the entire application. When dealing with large articles or books, do not rely on `SavedStateHandle` or `Bundle` for full text storage. Instead, implement a debounce auto-save pattern that periodically serializes `state.richString` into a SQLite database (e.g. Room) or local disk.

---

### 3. Custom Attribute Serialization (`AttributeSerializer`)

If you define custom `AttributeKey<T>` types, register them with `AttributeSerializer` so `RichTextState.Saver` knows how to serialize and restore your custom values:

```kotlin
data class NoteAnnotation(val comment: String)

val NoteAnnotationKey = object : SpanAttributeKey<NoteAnnotation> {
    override val name: String = "noteAnnotation"
    override val defaultValue: NoteAnnotation = NoteAnnotation(comment = "")
}

// 1. Define custom serializer
val noteSerializer = attributeSerializer(
    key = NoteAnnotationKey,
    save = { note -> note.comment },
    restore = { saved -> NoteAnnotation(comment = saved as String) },
)

// 2. Create custom Saver
val customSaver = RichTextState.saver(
    customSerializers = listOf(noteSerializer),
)

// 3. Use in Composable
@Composable
fun CustomEditorScreen() {
    val state = rememberRichTextState(
        saver = customSaver,
    )

    RichTextEditor(state = state)
}
```

---

## Related Documentation

- [**RichTextEditor Basics**](rich-text-editor.md): Editor component placement and UI parameters.
- [**Spans and Paragraphs**](../styling/spans-and-paragraphs.md): Differences between span and paragraph attributes.
- [**Toolbar Integration**](../interactions/toolbars.md): High-level helper APIs such as `toggleFormat` and focus protection tips.

