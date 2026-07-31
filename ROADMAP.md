# Arranger Development Roadmap

Arranger is actively evolving towards a stable **v1.0.0 (Production-Ready Release)**.
This document outlines our release plan, current focus, future aspirations, and historical milestones.

---

## Target: v1.0.0 (Production-Ready Release)

The goal of `v1.0.0` is to deliver a robust, multiplatform-ready, rich text editing engine with stable public APIs, comprehensive format support (Markdown / HTML), visual block decorations, and high reliability across all supported platforms (Android, iOS, Desktop, Wasm).

---

## Version Release Plan (Targeting v1.0.0)

### 🚀 v0.4.0 (Phase 4: Interoperability & Rich Features) [Next Phase]
*Pre-releases during active development will follow `v0.4.0-alphaXX` tag conventions before final `v0.4.0` release.*

- [ ] **Markdown & HTML Support:** Import / Export converters for Markdown and HTML representations.
- [ ] **Visual Block Decorations:** Visual container rendering for blockquotes, code blocks, callouts, and background styling.
- [ ] **WYSIWYG Auto-formatting:** Live markdown syntax replacement while typing (e.g., typing `# ` converts to H1 automatically).
- [ ] **Hyperlink Support:** `LinkKey` attribute, URL parsing, and interactive tap/click handling across platforms.

### 🔒 v1.0.0-alpha / v1.0.0-rc (Release Preparation)
- [ ] **Public API Audit & Freeze:** Finalize and freeze public API signatures across `arranger-richtext`, `arranger-richtext-editor`, and platform-specific modules.
- [ ] **Cross-platform Quality Verification:** Rigorous testing, memory footprint analysis, and multiplatform behavior alignment (Android, iOS, Desktop JVM, WasmJs).

---

## Post-1.0.0 Exploration (v1.x / Phase 5)

- [ ] **Inline Media Support:** Support for embedded images, videos, and custom attachments within the text engine.
- [ ] **Table / Grid Support:** Rich text table layout structures and cell-based editing models.
- [ ] **Declarative Constraints:** Schema-based validation rules to restrict or sanitize available attributes in specific text fields.
- [ ] **Performance Optimization:** Internal refactoring to data structures like Piece Table or Rope for ultra-large text files.

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
