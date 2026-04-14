# Arranger - Type-safe Rich Text for Jetpack Compose

> [!WARNING]
> **Work In Progress**: This library is currently under active development. APIs are unstable and subject to change without notice.

## Project Vision
The goal of "Arranger" is to provide a "declarative, type-safe, and immutable string manipulation experience similar to SwiftUI's `AttributedString`" to Jetpack Compose and Kotlin Multiplatform (KMP). We aim to break away from the tedious, error-prone index manipulations required by the existing `AnnotatedString` and the traditional WYSIWYG approaches.

## Target Developer Experience (DX)
* **Type-Safe Custom Attributes:** Define and apply UI-specific styles (like `SpanStyle`) and domain-specific attributes (e.g., `@Mention`, `#Hashtag`) with full compile-time safety.
* **Run-Based Manipulation:** Treat text not just as an array of characters, but as "Runs" (chunks of text with identical attributes). This allows for semantic iteration, searching, and editing.
* **Declarative Formatting Constraints:** Provide a way to declaratively define constraints (e.g., "This text field only allows bold text and links") to automatically strip unwanted styles during paste or input.
* **Native Compose Integration (1.7+):** Elegantly separate state management and UI rendering by leveraging the latest `TextFieldState` and `OutputTransformation`.

## Why SwiftUI's Paradigm? (Inspiration)
Compared to Android's traditional `SpannableStringBuilder` or Compose's `AnnotatedString`, SwiftUI offers superior API design:
* **Type Safety:** We will simulate SwiftUI's `@dynamicMemberLookup` using Kotlin's Extension Properties to allow intuitive, property-like access to attributes.
* **Semantic "Runs":** Instead of managing `startIndex` and `endIndex`, developers can iterate over `Runs` (e.g., "find all chunks of mentions").
* **Value Semantics:** The core text data structures will be immutable, ensuring thread safety and predictable UI re-rendering, which is highly compatible with Compose.
* **Paste Protection:** Prevent "paste pollution" (unintended massive fonts or weird colors) via declarative constraints without writing messy parsers.

## Core Architecture Overview
To ensure scalability up to PC-class text sizes and pure Kotlin compatibility (KMP), the architecture is layered:

### Pure Kotlin Core (Data Structures)
* **`RichTextBuffer`**: An abstraction interface for the underlying string storage. The MVP will use a simple implementation, but it is designed to be replaceable with advanced structures (like Rope or Piece Table) for handling massive documents in the future.
* **`AttributeKey<T>`**: Defines the data type of an attribute.
* **`RichString` & `RichRun`**: Immutable representations of text and its semantic chunks.
* **`AttributeRangedTree`**: An internal data structure (like an Interval Tree) to manage attributes by range, independent of string indices.

### Compose UI Layer
* **`RichTextState`**: Wraps `TextFieldState` and holds the `AttributeRangedTree`. It acts as the single source of truth.
* **`RichTextOutputTransformation`**: Converts the plain text and internal attribute tree into Compose's `AnnotatedString` purely at render time.
* **`RichTextEditor`**: A simple, declarative Composable wrapping `BasicTextField` with our state and transformation.

## Quick Start (Current Usage)

```kotlin
// 1. Define custom Attribute Keys
object BoldAttributeKey : AttributeKey<Unit>
object LinkAttributeKey : AttributeKey<String>

@Composable
fun ArrangerExample() {
    // 2. Initialize RichTextState
    val state = remember { 
        RichTextState(
            initialRichString = RichString(
                text = "Hello Arranger!",
                attributes = attributeContainerOf(BoldAttributeKey to Unit)
            )
        ) 
    }

    // 3. Declaratively map domain Attributes to Compose UI Styles
    val styleResolver = remember {
        AttributeStyleResolver {
            spanStyle(BoldAttributeKey) {
                SpanStyle(fontWeight = FontWeight.Bold)
            }
            spanStyle<String>(LinkAttributeKey) { url ->
                SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)
            }
        }
    }

    // 4. Render and Edit natively via Compose 1.7
    RichTextEditor(
        state = state,
        styleResolver = styleResolver,
        textStyle = TextStyle(fontSize = 16.sp)
    )

    // 5. Safely modify attributes using the edit DSL
    Button(onClick = {
        state.edit {
            setAttribute(key = LinkAttributeKey, value = "https://example.com", range = 0..4)
        }
    }) {
        Text("Make 'Hello' a Link")
    }
}
```

### Development Roadmap

- [x] **1. Core Data Structures (The Core)**
    * Implementation of a range-based data structure (e.g., Interval Tree) to manage attributes by range rather than character indices.
    * Foundation setup for `AttributeKey` and extension properties.
- [x] **2. Runs API Implementation**
    * Logic to segment strings into semantic chunks (`RichRun`) that can be operated on as an iterator.
- [x] **3. Integration with TextFieldState / OutputTransformation**
    * Logic to hook into `TextFieldBuffer` modifications (insertions/deletions) and dynamically track/shift the indices of the underlying attribute tree.
- [ ] **4. Implementation of Basic Built-in AttributeKeys**
    * Basic character-level decorations (e.g., Bold, Text Color, Underline, Italics, Font Size).
    * Paragraph-level decorations (e.g., Headings, Bullet Lists).
- [ ] **5. Custom Attribute Mapping APIs**
    * Expose mechanisms allowing developers to customize how default `AttributeKey`s are translated into Compose `AnnotatedString` styles.
- [ ] **6. Declarative Formatting Constraints**
    * Mechanism leveraging `InputTransformation` to parse pasted clipboard HTML/RichText and strictly filter allowed attributes based on an access list.
- [ ] **7. Full Attribute Restoration on Undo/Redo**
    * Seamlessly align with `TextFieldState`'s native Undo/Redo to accurately restore historical attribute ranges.
- [ ] **8. Performance Tuning**
    * Optimize internal data structures to production-grade performance variants (e.g., Rope or Piece Table) for large text handling.
- [ ] **9. Kotlin Multiplatform (KMP) Support**
    * Ensure the core data structures, state management, and formatting logic are fully platform-agnostic to support Compose Multiplatform distribution (iOS, Desktop, Web).