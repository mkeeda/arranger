# Arranger - Declarative, Type-safe Rich Text Editor Engine for Compose Multiplatform

[![CI](https://github.com/mkeeda/arranger/actions/workflows/ci.yml/badge.svg)](https://github.com/mkeeda/arranger/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.mkeeda.arranger/arranger-richtext-editor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.mkeeda.arranger%22%20AND%20a:%22arranger-richtext-editor%22)
[![Documentation](https://img.shields.io/badge/docs-mkeeda.github.io%2Farranger-deep_orange)](https://mkeeda.github.io/arranger/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Arranger is a declarative, type-safe rich text editor engine and UI ecosystem for **Compose Multiplatform** (Android, Desktop, iOS, Web/Wasm).  
Think of Arranger as the foundational framework (analogous to ProseMirror or Lexical) for building modern, full-featured text editing experiences on Compose—providing Notion-like WYSIWYG auto-formatting, interactive `@mentions` with zero-math cursor popups, dynamic enter-key strategies, semantic `Runs` querying, atomic undo/redo, and lossless Markdown/HTML interoperability.

<div align="center">
  <img src="./docs/images/rich-text-editor-demo.gif" width="380" alt="Arranger Rich Text Editor Demo"/>
</div>

---

### 📖 Official Documentation & Guides

Comprehensive guides, deep-dive architecture tutorials, and complete API references are available at our official documentation site:

👉 **[https://mkeeda.github.io/arranger/](https://mkeeda.github.io/arranger/)**

- **[Getting Started](https://mkeeda.github.io/arranger/getting-started/quickstart/)**: Installation, setup, and minimal editor examples.
- **[Editor Basics](https://mkeeda.github.io/arranger/editor-basics/rich-text-editor/)**: `RichTextEditor` vs `WysiwygEditor`, state management, and undo/redo.
- **[Styling & Formatting](https://mkeeda.github.io/arranger/styling/built-in-attributes/)**: Spans, paragraphs, headings, lists, custom attributes, and M3 integration.
- **[Inspecting Runs](https://mkeeda.github.io/arranger/styling/inspecting-runs/)**: Deep dive into semantic run querying and batch-editing.
- **[Interactions](https://mkeeda.github.io/arranger/interactions/autocomplete/)**: Mentions, hashtags, zero-math popup positioning, and clickable spans.
- **[Advanced Behaviors](https://mkeeda.github.io/arranger/advanced-behaviors/enter-key-strategies/)**: Enter-key strategies, nested list handling, and span merging rules.
- **[Markdown & HTML Interop](https://mkeeda.github.io/arranger/interop/markdown/)**: Bi-directional conversion and custom AST serializers.
- **[API Reference](https://mkeeda.github.io/arranger/api/)**: Multi-module Dokka API reference generated directly from KDoc.

---

## Supported Platforms

| Platform | Support Status | Target |
| :--- | :---: | :--- |
| **Android** | ✅ Supported | API Level 26+ |
| **Desktop (JVM)** | ✅ Supported | macOS, Windows, Linux |
| **iOS** | ✅ Supported | iOS 14+ |
| **Web** | ✅ Supported | WasmJs |

*Requires **Kotlin 2.4.10+** and Compose Multiplatform.*

---

## Installation

Arranger artifacts are published on **Maven Central**:

```kotlin
// build.gradle.kts (commonMain)
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core editor UI (includes arranger-richtext)
            implementation("dev.mkeeda.arranger:arranger-richtext-editor:0.4.0-alpha04")

            // Optional: Markdown / HTML conversions
            implementation("dev.mkeeda.arranger:arranger-richtext-markdown:0.4.0-alpha04")
            implementation("dev.mkeeda.arranger:arranger-richtext-html:0.4.0-alpha04")

            // Optional: Material 3 style resolver
            implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.4.0-alpha04")
        }
    }
}
```

---

## Quick Start

Create a rich text editor in just a few lines of Compose code:

```kotlin
@Composable
fun SimpleEditor() {
    val state = remember {
        RichTextState(
            initialText = RichString("Hello Compose!").edit {
                editAttributes(range = 6..12) { bold() }
            }
        )
    }

    RichTextEditor(
        state = state,
        modifier = Modifier.fillMaxWidth(),
    )
}
```

For live Markdown shortcuts as you type (Notion-style auto-formatting), simply swap in `WysiwygEditor`:

```kotlin
@Composable
fun WysiwygSample() {
    val state = remember { RichTextState() }
    WysiwygEditor(state = state, modifier = Modifier.fillMaxSize())
}
```

---

## 🚀 Key Highlights & What Makes Arranger Unique

While Compose provides basic text editing primitives, building a production-grade rich text editor (such as Notion, Slack, or Bear) requires orchestrating complex editing interactions. Arranger delivers that complete framework out of the box.

### 1. Dual Editor Flavors: Toolbar or WYSIWYG
Arranger provides two complementary editor components to fit any writing experience:
- **`RichTextEditor`**: Clean canvas designed for toolbar-driven editing without unexpected text transformations.
- **`WysiwygEditor`**: Notion-style instant Markdown auto-formatting as you type (`# `, `**bold**`, `- `, `> `) with immediate **Backspace reversal** (pressing Backspace restores raw markdown text) and seamless undo/redo integration.

<div align="center">
  <img src="./docs/images/wysiwyg.gif" width="480" alt="WYSIWYG Markdown Formatting Demo"/>
</div>

### 2. Autocomplete, Mentions & Zero-Math Cursor Popups
Building suggestion menus (`@mentions`, `#channels`, `:emojis:`) usually requires complex cursor coordinate calculations and scroll adjustments. Arranger solves this declaratively:
- **Declarative Triggers**: Configure `autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@"))`.
- **Zero-Math Positioning**: `match.createPopupPositionProvider()` automatically subtracts scroll offsets, aligns the popup to the cursor bottom, flips upward when screen space is constrained, and clamps within screen bounds.
- **Atomic Replacement**: `state.applyCompletion(match, replacement, attributes)` replaces the query, styles the mention, positions the caret, and commits an atomic undo step.

```kotlin
var autocompleteMatch by remember { mutableStateOf<AutocompleteMatch?>(null) }

Box {
    RichTextEditor(
        state = state,
        autocompleteTriggers = listOf(AutocompleteTrigger(prefix = "@")),
        onAutocompleteChange = { match -> autocompleteMatch = match },
    )

    autocompleteMatch?.let { match ->
        Popup(
            popupPositionProvider = match.createPopupPositionProvider(),
            onDismissRequest = { autocompleteMatch = null },
        ) {
            UserSuggestionMenu(
                query = match.query,
                onSelectUser = { user ->
                    state.applyCompletion(
                        match = match,
                        replacement = "@${user.name} ",
                        attributes = attributeContainerOf(BoldKey to Unit),
                    )
                    autocompleteMatch = null
                },
            )
        }
    }
}
```

### 3. Interactive Spans & Click Handling
Make mentions, hashtags, and hyperlinks actionable with `onSpanClick`. By explicitly calling `event.consume()`, you handle the tap without triggering default editor caret movement or text selection:

```kotlin
val uriHandler = LocalUriHandler.current

RichTextEditor(
    state = state,
    onSpanClick = { event ->
        val span = event.span
        when {
            span.attributes.containsKey(MentionKey) -> {
                showUserProfile(span.attributes[MentionKey])
                event.consume() // Suppresses editor caret placement
            }
            span.attributes.containsKey(LinkKey) -> {
                uriHandler.openUri(span.attributes[LinkKey]!!)
                event.consume()
            }
        }
    },
)
```

### 4. Declarative Mutation DSL & Semantic "Runs"
Never calculate substring indices or slice strings manually. Arranger's immutable `RichString` and mutable `RichTextState` provide a type-safe DSL for synchronized text and formatting updates:

```kotlin
// Declarative mutation DSL
state.edit {
    insert(index = textLength, text = "Important Note") {
        bold()
        textColor(Color.Red)
        headingLevel(HeadingLevel.H2)
    }
}
```

Inspired by SwiftUI's `AttributedString.Runs`, Arranger lets you query and iterate over contiguous chunks of text sharing identical attributes without complex regex:

```kotlin
// Inspect and batch-edit all Bold text runs
state.edit {
    val boldRuns = state.richString.runs(BoldKey)
    editAll(boldRuns) {
        textColor(Color.Magenta)
    }
}
```

### 5. Intelligent Enter-Key Strategies & List Handling
Orchestrate what happens when users press Enter via composable `EnterKeyStrategy` implementations:
- **`InheritParagraphStrategy`**: Inherits alignment and blockquote formatting to subsequent lines.
- **`ListEnterStrategy`**: Automatically increments ordered list numbers (1., 2., 3.), continues bullet points, and outdents or removes list markers when Enter is pressed on an empty line.
- **`HeadingEnterStrategy`**: Automatically reverts to normal body text on newlines following a heading.

### 6. Lossless Markdown & HTML Interoperability
Convert rich text to and from CommonMark Markdown and HTML without data loss:

```kotlin
// Markdown export / import (:arranger-richtext-markdown)
val markdown: String = state.richString.toMarkdown()
val importedString = RichString.fromMarkdown("# Hello **World**\n- Item 1\n- Item 2")

// HTML export / import (:arranger-richtext-html)
val html: String = state.richString.toHtml()
val htmlString = RichString.fromHtml("<p>Hello <span style=\"color: #ff0000;\"><strong>Red Bold</strong></span></p>")
```

---

## 📱 Practical Examples & Running the Samples

Arranger includes fully functioning sample applications demonstrating real-world integration across Android, Desktop, iOS, and Web:

| Sample | Screenshot | Description |
| :--- | :---: | :--- |
| **[Document Editor UI](./sample/shared/src/commonMain/kotlin/dev/mkeeda/arranger/sample/shared/DocumentEditorSample.kt)** | <img src="./docs/images/document-editor.png" width="200" alt="Document Editor"/> | Full-screen document editor with a rich formatting toolbar, heading dropdowns, list indentation, hyperlinks dialog, and undo/redo history controls. |
| **[Mention Autocomplete Chat](./sample/shared/src/commonMain/kotlin/dev/mkeeda/arranger/sample/shared/MentionAutocompleteSample.kt)** | <img src="./docs/images/mention-autocomplete.png" width="200" alt="Mention Autocomplete"/> | Modern chat composer featuring real-time `@mention` and `#channel` suggestion popups positioned dynamically with `createPopupPositionProvider()`. |
| **[WYSIWYG Markdown Shortcuts](./sample/shared/src/commonMain/kotlin/dev/mkeeda/arranger/sample/shared/WysiwygEditorSample.kt)** | <img src="./docs/images/wysiwyg.gif" width="200" alt="WYSIWYG Shortcuts"/> | Keyboard-first writing experience with instant Markdown shorthand expansion and one-tap backspace reversal. |

### Running the Samples Locally

Run the sample application across any supported platform:

- **Web (Wasm)**: `./gradlew :sample:web:wasmJsBrowserDevelopmentRun`
- **Desktop**: `./gradlew :sample:desktop:run`
- **Android**: Run the `:sample:android` run configuration in Android Studio.
- **iOS**: Open `sample/ios/ArrangerSample.xcodeproj` in Xcode and press **Cmd + R**.

---

## Roadmap & Contributing

- **Roadmap**: See [ROADMAP.md](ROADMAP.md) for our phased evolution toward v1.0.0 and Compose 1.12 native TrackedRange alignment.
- **Contributing**: Contributions are warmly welcomed! Please read [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Arranger is licensed under the [Apache License 2.0](LICENSE).
