# Project: Arranger WYSIWYG Auto-formatting

## Architecture
本プロジェクトは、ArrangerにおけるMarkdownショートカット自動整形（WYSIWYG Auto-formatting）および独立したエディタUIコンポーネントを提供する。

- **モジュール境界**:
  - `:richtext`: pure Kotlin Multiplatform コア。ドキュメントモデル（`RichString`, `RichSpan`, `AttributeContainer`）および属性定義（`Attributes.kt`）を保持。
  - `:richtext-editor`: Compose Multiplatform UI エディタ。`BasicTextField` (Compose Foundation Text 2) を用いたエディタコンポーネント、`InputTransformation`、Undo/Redo管理、スタイル解決を担当。
- **データフロー**:
  1. ユーザーのキー入力 -> Compose `BasicTextField`
  2. `WysiwygInputTransformation.transformInput(TextFieldBuffer)` が同期的にトリガー検出
  3. ブロック変換（行頭 `# ` 等）またはインライン変換（閉じ記号 `**` 等）を検出
  4. 変換前状態（生テキスト＋カーソル位置）を `EditorSnapshot` として Undo スタックへ保存（`UndoMergePolicy.Separate`）
  5. トリガー記号を `TextFieldBuffer` から削除し、対応する属性を `RichTextState.spans` に適用
  6. カーソル位置を更新し、タイピング属性をクリア
  7. 直後の Undo または Backspace で変換前状態へ巻き戻し
- **コンポーネント分離**:
  - `BaseRichTextEditor`: 内部共通描画・リンクハンドリング基底
  - `RichTextEditor`: 既存APIシグネチャ・挙動を100%維持（自動整形なし）
  - `WysiwygEditor`: 新規公開Composable。WYSIWYG自動整形パイプラインとBackspace復元キーイベントハンドラを注入

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Inline Code Attribute | `CodeKey` 属性の定義および Monospace 等幅フォント解決 | M1 | Survey (R3) |
| F2 | Block: Heading 1..3 | 行頭 `# ` で Heading 1、`## ` で Heading 2、`### ` で Heading 3 へ変換・記号削除 | M2 | ORIGINAL_REQUEST R2 |
| F3 | Block: Bullet List | 行頭 `- ` または `* ` で Bullet List へ変換・記号削除 | M2 | ORIGINAL_REQUEST R2 |
| F4 | Block: Ordered List | 行頭 `1. ` で Ordered List へ変換・記号削除 | M2 | ORIGINAL_REQUEST R2 |
| F5 | Block: Blockquote | 行頭 `> ` で Blockquote へ変換・記号削除 | M2 | ORIGINAL_REQUEST R2 |
| F6 | Inline: Bold | `**text**` 入力で Bold 適用・アスタリスク削除 | M2 | ORIGINAL_REQUEST R3 |
| F7 | Inline: Italic (Asterisk) | `*text*` 入力で Italic 適用・アスタリスク削除 | M2 | ORIGINAL_REQUEST R3 |
| F8 | Inline: Italic (Underscore) | `_text_` 入力で Italic 適用・アンダースコア削除 | M2 | ORIGINAL_REQUEST R3 |
| F9 | Inline: Code | `` `text` `` 入力で Inline Code 適用・バッククォート削除 | M2 | ORIGINAL_REQUEST R3 |
| F10 | Inline: Strikethrough | `~text~` 入力で Strikethrough 適用・チルダ削除 | M2 | ORIGINAL_REQUEST R3 |
| F11 | False Positive Prevention | 単語途中記号（`foo_bar_baz`）、未閉じ記号、エスケープ、空文字での誤変換防止 | M2 | ORIGINAL_REQUEST R3 |
| F12 | Undo / Redo Integration | 変換直後の Undo (Cmd+Z) で変換前の生記号テキストとカーソル位置に復元 | M2 | ORIGINAL_REQUEST R4 |
| F13 | Backspace Reversal | 変換直後の Backspace で変換前の生記号テキストとカーソル位置に復元 | M2 | ORIGINAL_REQUEST R4 |
| F14 | Cursor & Typing Attribute Ergonomics | 変換直後のカーソル位置維持および後続文字への装飾漏れ防止 | M2 | ORIGINAL_REQUEST R3, R4 |
| F15 | WysiwygEditor Component | 独立したUIコンポーネント `WysiwygEditor` の提供 | M3 | ORIGINAL_REQUEST R1 |
| F16 | RichTextEditor Non-regression | 既存 `RichTextEditor` の挙動・APIシグネチャ完全保持（自動整形非発動） | M3 | ORIGINAL_REQUEST R1 |
| F17 | E2E Testing Suite (Tiers 1-4) | 要件駆動のブラックボックステストスイート構築・100%全パス | M4 | Quality Verification |
| F18 | Adversarial Coverage Hardening (Tier 5) | ホワイトボックス・敵対的エッジケーステスト検証 | M4 | Quality Verification |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Inline Code Attribute | `:richtext` への `CodeKey` 追加および `:richtext-editor` でのスタイル解決 (commits f3211c6, b27be49) | none | DONE |
| M2 | WYSIWYG Auto-formatting Engine | ブロック・インライン変換ロジック、エッジケース判定、Undo/Backspace復元 (commits 5887f96, f1a1f76, Gate PASS) | M1 | DONE |
| M3 | Component Separation & WysiwygEditor | `BaseRichTextEditor` 抽出、`WysiwygEditor` 新設、`RichTextEditor` 非破壊保証 (commits c5447a3, cef8052, Gate PASS) | M2 | DONE |
| M4 | Final Milestone (E2E Pass & Hardening) | Phase 1: E2E テスト 100% パス, Phase 2: 敵対的カバレッジ強化 (commits 8dcbad1, Gate PASS) | M3, TEST_READY.md | DONE |

## Interface Contracts

### `:richtext` ↔ `:richtext-editor`
- `CodeKey`:
  ```kotlin
  package dev.mkeeda.arranger.richtext

  public data object CodeKey : SpanAttributeKey<Unit>
  ```
  `AttributeEditScope` に `code()` / `fun code()` ヘルパー関数を提供。
- `DefaultAttributeStyleResolver`:
  `CodeKey` に対して `SpanStyle(fontFamily = FontFamily.Monospace, background = ...)` を返すようマッピング。

### WYSIWYG Auto-formatting Engine ↔ Editor State
- `WysiwygInputTransformation`:
  ```kotlin
  package dev.mkeeda.arranger.richtext.editor.wysiwyg

  internal class WysiwygInputTransformation(
      private val state: RichTextState,
  ) : InputTransformation {
      override fun TextFieldBuffer.transformInput()
  }
  ```
- `WysiwygAutoFormatHandler`:
  - `handleBlockAutoFormat(buffer: TextFieldBuffer, state: RichTextState): Boolean`
  - `handleInlineAutoFormat(buffer: TextFieldBuffer, state: RichTextState): Boolean`
  - `revertLastAutoFormat(state: RichTextState): Boolean`

### Public API Contract
- `RichTextEditor` (変更なし):
  ```kotlin
  @Composable
  public fun RichTextEditor(
      state: RichTextState,
      modifier: Modifier = Modifier,
      readOnly: Boolean = false,
      textStyle: TextStyle = TextStyle.Default,
      attributeStyleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
      interactionSource: MutableInteractionSource? = null,
      cursorBrush: Brush = SolidColor(Color.Black),
      onLinkClick: ((String) -> Unit)? = null,
  )
  ```
- `WysiwygEditor` (新規公開):
  ```kotlin
  @Composable
  public fun WysiwygEditor(
      state: RichTextState,
      modifier: Modifier = Modifier,
      readOnly: Boolean = false,
      textStyle: TextStyle = TextStyle.Default,
      attributeStyleResolver: AttributeStyleResolver = DefaultAttributeStyleResolver,
      interactionSource: MutableInteractionSource? = null,
      cursorBrush: Brush = SolidColor(Color.Black),
      onLinkClick: ((String) -> Unit)? = null,
  )
  ```

## Code Layout
- `:richtext`:
  - `richtext/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/Attributes.kt` (CodeKey 追加)
  - `richtext/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/AttributeEditScope.kt`
  - `richtext/src/commonTest/kotlin/dev/mkeeda/arranger/richtext/AttributesTest.kt`
- `:richtext-editor`:
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/DefaultAttributeStyleResolver.kt` (CodeKey スタイル解決)
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/BaseRichTextEditor.kt` (共通基底)
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/RichTextEditor.kt` (委譲)
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/WysiwygEditor.kt` (公開UI)
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/WysiwygInputTransformation.kt`
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/WysiwygAutoFormatter.kt`
  - `richtext-editor/src/commonMain/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/WysiwygState.kt`
  - `richtext-editor/src/commonTest/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/WysiwygAutoFormatterTest.kt`
  - `richtext-editor/src/commonTest/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/WysiwygEditorTest.kt`
