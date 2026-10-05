# Theming and Material 3

Arranger cleanly decouples data models (`AttributeContainer`) from Compose visual styling (`SpanStyle` and `ParagraphStyle`). The bridge between data and visual presentation is the `AttributeStyleResolver`, complemented by the `:richtext-editor-material3` module for seamless Material 3 integration.

---

## Architecture: AttributeStyleResolver

When rendering text, `RichTextEditor` and `WysiwygEditor` consult `AttributeStyleResolver` to compute Compose styles:

```kotlin
fun interface AttributeStyleResolver {
    fun resolve(attributes: AttributeContainer): ResolvedRichStyle
}

data class ResolvedRichStyle(
    val spanStyle: SpanStyle? = null,
    val paragraphStyle: ParagraphStyle? = null,
)
```

- **`spanStyle`**: Character-level styling including font weight, text color, background highlight, font size, and text decoration.
- **`paragraphStyle`**: Block-level styling including horizontal alignment (Left, Center, Right) and line height.

### DefaultAttributeStyleResolver Standard Specification

When no custom resolver is specified, `DefaultAttributeStyleResolver` is used by default:

- `BoldKey` -> `FontWeight.Bold`
- `ItalicKey` -> `FontStyle.Italic`
- `UnderlineKey` -> `TextDecoration.Underline`
- `StrikethroughKey` -> `TextDecoration.LineThrough`
- `InlineCodeKey` -> Monospace font (`FontFamily.Monospace`) with neutral semi-transparent background (`Color(0x1F888888)`) adapting cleanly to both light and dark backgrounds
- `CodeBlockKey` -> Monospace font (`FontFamily.Monospace`) and block paragraph indent (`12.sp`)
- `LinkKey` -> Underlined text with default link color (`Color(0xFF1E88E5)`)
- `TextColorKey` -> Specified `RgbaColor`
- `BackgroundColorKey` -> Specified `RgbaColor`
- `HeadingKey` -> Scaled font size and bold weight matching heading levels
- `TextAlignmentKey` -> Specified horizontal alignment
- `BlockTypeAttributeKey` -> Uniform vertical line-height padding (`DefaultBlockLineHeight = 24.sp`, `LineHeightStyle(Center, Trim.None)`) applied across all block types (quotes, code blocks, lists)

To customize the link color or inline code background for non-Material setups, use the `defaultAttributeStyleResolver` factory function:

```kotlin
val resolver = defaultAttributeStyleResolver(
    linkColor = Color(0xFF2196F3),
    inlineCodeBackgroundColor = Color(0x33888888),
)
```

---

## Customizing Styles with the DSL Builder

You can inherit from an existing resolver and override or add styling rules for specific attribute keys:

```kotlin
val CustomStyleResolver = AttributeStyleResolver(base = DefaultAttributeStyleResolver) {
    // Override inline code background and font styling
    spanStyle(InlineCodeKey) {
        SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = Color(0xFF2D3748),
            color = Color(0xFFF7FAFC),
        )
    }
}
```

---

## Material 3 Integration (:richtext-editor-material3)

When building Material 3 applications, heading typography and blockquote colors should naturally align with your `MaterialTheme` design tokens.

### 1. Add Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.5.0-alpha01")
}
```

### 2. Use rememberMaterial3AttributeStyleResolver

Invoke the `@Composable fun rememberMaterial3AttributeStyleResolver()` function and pass the result to the editor's `styleResolver` parameter:

```kotlin
@Composable
fun Material3EditorScreen() {
    val state = rememberRichTextState()

    // Generate a style resolver synchronized with current MaterialTheme (typography & colorScheme)
    val m3StyleResolver = rememberMaterial3AttributeStyleResolver()

    Scaffold { innerPadding ->
        RichTextEditor(
            state = state,
            styleResolver = m3StyleResolver,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
```

---

## Material 3 Token Mapping Specification

`rememberMaterial3AttributeStyleResolver()` automatically maps Arranger attributes to the following Material 3 design tokens:

### Heading Typography Mapping (HeadingKey)

| Heading Level | Material 3 Typography Token |
|---|---|
| `HeadingLevel.H1` | `MaterialTheme.typography.displayLarge` |
| `HeadingLevel.H2` | `MaterialTheme.typography.displayMedium` |
| `HeadingLevel.H3` | `MaterialTheme.typography.headlineLarge` |
| `HeadingLevel.H4` | `MaterialTheme.typography.headlineMedium` |
| `HeadingLevel.H5` | `MaterialTheme.typography.titleLarge` |
| `HeadingLevel.H6` | `MaterialTheme.typography.titleMedium` |

### Blockquote Mapping (BlockquoteKey)

- **Typography**: `MaterialTheme.typography.bodyMedium`
- **Color**: `MaterialTheme.colorScheme.onSurfaceVariant` (subtle contrast against body text)

### Hyperlink Mapping (LinkKey)

- **Style**: `TextDecoration.Underline`
- **Color**: `MaterialTheme.colorScheme.primary` (theme-aware brand color adapting to dark mode and Dynamic Color)

### Inline Code Mapping (InlineCodeKey)

- **Typography**: `FontFamily.Monospace`
- **Background**: `MaterialTheme.colorScheme.surfaceContainerHighest`
- **Color**: `MaterialTheme.colorScheme.onSurfaceVariant`

### Material 3 Block Decoration (Material3BlockDecorator)

The `:arranger-richtext-editor-material3` module also provides `rememberMaterial3BlockDecorator()` to render theme-consistent container decorations behind multi-line blocks:

- **Blockquote**: Draws a vertical quote bar using `colorScheme.primary`.
- **Code Block**: Draws a rounded container using `colorScheme.surfaceVariant` background and `colorScheme.outlineVariant` border outline.

```kotlin
RichTextEditor(
    state = state,
    styleResolver = rememberMaterial3AttributeStyleResolver(),
    blockDecorator = rememberMaterial3BlockDecorator(),
)
```

See [Visual Block Decorations](block-decorations.md) for full architecture details and custom decorator examples.

### Automatic Dark / Light Theme Adaptation & Cursor Behavior

`rememberMaterial3AttributeStyleResolver()` internally tracks `remember(typography, colorScheme)`. When users toggle system dark mode or switch themes within the application, the entire editor's text colors, headings, links, inline code chips, and blockquote styles adapt dynamically without manual recomposition logic.

Furthermore, `RichTextEditor` and `WysiwygEditor` automatically resolve their cursor (`cursorBrush`) to the foreground text color (`textStyle.color`) when not explicitly configured. Setting `textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)` ensures both the text and the caret seamlessly adapt to dark backgrounds.

---

## Related Documentation

- [**Visual Block Decorations**](block-decorations.md): BlockContainer, quote bars, code block containers, and uniform padding.
- [**Spans and Paragraphs**](spans-and-paragraphs.md): Scopes and applicability of span and paragraph attributes.
- [**Built-in Attributes Reference**](built-in-attributes.md): Complete list of all built-in attribute keys provided by Arranger.
- [**Custom Attributes**](custom-attributes.md): Defining domain-specific attribute keys and custom resolvers.
