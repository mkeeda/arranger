# Module :richtext API Reference

The `:richtext` module is the foundation of the Arranger ecosystem. It provides the pure Kotlin Multiplatform data models, attribute management containers, transformation scopes, span-merging algorithms, and text format abstractions. It has zero dependencies on Compose UI or Android frameworks, making it suitable for shared core logic, CLI utilities, and server-side processing.

---

## Interactive Dokka KDoc

For complete class hierarchies, function signatures, and method parameters, visit the interactive Dokka reference:

👉 **[Module :richtext Dokka KDoc](https://mkeeda.github.io/arranger/api/dokka/richtext/)**

---

## Coordinates & Dependency

```kotlin
// build.gradle.kts (commonMain)
implementation("dev.mkeeda.arranger:arranger-richtext:0.4.0-alpha03")
```

---

## Core Data Structures

### `RichString`
The primary immutable data class representing rich formatted text.
```kotlin
data class RichString(
    val text: String,
    val spans: List<RichSpan> = emptyList(),
)
```
- **Constructors**:
  - `RichString(text: String, spans: List<RichSpan> = emptyList())`
  - `RichString(text: String, initialAttributes: AttributeContainer)`
- **Key Methods**:
  - `fun <T : Any> runs(key: AttributeKey<T>): Sequence<RichRun<T>>`: Extracts consecutive text chunks that share identical values for the specified attribute key.
  - `fun runs(predicate: (AttributeContainer) -> Boolean): Sequence<RichRun<AttributeContainer>>`: Extracts runs satisfying an arbitrary attribute filter predicate.
  - `fun edit(block: RichStringScope.() -> Unit): RichString`: Creates a mutated copy of this `RichString` through the builder scope.

### `RichSpan`
Associates an inclusive-exclusive text range with an immutable attribute container.
```kotlin
data class RichSpan(
    val range: IntRange,
    val attributes: AttributeContainer,
)
```

### `RichRun<T>`
A contiguous slice of text that shares the exact same attribute value `T`.
```kotlin
data class RichRun<T>(
    val text: String,
    val range: IntRange,
    val value: T,
)
```

---

## Attribute System & Keys

Formatting and semantic metadata in Arranger are strongly typed using `AttributeKey<T>`.

### Key Hierarchy
```
AttributeKey<T> (sealed interface)
├── SpanAttributeKey<T> (character-level formatting)
└── ParagraphAttributeKey<T> (block/paragraph-level formatting)
    ├── BlockTypeAttributeKey<T> (mutually exclusive structural blocks)
    └── AlignmentAttributeKey<T> (mutually exclusive text alignment)
```

- **`SpanAttributeKey<T>`**: Applied to character ranges (e.g., bold, color, hyperlink).
- **`ParagraphAttributeKey<T>`**: Applied to full paragraph ranges. Defines an `enterKeyStrategy: EnterKeyStrategy` (defaults to `InheritParagraphStrategy`).
- **`BlockTypeAttributeKey<T>`**: Structural paragraph blocks. **Only one block-type attribute may exist per paragraph**. Setting a new `BlockTypeAttributeKey` automatically evicts any previously active block attribute (e.g. replacing a Heading with a Bullet List).
- **`AlignmentAttributeKey<T>`**: Text alignment. Setting a new alignment automatically evicts the previous alignment.

### Standard Built-In Keys

| Attribute Key | Type Parameter `T` | Category | Default Value | Notes |
|---|---|---|---|---|
| `BoldKey` | `Unit` | Span | `Unit` | Bold weight |
| `ItalicKey` | `Unit` | Span | `Unit` | Italic style |
| `UnderlineKey` | `Unit` | Span | `Unit` | Text underline |
| `StrikethroughKey` | `Unit` | Span | `Unit` | Text strike-through |
| `InlineCodeKey` | `Unit` | Span | `Unit` | Monospace code snippet |
| `TextColorKey` | `RgbaColor` | Span | `RgbaColor.Unspecified` | Foreground text color |
| `BackgroundColorKey` | `RgbaColor` | Span | `RgbaColor.Unspecified` | Background highlight |
| `FontSizeKey` | `TextSize` | Span | `TextSize.Unspecified` | Explicit font size in `sp` |
| `LinkKey` | `String` | Span | `""` | Target URL destination |
| `HeadingKey` | `HeadingLevel` | Block | `HeadingLevel.Unspecified` | `H1` through `H6` |
| `TextAlignmentKey` | `TextAlignment` | Alignment | `TextAlignment.Unspecified` | `Left`, `Center`, `Right`, `Justify` |
| `BlockquoteKey` | `Unit` | Block | `Unit` | Quotation block |
| `BulletListKey` | `ListIndentLevel` | Block | `ListIndentLevel.Unspecified` | Unordered list item |
| `OrderedListKey` | `ListIndentLevel` | Block | `ListIndentLevel.Unspecified` | Ordered numeric list item |

### Supporting Value Types
- `RgbaColor`: 64-bit value class representing platform-independent color (`RgbaColor.Unspecified`).
- `TextSize`: Value class wrapping sp float values (`TextSize.Unspecified`).
- `HeadingLevel`: Enum: `H1`, `H2`, `H3`, `H4`, `H5`, `H6`, `Unspecified`.
- `TextAlignment`: Enum: `Left`, `Center`, `Right`, `Justify`, `Unspecified`.
- `ListIndentLevel`: Enum: `Level1` through `Level6`, `Unspecified`.

---

## `AttributeContainer`

An immutable, type-safe collection of attributes.

```kotlin
val container = attributeContainerOf(
    BoldKey to Unit,
    TextColorKey to RgbaColor(0xFFE91E63),
)
```

- **Lookup**:
  - `operator fun <T> get(key: AttributeKey<T>): T?`
  - `fun <T> getOrDefault(key: AttributeKey<T>): T`
  - `fun containsKey(key: AttributeKey<*>): Boolean`
  - `fun containsAll(vararg keys: AttributeKey<*>): Boolean`
  - `fun containsAny(vararg keys: AttributeKey<*>): Boolean`
- **Mutations (returning new instance)**:
  - `fun <T> plus(key: AttributeKey<T>, value: T): AttributeContainer`
  - `operator fun plus(other: AttributeContainer): AttributeContainer`
  - `operator fun <T> minus(key: AttributeKey<T>): AttributeContainer`
  - `fun filterKeys(predicate: (AttributeKey<*>) -> Boolean): AttributeContainer`

---

## Builder Scopes

### `RichStringScope`
Receiver for `RichString.edit { }`.
- `setSpanAttribute(key, value, range)`: Applies a span attribute across `range`.
- `removeSpanAttribute(key, range)`: Removes the specified span attribute.
- `setParagraphAttribute(key, value, range)`: Automatically snaps `range` to line boundaries and sets the paragraph attribute.
- `removeParagraphAttribute(key, range)`: Snaps to line boundaries and removes the paragraph attribute.
- `clearAllAttributes(range)`: Clears all attributes within `range`.
- `editAttributes(range) { ... }`: Enters `AttributeEditScope` for DSL-style configuration.
- `editAll(ranges) { ... }` / `editAll(runs) { ... }`: Batch-modifies multiple ranges or runs.

### `AttributeEditScope`
DSL helper for setting and clearing standard attributes fluently:
```kotlin
richString.edit {
    editAttributes(0..5) {
        bold()
        textColor(RgbaColor(0xFF007ACC))
        fontSize(TextSize(18f))
    }
}
```

---

## Enter-Key Strategies & Algorithms

- **`EnterKeyStrategy`**: Interface dictating behavior when the user presses Enter in an editor:
  - `execute(context: EnterKeyContext): EnterKeyResult`
  - Results: `InheritAttributes`, `ClearAttributes`, `Outdent`.
- **Implementations**:
  - `InheritParagraphStrategy`: Carries the paragraph attribute to the next line (used by blockquotes).
  - `HeadingEnterStrategy`: Resets heading formatting on the new line to standard body text.
  - `ListEnterStrategy`: Automatically continues lists; outdents or clears list markers when Enter is pressed on an empty item.
- **Span Normalization**:
  - `snapToParagraphs(text, range)`: Expands a character range to encompass complete paragraphs bounded by `\n`.
  - `transformSpans(spans, targetRange, transform)`: Implements a Sweep-Line interval splitting algorithm that guarantees non-overlapping, contiguous span segmentation.

---

## Interoperability SPI & Parsers

- **`RichTextFormat<T>`**: Common abstraction combining `RichTextExporter<T>` and `RichTextImporter<T>`.
- **`UrlParser`**: Object providing `findUrls(text: CharSequence): List<DiscoveredUrl>` with smart punctuation trimming and automatic `https://` prefix completion.
