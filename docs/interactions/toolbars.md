# Toolbars & Focus Management

Building a rich text formatting toolbar in Jetpack Compose requires solving two key engineering challenges:

1. **Focus Stealing Prevention**: By default in Compose, tapping any `Button` or `IconButton` requests focus, immediately stealing focus away from the editor, dismissing the software keyboard, or collapsing the active text selection.
2. **State Synchronization**: High-level commands must seamlessly toggle formatting on selections when text is highlighted, or apply typing attributes when the cursor is collapsed.

Arranger provides the `RichTextStateFormatExt` extension suite and recommended Compose focus management patterns to build professional formatting toolbars with minimal boilerplate.

---

## The Focus Stealing Problem & Modifier.focusProperties

When a user taps a "Bold" button on a toolbar:

1. Compose's default pointer focus pipeline attempts to transfer focus to the button.
2. The editor loses focus.
3. The software keyboard closes, and the selection range collapses.

### The Solution: canFocus = false

To prevent toolbar buttons from ever requesting focus, apply `Modifier.focusProperties { canFocus = false }` to every interactive toolbar element:

```kotlin
val unfocusableModifier = Modifier.focusProperties { canFocus = false }

IconButton(
    onClick = { state.toggleFormat(BoldKey) },
    modifier = unfocusableModifier,
) {
    Icon(painter = painterResource(Res.drawable.format_bold), contentDescription = "Bold")
}
```

This guarantees that:

- The cursor remains active in the editor.
- The software keyboard stays open.
- The active text selection is preserved.

---

## High-Level Formatting APIs (RichTextStateFormatExt)

The `:richtext-editor` module provides high-level convenience extensions on `RichTextState`:

```kotlin
package dev.mkeeda.arranger.richtext.editor
```

### 1. toggleFormat

Toggles Unit-based span or paragraph attributes. If text is selected, the format is toggled across the selection. If the cursor is collapsed, the format is toggled in `typingAttributes`:

```kotlin
// Toggle Bold, Italic, Underline, Strikethrough, or Blockquote
state.toggleFormat(BoldKey)
state.toggleFormat(ItalicKey)
state.toggleFormat(BlockquoteKey)
```

### 2. applyFormat & removeFormat

Applies or removes parameterized attributes (such as colors, font sizes, heading levels, or list levels):

```kotlin
// Apply Heading 1
state.applyFormat(HeadingKey, HeadingLevel.H1)

// Apply custom text color
state.applyFormat(TextColorKey, Color.Red.toRgbaColor())

// Remove Heading
state.removeFormat(HeadingKey)
```

### 3. clearFormats

Instantly removes all styling from the selected text or clears typing attributes:

```kotlin
state.clearFormats()
```

### 4. detectAndApplyLinks

Scans the document text with `UrlParser` and automatically converts all discovered web URLs into `LinkKey` spans:

```kotlin
state.detectAndApplyLinks()
```

---

## Complete Working Compose Toolbar Example

Here is a complete, production-ready document editor with a formatting toolbar including Undo, Redo, Bold, Italic, Heading, List controls, and Clear Formatting:

![Document Editor](../images/document-editor.png){ width="600" }

```kotlin
@Composable
fun DocumentEditorScreen() {
    val state = remember { RichTextState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        DocumentToolbar(state = state)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            RichTextEditor(
                state = state,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun DocumentToolbar(state: RichTextState, modifier: Modifier = Modifier) {
    // Crucial: Prevent all buttons from stealing focus from the editor!
    val unfocusable = Modifier.focusProperties { canFocus = false }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Undo / Redo
        IconButton(
            onClick = { state.undoState.undo() },
            enabled = state.undoState.canUndo,
            modifier = unfocusable
        ) {
            Text("↶")
        }
        IconButton(
            onClick = { state.undoState.redo() },
            enabled = state.undoState.canRedo,
            modifier = unfocusable
        ) {
            Text("↷")
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Bold Toggle
        IconToggleButton(
            checked = state.currentAttributes.containsKey(BoldKey),
            onCheckedChange = { state.toggleFormat(BoldKey) },
            modifier = unfocusable
        ) {
            Text("B", style = MaterialTheme.typography.titleMedium)
        }

        // Italic Toggle
        IconToggleButton(
            checked = state.currentAttributes.containsKey(ItalicKey),
            onCheckedChange = { state.toggleFormat(ItalicKey) },
            modifier = unfocusable
        ) {
            Text("I", style = MaterialTheme.typography.titleMedium)
        }

        // Heading 1 Toggle
        IconToggleButton(
            checked = state.currentAttributes[HeadingKey] == HeadingLevel.H1,
            onCheckedChange = {
                if (state.currentAttributes[HeadingKey] == HeadingLevel.H1) {
                    state.removeFormat(HeadingKey)
                } else {
                    state.applyFormat(HeadingKey, HeadingLevel.H1)
                }
            },
            modifier = unfocusable
        ) {
            Text("H1")
        }

        // Bullet List Toggle
        IconToggleButton(
            checked = state.currentAttributes.containsKey(BulletListKey),
            onCheckedChange = {
                if (state.currentAttributes.containsKey(BulletListKey)) {
                    state.removeFormat(BulletListKey)
                } else {
                    state.applyFormat(BulletListKey, ListIndentLevel.Level1)
                }
            },
            modifier = unfocusable
        ) {
            Text("• List")
        }

        // Clear All Formats
        IconButton(
            onClick = { state.clearFormats() },
            modifier = unfocusable
        ) {
            Text("✕ Clear")
        }
    }
}
```

---

## Summary

- Always add `Modifier.focusProperties { canFocus = false }` to toolbar buttons to prevent focus loss, keyboard closing, and selection collapse.
- Use `state.toggleFormat()` for Unit-based attributes (`BoldKey`, `ItalicKey`, `UnderlineKey`, etc.).
- Use `state.applyFormat()` and `state.removeFormat()` for parameterized attributes (`TextColorKey`, `HeadingKey`, `BulletListKey`, etc.).
- Use `state.detectAndApplyLinks()` to auto-link URLs in document text.
