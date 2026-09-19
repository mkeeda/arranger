# HTML Interoperability

Arranger offers rich bi-directional HTML import and export capabilities via the `arranger-richtext-html` module. Utilizing [ksoup](https://github.com/fleeksoft/ksoup) — a pure Kotlin Multiplatform port of the popular jsoup HTML parsing library — this module enables full HTML round-tripping across Android, iOS, Desktop (JVM), and Web (Wasm/JS) without platform-specific webview or browser dependencies.

![Advanced Formatting](../images/advanced-formatting.png){ width="600" }

---

## Installation

Add `arranger-richtext-html` to your module's build configuration:

=== "Kotlin DSL (build.gradle.kts)"
    ```kotlin
    dependencies {
        // Pure KMP HTML serialization with inline CSS styling
        implementation("dev.mkeeda.arranger:arranger-richtext-html:0.4.0-alpha")
        implementation("dev.mkeeda.arranger:arranger-richtext:0.4.0-alpha")
    }
    ```

=== "Version Catalog (libs.versions.toml)"
    ```toml
    [versions]
    arranger = "0.4.0-alpha"

    [libraries]
    arranger-richtext-html = { module = "dev.mkeeda.arranger:arranger-richtext-html", version.ref = "arranger" }
    ```

---

## Bi-Directional Conversion API

Arranger provides high-level extension functions on `RichString` and its companion object:

```kotlin
// Export a RichString model to an HTML string
public fun RichString.toHtml(): String

// Import an HTML string into a RichString model
public fun RichString.Companion.fromHtml(html: String): RichString
```

### Exporting to HTML (`toHtml`)

When converting a `RichString` to HTML, Arranger generates clean, semantic, and well-formed markup:

```kotlin
val richString = RichString("Styled text with colors") {
    textColor(RgbaColor(0xFFFF0000)) // Red
    bold()
}

val html: String = richString.toHtml()
// Result: "<p><span style=\"color: #ff0000;\"><strong>Styled text with colors</strong></span></p>"
```

### Importing from HTML (`fromHtml`)

Importing HTML fragments, CMS contents, or clipboard payloads into Arranger is fully automated:

```kotlin
val rawHtml = """
    <h2>Product Highlights</h2>
    <ul>
        <li>Super fast rendering</li>
        <li><strong>Zero</strong> platform dependencies</li>
    </ul>
    <blockquote style="text-align: center;">Crafted with Compose</blockquote>
""".trimIndent()

val richString = RichString.fromHtml(rawHtml)
val state = RichTextState(initialText = richString)
```

---

## Under the Hood: HtmlFormat

Both extension functions operate through `HtmlFormat`, which implements the `RichTextFormat<String>` contract:

```kotlin
public object HtmlFormat :
    RichTextFormat<String>,
    RichTextExporter<String> by HtmlExporter(),
    RichTextImporter<String> by HtmlImporter()
```

You can use `HtmlFormat` directly with generic export and import routines:

```kotlin
val html: String = richString.export(HtmlFormat)
val restored: RichString = RichString.import(html, HtmlFormat)
```

---

## Supported HTML Tags & Attribute Mapping

### Semantic Tags

Arranger parses and generates standard semantic HTML elements:

| HTML Element | Arranger Attribute | Scope | Behavior / Notes |
|---|---|---|---|
| `<b>`, `<strong>` | `BoldKey` | Span (`Unit`) | Renders strong emphasis |
| `<i>`, `<em>` | `ItalicKey` | Span (`Unit`) | Renders italicized text |
| `<s>`, `<del>`, `<strike>` | `StrikethroughKey` | Span (`Unit`) | All strikethrough variants supported |
| `<u>` | `UnderlineKey` | Span (`Unit`) | Underline decoration |
| `<a href="...">` | `LinkKey` | Span (`String`) | URL target stored in `LinkKey` |
| `<span style="...">` | `TextColorKey`, `BackgroundColorKey`, `FontSizeKey` | Span | Extracted from inline CSS |
| `<h1>` to `<h6>` | `HeadingKey` | Paragraph (`HeadingLevel`) | Also parses optional text alignment |
| `<p>` | Standard paragraph | Paragraph | Splits paragraphs by `\n` |
| `<blockquote>` | `BlockquoteKey` | Paragraph (`Unit`) | Supports nested blocks |
| `<ul>`, `<ol>`, `<li>` | `BulletListKey`, `OrderedListKey` | Paragraph (`ListIndentLevel`) | Nested lists up to Level 6 |
| `<br>` | Newline character | Control | Injects `\n` without closing paragraph |

### Ignored & Non-Content Tags

To prevent unexpected DOM elements from injecting unwanted characters, the HTML importer strictly filters out:
`<script>`, `<style>`, `<meta>`, `<noscript>`, `<template>`, `<head>`, and `<title>`.

---

## Inline CSS Styling Support

The HTML importer and exporter support fine-grained CSS declarations directly within `style` attributes.

### 1. Colors (`color` and `background-color`)

- **Hex formats:** `#RGB`, `#RRGGBB`, and `#AARRGGBB` (e.g., `#f00`, `#ff0000`, `#80ff0000`).
- **RGB / RGBA functions:** `rgb(255, 0, 0)` and `rgba(255, 0, 0, 0.5)`.
- **Mapping:** Converted to and from Arranger's platform-agnostic `RgbaColor` value class.

### 2. Font Size (`font-size`)

- Supports numeric values with `sp`, `px`, and `pt` units (e.g., `font-size: 18sp;`, `font-size: 24px;`).
- Cleaned and parsed into Arranger's `TextSize(sp: Float)` value class.

### 3. Text Alignment (`text-align` and `align`)

- Supports `left`, `center`, `right`, and `justify`.
- Can be declared via inline CSS (`style="text-align: center;"`) or legacy HTML attribute (`align="center"`).
- Automatically mapped to `TextAlignmentKey` (`TextAlignment.Left`, `Center`, `Right`, `Justify`).

---

## Advanced Behaviors

### Nested List Serialization & Hierarchy Stack

HTML lists often feature complex hierarchies with mixed ordered and unordered sub-items:

```html
<ul>
    <li>Main Topic
        <ol>
            <li>Subtopic 1</li>
            <li>Subtopic 2</li>
        </ol>
    </li>
</ul>
```

`HtmlExporter` automatically balances nested `<ul>`, `<ol>`, and `<li>` tags according to the paragraph `ListIndentLevel`. When transitioning back to normal paragraphs or headings, open lists are cleanly closed.

### HTML Entity Escaping & Security

During export, text and link attributes are safely sanitized to prevent injection attacks and broken DOM trees:

- `&` -> `&amp;`
- `<` -> `&lt;`
- `>` -> `&gt;`
- `"` -> `&quot;`
- `'` -> `&#39;`

On import, `ksoup` decodes all valid HTML entities (`&copy;`, `&mdash;`, `&#128512;`, etc.) back to unicode characters automatically.

---

## Current Nuances & Limitations

!!! warning "Inline Code (<code>) Tag in v0.4.0-alpha"
    The current `HtmlImporter` and `HtmlExporter` implementations do not map `<code>` or `<pre>` tags to Arranger's `InlineCodeKey`. If code formatting is required, wrap text with inline styles or implement a custom format handler.

!!! info "Block vs Inline Separation"
    Arranger strictly enforces paragraph snapping for block tags (`h1`-`h6`, `p`, `blockquote`, `li`). In-line elements nested inside blocks are split across line breaks if they span multiple paragraphs.
