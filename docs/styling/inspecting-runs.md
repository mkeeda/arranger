# Inspecting & Exporting Formats with Runs

When users apply multiple overlapping styles, the editor engine internally organizes formatting into individual spans (`RichSpan`). While this representation is ideal for fast real-time typing and diffing, applications often need to inspect formatting at a higher level:

- Extracting all **hyperlinks** or **@mentions** embedded across a document
- Serializing formatted text into **custom formats** (such as Slack mrkdwn, Markdown, or HTML)
- Highlighting, counting, or analyzing contiguous styled segments
- Finding the exact character boundaries of a style, even if other overlapping styles intersect it

Arranger provides the **Runs API** (`RichString.runs(...)`), a powerful extraction mechanism that automatically merges internally split spans into clean, contiguous segments called `RichRun`.

---

## What is a Run?

Consider a document where the user applies **Bold** across an entire sentence, and then changes the **Text Color** of just one word in the middle:

```
"Hello wonderful world"
 [=== Bold ===================]
       [=== Green ===]
```

Internally, Arranger tracks this as three distinct spans:

1. `0..5` ("Hello "): Bold
2. `6..14` ("wonderful"): Bold + TextColor(Green)
3. `15..20` (" world"): Bold

If you only iterate over `richString.spans`, finding the entire bold sentence requires manually inspecting boundaries and stitching pieces together.

**Runs solve this completely**: calling `richString.runs(BoldKey)` returns a single contiguous `RichRun` covering the entire range `0..20`, effortlessly bridging across the inner color change.

---

## The RichRun Data Class

Each discovered segment is represented by a `RichRun<T>`:

```kotlin
public data class RichRun<T>(
    val text: String,
    val range: IntRange,
    val value: T,
)
```

- `text`: The exact substring for this run.
- `range`: The 0-based character range (inclusive) in the original document.
- `value`: The resolved attribute value for the queried key (`T`), or the full `AttributeContainer` when queried with a predicate.

---

## 1. Extracting Runs by Attribute Key

Signature:
```kotlin
public fun <T : Any> RichString.runs(key: AttributeKey<T>): Sequence<RichRun<T>>
```

Pass an `AttributeKey` to extract all contiguous blocks where that attribute is continuously applied.

### Flag Attributes (Bold, Italic, Underline)

For Unit-based attributes, any adjacent spans possessing the attribute are combined into one run, ignoring differences in other attributes:

```kotlin
val boldRuns = richString.runs(BoldKey)

boldRuns.forEach { run ->
    println("Bold text: '${run.text}' spanning ${run.range}")
}
```

### Parameterized Attributes (Colors & Links)

For parameterized attributes (such as `TextColorKey` or `LinkKey`), adjacent spans are merged **if and only if they share the exact same parameter value**. When the value changes, a new run begins:

```text
Document: "Apple Orange Grape Kiwi"
Color:    [#FF0000] [#FF0000] [#0000FF] [#0000FF]
Runs:     [== Run 1: Red ==] [== Run 2: Blue ====]
```

```kotlin
// Query all text color runs
val colorRuns = richString.runs(TextColorKey)

colorRuns.forEach { run ->
    val color: RgbaColor = run.value
    println("Text: '${run.text}', Color: $color, Range: ${run.range}")
}

// Output:
// Text: 'Apple Orange ', Color: RgbaColor(0xFFFF0000), Range: 0..12
// Text: 'Grape Kiwi',    Color: RgbaColor(0xFF0000FF), Range: 13..22
```

Unstyled text between formatted regions produces no runs; gaps are skipped automatically.

---

## 2. Filtering Runs with a Predicate

Signature:
```kotlin
public fun RichString.runs(
    predicate: (AttributeContainer) -> Boolean,
): Sequence<RichRun<AttributeContainer>>
```

When you need multi-attribute queries, pass a predicate `(AttributeContainer) -> Boolean`. 

The returned sequence yields `RichRun<AttributeContainer>`, where `run.value` is the complete attribute container for that block.

### Merging Rules for Predicates

1. Only spans satisfying the predicate are included.
2. Adjacent matching spans are merged **only if their entire attribute sets are identical** (`attributes == currentVal`).
3. If an adjacent span matches the predicate but has different attributes (for example, one is Bold+Red and the next is Bold+Italic+Red), they are emitted as separate runs.

### Example 1: Multi-Attribute Query (Bold AND Italic)

```kotlin
// Extract text that is simultaneously Bold AND Italic
val boldAndItalicRuns = richString.runs { attrs ->
    attrs.containsKey(BoldKey) && attrs.containsKey(ItalicKey)
}

boldAndItalicRuns.forEach { run ->
    println("Bold+Italic: '${run.text}' at ${run.range}")
}
```

### Example 2: Interactive Element Inspection (Links or Mentions)

```kotlin
// Extract any interactive spans (either a link or a mention attribute)
val interactiveRuns = richString.runs { attrs ->
    attrs.containsKey(LinkKey) || attrs.containsKey(MentionKey)
}

interactiveRuns.forEach { run ->
    when {
        run.value.containsKey(LinkKey) -> {
            println("Link to ${run.value[LinkKey]}: '${run.text}'")
        }
        run.value.containsKey(MentionKey) -> {
            println("Mention of ${run.value[MentionKey]}: '${run.text}'")
        }
    }
}
```

---

## Lazy Evaluation & Performance

`RichString.runs(...)` returns a standard Kotlin `Sequence<RichRun<T>>`.

Runs are computed **lazily on demand**:

- If you only need the first matching link or mention (`runs(LinkKey).firstOrNull()`), evaluation halts immediately once found without scanning the remainder of the document.
- Zero extra memory allocations for unconsumed elements.

```kotlin
// Efficiently check if any headings exist without processing the entire document
val hasHeadings = richString.runs(HeadingKey).any()
```

---

## Practical Use Cases

### 1. Extracting Document Links or Mentions

```kotlin
fun extractAllUrls(richString: RichString): List<String> {
    return richString.runs(LinkKey)
        .map { it.value }
        .distinct()
        .toList()
}
```

### 2. Custom Format Serialization

When exporting a `RichString` to external systems (such as chat services or Markdown processors), `runs` simplifies tokenization:

```kotlin
fun exportToSlackFormat(richString: RichString): String {
    val boldRanges = richString.runs(BoldKey).map { it.range }.toSet()
    val italicRanges = richString.runs(ItalicKey).map { it.range }.toSet()
    // Process boundaries and wrap delimiters (*bold*, _italic_)
    return formattedText
}
```

### 3. Word and Character Statistics for Formatted Text

```kotlin
// Count how many words in the document are highlighted with a custom key
val highlightedWordCount = richString.runs(HighlightKey)
    .sumOf { run ->
        run.text.split(Regex("\\s+")).count { it.isNotBlank() }
    }
```

---

## Summary

- **By Key (`runs(key)`)**: Merges contiguous spans sharing the same value for `key`, bridging across differences in other overlapping styles. Returns `Sequence<RichRun<T>>`.
- **By Predicate (`runs { predicate }`)**: Merges contiguous spans satisfying the predicate that share the exact same full attribute set. Returns `Sequence<RichRun<AttributeContainer>>`.
- **Lazy Evaluation**: Evaluates on demand using Kotlin sequences for optimal speed and memory usage.
