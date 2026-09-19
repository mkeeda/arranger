# Overlapping Styles & Automatic Normalization

When users format rich text documents, formatting ranges frequently overlap, fragment, and shift. For instance, a user might apply **Bold** to a full sentence, make a few words *Italic*, and later remove Bold from a partial selection.

Arranger handles these range intersections automatically behind the scenes. You never have to calculate character offsets or manage nested formatting spans manually.

![Attribute Batch Editing](../images/attribute-batch-edit.gif){ width="600" }

---

## How Range Overlaps Work

Arranger normalizes formatting so that every character belongs to a predictable set of attributes.

### 1. Overlapping Multiple Styles

Applying different attributes over intersecting ranges seamlessly combines them:

```text
Existing Text:
"Compose Multiplatform Rich Text Editor"
 [==== Bold (0..19) ====]
                 [==== Italic (16..38) ====]

Resulting Formatting:
- "Compose Multi"   (0..15)  -> Bold
- "platform"        (16..19) -> Bold + Italic
- " Rich Text Editor" (20..38) -> Italic
```

### 2. Partial Formatting Removal

Clearing an attribute from a sub-range cleanly slices the surrounding format into separate spans:

```text
Existing Text:
"A quick brown fox jumps over"
 [======== Bold (0..27) ========]

Action: Remove Bold from "brown fox" (8..17)

Resulting Formatting:
- "A quick " (0..7)   -> Bold
- "brown fox" (8..17) -> Plain text (unformatted)
- " jumps over" (18..27) -> Bold
```

### 3. Automatic Coalescing (De-fragmentation)

When adjacent text ranges share identical formatting attributes, Arranger automatically coalesces them into a single continuous range. This prevents memory bloat and span fragmentation as users type, edit, and paste text.

---

## Applying Formatting via the Public API

You can manipulate formatting ranges using intuitive methods on `RichTextState`:

### Toggling Format on Selection or Typing Position

The simplest and most common approach (e.g. from toolbar buttons) is `toggleSpanAttribute`:

```kotlin
// If text is selected, toggles Bold over the selected range.
// If no text is selected, sets active typing attributes for incoming characters.
state.toggleSpanAttribute(BoldKey)
```

### Explicit Range Editing with `state.edit`

For custom toolbar actions or programmatic formatting, use `state.edit`:

```kotlin
state.edit {
    // Apply highlight to a specific character range
    setSpanAttribute(
        range = 0..15,
        key = BackgroundColorKey,
        value = Color.Yellow,
    )

    // Remove an attribute from a range
    removeSpanAttribute(range = 5..10, key = BoldKey)
}
```

### Batch Editing with Semantic "Runs"

Arranger's `runs()` API lets you query and transform all text segments sharing a given attribute without manual string searching:

```kotlin
state.edit {
    // Find all bold runs in the document and color them blue
    val boldRuns = state.richString.runs(BoldKey)
    editAll(boldRuns) {
        textColor(Color.Blue)
    }
}
```

---

## Automatic Paragraph Snapping

While inline character styles (bold, italic, color) apply to exact character ranges, **block-level paragraph attributes** (such as headings, blockquotes, lists, and alignments) must always apply to whole paragraphs.

Whenever a paragraph attribute is applied, Arranger automatically **snaps** the range to the enclosing newline (`\n`) boundaries:

```kotlin
// Even if the user only selected a single word in a paragraph:
state.toggleParagraphAttribute(HeadingKey(HeadingLevel.H1))

// Arranger automatically expands the range to cover the entire line:
// "\nFirst line of text\n" -> The whole line becomes an H1 heading.
```

If the user deletes characters or inserts newlines within a paragraph block, Arranger dynamically realigns paragraph boundaries so your document formatting always remains structurally sound.

---

## Key Takeaways

- **Automatic Range Slicing**: Overlapping styles seamlessly combine without conflicting tags or corrupted boundaries.
- **De-fragmentation**: Adjacent identical styles are automatically unified.
- **Zero Offset Math**: Use `state.toggleSpanAttribute()`, `state.edit { ... }`, and `runs()` to manipulate rich text safely.
- **Automatic Paragraph Snapping**: Block-level styles (headings, lists, quotes) automatically cover complete lines from newline to newline.
