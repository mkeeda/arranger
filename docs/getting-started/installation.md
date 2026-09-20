# Installation

Arranger is published on Maven Central. Configure dependencies according to your project setup (Kotlin Multiplatform or Android Single-Platform).

---

## Requirements

To use Arranger, ensure your development environment meets the following minimum requirements:

- **Kotlin**: 2.4.10 or higher
- **Compose Multiplatform**: 1.12.0 or higher

---

## Dependency Configuration

=== "Kotlin Multiplatform (KMP)"

    Add dependencies to the `commonMain` source set of your shared module:

    ```kotlin
    // build.gradle.kts (shared module)
    kotlin {
        sourceSets {
            commonMain.dependencies {
                // UI editor components (RichTextEditor, WysiwygEditor)
                // Core data models (:arranger-richtext) are included transitively
                implementation("dev.mkeeda.arranger:arranger-richtext-editor:0.4.0-alpha04")

                // Optional: Material 3 style resolver (rememberMaterial3AttributeStyleResolver)
                implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.4.0-alpha04")

                // Optional: Bidirectional Markdown conversion (toMarkdown / fromMarkdown)
                implementation("dev.mkeeda.arranger:arranger-richtext-markdown:0.4.0-alpha04")

                // Optional: Bidirectional HTML conversion (toHtml / fromHtml)
                implementation("dev.mkeeda.arranger:arranger-richtext-html:0.4.0-alpha04")
            }
        }
    }
    ```

=== "Android (Single Platform)"

    For Android-only projects, add dependencies to the `dependencies` block of `app/build.gradle.kts`:

    ```kotlin
    // app/build.gradle.kts
    dependencies {
        // Core UI editor component
        implementation("dev.mkeeda.arranger:arranger-richtext-editor:0.4.0-alpha04")

        // Optional: Material 3 integration
        implementation("dev.mkeeda.arranger:arranger-richtext-editor-material3:0.4.0-alpha04")

        // Optional: Bidirectional Markdown conversion
        implementation("dev.mkeeda.arranger:arranger-richtext-markdown:0.4.0-alpha04")

        // Optional: Bidirectional HTML conversion
        implementation("dev.mkeeda.arranger:arranger-richtext-html:0.4.0-alpha04")
    }
    ```

=== "Version Catalog (libs.versions.toml)"

    Recommended configuration when centralizing dependency versions with a Gradle Version Catalog:

    ```toml
    # gradle/libs.versions.toml
    [versions]
    arranger = "0.4.0-alpha04"

    [libraries]
    arranger-richtext-core = { module = "dev.mkeeda.arranger:arranger-richtext", version.ref = "arranger" }
    arranger-richtext-editor = { module = "dev.mkeeda.arranger:arranger-richtext-editor", version.ref = "arranger" }
    arranger-richtext-editor-material3 = { module = "dev.mkeeda.arranger:arranger-richtext-editor-material3", version.ref = "arranger" }
    arranger-richtext-markdown = { module = "dev.mkeeda.arranger:arranger-richtext-markdown", version.ref = "arranger" }
    arranger-richtext-html = { module = "dev.mkeeda.arranger:arranger-richtext-html", version.ref = "arranger" }
    ```

    Usage in `build.gradle.kts`:

    ```kotlin
    dependencies {
        implementation(libs.arranger.richtext.editor)
        implementation(libs.arranger.richtext.editor.material3)
    }
    ```

---

## Artifacts & Responsibilities

Arranger is published as discrete artifacts so you can include only what your application requires:

| Artifact | Responsibilities & Key Features | Dependencies |
|---|---|---|
| `arranger-richtext` | **Pure core data model layer**.<br>Independent of Compose UI. Provides immutable data structures (`RichString`, `RichSpan`), attribute systems (`AttributeKey`, `AttributeContainer`), sweep-line interval partitioning, paragraph snapping, and list extraction algorithms. | None (Kotlin stdlib only) |
| `arranger-richtext-editor` | **Compose UI editor engine layer**.<br>Provides `RichTextEditor`, `WysiwygEditor`, state management via `RichTextState`, undo/redo history, enter key strategies, tap detection, and autocomplete support. | `arranger-richtext`, Compose UI / Foundation |
| `arranger-richtext-editor-material3` | **Material 3 integration layer**.<br>Reads `MaterialTheme.typography` and `colorScheme`, providing `rememberMaterial3AttributeStyleResolver` to automatically align headings and blockquotes with M3 design tokens. | `arranger-richtext-editor`, Compose Material 3 |
| `arranger-richtext-markdown` | **Bidirectional Markdown conversion layer**.<br>Leverages the JetBrains Markdown parser (GFM) to convert bidirectionally between `RichString` and Markdown text. | `arranger-richtext`, `org.jetbrains:markdown` |
| `arranger-richtext-html` | **Bidirectional HTML conversion layer**.<br>Uses the multiplatform HTML parser `ksoup` to convert between `RichString` and HTML, with full support for inline CSS styling (colors, font sizes, alignments, etc.). | `arranger-richtext`, `ksoup` |

!!! tip "Using Core Artifacts Headless"
    If you are building backend services, Ktor server-side applications, or CLI tools that only need to manipulate rich text data models or perform Markdown/HTML conversions, you can include `arranger-richtext`, `arranger-richtext-markdown`, and `arranger-richtext-html` without any Compose UI dependencies for a lightweight footprint.

---

## Supported Platforms & Targets

Arranger provides full support across the following platforms:

| Platform | Support Status | Minimum Requirements / Notes |
|---|:---:|---|
| **Android** | ✅ Supported | API Level 26 (Android 8.0) or higher |
| **Desktop (JVM)** | ✅ Supported | macOS (Apple Silicon / Intel), Windows, Linux (JDK 17+) |
| **iOS** | ✅ Supported | iOS 14.0 or higher (CocoaPods / SPM / direct framework linking) |
| **Web** | ✅ Supported | WebAssembly (WasmJs) / Canvas rendering |

!!! note "Continuous Alignment"
    Arranger actively tracks the latest Compose Multiplatform releases, delivering native-like typing responsiveness, IME support, and platform keyboard shortcuts across Desktop and Web (Wasm).
