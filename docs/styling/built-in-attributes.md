# Built-in Attributes

Arranger includes a rich set of built-in character and paragraph attributes covering standard document editing requirements. All attributes are defined as compile-time type-safe `AttributeKey<T>` instances and can be manipulated via an expressive DSL.

---

## Standard Attribute Keys

| Attribute Key | Kind | Value Type `T` | Default Value | Description & Purpose |
|---|---|---|---|---|
| `BoldKey` | Span | `Unit` | `Unit` | Bold font weight styling. |
| `ItalicKey` | Span | `Unit` | `Unit` | Italic font style. |
| `UnderlineKey` | Span | `Unit` | `Unit` | Underline text decoration. |
| `StrikethroughKey` | Span | `Unit` | `Unit` | Strikethrough text decoration. |
| `InlineCodeKey` | Span | `Unit` | `Unit` | Monospace font family and background color styling for inline code. |
| `TextColorKey` | Span | `RgbaColor` | `RgbaColor.Unspecified` | Foreground text color. |
| `BackgroundColorKey` | Span | `RgbaColor` | `RgbaColor.Unspecified` | Text highlight background color. |
| `FontSizeKey` | Span | `TextSize` | `TextSize.Unspecified` | Text font size (in sp units). |
| `LinkKey` | Span | `String` | `""` | Clickable hyperlink URL. |
| `HeadingKey` | BlockType | `HeadingLevel` | `HeadingLevel.Unspecified` | Heading level (H1 to H6). |
| `TextAlignmentKey` | Alignment | `TextAlignment` | `TextAlignment.Unspecified` | Paragraph horizontal text alignment (Left, Center, Right, Justify). |
| `BlockquoteKey` | BlockType | `Unit` | `Unit` | Quoted callout block formatting. |
| `BulletListKey` | BlockType | `ListIndentLevel` | `ListIndentLevel.Unspecified` | Bulleted list item (indentation Level1 to Level6). |
| `OrderedListKey` | BlockType | `ListIndentLevel` | `ListIndentLevel.Unspecified` | Numbered list item (automatic numbering, indentation Level1 to Level6). |

---

## Domain Value Classes & Types

To enable pure Kotlin Multiplatform usage without UI dependencies, lightweight value classes are provided for color and size representations.

### 1. `RgbaColor` (Color Representation)

A 64-bit value class packing RGBA components. When using Compose UI, bidirectional conversions with Compose `Color` are available.

```kotlin
// Convert to and from Compose Color
val composeColor: Color = rgbaColor.toColor()
val arrangerColor: RgbaColor = Color.Red.toRgbaColor()
```

### 2. `TextSize` (Font Size)

A value class representing dimensions in sp units. Bidirectional conversions with Compose `TextUnit` are available.

```kotlin
// Convert to and from Compose TextUnit
val textUnit: TextUnit = textSize.toTextUnit()
val arrangerSize: TextSize = 18.sp.toTextSize()
```

### 3. `HeadingLevel` (Heading Levels)

```kotlin
enum class HeadingLevel {
    H1, H2, H3, H4, H5, H6, Unspecified
}
```

### 4. `TextAlignment` (Text Alignment)

```kotlin
enum class TextAlignment {
    Left, Center, Right, Justify, Unspecified
}
```

### 5. `ListIndentLevel` (List Indentation Depth)

```kotlin
enum class ListIndentLevel {
    Level1, Level2, Level3, Level4, Level5, Level6, Unspecified
}
```

---

## AttributeEditScope DSL Functions

Within `RichStringScope.editAttributes` or `RichTextBuffer` scopes, the following DSL functions apply and clear attributes intuitively.

### Application and Clearing Pairs

| Style | Apply Function | Clear Function |
|---|---|---|
| Bold | `bold()` | `clearBold()` |
| Italic | `italic()` | `clearItalic()` |
| Underline | `underline()` | `clearUnderline()` |
| Strikethrough | `strikethrough()` | `clearStrikethrough()` |
| Inline Code | `inlineCode()` | `clearInlineCode()` |
| Text Color | `textColor(color: Color)` / `textColor(color: RgbaColor)` | `clearTextColor()` |
| Background Color | `backgroundColor(color: Color)` / `backgroundColor(...)` | `clearBackgroundColor()` |
| Font Size | `fontSize(size: TextUnit)` / `fontSize(size: TextSize)` | `clearFontSize()` |
| Hyperlink | `link(url: String)` | `clearLink()` |
| Heading | `headingLevel(level: HeadingLevel?)` | `clearHeadingLevel()` |
| Alignment | `textAlignment(alignment: TextAlignment?)` | `clearTextAlignment()` |
| Blockquote | `blockquote()` | `clearBlockquote()` |
| Bullet List | `bulletList(level: ListIndentLevel)` | `clearBulletList()` |
| Ordered List | `orderedList(level: ListIndentLevel)` | `clearOrderedList()` |
| Clear All | `clearAll()` | - |

---

## Applying Formatting from Toolbars

To apply, toggle, or clear formatting from toolbar buttons and menus without writing low-level `state.edit` blocks, `RichTextState` provides high-level convenience extensions (`toggleFormat`, `applyFormat`, `removeFormat`, `clearFormats`).

For full details, patterns, and focus-prevention guidelines, see [**Toolbars & Focus Management**](../interactions/toolbars.md).

---

## Related Documentation

- [**Spans and Paragraphs**](spans-and-paragraphs.md): Operational differences between span and paragraph attributes and mutual exclusion.
- [**Custom Attributes**](custom-attributes.md): Defining domain-specific custom attribute keys.
- [**Theming and Material 3**](theming-and-m3.md): How attributes resolve into Compose visual styles.
