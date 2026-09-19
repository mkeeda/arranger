# Arranger

Type-safe Rich Text Editor Engine for Compose Multiplatform.

<div align="center" markdown>

![Arranger Rich Text Editor Demo](images/rich-text-editor-demo.gif){ width="400" }

</div>

[![CI](https://github.com/mkeeda/arranger/actions/workflows/ci.yml/badge.svg)](https://github.com/mkeeda/arranger/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.mkeeda.arranger/arranger-richtext-editor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.mkeeda.arranger%22%20AND%20a:%22arranger-richtext-editor%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Arranger is a declarative, type-safe rich text editor engine and UI framework designed for **Compose Multiplatform** (Android, Desktop JVM, iOS, Web/WasmJs).

Much like ProseMirror and Lexical in the web ecosystem, Arranger provides the robust foundational layer required to build modern rich document editing experiences (such as Notion, Bear, or Slack-like editors) natively in Compose.

---

## Key Features

### ✍️ Polished Editor Experiences
- **Dual Editor Components**: `RichTextEditor` for toolbar-driven rich editing and `WysiwygEditor` for real-time Markdown shortcut styling while typing.
- **Intuitive Undo / Redo**: Full tracking of both text edits and formatting mutations with native keyboard shortcut support (<kbd>Cmd/Ctrl</kbd> + <kbd>Z</kbd>, <kbd>Shift</kbd> + <kbd>Cmd/Ctrl</kbd> + <kbd>Z</kbd>).
- **Smart Enter Key Handling**: Automatic heading reset, list continuation, and multi-level outdenting via `EnterKeyStrategy`.
- **Interactive Spans**: Tap detection (`onSpanClick`) for mentions, hashtags, and hyperlinks with cursor movement suppression (`event.consume()`).
- **Autocomplete Support**: Input query detection for `@mentions` and `#tags` with zero-calculation cursor-following popup positioning (`createPopupPositionProvider`).

### 🛡️ Type-Safe & Declarative Core Engine
- **Index-Free Mutation DSL**: Atomic, index-safe text and attribute editing via `state.edit { ... }` without manual offset arithmetic.
- **Semantic Attribute Queries ("Runs")**: Extraction and batch transformation of contiguous styled text ranges inspired by SwiftUI `AttributedString.Runs`.
- **Extensible Custom Attributes**: Compile-time type-safe `AttributeKey<T>` system for custom styles and domain metadata.
- **Exclusive Paragraph Categorization**: Automatic mutual exclusion between headings, blockquotes, and lists, as well as horizontal text alignments.

### 🌐 Interoperability & Ecosystem
- **Bidirectional Markdown & HTML Conversion**: Reversible import and export with external formats via `:richtext-markdown` and `:richtext-html`.
- **Material 3 Design System Integration**: Native alignment with `MaterialTheme` Typography and ColorScheme via `:richtext-editor-material3`.
- **Full Kotlin Multiplatform Conformance**: Identical behavior and architecture across Android, macOS, Windows, Linux, iOS, and Web (WasmJs).

---

## Why Arranger?

Compose Foundation (1.12+) provides low-level primitives such as `BasicTextField` and `addStyle`, but building a practical document editor surfaces numerous architectural challenges:

1. **Index Synchronization Complexity**: Manually recalculating span start and end offsets across insertions and deletions.
2. **Paragraph Boundaries & Formatting Inheritance**: Managing formatting reset upon newlines, heading dismissal, and nested list indentation.
3. **Atomic State Synchronization & Undo History**: Atomically undoing and redoing simultaneous plain-text edits and styling mutations.

Arranger resolves these complexities deep inside the core engine, exposing a clean, expressive API to developers.

```kotlin
// Arranger: Declarative, safe text and style mutation DSL
state.edit {
    insert(index = textLength, text = "New Chapter Title\n") {
        bold()
        headingLevel(HeadingLevel.H2)
    }
}
```

```kotlin
// Semantic batch editing with Runs: change all bold ranges to red text
state.edit {
    val boldRuns = state.richString.runs(BoldKey)
    editAll(boldRuns) {
        textColor(Color.Red)
    }
}
```

---

## Architecture Overview

Arranger is designed around a strict layered architecture with clean separation of concerns.

```mermaid
graph TD
    subgraph UI ["Compose UI Layer"]
        Editor["RichTextEditor / WysiwygEditor"]
        M3["richtext-editor-material3"]
        State["RichTextState (Compose State Hoisting)"]
    end

    subgraph Core ["Pure Kotlin Multiplatform Layer (:richtext)"]
        RS["RichString (Immutable Data Model)"]
        Attrs["AttributeContainer & AttributeKey"]
        Merger["Sweep-line Span Merging Algorithm"]
        Snapper["Paragraph Snapping & Mutual Exclusion"]
    end

    subgraph Interop ["Interoperability Layer"]
        MD["richtext-markdown (GFM)"]
        HTML["richtext-html (ksoup)"]
    end

    Editor --> State
    State --> RS
    M3 --> Editor
    RS --> Attrs
    RS --> Merger
    RS --> Snapper
    MD --> RS
    HTML --> RS
```

- **`:richtext` (Core)**: A pure Kotlin Multiplatform library with zero UI dependencies. Provides the immutable data model `RichString`, type-safe `AttributeKey`, sweep-line interval partitioning, and paragraph boundary snapping algorithms.
- **`:richtext-editor` (UI Engine)**: Integrates with Compose Foundation's `BasicTextField`, providing bidirectional state management via `RichTextState`, real-time keystroke processing, WYSIWYG conversions, and keyboard shortcuts.
- **`:richtext-editor-material3`**: Automatically maps Material 3 design tokens (Typography and ColorScheme) to editor styles.
- **`:richtext-markdown` / `:richtext-html`**: Provides bidirectional data conversion for external document formats.

---

## Documentation Guide

Browse the documentation by topic:

- [**Installation Guide**](getting-started/installation.md): Gradle dependency setup (KMP, Android, Version Catalog) and supported platforms.
- [**Quick Start**](getting-started/quickstart.md): Render a minimal rich text editor in just a few lines of code.
- [**RichTextEditor Basics**](editor-basics/rich-text-editor.md): In-depth guide to the standard editor component and its parameter reference.
- [**WysiwygEditor Basics**](editor-basics/wysiwyg-editor.md): Real-time Markdown shortcut styling editor component.
- [**State Management**](editor-basics/state-management.md): `RichTextState`, typing attributes, logical intersection model, and undo/redo history.
- [**Spans and Paragraphs**](styling/spans-and-paragraphs.md): Inline span attributes vs block paragraph attributes, snapping, and mutual exclusion.
- [**Built-in Attributes**](styling/built-in-attributes.md): Standard attributes for bold, italic, text color, headings, lists, and more.
- [**Custom Attributes**](styling/custom-attributes.md): Defining domain-specific attributes and custom style resolvers.
- [**Theming and Material 3**](styling/theming-and-m3.md): Custom styling via `AttributeStyleResolver` and seamless Material 3 integration.
