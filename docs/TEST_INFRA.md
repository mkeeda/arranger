# Test Infrastructure — Arranger WYSIWYG Auto-formatting Test Suite

## 1. Testing Philosophy

The test infrastructure for Arranger's WYSIWYG Auto-formatting feature is designed as an end-to-end, multi-tier quality assurance framework covering real-time Markdown shortcut auto-formatting and the standalone `WysiwygEditor` component.

- **Requirement-Driven**:
  Test cases are derived systematically from the feature inventory (F1 through F16) and acceptance criteria (R1–R4). Tests verify externally observable input/output behavior and styled ranges rather than coupling to internal implementation details.
- **Black-Box End-to-End Simulation**:
  Simulates user keyboard input, Backspace key events, Undo/Redo operations, and cursor repositioning. Output text, applied span attributes, paragraph block styles, and cursor coordinates are asserted via a clean test harness.
- **Layered 4-Tier Test Architecture**:
  Structured into four distinct tiers: single-feature happy paths, boundary and false-positive prevention cases, cross-feature combinations, and real-world authoring scenarios.
- **Production-Driven Test Harness (`WysiwygTestDriver`)**:
  Interacts directly with production engine components (`RichTextState`, `WysiwygState`, `WysiwygInputTransformation`, and `handleWysiwygKey`), ensuring realistic execution across JVM, WasmJs, and iOS targets.
- **Strict Non-Regression Guarantee**:
  Verifies that the standard `RichTextEditor` remains completely unaffected by auto-formatting rules, preserving 100% of its existing behavior and API contracts.

---

## 2. Infrastructure Architecture

### 2.1 Directory Structure
```
richtext-editor/src/commonTest/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/
├── WysiwygTestHarness.kt                  # Unified test driver interface, harness, and assertion DSL
├── Tier1FeatureCoverageTest.kt            # Tier 1: Happy-path coverage for F1–F16 (5+ cases each, 80+ total)
├── Tier2BoundaryCornerCaseTest.kt         # Tier 2: Boundary, edge-case, and false-positive tests for F1–F16 (80+ total)
├── Tier3CrossFeatureCombinationTest.kt    # Tier 3: Cross-feature combinations (pairwise, nested, block × inline)
├── Tier4RealWorldScenarioTest.kt          # Tier 4: Real-world document editing and authoring workflows
├── Tier5AdversarialCoverageHardeningTest.kt # Tier 5: Adversarial stress and rapid typing boundary verification
├── WysiwygEditorTest.kt                   # WysiwygEditor Composable bindings and integration tests
├── WysiwygAutoFormatterTest.kt            # Engine-level unit tests for block and inline formatters
├── WysiwygChallenger4ErgonomicsTest.kt    # Ergonomics, Undo/Redo, and Backspace reversal contract verification
└── WysiwygChallengerStressTest.kt         # Stress tests for rapid typing, multi-paragraph, and stack consistency
```

### 2.2 Test Driver Contract (`WysiwygTestDriver`)
Tests execute through the `WysiwygTestDriver` abstraction, which drives the input transformation pipeline, key events, and undo history:

```kotlin
public interface WysiwygTestDriver {
    public fun initialize(initialText: String = "", initialSpans: List<RichSpan> = emptyList())
    public fun typeText(text: String)
    public fun typeChar(char: Char)
    public fun pressBackspace()
    public fun undo()
    public fun redo()
    public fun setCursor(offset: Int)
    public fun setSelection(start: Int, end: Int)

    public val currentText: String
    public val currentSelection: TextRange
    public val currentSpans: List<RichSpan>

    public fun isBold(range: IntRange): Boolean
    public fun isItalic(range: IntRange): Boolean
    public fun isStrikethrough(range: IntRange): Boolean
    public fun isInlineCode(range: IntRange): Boolean
    public fun isHeading(level: HeadingLevel, range: IntRange): Boolean
    public fun isBulletList(range: IntRange): Boolean
    public fun isOrderedList(range: IntRange): Boolean
    public fun isBlockquote(range: IntRange): Boolean
}
```

---

## 3. Tiered Test Specifications

### Tier 1: Feature Coverage (F1–F16, 5+ Cases Each, 80+ Total)
Validates primary user journeys and expected conversions for every feature:

| Feature ID | Feature Name | Test Coverage Focus |
|------------|--------------|---------------------|
| **F1** | Inline Code Attribute | `InlineCodeKey` metadata, Monospace font family resolution, subtle background styling, coexistence with other attributes, container equality |
| **F2** | Block: Heading 1..3 | Heading 1 (`# `), Heading 2 (`## `), Heading 3 (`### `), newline continuation, multibyte/accented text |
| **F3** | Block: Bullet List | Dash prefix (`- `), asterisk prefix (`* `), multiline continuation, list text content, subsequent typing |
| **F4** | Block: Ordered List | Ordered prefix (`1. `), multiline continuation, list text content, cursor placement, subsequent typing |
| **F5** | Block: Blockquote | Quote prefix (`> `), newline continuation, quoted text content, paragraph isolation, cursor placement |
| **F6** | Inline: Bold | Double asterisk (`**bold**`), mid-sentence bolding, multi-word bolding, unicode text, post-format cursor position |
| **F7** | Inline: Italic (Asterisk) | Single asterisk (`*italic*`), mid-sentence italic, multi-word italic, unicode text, post-format cursor position |
| **F8** | Inline: Italic (Underscore) | Single underscore (`_italic_`), mid-sentence italic, multi-word italic, unicode text, post-format cursor position |
| **F9** | Inline: Inline Code | Backticks (`` `code` ``), inline code snippets, symbols within code, unicode text, post-format cursor position |
| **F10**| Inline: Strikethrough | Tildes (`~strike~`), mid-sentence strikethrough, multi-word, unicode text, post-format cursor position |
| **F11**| False Positive Prevention | Identifier protection (`foo_bar_baz`), unclosed delimiters, empty delimiter runs (`****`), mid-line triggers, whitespace-padded tokens |
| **F12**| Undo / Redo Integration | Immediate undo after heading, list, or inline conversion; redo restoration; snapshot integrity and cursor recovery |
| **F13**| Backspace Reversal | Immediate Backspace restoring raw symbols (State B); subsequent Backspace performing standard character deletion |
| **F14**| Cursor & Ergonomics | Typing attribute clearance preventing style leakage to subsequent characters; accurate cursor positioning immediately after delimiters |
| **F15**| WysiwygEditor Component | Standalone component instantiation, pipeline integration, Backspace event interception, custom style resolver propagation |
| **F16**| RichTextEditor Non-regression | Verification that `# `, `- `, `**`, and other triggers remain literal raw characters in `RichTextEditor`, preserving existing behavior |

### Tier 2: Boundary & Corner Cases (F1–F16, 5+ Cases Each, 80+ Total)
Exhaustively checks edge cases, boundary values, escape sequences, and false-positive prevention:

- **F1**: Multiline inline code rejection, consecutive backticks safety, empty backticks, special characters and emojis, boundary spacing
- **F2**: Unsupported levels (`#### `) rejected, unspaced prefixes (`#text`) ignored, mid-line `# ` ignored, empty heading backspace, re-triggering on existing heading
- **F3**: Unspaced dashes (`-text`) ignored, mid-line `a - b` ignored, horizontal rules (`--- `) rejected, empty list newlines, indented triggers
- **F4**: Non-1 initial numbers (`2. `) ignored, unspaced digits (`1.text`) ignored, mid-line markers (`v1. 0`) ignored, double spacing, non-dot markers (`1) `) ignored
- **F5**: Unspaced brackets (`>text`) ignored, mid-line `x > y` ignored, multiple consecutive brackets (`>> `) rejected, fullwidth symbols (`＞ `) rejected
- **F6**: Triple asterisks handling, whitespace adjacent to delimiters (`** text**`) rejected, intra-word asterisks (`foo**bar**baz`), escaped symbols (`\*\*`), multiline rejection
- **F7**: Mathematical expressions (`2 * 3 * 4`) ignored, intra-word asterisks (`foo*bar*baz`), whitespace adjacent to delimiters, escaped asterisks (`\*`), multiline rejection
- **F8**: Snake case identifiers (`my_var_name`) ignored, screaming snake case (`MAX_VALUE`), dunder methods (`__init__`), URLs with underscores (`a_b_c`), whitespace boundaries
- **F9**: Escaped backticks (``\`code\``) ignored, isolated single backtick, filesystem path expressions (`/usr/`local`/bin`), URL query parameters
- **F10**: Home directory paths (`~/dir`) ignored, approximation expressions (`~50%`) ignored, double tildes (`~~text~~`), whitespace boundaries, escaped tildes (`\~`)
- **F11**: Empty lines, symbol-only inputs (`***`, `___`), surrogate pairs and emoji stability, rapid burst typing
- **F12**: Undo after typing additional characters, chained undos across block + inline conversions, re-triggering after undo, empty redo stack safety
- **F13**: Backspace after cursor movement (standard character deletion), Backspace on active selection, space re-triggering after reversal, continuous backspacing
- **F14**: Cursor placement at document boundary (index 0), cursor placement at end of line, selection overwrite behavior, typing attribute lock release
- **F15**: Disabled editor (`enabled = false`) suppresses formatting, `readOnly = true` suppresses input, large text buffer stability, external `RichTextState.edit` synchronization
- **F16**: Rapid typing in standard `RichTextEditor`, zero interference with standard undo history, existing keyboard shortcut compatibility (`Cmd+B`, `Cmd+I`)

### Tier 3: Cross-Feature Combinations (15 Integration Scenarios)
Verifies interactions when multiple features are combined within the same document or line:
- Bullet lists containing bold text (`- Item with **bold** word`)
- Bullet lists containing inline code (`- Item with `code` syntax`)
- Headings containing inline code (`# Heading with `inline code``)
- Headings containing bold text (`## Heading with **bold** text`)
- Blockquotes containing strikethrough text (`> Quote with ~strikethrough~ text`)
- Blockquotes containing bold and italic text (`> **Bold** and *italic* in quote`)
- Multiple inline styles on the same line (bold + asterisk italic + underscore italic)
- Ordered lists containing inline code and bold spans (`1. Step 1: run `install` and **verify**`)
- Sequential multiline transitions across distinct block types (H1 -> Bullet -> Ordered -> Quote)
- Inline formatting immediately followed by a newline and block conversion
- Block conversion immediately followed by inline formatting
- Adjacent inline code and bold spans remaining independent without merging
- Undo of inline decoration inside a list item preserving the list block attribute
- Backspace reversal of inline decoration inside a list item preserving the list block attribute
- Full-document multi-step Undo/Redo chains

### Tier 4: Real-World Application Scenarios (5 Comprehensive Workflows)
Validates realistic end-to-end user workflows:
- **Scenario 1: Technical Blog Post Authoring**
  Title (H1), summary (Blockquote), key features (Bullet List), installation guide (Ordered List), inline code and bold highlights, Backspace correction.
- **Scenario 2: Sprint Planning & Meeting Notes**
  Agenda (H2), attendee list with bold and italic names, key takeaways (Blockquote), task breakdown (Ordered List), Undo correction.
- **Scenario 3: Developer Release Checklist**
  Title (H1), completed items with strikethrough, active tasks with bold, version numbers with inline code, Backspace reversal.
- **Scenario 4: Academic & Technical Documentation**
  Hierarchical headings (H1, H2, H3), math and variable references in inline code, protected snake_case symbols, italic emphasis, blockquotes.
- **Scenario 5: Interactive Trial-and-Error Editing**
  Heading creation -> Backspace reversal -> re-entry -> list creation -> Undo reversal -> bolding -> Undo correction -> final document verification.

---

## 4. Coverage Targets & Quality Gates

| Metric | Target | Verification Method |
|--------|--------|---------------------|
| **Feature Coverage (F1–F16)** | 100% (80+ cases) | `Tier1FeatureCoverageTest` passes |
| **Boundary & False Positive** | 100% (80+ cases) | `Tier2BoundaryCornerCaseTest` passes |
| **Cross-Feature Combinations**| 100% (15 cases) | `Tier3CrossFeatureCombinationTest` passes |
| **Real-World Scenarios**      | 100% (5 scenarios)| `Tier4RealWorldScenarioTest` passes |
| **RichTextEditor Non-regression** | 100% (F16) | `RichTextEditorNonRegressionTest` and existing unit tests pass |
| **Code Style & Linting**      | 0 violations | `./gradlew spotlessCheck` passes |

---

## 5. Execution & Verification

### Running Tests via Gradle
```bash
# Run the full test suite across all targets
./gradlew :richtext-editor:allTests

# Run JVM tests only (fast iteration)
./gradlew :richtext-editor:jvmTest

# Run a specific tier
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier1FeatureCoverageTest"
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier2BoundaryCornerCaseTest"
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier3CrossFeatureCombinationTest"
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier4RealWorldScenarioTest"

# Verify code formatting and lint rules
./gradlew spotlessCheck
```
