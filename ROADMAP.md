# Arranger Development Roadmap

Arranger is evolving towards a stable **v1.0.0 (Production-Ready Release)** as a comprehensive, high-level rich text editor engine and UI ecosystem for Compose Multiplatform.
This document outlines our strategic pivot, release plan, current focus, and historical milestones.

---

## Strategic Vision & Positioning (Compose 1.12+ Alignment)

With the release of **Compose 1.12**, `TextField` introduced native support for rich text styling primitives (`addStyle()` and tracked range mutations). This represents a major milestone for the Compose ecosystem, solving low-level text index shifting directly at the framework layer.

In response, Arranger is evolving its positioning:

* **From:** A low-level utility library managing mutable string index math and span tracking.
* **To:** A **High-Level Rich Text Editor Engine & Framework** for Compose Multiplatform.

Think of Arranger as the foundational framework (analogous to ProseMirror or Lexical in the web ecosystem) for building full-featured editing experiences on Compose. While Compose 1.12 provides the raw canvas and text primitives, Arranger orchestrates complete editor behaviors—Markdown/HTML interop, WYSIWYG auto-formatting, interactive mentions/links, paragraph strategies, visual container styling, and rich state management.

---

## Phased Release Plan

### 🚀 v0.3.x (Strategic Pivot & Compatibility Baseline)
*Verifying Compose 1.12 baseline and realigning library architecture and documentation.*

- [ ] **Compose 1.12 Compatibility Verification:** Verify BOM updates, dependency alignment, and cross-platform build validation across Android, iOS, Desktop (JVM), and Web (WasmJs).
- [x] **Concept & Documentation Pivot:** Redefine Arranger's unique value proposition (USP) across README, architecture guides, and developer documentation.

---

### ⭐ v0.4.0 – v0.5.0 (High-Value Editor Engine & Interoperability) [Highest Priority Focus]
*Building high-value editor capabilities that standard Compose does not provide.*

- [x] **Markdown & HTML Bi-directional Conversion:**
  - Import / Export converters between `RichString` and CommonMark Markdown / HTML representations (`:richtext-markdown` and `:richtext-html`).
  - Lossless parsing and serialization of inline and block formatting.
- [ ] **WYSIWYG Auto-formatting:**
  - Live typing auto-conversions (e.g., `# ` triggers H1 heading, `- ` / `* ` triggers bullet lists, `1. ` triggers ordered lists).
  - Inline syntax auto-formatting (e.g., `*italic*`, `**bold**`, `~strikethrough~`, `` `code` ``).
- [ ] **Interactive Custom Attribute UX:**
  - [x] **Hyperlink Support:** `LinkKey` attribute, URL parsing (`UrlParser`), interactive tap/click navigation via `LocalUriHandler`, and `detectAndApplyLinks()` API.
  - [ ] **Domain-Specific Interactive Attributes:** `@mentions`, `#hashtags`, and custom interactive spans with popup/chip support.
- [ ] **Visual Block Decorations:**
  - Visual container rendering for blockquotes, code blocks, callouts, and background/border styling.
- [ ] **Advanced Editor Toolbars & Selection Actions:**
  - Out-of-the-box floating toolbars and context-aware formatting menus.

---

### 🔒 v1.0.0 (Native Compose 1.12 Integration & Production Readiness)
*Adopting native primitives under the hood once Compose Multiplatform matures across all targets, finalizing public APIs, and establishing production stability.*

- [ ] **Adopt Native Primitives (`TrackedRange` Migration):**
  - Transition the internal span-tracking engine (`shiftSpans`) to adopt Compose 1.12's native `TrackedRange` under the hood for zero-overhead index tracking.
  - *Timing Strategy:* Executed once JetBrains Compose Multiplatform fully stabilizes Compose 1.12 across all non-Android targets (iOS, Desktop JVM, WasmJs), ensuring 100% multiplatform API parity.
- [ ] **Public API Audit & Freeze:**
  - Finalize and freeze public API signatures across `arranger-richtext`, `arranger-richtext-editor`, and platform/design modules (`material3`).
- [ ] **Cross-Platform Quality & Production Hardening:**
  - Multiplatform behavior alignment, comprehensive Roborazzi visual tests, memory footprint benchmarks, and automated regression suites.

---

## Post-1.0.0 Exploration (v1.x)

- [ ] **Inline Media Support:** Embedded images, videos, and arbitrary custom composable attachments inline within the text flow.
- [ ] **Table / Grid Support:** Rich text table layout structures and cell-based navigation/editing models.
- [ ] **Declarative Schema Constraints:** Schema-based validation rules to restrict or sanitize available attributes in specific text fields (e.g., allow bold/italic only, strip headings).
- [ ] **Large-Document Performance Optimization:** Internal refactoring to data structures like Piece Table or Rope for ultra-large documents.

---

## Completed Milestones

<details>
<summary><b>v0.1.0 - Phase 1: Core Foundation</b></summary>

- [x] Initial Type-Safe Attribute System (`SpanAttributeKey`, `ParagraphAttributeKey`, `AttributeContainer`).
- [x] Range tracking and automatic span index shifting during basic mutations.
- [x] Primitive Compose UI integration with `AnnotatedString` conversion.
</details>

<details>
<summary><b>v0.2.0 - Phase 2: The Real "Editor" Engine</b></summary>

- [x] Advanced dynamic editing with `RichTextBuffer` and `RichTextState`.
- [x] Built-in paragraph formatting (Headings, Blockquotes, Alignments, Bullet & Ordered Lists).
- [x] Custom `ListMarkerResolver` and dynamic enter key strategies (`InheritParagraphStrategy`, `ListEnterStrategy`, `HeadingEnterStrategy`).
- [x] Full Undo / Redo engine tracking text mutations and span modifications.
- [x] Semantic "Runs" iteration and batch attribute editing (`editAll`, `runs`).
</details>

<details>
<summary><b>v0.3.0 - Phase 3: Kotlin Multiplatform (KMP) Architecture Migration</b></summary>

- [x] Full KMP refactoring supporting Android, Desktop (JVM), iOS, and Web (WasmJs).
- [x] Separation of pure Kotlin core (`arranger-richtext`) and Compose UI binding layer (`arranger-richtext-editor`).
- [x] Material 3 integration module (`arranger-richtext-editor-material3`).
- [x] Sample app for all multiplatform targets.
</details>
