# Interactive Spans & Click Handling

Rich text editors often require interactive in-text elements:

- Clicking a **hyperlink** to open a web browser
- Tapping a **@mention** badge to open a user profile card
- Clicking a **#hashtag** to filter related content or navigate to a channel
- Activating interactive footnotes, tooltips, or preview cards

Arranger provides a first-class `onSpanClick` callback with event consumption control to distinguish between navigating an interactive span and repositioning the editor cursor.

---

## The onSpanClick Callback

Both `RichTextEditor` and `WysiwygEditor` expose the `onSpanClick` callback:

```kotlin
@Composable
public fun RichTextEditor(
    state: RichTextState,
    // ...
    onSpanClick: ((SpanClickEvent) -> Unit)? = null,
)
```

---

## SpanClickEvent & Event Consumption

When a user taps inside the editor, Arranger maps the tap coordinate to the character index and inspects the active `RichSpan`. If a span exists at that position, `onSpanClick` is called with a `SpanClickEvent`:

```kotlin
package dev.mkeeda.arranger.richtext.editor

public class SpanClickEvent(
    public val span: RichSpan,
) {
    public var isConsumed: Boolean = false
        private set

    public fun consume() {
        isConsumed = true
    }
}
```

### Why event.consume() is Critical

In standard Compose text fields, any pointer tap relocates the cursor to the tapped position and clears text selection.

When handling an interactive span:

- Calling `event.consume()` informs Arranger to consume the pointer event in `PointerEventPass.Initial`.
- This **suppresses default cursor relocation**, maintains the current cursor position, and prevents keyboard focus disruptions.
- If `event.consume()` is **not** called, the click falls through to standard editor handling, positioning the cursor where the user tapped.

---

## Handling Links, Mentions, and Hashtags

### 1. Handling Hyperlinks (`LinkKey`)

```kotlin
val uriHandler = LocalUriHandler.current

RichTextEditor(
    state = state,
    onSpanClick = { event ->
        val url = event.span.attributes[LinkKey]
        if (!url.isNullOrBlank()) {
            event.consume() // Prevent cursor movement
            uriHandler.openUri(url)
        }
    }
)
```

### 2. Handling Mentions with Custom Attributes

You can attach custom metadata (such as a User ID) to a mention span:

```kotlin
// Define a custom mention attribute key
public data object MentionUserIdKey : SpanAttributeKey<String> {
    override val name: String = "mention-user-id"
    override val defaultValue: String = ""
}

// In the editor:
RichTextEditor(
    state = state,
    onSpanClick = { event ->
        val userId = event.span.attributes[MentionUserIdKey]
        if (userId != null) {
            event.consume()
            navController.navigate("profile/$userId")
        }
    }
)
```

### 3. Read-Only Mode Interaction

A common pattern is displaying rich articles or chat messages in read-only mode:

```kotlin
RichTextEditor(
    state = state,
    readOnly = true, // Disables text input and keyboard editing
    onSpanClick = { event ->
        val url = event.span.attributes[LinkKey]
        if (url != null) {
            event.consume()
            openUrl(url)
        }
    }
)
```

!!! note "Read-Only vs Disabled"
    Even when `readOnly = true`, pointer click detection and `onSpanClick` remain fully operational. Pointer events are only disabled if `enabled = false`.

---

## Complete Multi-Type Click Handler Example

```kotlin
@Composable
fun InteractiveArticleViewer(
    state: RichTextState,
    onOpenUrl: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    RichTextEditor(
        state = state,
        readOnly = true,
        onSpanClick = { event ->
            val attrs = event.span.attributes

            when {
                attrs.containsKey(LinkKey) -> {
                    event.consume()
                    onOpenUrl(attrs.getOrDefault(LinkKey))
                }
                attrs.containsKey(MentionUserIdKey) -> {
                    event.consume()
                    onOpenProfile(attrs.getOrDefault(MentionUserIdKey))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## Summary

- Pass `onSpanClick = { event -> ... }` to intercept taps on formatted spans.
- Inspect `event.span.attributes` to retrieve link URLs or custom metadata.
- Call `event.consume()` to prevent the cursor from jumping to the tapped span.
- Works seamlessly in both editable and `readOnly = true` modes.
