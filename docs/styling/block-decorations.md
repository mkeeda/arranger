# Visual Block Decorations

Arranger provides a declarative, Composable slot-based architecture for rendering rich container decorations around block-level paragraph elements—such as quote accent bars for **Blockquotes**, rounded background containers with borders for **Code Blocks**, and custom user-defined blocks like **Callouts**.

<div align="center" markdown>

![Visual Block Decorations](../images/visual-block-decorations.png){ width="500" }

</div>

---

## Why Visual Block Decorations?

In standard text editors, paragraph styling is typically confined to typography (such as margins, line height, and font styles). However, modern document editors (such as Notion, GitHub, and Slack) render distinct visual containers around multi-line blocks:

- **Blockquote**: A vertical accent bar running along the left margin of all contiguous quoted lines.
- **Code Block**: A distinct container background, border outline, and monospace styling encompassing all contiguous code lines.
- **Custom Blocks (e.g., Callout)**: Custom background colors, icons, and borders for specialized paragraph content.

In Compose, rendering background overlays behind dynamic text without layout jitter or coordinate misalignment is notoriously complex. Arranger solves this by decoupling decoration geometry into an overlay layer driven by `BlockDecorator`, while automatically merging contiguous paragraphs sharing identical attribute keys and values under the hood.

---

## Core Concepts & Architecture

### 1. Automatic Paragraph Merging (Internal)

Under the hood, Arranger automatically merges adjacent paragraph spans that share the same registered `AttributeKey` and attribute value into contiguous visual blocks. You do not need to create or manage any intermediate block data classes—Arranger handles the range calculations internally.

### 2. `BlockDecorator` & Builder DSL

`BlockDecorator` defines the set of supported `AttributeKey`s and provides composable decorations for them. You configure it declaratively with the builder DSL:

```kotlin
public interface BlockDecorator {
    public val supportedKeys: Set<AttributeKey<*>>

    @Composable
    public fun Decoration(
        key: AttributeKey<*>,
        value: Any?,
        context: BlockDecorationContext,
    )
}
```

Using the `BlockDecorator` factory function:

```kotlin
val myDecorator = BlockDecorator(base = rememberMaterial3BlockDecorator()) {
    on(CalloutKey) { type, context ->
        // Composable decoration for CalloutKey
    }
}
```

- **`base` delegation**: Any unhandled block types (such as `BlockquoteKey` or `CodeBlockKey`) automatically fallback to the `base` decorator.
- **Overriding**: Registering a key that exists in `base` overrides the base decoration for that key.

### 3. `BlockDecorationContext`

The `BlockDecorationContext` encapsulates all layout geometry, positioning, and line metrics:

- **`context.range`**: The character index range covering all paragraphs in this block.
- **`context.lineCount`**: The number of text lines spanned by this block.
- **`context.modifier`**: A pre-configured `Modifier` that automatically positions, sizes, and tracks the visual block across scrolling and layout changes without exposing raw coordinates.

### 4. `BlockContainer` Helper

Arranger provides `BlockContainer` to arrange common decoration elements safely behind or beside text:

```kotlin
@Composable
public fun BlockContainer(
    context: BlockDecorationContext,
    modifier: Modifier = Modifier,
    background: @Composable (BoxScope.() -> Unit)? = null,
    leading: @Composable (BoxScope.() -> Unit)? = null,
    trailing: @Composable (BoxScope.() -> Unit)? = null,
)
```

- **`background`**: Fills the entire block bounds with a background color or surface brush.
- **`leading`**: Places an accent bar or icon on the left edge (e.g. quote bar).
- **`trailing`**: Places content along the right edge.

---

## Built-in Decorators

### 1. `DefaultBlockDecorator`

The standard decorator included in `:arranger-editor` by default in both `RichTextEditor` and `WysiwygEditor`:

- **Blockquote**: A `3.dp` wide rounded vertical bar using an accent color with a subtle alpha tint.
- **Code Block**: A subtle neutral container background (`Color(0x0A000000)`) with `6.dp` rounded corners.

```kotlin
RichTextEditor(
    state = state,
    blockDecorator = DefaultBlockDecorator, // Applied by default
)
```

### 2. `Material3BlockDecorator` (:arranger-editor-material3)

For Material 3 applications, use `rememberMaterial3BlockDecorator()` to synchronize decorations with your current `ColorScheme`:

- **Blockquote**: Uses `colorScheme.outlineVariant` for the vertical quote bar.
- **Code Block**: Uses `colorScheme.surfaceVariant` for the container background and `colorScheme.outlineVariant` for the border outline.

```kotlin
@Composable
fun MyMaterial3Editor() {
    val state = rememberRichTextState()

    RichTextEditor(
        state = state,
        styleResolver = rememberMaterial3AttributeStyleResolver(),
        blockDecorator = rememberMaterial3BlockDecorator(),
    )
}
```

---

## Creating a Custom Block Decorator

Adding a custom visual block is straightforward:

1. **Define your attribute key**:
```kotlin
enum class CalloutType {
    Info,
    Warning,
}

object CalloutKey : BlockTypeAttributeKey<CalloutType> {
    override val name: String = "callout"
    override val defaultValue: CalloutType = CalloutType.Info
}
```

2. **Register decoration in `BlockDecorator`**:
```kotlin
val customBlockDecorator = BlockDecorator(base = rememberMaterial3BlockDecorator()) {
    on(CalloutKey) { type, context ->
        BlockContainer(
            context = context,
            background = {
                val bg = when (type) {
                    CalloutType.Info -> Color(0xFFE3F2FD)
                    CalloutType.Warning -> Color(0xFFFFF3E0)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg, RoundedCornerShape(8.dp))
                )
            },
            leading = {
                val accent = when (type) {
                    CalloutType.Info -> Color(0xFF1976D2)
                    CalloutType.Warning -> Color(0xFFF57C00)
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accent, RoundedCornerShape(2.dp))
                )
            },
        )
    }
}
```

3. **Pass to `RichTextEditor`**:
```kotlin
RichTextEditor(
    state = state,
    blockDecorator = customBlockDecorator,
)
```
