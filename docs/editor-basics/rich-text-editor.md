# RichTextEditor

`RichTextEditor` is the core Compose UI editor component of Arranger. Built on Compose Foundation 2.x (`BasicTextField`), it integrates rich attribute rendering, bidirectional state synchronization, keyboard shortcuts, tap detection, and autocomplete while preserving peak rendering performance.

<div align="center" markdown>

![RichTextEditor Basic Usage](../images/basic-usage.png){ width="500" }

</div>

---

## Overview & Signature

`RichTextEditor` can be positioned and customized intuitively, adhering to standard Compose Foundation conventions.

```kotlin
@Composable
fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null,
    styleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
    listMarkerResolver: ListMarkerResolver = DefaultListMarkerResolver,
    onSpanClick: ((SpanClickEvent) -> Unit)? = null,
    autocompleteTriggers: List<AutocompleteTrigger> = emptyList(),
    onAutocompleteChange: ((AutocompleteMatch?) -> Unit)? = null,
)
```

---

## Parameter Reference

| Parameter | Type | Default Value | Description |
|---|---|---|---|
| `state` | `RichTextState` | *(Required)* | State object managing the editor's text content, attribute spans, selection range, and undo/redo history. |
| `modifier` | `Modifier` | `Modifier` | Compose modifier specifying size, layout, padding, and outer styling. |
| `enabled` | `Boolean` | `true` | Controls whether the editor is interactive. When `false`, focus, text input, and tap events are disabled. |
| `readOnly` | `Boolean` | `false` | Read-only mode. Disables text mutations while preserving text selection and span click interactions (`onSpanClick`). |
| `textStyle` | `TextStyle` | `TextStyle.Default` | Base typography and styling (font family, font size, default text color) applied across the editor. |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | Software keyboard configuration (IME action, capitalization, autocorrect, keyboard type). |
| `onKeyboardAction` | `KeyboardActionHandler?` | `null` | Callback handler executed when the user triggers an IME action (e.g. Done, Search, Send). |
| `lineLimits` | `TextFieldLineLimits` | `Default` | Line count configuration (`SingleLine` or `MultiLine(min, max)`). |
| `onTextLayout` | `(Density.(...) -> Unit)?` | `null` | Low-level callback invoked when text layout calculation completes. |
| `scrollState` | `ScrollState` | `rememberScrollState()` | Scroll state managing and synchronizing vertical scrolling for multi-line editors. |
| `interactionSource` | `MutableInteractionSource?` | `null` | Stream of interaction events (hover, press, focus) for custom UI feedback. |
| `cursorBrush` | `Brush` | `SolidColor(Color.Black)` | Brush used to draw the cursor (caret). Can be styled with solid colors or gradients matching your theme. |
| `decorator` | `TextFieldDecorator?` | `null` | Compose Foundation 2.x decorator for outer borders, padding, and placeholders. |
| `styleResolver` | `AttributeStyleResolver` | `DefaultAttributeStyleResolver` | Resolver translating `AttributeContainer` entries into Compose `SpanStyle` and `ParagraphStyle`. |
| `listMarkerResolver` | `ListMarkerResolver` | `DefaultListMarkerResolver` | Renderer mapping list depth and paragraph index to bullet characters or numbering text. |
| `onSpanClick` | `((SpanClickEvent) -> Unit)?` | `null` | Event handler invoked when a formatted span is clicked or tapped. Calling `event.consume()` suppresses cursor movement. |
| `autocompleteTriggers` | `List<AutocompleteTrigger>` | `emptyList()` | List of trigger definitions monitoring autocomplete queries (such as `@mentions` and `#tags`). |
| `onAutocompleteChange` | `((AutocompleteMatch?) -> Unit)?` | `null` | Invoked when an autocomplete query matches or is dismissed, used to drive suggestion popups. |

---

## Key Capabilities & Patterns

### 1. Placeholders and Decorators (`decorator`)

Compose Foundation 2.x provides the `TextFieldDecorator` interface, allowing developers to wrap the text input area with outer borders, padding, and conditional placeholder text when content is empty.

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState

@Composable
fun DecoratedRichTextEditor(state: RichTextState) {
    RichTextEditor(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(12.dp),
        decorator = { innerTextField ->
            Box {
                if (state.richString.text.isEmpty()) {
                    Text(
                        text = "Type here...",
                        color = Color.Gray,
                    )
                }
                innerTextField()
            }
        }
    )
}
```

### 2. Interactive Span Clicks (`onSpanClick`)

You can handle click and tap events on formatted spans (such as links, user mentions, or hashtag badges).

```kotlin
RichTextEditor(
    state = state,
    onSpanClick = { event ->
        val url = event.span.attributes[LinkKey]
        if (url != null) {
            println("URL clicked: $url")
            // Consume the event to prevent moving the text cursor to the tapped position
            event.consume()
        }
    }
)
```

!!! tip "Importance of event.consume()"
    Calling `event.consume()` informs the editor engine that this tap was handled as a link or span action, automatically canceling the default caret relocation and text selection. See [Interactive Spans](../interactions/span-clicks.md) for full details.

### 3. Read-Only Rich Text Viewer (`readOnly = true`)

By setting `readOnly = true`, the editor functions as a non-editable rich text viewer while preserving text selection and span click interactions.

```kotlin
RichTextEditor(
    state = state,
    readOnly = true,
    onSpanClick = { event ->
        // Link clicks remain fully functional in read-only mode
        event.span.attributes[LinkKey]?.let { openBrowser(it) }
    }
)
```

### 4. Comprehensive Demo

See the full editor in action below, showcasing toolbar interaction, formatting, and style resolution:

<div align="center" markdown>

![RichTextEditor Demo](../images/rich-text-editor-demo.gif){ width="400" }

</div>

---

## Related Documentation

- [**WysiwygEditor**](wysiwyg-editor.md): Real-time Markdown shortcut styling editor component.
- [**State Management (RichTextState)**](state-management.md): Batch edits, typing attributes, and undo/redo history.
- [**Theming and Material 3**](../styling/theming-and-m3.md): Custom styling via `AttributeStyleResolver` and Material 3 token mapping.
