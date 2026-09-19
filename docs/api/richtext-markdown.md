# Module :richtext-markdown API Reference

The `:richtext-markdown` module provides bi-directional serialization and deserialization between `RichString` and GitHub Flavored Markdown (GFM). Powered by JetBrains' multiplatform `markdown` parser, it transforms Markdown strings into fully attributed `RichString` instances and serializes rich editor documents back to standard Markdown syntax.

---

## Interactive Dokka KDoc

For complete class specifications and method signatures, view the interactive Dokka documentation:

👉 **[Module :richtext-markdown Dokka KDoc](https://mkeeda.github.io/arranger/api/dokka/richtext-markdown/)**

---

## Coordinates & Dependency

```kotlin
// build.gradle.kts (commonMain)
implementation("dev.mkeeda.arranger:arranger-richtext-markdown:0.4.0-alpha03")
```

---

## Primary APIs

### Extension Functions

The primary developer-facing APIs are defined as extension functions on `RichString`:

```kotlin
// Exporting RichString to a Markdown string
fun RichString.toMarkdown(): String

// Importing a Markdown string into an immutable RichString
fun RichString.Companion.fromMarkdown(markdown: String): RichString
```

### `MarkdownFormat`

A singleton object that implements Arranger's core format SPI:

```kotlin
object MarkdownFormat : RichTextFormat<String>, RichTextExporter<String>, RichTextImporter<String> {
    override fun export(richString: RichString): String
    override fun import(input: String): RichString
}
```

You can use it interchangeably with generic format APIs:
```kotlin
val markdownText = richString.export(MarkdownFormat)
val richString = RichString.import(markdownText, MarkdownFormat)
```

---

## Supported Syntax & Feature Mapping

| Markdown Syntax | Parsed AST Node | Arranger Attribute Key | Rendered Styling |
|---|---|---|---|
| `# H1` .. `###### H6` | `ATX_1` .. `ATX_6` | `HeadingKey` | `HeadingLevel.H1` .. `H6` |
| `**bold**` or `__bold__` | `EMPH` / `STRONG` | `BoldKey` | `FontWeight.Bold` |
| `*italic*` or `_italic_` | `EMPH` | `ItalicKey` | `FontStyle.Italic` |
| `~~strikethrough~~` | `STRIKETHROUGH` | `StrikethroughKey` | `TextDecoration.LineThrough` |
| `<u>underline</u>` | `HTML_TAG` | `UnderlineKey` | `TextDecoration.Underline` |
| `` `inline code` `` | `CODE_SPAN` | `InlineCodeKey` | Monospace font family & background |
| `> blockquote` | `BLOCK_QUOTE` | `BlockquoteKey` | Left border & italicized quote |
| `- item` or `* item` | `LIST_ITEM` | `BulletListKey` | Bullet marker with `ListIndentLevel` |
| `1. item` | `LIST_ITEM` | `OrderedListKey` | Numeric marker with `ListIndentLevel` |
| `[label](https://...)` | `INLINE_LINK` | `LinkKey` | Clickable hyperlink with target URL |

---

## Usage Examples

### Loading Markdown into an Editor

```kotlin
val initialMarkdown = """
    # Compose Multiplatform Rich Text
    Arranger delivers **declarative** formatting and *WYSIWYG* shortcuts.
    - Pure Kotlin core
    - High performance
""".trimIndent()

val state = remember {
    RichTextState(initialText = RichString.fromMarkdown(initialMarkdown))
}

RichTextEditor(state = state)
```

### Exporting Editor Content as Markdown

```kotlin
val markdownOutput: String = state.richString.toMarkdown()
saveToFile(markdownOutput)
```

---

## Nuances & Behavioral Notes

- **Inline Code Key (`InlineCodeKey`)**: Inline code blocks are parsed as literal text spans. Other formatting markers nested within backticks (e.g. `` `**bold**` ``) are treated as literal characters rather than nested formatting.
- **HTML Underline Tags**: While CommonMark lacks a native underline token, the importer automatically recognizes `<u>...</u>` tags and converts them into `UnderlineKey` spans.
- **List Indentation**: Tab indentation or 2/4-space indents before list markers are mapped to `ListIndentLevel.Level1` through `Level6`.
