# Markdown Interoperability

Arranger provides first-class support for bi-directional Markdown conversion through the `arranger-richtext-markdown` module. Powered by JetBrains' official [Markdown parser](https://github.com/JetBrains/markdown) using the GitHub Flavored Markdown (GFM) specification, this module enables seamless serialization between rich text in-memory models (`RichString`) and standardized Markdown text.

---

## Installation

Add the `arranger-richtext-markdown` dependency to your Gradle build script alongside the core library:

=== "Kotlin DSL (build.gradle.kts)"
    ```kotlin
    dependencies {
        // Pure KMP Markdown serialization
        implementation("dev.mkeeda.arranger:arranger-richtext-markdown:0.4.0-alpha")
        implementation("dev.mkeeda.arranger:arranger-richtext:0.4.0-alpha")
    }
    ```

=== "Version Catalog (libs.versions.toml)"
    ```toml
    [versions]
    arranger = "0.4.0-alpha"

    [libraries]
    arranger-richtext-markdown = { module = "dev.mkeeda.arranger:arranger-richtext-markdown", version.ref = "arranger" }
    ```

---

## Bi-Directional Conversion API

The module exposes two idiomatic Kotlin extension functions that wrap the underlying `MarkdownFormat` singleton:

```kotlin
// Export a RichString model to a Markdown string
public fun RichString.toMarkdown(): String

// Import a Markdown string into a RichString model
public fun RichString.Companion.fromMarkdown(markdown: String): RichString
```

### Exporting to Markdown (`toMarkdown`)

Converting an active editor state or a statically constructed `RichString` to Markdown is as simple as calling `toMarkdown()`:

```kotlin
val richString = RichString("Compose Multiplatform is amazing") {
    setSpanAttribute(BoldKey, Unit, range = 0..20)
}

val markdownOutput: String = richString.toMarkdown()
// Result: "**Compose Multiplatform** is amazing"
```

### Importing from Markdown (`fromMarkdown`)

To load persisted Markdown documents or network payloads into Arranger, use `RichString.fromMarkdown()`:

```kotlin
val rawMarkdown = """
    # Release Notes
    
    * High-performance editing
    * Multiplatform ready
    
    Read the [documentation](https://mkeeda.github.io/arranger/) for details.
""".trimIndent()

val richString = RichString.fromMarkdown(rawMarkdown)

// Initialize an editor state directly
val editorState = RichTextState(initialText = richString)
```

---

## Under the Hood: MarkdownFormat

Both extension functions delegate to the `MarkdownFormat` object, which implements Arranger's universal format SPI (`RichTextFormat<String>`):

```kotlin
public object MarkdownFormat :
    RichTextFormat<String>,
    RichTextExporter<String> by MarkdownExporter(),
    RichTextImporter<String> by MarkdownImporter()
```

You can pass `MarkdownFormat` directly into Arranger's generic format APIs:

```kotlin
val markdown: String = richString.export(MarkdownFormat)
val restored: RichString = RichString.import(markdown, MarkdownFormat)
```

---

## Supported Markdown Elements Matrix

Arranger maps CommonMark and GFM AST nodes directly to its typed attribute system (`AttributeKey<T>`):

| Markdown Syntax | AST Node / Flavour | Arranger Attribute | Scope |
|---|---|---|---|
| `**bold**` or `__bold__` | `MarkdownElementTypes.STRONG` | `BoldKey` | Span (`Unit`) |
| `*italic*` or `_italic_` | `MarkdownElementTypes.EMPH` | `ItalicKey` | Span (`Unit`) |
| `~~strikethrough~~` | `GFMElementTypes.STRIKETHROUGH` | `StrikethroughKey` | Span (`Unit`) |
| `<u>underline</u>` | HTML tag token (`<u>...</u>`) | `UnderlineKey` | Span (`Unit`) |
| `[text](url)` | `MarkdownElementTypes.INLINE_LINK` | `LinkKey` | Span (`String`) |
| `<https://example.com>` | `MarkdownElementTypes.AUTOLINK` | `LinkKey` | Span (`String`) |
| `# ` to `###### ` | `MarkdownElementTypes.ATX_1`..`6` | `HeadingKey` | Paragraph (`HeadingLevel.H1`..`H6`) |
| Setext `===` and `---` | `MarkdownElementTypes.SETEXT_1`..`2` | `HeadingKey` | Paragraph (`HeadingLevel.H1`..`H2`) |
| `> Quote` | `MarkdownElementTypes.BLOCK_QUOTE` | `BlockquoteKey` | Paragraph (`Unit`) |
| `* `, `- `, `+ ` | `MarkdownElementTypes.UNORDERED_LIST` | `BulletListKey` | Paragraph (`ListIndentLevel.Level1`..`Level6`) |
| `1. `, `2. ` | `MarkdownElementTypes.ORDERED_LIST` | `OrderedListKey` | Paragraph (`ListIndentLevel.Level1`..`Level6`) |

---

## Advanced Behaviors & Implementation Details

### Nested List Serialization

The exporter maintains an active nesting counter stack to generate properly indented ordered and unordered lists:

```markdown
1. First step
   1. Sub-step A
   2. Sub-step B
2. Second step
```

When exporting:

- Bullet lists are indented by `2 * indentLevel.ordinal` spaces followed by `* `.
- Ordered lists are indented by `3 * indentLevel.ordinal` spaces followed by `$count. `, with counter tracks isolated per indentation depth.

### Boundary Normalization & Delimiter Nesting

When rich text contains overlapping inline styles (for example, text that is both bold and italic, or a link with underlined text), `MarkdownExporter` automatically resolves delimiter nesting to guarantee that closing tags never cross or produce malformed Markdown:

```kotlin
// Given: "Bold and italic" where Bold covers 0..14 and Italic covers 9..14
// Exported as: "**Bold and *italic***"
```

The delimiter stack ensures correct opening and closing order: `Link` -> `Underline` -> `Strikethrough` -> `Bold` -> `Italic`.

---

## Current Nuances & Limitations

!!! warning "Inline Code Formatting in v0.4.0-alpha"
    While Arranger's core model defines `InlineCodeKey` and `WysiwygEditor` supports typing `` `code` `` shortcuts, the current `arranger-richtext-markdown` parser treats code spans as plain unstyled text, and `MarkdownExporter` does not yet emit backticks for `InlineCodeKey`. Full round-trip inline code serialization is planned for an upcoming release.

!!! note "Block-Level Code Fences"
    Fenced code blocks and indented code blocks are currently imported as plain text. Arranger core prioritizes text styling and document formatting over full syntax-highlighted IDE code blocks.

!!! info "Unsupported GFM Extensions"
    GFM tables, footnotes, task list checkboxes (`- [ ]`), and mathematical expressions are parsed as fallback plain text or standard bullet lists. If your application requires structured table editing, consider implementing a custom format or storing tables as metadata.
