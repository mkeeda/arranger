# Arranger - Type-safe Rich Text for Jetpack Compose

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
* **`ArrangerEditor`**: A simple, declarative Composable wrapping `BasicTextField` with our state and transformation.

## Roadmap
* **The Core (Pure Kotlin):** Build the `RichTextBuffer` abstraction, `AttributeRangedTree`, and `AttributeKey` infrastructure.
* **Runs API:** Implement logic to chunk strings into semantic `RichRun` iterators.
* **Compose Integration:** Hook into `TextFieldBuffer` changes to shift/update the attribute tree indices reliably.
* **Formatting Constraints:** Implement clipboard parsing and filtering using `InputTransformation`.