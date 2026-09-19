# Arranger - Type-safe Rich Text Editor Engine for Compose Multiplatform

[![CI](https://github.com/mkeeda/arranger/actions/workflows/ci.yml/badge.svg)](https://github.com/mkeeda/arranger/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.mkeeda.arranger/arranger-richtext-editor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.mkeeda.arranger%22%20AND%20a:%22arranger-richtext-editor%22)
[![Documentation](https://img.shields.io/badge/docs-mkeeda.github.io%2Farranger-blue)](https://mkeeda.github.io/arranger/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Arranger is a declarative, type-safe rich text editor engine and UI framework for **Compose Multiplatform**.  
Think of Arranger as the foundational framework (analogous to ProseMirror or Lexical) for building full-featured editing experiences on Compose—providing out-of-the-box paragraph formatting, WYSIWYG auto-formatting, dynamic enter-key strategies, interactive mentions & hyperlinks, atomic undo/redo, and lossless Markdown/HTML interoperability.

<div align="center">
  <img src="./docs/images/rich-text-editor-demo.gif" width="360" alt="Arranger Rich Text Editor Demo"/>
</div>

---

### 📖 Documentation & Guides

Comprehensive guides, API references, and interactive tutorials are available at our official documentation site:

👉 **[https://mkeeda.github.io/arranger/](https://mkeeda.github.io/arranger/)**

- **[Getting Started](https://mkeeda.github.io/arranger/getting-started/quickstart/)**: Installation, setup, and minimal examples.
- **[Editor Basics](https://mkeeda.github.io/arranger/editor-basics/rich-text-editor/)**: `RichTextEditor` vs `WysiwygEditor`, state management, and undo/redo.
- **[Formatting & Styling](https://mkeeda.github.io/arranger/styling/built-in-attributes/)**: Headings, lists, custom attributes, and Material 3 integration.
- **[Advanced Behaviors](https://mkeeda.github.io/arranger/advanced-behaviors/enter-key-strategies/)**: Enter-key strategies, list handling, and sweep-line span merging.
- **[Interactive Features](https://mkeeda.github.io/arranger/interactions/autocomplete/)**: Mentions, hashtags, popup positioning, and clickable spans.
- **[Markdown & HTML Interop](https://mkeeda.github.io/arranger/interop/markdown/)**: Bi-directional conversion and custom AST serializers.
- **[API Reference](https://mkeeda.github.io/arranger/api/overview/)**: Detailed specifications for all modules and interactive Dokka reference.

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

## Core Features

- ✍️ **Dual Editor Components**:
  - `RichTextEditor`: Clean canvas for toolbar-driven rich editing.
  - `WysiwygEditor`: Instant Markdown shortcut conversions (headings, lists, blockquotes, bold/italic) with backspace reversal.
- 💬 **Interactive Spans & Autocomplete**: Built-in `@mention`, `#hashtag`, and hyperlink handling with zero-math cursor popup positioning (`createPopupPositionProvider`).
- ↵ **Intelligent Enter-Key Strategies**: Built-in newline inheritance, list auto-continuation/outdent, and heading reset.
- 🛡️ **Declarative & Type-Safe Core**: Mutate text and styling atomically via `state.edit { }`. Query and batch-edit text chunks via semantic `Runs`.
- 🌐 **Lossless Format Interop**: Bi-directional conversion between `RichString` and CommonMark Markdown / HTML.

---

## Installation

Published on Maven Central:

```kotlin
// build.gradle.kts (commonMain)
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core editor UI (includes arranger-richtext)
            implementation("dev.mkeeda.arranger:arranger-richtext-editor:0.4.0-alpha03")

            // Optional: Markdown / HTML conversions
            implementation("dev.mkeeda.arranger:arranger-richtext-markdown:0.4.0-alpha03")
            implementation("dev.mkeeda.arranger:arranger-richtext-html:0.4.0-alpha03")

            // Optional: Material 3 style resolver
            implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.4.0-alpha03")
        }
    }
}
```

---

## Quick Start

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

For live Markdown shortcuts as you type, simply swap with `WysiwygEditor`:

```kotlin
@Composable
fun WysiwygSample() {
    val state = remember { RichTextState() }
    WysiwygEditor(state = state, modifier = Modifier.fillMaxSize())
}
```

---

## Running the Samples

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
