# Custom Attributes

Beyond standard formatting, applications frequently require domain-specific styling or metadata (e.g. pastel highlighter pens, @mentions, #hashtags, editorial comment anchors, spoiler tags). Arranger enables developers to create custom `AttributeKey<T>` types and resolve them into Compose styles with complete compile-time type safety.

<div align="center" markdown>

![Custom Attribute Sample](../images/custom-attribute.png){ width="500" }

</div>

---

## Four Implementation Steps

Introducing custom attributes involves four straightforward steps:

1. **Define Attribute Key**: Implement `SpanAttributeKey<T>` or `ParagraphAttributeKey<T>`.
2. **Add DSL Extensions**: Define helper functions on `AttributeEditScope` (optional).
3. **Configure Style Resolver**: Map the attribute key to Compose `SpanStyle` or `ParagraphStyle` in `AttributeStyleResolver`.
4. **Render in Editor**: Pass the custom resolver to `RichTextEditor(styleResolver = ...)`.

---

## Implementation Example 1: Highlighter Pen (`HighlightKey`)

An example of a custom flag span attribute without a parameter value (`Unit`):

### Step 1: Define Attribute Key

```kotlin
import dev.mkeeda.arranger.richtext.SpanAttributeKey

// Define as a singleton object or data object
object HighlightKey : SpanAttributeKey<Unit> {
    override val name: String = "Highlight"
    override val defaultValue: Unit = Unit
}
```

### Step 2: Add DSL Extensions

Add extension functions so the attribute can be called intuitively inside `edit { ... }` blocks.

```kotlin
import dev.mkeeda.arranger.richtext.AttributeEditScope

fun AttributeEditScope.highlight() {
    setSpanAttribute(HighlightKey, Unit)
}

fun AttributeEditScope.clearHighlight() {
    setSpanAttribute(HighlightKey, null)
}
```

### Step 3: Configure Style Resolver

Using `AttributeStyleResolver(base = DefaultAttributeStyleResolver)` allows you to retain all default formatting rules (bold, underline, etc.) while layering in custom styling rules.

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver

val CustomAttributeResolver = AttributeStyleResolver(base = DefaultAttributeStyleResolver) {
    spanStyle(HighlightKey) {
        SpanStyle(
            background = Color(0xFFFFF59D), // Soft highlighter yellow
            color = Color(0xFFE65100),      // Deep orange text
            fontWeight = FontWeight.Bold,
        )
    }
}
```

### Step 4: Render in Editor

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.rangeOf

@Composable
fun CustomAttributeScreen() {
    val text = "Arranger provides robust support for custom attributes."

    val state = remember {
        RichTextState(
            initialText = RichString(text).edit {
                editAttributes(range = text.rangeOf("custom attributes")) {
                    highlight()
                }
            }
        )
    }

    RichTextEditor(
        state = state,
        styleResolver = CustomAttributeResolver,
        modifier = Modifier.fillMaxWidth(),
    )
}
```

---

## Implementation Example 2: Metadata Attribute (Mentions)

You can also attach arbitrary structured objects (such as user IDs or metadata) as attribute values:

```kotlin
data class MentionData(val userId: String, val username: String)

object MentionKey : SpanAttributeKey<MentionData> {
    override val name: String = "Mention"
    override val defaultValue: MentionData = MentionData(userId = "", username = "")
}

// Define resolver mapping
val MentionStyleResolver = AttributeStyleResolver(base = DefaultAttributeStyleResolver) {
    spanStyle(MentionKey) { data ->
        SpanStyle(
            color = Color(0xFF1976D2), // Blue link-style text
            fontWeight = FontWeight.SemiBold,
        )
    }
}
```

When tapped, you can retrieve `MentionData` directly via `event.span.attributes[MentionKey]` to navigate to a user profile or display a hovercard.

---

## Related Documentation

- [**Theming and Material 3**](theming-and-m3.md): Style resolution architecture and Material 3 integration.
- [**Interactive Spans**](../interactions/span-clicks.md): Handling click events on custom attributes.
- [**Autocomplete**](../interactions/autocomplete.md): Inserting custom attributes via trigger suggestions.
