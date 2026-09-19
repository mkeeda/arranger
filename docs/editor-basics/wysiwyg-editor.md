# WysiwygEditor

`WysiwygEditor` is a real-time WYSIWYG editor component that detects Markdown formatting syntax on the fly and immediately converts it into rich text styles. Like Notion, Bear, or Slack, users can format documents effortlessly without lifting their hands from the keyboard.

<div align="center" markdown>

![WysiwygEditor Demo](../images/wysiwyg.gif){ width="500" }

</div>

---

## Comparison: RichTextEditor vs. WysiwygEditor

| Feature | `RichTextEditor` | `WysiwygEditor` |
|---|---|---|
| **Primary Use Case** | Explicit formatting via toolbars, format buttons, and menus | Real-time automatic formatting via keyboard Markdown shortcuts |
| **Automatic Markdown Conversion** | None (entered characters remain literal plain text) | Enabled (`# ` or `**bold**` convert immediately into styled spans) |
| **Backspace Reversal** | None (deletes characters one by one) | Enabled (pressing Backspace immediately after conversion restores raw Markdown syntax) |
| **API Signature** | 18 common parameters | Identical parameter set and signature to `RichTextEditor` |

---

## Basic Usage

Like `RichTextEditor`, simply provide a `RichTextState` to run `WysiwygEditor`.

```kotlin
@Composable
fun SimpleWysiwygScreen() {
    val state = remember { RichTextState() }

    WysiwygEditor(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    )
}
```

---

## Supported Markdown Shortcuts

When typing, entering the following syntax triggers immediate conversion as soon as the delimiter (space, closing symbol) is entered:

### Block Formatting (Line Prefix)

| Shortcut | Formatted Style | Attribute Key / Value |
|---|---|---|
| `# ` (Line start + space) | Heading 1 | `HeadingKey` (`HeadingLevel.H1`) |
| `## ` (Line start + space) | Heading 2 | `HeadingKey` (`HeadingLevel.H2`) |
| `### ` (Line start + space) | Heading 3 | `HeadingKey` (`HeadingLevel.H3`) |
| `- ` or `* ` (Line start + space) | Bullet List | `BulletListKey` (`ListIndentLevel.Level1`) |
| `1. ` (Line start + space) | Ordered List | `OrderedListKey` (`ListIndentLevel.Level1`) |
| `> ` (Line start + space) | Blockquote | `BlockquoteKey` (`Unit`) |

### Inline Formatting (Enclosing Symbols)

| Shortcut | Formatted Style | Attribute Key / Value |
|---|---|---|
| `**text**` | Bold | `BoldKey` (`Unit`) |
| `*text*` or `_text_` | Italic | `ItalicKey` (`Unit`) |
| `` `text` `` | Inline Code | `InlineCodeKey` (`Unit`) |
| `~text~` | Strikethrough | `StrikethroughKey` (`Unit`) |

---

## Ergonomic Design & Typing Precision

### 1. Instant Backspace Reversal

A common frustration with automatic Markdown styling is wanting to enter a literal `#` or `*` symbol without triggering a formatting transformation.

In Arranger, **pressing <kbd>Backspace</kbd> immediately after conversion instantly undoes the styling and restores the raw syntax (e.g. `# ` or `**bold**`)**.

```text
[Type] # Heading Title
   ↓ (Automatic Conversion)
Heading style applied (Large typography)
   ↓ (Press <kbd>Backspace</kbd>)
# Heading Title (Raw Markdown restored immediately)
```

This reversal bypasses the undo stack, keeping your typing flow completely seamless.

### 2. Escaping & False-Positive Prevention

- **Backslash Escaping**: When preceded by an odd number of backslashes (such as `\*\*not bold\*\*`), automatic styling is disabled.
- **Identifier & Snake_Case Protection**: In word boundaries with alphanumeric characters adjacent to underscores (such as `user_name_variable` or `API_KEY_SECRET`), italic conversions are safely suppressed.

---

## Full Working Example

```kotlin
@Composable
fun WysiwygEditorExample() {
    val state = remember { RichTextState() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WYSIWYG Markdown Editor",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Try typing # for heading, - for lists, **bold**, and more",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            WysiwygEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}
```

---

## Related Documentation

- [**RichTextEditor Basics**](rich-text-editor.md): Toolbar-driven rich text editor component.
- [**State Management (RichTextState)**](state-management.md): Undo/redo, selection, and attribute logical intersection.
- [**Spans and Paragraphs**](../styling/spans-and-paragraphs.md): Paragraph boundaries and mutual exclusion.
