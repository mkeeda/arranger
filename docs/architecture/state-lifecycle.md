# State Lifecycle & Editing Pipeline

Managing rich text inside a reactive declarative framework like Jetpack Compose presents a fundamental synchronization challenge: text input operates on mutable character buffers and cursor selections, whereas formatting attributes operate on style ranges.

This guide provides an overview of how `RichTextState` coordinates text buffers and formatting models, executes atomic edits, and manages the undo snapshot lifecycle.

---

## State Coordination Architecture

At the heart of Arranger lies `RichTextState`. Rather than reinventing text selection, cursor kinematics, and IME (Input Method Editor) communication from scratch, Arranger builds directly upon Compose Foundation's `TextFieldState`.

```mermaid
classDiagram
    direction TB
    class RichTextState {
        +RichString richString
        +AttributeContainer currentAttributes
        +AttributeContainer typingAttributes
        +TextRange selection
        +canUndo: Boolean
        +canRedo: Boolean
        +undo()
        +redo()
        +toggleSpanAttribute(key)
        +toggleParagraphAttribute(key)
        +edit(block: RichTextBuffer.() -> Unit)
    }

    class TextFieldState {
        <<Compose Foundation>>
        +text: CharSequence
        +selection: TextRange
    }

    class RichString {
        <<Immutable>>
        +text: String
        +spans: List~RichSpan~
    }

    RichTextState *-- TextFieldState : wraps & coordinates
    RichTextState --> RichString : produces snapshots
```

### Immutable Snapshots vs Mutable State

Arranger enforces a clear distinction between observation and mutation:

| Component | Nature | Primary Role |
|---|---|---|
| `RichString` | **Immutable** Value Object | External consumption, serialization (Markdown/HTML), and testing. Accessible via `state.richString`. |
| `RichSpan` | **Immutable** Value Object | Represents a discrete `IntRange` mapped to an `AttributeContainer`. |
| `RichTextState` | **Stable** Reactive State | The Single Source of Truth hoisted across UI components, toolbars, and viewmodels. |
| `RichTextBuffer` | **Ephemeral Mutable** Buffer | Provided as the receiver within `state.edit { ... }` transactions for safe multi-step edits. |

---

## The Edit Transaction Lifecycle

When text is modified — whether through user keyboard input or programmatic toolbar commands — Arranger processes the modification through a predictable pipeline:

```mermaid
flowchart TD
    Start(["Edit Initiated (User Input or state.edit)"]) --> P1["1. Capture Pre-Mutation State for Undo"]
    P1 --> P2["2. Shift Formatting Span Ranges"]
    P2 --> P3["3. Apply Active Typing Attributes or Enter Key Rules"]
    P3 --> P4["4. Normalize Overlapping Styles & Snap Paragraphs"]
    P4 --> P5["5. Atomic State Update & Compose Recomposition"]
    P5 --> Finish(["Visual Decoration via AttributeStyleResolver"])
```

### 1. Pre-Mutation Snapshot Recording
Before text or styling is altered, the editor's current state (text, spans, selection, and typing attributes) is captured in the undo stack according to intelligent merging policies (e.g. continuous typing is grouped into a single undo step).

### 2. Automatic Span Range Adjustment
When text is inserted or deleted, existing formatting ranges located at or after the edit position shift automatically, preserving your styling alignment without manual calculation.

### 3. Typing Attributes & Enter Key Strategies
- **Regular Typing:** Characters typed inherit active `typingAttributes` (such as bold or text color selected from a toolbar).
- **Enter Key:** Pressing Enter evaluates the active `EnterKeyStrategy` (e.g. automatically resetting headings to body text, continuing bullet lists, or outdenting nested items).

### 4. Style Normalization & Paragraph Snapping
Intersecting span ranges are automatically normalized to eliminate conflicts. Block-level styles (headings, quotes, lists) are automatically snapped to full paragraph boundaries (`\n` to `\n`).

### 5. Atomic Publication & Recomposition
The state update commits atomically: raw text and span ranges update together. Compose recomposes affected UI nodes, and `AttributeStyleResolver` supplies the corresponding `SpanStyle` and `ParagraphStyle` decorations to the layout.

---

## Undo Snapshot Mechanics & Memory Bounds

Arranger manages history via internal immutable snapshots captured before each mutation:

### Keystroke Coalescing Policies

Creating a separate undo entry for every individual character keystroke would force users to undo dozens of times just to revert a single word. Arranger automatically coalesces modifications:

- **Continuous Typing**: Successive character insertions within a single word boundary merge into a single undo frame.
- **Boundary Events**: Whitespace, newlines, deletions (Backspace/Delete), and external paste events create new, discrete undo checkpoints.
- **Explicit Formatting**: Toolbar style toggles and programmatic `state.edit` blocks always create isolated checkpoints.

### Bounded Memory Capacity

To prevent unbound memory growth during long editing sessions, the undo stack enforces a strict limit of **100 operations**. When capacity is reached, the oldest snapshots are dropped in FIFO order.

For practical UI integration (such as wiring toolbar buttons and checking `canUndo`/`canRedo`), refer to [**State Management & History**](../editor-basics/state-management.md).

