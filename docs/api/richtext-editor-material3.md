# Module :richtext-editor-material3 API Reference

The `:richtext-editor-material3` module provides first-class Material 3 design system integration for Arranger. It automatically resolves Arranger paragraph formatting keys (such as Headings and Blockquotes) using the active `MaterialTheme.typography` and `MaterialTheme.colorScheme` tokens.

---

## Interactive Dokka KDoc

For complete parameter types, return types, and composable signatures, view the interactive Dokka documentation:

👉 **[Module :richtext-editor-material3 Dokka KDoc](https://mkeeda.github.io/arranger/api/dokka/richtext-editor-material3/)**

---

## Coordinates & Dependency

```kotlin
// build.gradle.kts (commonMain)
implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.4.0-alpha03")
```

---

## Factory API

### `rememberMaterial3AttributeStyleResolver()`

A composable factory function that produces an `AttributeStyleResolver` connected to the current ambient Material 3 theme.

```kotlin
@Composable
fun rememberMaterial3AttributeStyleResolver(): AttributeStyleResolver
```

This resolver wraps `DefaultAttributeStyleResolver` and dynamically queries the local `MaterialTheme`:
- Re-evaluates typography and color styles when switching between light and dark themes.
- Keeps standard inline spans (bold weight, italic slant, monospace fonts, custom colors) intact while aligning structural headings and quotes with M3 standards.

---

## Design Token Mapping Specifications

| Arranger Attribute | Target Material 3 Typography Token | Target Material 3 Color Token |
|---|---|---|
| `HeadingLevel.H1` | `MaterialTheme.typography.displayLarge` | `Color.Unspecified` (inherits) |
| `HeadingLevel.H2` | `MaterialTheme.typography.displayMedium` | `Color.Unspecified` (inherits) |
| `HeadingLevel.H3` | `MaterialTheme.typography.headlineLarge` | `Color.Unspecified` (inherits) |
| `HeadingLevel.H4` | `MaterialTheme.typography.headlineMedium` | `Color.Unspecified` (inherits) |
| `HeadingLevel.H5` | `MaterialTheme.typography.titleLarge` | `Color.Unspecified` (inherits) |
| `HeadingLevel.H6` | `MaterialTheme.typography.titleMedium` | `Color.Unspecified` (inherits) |
| `BlockquoteKey` | `MaterialTheme.typography.bodyMedium` | `MaterialTheme.colorScheme.onSurfaceVariant` |

---

## Usage Example

Simply pass the remembered resolver into either `RichTextEditor` or `WysiwygEditor`:

```kotlin
@Composable
fun Material3DocumentEditor(state: RichTextState) {
    val m3StyleResolver = rememberMaterial3AttributeStyleResolver()

    RichTextEditor(
        state = state,
        modifier = Modifier.fillMaxSize(),
        styleResolver = m3StyleResolver,
    )
}
```

---

## Customizing the Material 3 Resolver

You can extend the Material 3 resolver with additional custom attributes using the `AttributeStyleResolver` builder DSL:

```kotlin
@Composable
fun CustomThemedEditor(state: RichTextState) {
    val baseResolver = rememberMaterial3AttributeStyleResolver()
    val customResolver = remember(baseResolver) {
        AttributeStyleResolver(base = baseResolver) {
            spanStyle(SpoilerKey) { spoiler ->
                SpanStyle(background = Color.DarkGray, color = Color.Transparent)
            }
        }
    }

    RichTextEditor(
        state = state,
        styleResolver = customResolver,
    )
}
```
