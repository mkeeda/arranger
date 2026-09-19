# Module :richtext-html API Reference

The `:richtext-html` module provides bi-directional HTML import and export for Arranger. Built on top of the pure Kotlin Multiplatform HTML parser `ksoup`, it converts semantic HTML markup and inline CSS styling into rich `RichString` models, and renders editor content back into clean, well-formed HTML.

---

## Interactive Dokka KDoc

For comprehensive method signatures, parameter tables, and return types, view the interactive Dokka documentation:

👉 **[Module :richtext-html Dokka KDoc](https://mkeeda.github.io/arranger/api/dokka/richtext-html/)**

---

## Coordinates & Dependency

```kotlin
// build.gradle.kts (commonMain)
implementation("dev.mkeeda.arranger:arranger-richtext-html:0.4.0-alpha03")
```

---

## Primary APIs

### Extension Functions

```kotlin
// Serializes a RichString to an HTML string
fun RichString.toHtml(): String

// Parses an HTML markup string into an immutable RichString
fun RichString.Companion.fromHtml(html: String): RichString
```

### `HtmlFormat`

A singleton object implementing Arranger's format SPI:

```kotlin
object HtmlFormat : RichTextFormat<String>, RichTextExporter<String>, RichTextImporter<String> {
    override fun export(richString: RichString): String
    override fun import(input: String): RichString
}
```

Usage via generic format APIs:
```kotlin
val htmlString = richString.export(HtmlFormat)
val imported = RichString.import(htmlString, HtmlFormat)
```

---

## Supported Tags & Inline CSS Specifications

### HTML Tags

| HTML Tag | Arranger Attribute Key | Notes |
|---|---|---|
| `<h1>` .. `<h6>` | `HeadingKey` | Mapped to `HeadingLevel.H1` through `H6` |
| `<b>`, `<strong>` | `BoldKey` | Bold text weight |
| `<i>`, `<em>` | `ItalicKey` | Italicized font style |
| `<u>` | `UnderlineKey` | Underline text decoration |
| `<s>`, `<del>`, `<strike>` | `StrikethroughKey` | Line-through text decoration |
| `<code>` | `InlineCodeKey` | Monospace inline code |
| `<blockquote>` | `BlockquoteKey` | Block quotation |
| `<ul>`, `<li>` | `BulletListKey` | Unordered list item with `ListIndentLevel` |
| `<ol>`, `<li>` | `OrderedListKey` | Ordered list item with `ListIndentLevel` |
| `<a href="...">` | `LinkKey` | Hyperlink target URI |
| `<p>`, `<div>` | Paragraph boundaries | Delimited by newline characters |

### Inline CSS Styling (`style="..."`)

The importer extracts CSS property declarations from `style` attributes on tags such as `<span>`, `<p>`, and `<div>`:

| CSS Property | Example Values | Arranger Attribute Key |
|---|---|---|
| `color` | `#FF0000`, `rgb(255, 0, 0)`, `red` | `TextColorKey` (`RgbaColor`) |
| `background-color` | `#FFFF00`, `yellow` | `BackgroundColorKey` (`RgbaColor`) |
| `font-size` | `18sp`, `20px`, `1.5em` | `FontSizeKey` (`TextSize`) |
| `text-align` | `left`, `center`, `right`, `justify` | `TextAlignmentKey` (`TextAlignment`) |

---

## Nested Lists & Structural Serialization

- **Nested Lists**: The HTML parser tracks the nesting depth of `<ul>` and `<ol>` tags, automatically setting `ListIndentLevel.Level1` through `Level6`.
- **Clean Export**: When exporting to HTML via `toHtml()`, Arranger properly nests opening tags, closes list elements in reverse hierarchical order, and writes inline CSS attributes only where non-default formatting values are defined.
- **Robust Error Handling**: Malformed or unclosed HTML tags are safely parsed and balanced by `ksoup` without throwing exceptions.

---

## Usage Example

```kotlin
val htmlSource = """
    <h2>Welcome to Arranger</h2>
    <p>Enjoy <span style="color: #4CAF50; font-size: 20sp;">colorful</span> and <b>bold</b> text!</p>
    <ul>
        <li>First bullet</li>
        <li>Second bullet with a <a href="https://example.com">link</a></li>
    </ul>
""".trimIndent()

val state = remember {
    RichTextState(initialText = RichString.fromHtml(htmlSource))
}

RichTextEditor(state = state)
```
