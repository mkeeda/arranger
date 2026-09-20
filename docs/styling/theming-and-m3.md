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
- `InlineCodeKey` -> Monospace font (`FontFamily.Monospace`) with light gray background
- `TextColorKey` -> Specified `RgbaColor`
- `BackgroundColorKey` -> Specified `RgbaColor`
- `HeadingKey` -> Scaled font size and bold weight matching heading levels
- `TextAlignmentKey` -> Specified horizontal alignment

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
    implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.4.0-alpha04")
}
```

### 2. Use rememberMaterial3AttributeStyleResolver

Invoke the `@Composable fun rememberMaterial3AttributeStyleResolver()` function and pass the result to the editor's `styleResolver` parameter:

```kotlin
@Composable
fun Material3EditorScreen() {
    val state = remember { RichTextState() }

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

### Automatic Dark / Light Theme Adaptation

`rememberMaterial3AttributeStyleResolver()` internally tracks `remember(typography, colorScheme)`. When users toggle system dark mode or switch themes within the application, the entire editor's text colors, headings, and callout styles adapt dynamically without manual recomposition logic.

---

## Related Documentation

- [**Spans and Paragraphs**](spans-and-paragraphs.md): Scopes and applicability of span and paragraph attributes.
- [**Built-in Attributes Reference**](built-in-attributes.md): Complete list of all built-in attribute keys provided by Arranger.
- [**Custom Attributes**](custom-attributes.md): Defining domain-specific attribute keys and custom resolvers.
