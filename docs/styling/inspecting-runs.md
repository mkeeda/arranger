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
- `value`: The resolved attribute value for the queried key or predicate.

---

## Extracting Runs by Attribute Key

To extract all regions where a specific attribute is applied, pass the `AttributeKey`:

```kotlin
val boldRuns = richString.runs(BoldKey)

boldRuns.forEach { run ->
    println("Bold text: '${run.text}' at ${run.range}")
}
```

Adjacent spans that share the **exact same attribute value** are automatically combined into a single `RichRun`, regardless of differences in any other attributes.

### Parameterized Attributes Example (Colors & Links)

For parameterized attributes (such as `TextColorKey` or `LinkKey`), adjacent spans are merged only if they have the same parameter value:

```kotlin
// Extract all hyperlinks from the document
val links = richString.runs(LinkKey)

links.forEach { run ->
    val url: String = run.value
    println("Found link '${run.text}' pointing to $url (${run.range})")
}
```

If adjacent words share the same URL, they merge into one link run. If the URL changes, a separate run begins.

---

## Filtering Runs with a Predicate

For advanced inspection, you can query runs using a custom predicate function `(AttributeContainer) -> Boolean`:

```kotlin
// Extract all text that is both Bold AND Italic
val boldItalicRuns = richString.runs { attrs ->
    attrs.containsKey(BoldKey) && attrs.containsKey(ItalicKey)
}

boldItalicRuns.forEach { run ->
    println("Bold+Italic: '${run.text}' (${run.range})")
}
```

When using a predicate, adjacent spans that satisfy the condition are merged only if their entire `AttributeContainer` is identical.

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

When exporting a `RichString` to external systems (such as chat services or CMS backends), `runs` simplifies tokenization:

```kotlin
fun exportToSlackFormat(richString: RichString): String {
    val boldRanges = richString.runs(BoldKey).map { it.range }.toSet()
    val italicRanges = richString.runs(ItalicKey).map { it.range }.toSet()
    // Process boundaries and wrap delimiters
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

- Use `richString.runs(key)` to extract continuous blocks of a specific formatting attribute, ignoring unrelated overlapping styles.
- Use `richString.runs { predicate }` for complex multi-attribute queries.
- Returns a lazy Kotlin `Sequence<RichRun<T>>` with `text`, `range`, and `value`.
- Ideal for extracting links/mentions, document statistics, and custom format serialization.
