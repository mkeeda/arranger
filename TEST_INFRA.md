# TEST_INFRA — Arranger WYSIWYG Auto-formatting Test Suite

## 1. テスト哲学 (Testing Philosophy)

本テストインフラは、ArrangerにおけるMarkdownショートカット自動整形（WYSIWYG Auto-formatting）および独立コンポーネント `WysiwygEditor` に対する包括的な品質保証基盤として設計されている。

- **要件駆動 (Requirement-Driven)**:
  `ORIGINAL_REQUEST.md` (R1〜R4) および `PROJECT.md` の Feature Inventory (F1〜F16) に記載されたすべての仕様と受け入れ基準からテストケースを演繹する。内部実装の詳細に依存せず、ユーザーが体感する入出力の振る舞いを検証する。
- **ブラックボックス E2E (Black-Box Testing)**:
  ユーザーのキータイピング、Backspace、Undo/Redo、カーソル移動をシミュレートし、エディタが出力するリッチテキスト文字列、適用スパン属性、段落ブロック属性、カーソル位置を外部から検証する。
- **4層テストアーキテクチャ (4-Tier Layered Testing)**:
  単機能のハッピーパスから、境界値・誤変換防止、機能間組み合わせ、そして実践的な長文執筆シナリオまで4つのTierに体系化する。
- **進歩的検証可能性 (Progressive Testability)**:
  `PROJECT.md § Interface Contracts` に準拠したテストドライバ/ハーネス抽象層（`WysiwygTestDriver`）を設けることで、M1/M2/M3 のコンポーネント開発と並行してテストスイートがコンパイル・実行可能であり、マイルストーン完了時には実実装へ透過的に接続できるように設計する。
- **非回帰性の絶対保証 (Non-regression Guarantee)**:
  既存の `RichTextEditor` が自動整形機能の影響を受けず、従来の動作およびAPIシグネチャを100%維持することを厳密に検証する。

---

## 2. テスト構成とアーキテクチャ (Test Infrastructure Architecture)

### 2.1 ディレクトリ構成
```
richtext-editor/src/commonTest/kotlin/dev/mkeeda/arranger/richtext/editor/wysiwyg/
├── WysiwygTestHarness.kt                  # テストドライバインターフェース、ハーネス、アサーションDSL
├── Tier1FeatureCoverageTest.kt            # Tier 1: F1〜F16 ハッピーパス各5件以上（計80件以上）
├── Tier2BoundaryCornerCaseTest.kt         # Tier 2: F1〜F16 境界値・異常系・誤変換防止各5件以上（計80件以上）
├── Tier3CrossFeatureCombinationTest.kt    # Tier 3: 機能間組み合わせ（ペアワイズ、ネスト、ブロック×インライン）
└── Tier4RealWorldScenarioTest.kt          # Tier 4: 実世界アプリケーションシナリオ（5大シナリオ）
```

### 2.2 テストドライバ抽象化 (`WysiwygTestDriver`)
テストスイートは、`RichTextState` と Compose `BasicTextField` の入力変換パイプライン（`InputTransformation`）、キーイベント（Backspace）、Undo/Redo を統一的に操作する `WysiwygTestDriver` インターフェースを通じて実行される。

```kotlin
interface WysiwygTestDriver {
    fun initialize(initialText: String = "", initialSpans: List<RichSpan> = emptyList())
    fun typeText(text: String)
    fun typeChar(char: Char)
    fun pressBackspace()
    fun undo()
    fun redo()
    fun setCursor(offset: Int)
    fun setSelection(start: Int, end: Int)

    val currentText: String
    val currentSelection: TextRange
    val currentSpans: List<RichSpan>

    fun isBold(range: IntRange): Boolean
    fun isItalic(range: IntRange): Boolean
    fun isStrikethrough(range: IntRange): Boolean
    fun isInlineCode(range: IntRange): Boolean
    fun isHeading(level: HeadingLevel, range: IntRange): Boolean
    fun isBulletList(range: IntRange): Boolean
    fun isOrderedList(range: IntRange): Boolean
    fun isBlockquote(range: IntRange): Boolean
}
```

- **Reference / Contract Implementation**:
  M1〜M3 が実装中または完了前の段階でも、仕様（ORIGINAL_REQUEST R1〜R4、PROJECT.md § Interface Contracts）に完全に準拠した契約シミュレーション・テストドライバを備えており、全テストケースが構文的・意味的に正しく検証される。
- **Real Implementation Adapter**:
  本番の `WysiwygInputTransformation` および `WysiwygEditor` が実装された際には、同一のインターフェース契約を通じて本番パイプラインを駆動・検証する。

---

## 3. Tier 1〜4 の設計一覧 (Tiered Test Specifications)

### Tier 1: Feature Coverage (F1〜F16, 各5件以上, 計80件以上)
各機能の典型的なユースケース（ハッピーパス）を網羅する。

| 機能ID | 機能名 | 代表テストケース内容（各5件） |
|--------|--------|------------------------------|
| **F1** | Inline Code Attribute | CodeKeyの定義、Monospaceフォント解決、背景色スタイル解決、他属性との共存、属性コンテナ等値性 |
| **F2** | Block: Heading 1..3 | 行頭 `# ` でH1変換、`## ` でH2変換、`### ` でH3変換、改行後の後続行でのH1変換、日本語見出し |
| **F3** | Block: Bullet List | 行頭 `- ` でBullet変換、`* ` でBullet変換、改行後の後続行変換、本文付き入力、リスト後の継続タイピング |
| **F4** | Block: Ordered List | 行頭 `1. ` でOrderedList変換、改行後の変換、本文付き入力、変換後カーソル位置、継続タイピング |
| **F5** | Block: Blockquote | 行頭 `> ` でBlockquote変換、改行後の後続行変換、本文付き入力、次段落への非波及、カーソル位置 |
| **F6** | Inline: Bold | `**bold**` で太字変換、文中の太字、複数単語太字、日本語太字、変換後カーソル位置 |
| **F7** | Inline: Italic (Asterisk) | `*italic*` で斜体変換、文中の斜体、複数単語斜体、日本語斜体、変換後カーソル位置 |
| **F8** | Inline: Italic (Underscore) | `_italic_` で斜体変換、文中の斜体、複数単語斜体、日本語斜体、変換後カーソル位置 |
| **F9** | Inline: Code | `` `code` `` でコード変換、文中のコード、記号を含むコード、日本語コード、変換後カーソル位置 |
| **F10**| Inline: Strikethrough | `~strike~` で取り消し線変換、文中の取り消し線、複数単語、日本語取り消し線、カーソル位置 |
| **F11**| False Positive Prevention | スネークケース `foo_bar_baz` の非変換、未閉じ記号の非変換、空記号 `****` の非変換、行途中記号の非変換、空白囲み記号の非変換 |
| **F12**| Undo / Redo Integration | 見出し変換直後Undo、リスト変換直後Undo、太字変換直後Undo、Undo後のRedo、カーソル位置復元 |
| **F13**| Backspace Reversal | 見出し変換直後Backspace復元、リスト直後Backspace、太字直後Backspace、コード直後Backspace、復元後通常Backspace |
| **F14**| Cursor & Ergonomics | 太字直後タイピングの装飾非漏洩、コード直後の非漏洩、斜体直後の非漏洩、取り消し線直後の非漏洩、変換直後カーソル位置 |
| **F15**| WysiwygEditor Component | コンポーネント生成、ブロック変換パイプライン統合、インライン変換統合、Backspaceイベント捕捉、カスタムリゾルバ |
| **F16**| RichTextEditor Non-regression | `# ` で非変換確認、`- ` で非変換確認、`**` で非変換確認、`` ` `` で非変換確認、既存API完全互換 |

### Tier 2: Boundary & Corner Cases (F1〜F16, 各5件以上, 計80件以上)
極限値、境界条件、構文衝突、エスケープ、誤変換防止などのコーナーケースを網羅する。

- **F1**: 複数行インラインコード抑止、連続バッククォートの扱い、空バッククォート、特殊文字・絵文字、前後スペース
- **F2**: H4以上 (`#### `) 変換抑止、スペースなし `#text` 抑止、行途中 ` # ` 抑止、空行での動作、既存H1での再トリガー
- **F3**: スペースなし `-text` 抑止、行途中 `a - b` 抑止、水平線記号 `--- ` 抑止、空リスト改行、インデント後トリガー
- **F4**: 先頭 `2. ` の判定、スペースなし `1.text` 抑止、行途中 `v1. 0` 抑止、スペース複数 `1.  `、記号違い `1) ` 抑止
- **F5**: スペースなし `>text` 抑止、行途中 `x > y` 抑止、連続大なり `>> `、引用末尾改行、全角 `＞ ` 抑止
- **F6**: 3連続アスタリスク `***text***`、片側スペース `** text**` 抑止、単語内 `foo**bar**baz`、エスケープ `\*\*`、改行跨ぎ
- **F7**: 数式 `2 * 3 * 4` 抑止、単語内 `foo*bar*baz`、片側スペース `* text*`、エスケープ `\*`、改行跨ぎ
- **F8**: スネークケース `my_var_name` 抑止、定数 `MAX_VALUE` 抑止、特殊記号 `__init__` 抑止、URL `a_b_c` 抑止、片側スペース
- **F9**: エスケープ ``\`code\`` 抑止、単一バッククォート、パス表現 `/usr/`local`/bin`、リンク内記号、長文テキスト
- **F10**: パス `~/dir` 抑止、波線 `~50%` 抑止、二重チルダ `~~text~~`、片側スペース `~ text~`、エスケープ `\~`
- **F11**: 空行トリガー、記号のみ（`***`, `___`）、サロゲートペア/絵文字境界、ゼロ幅スペース、高速タイピング・バッファ一括
- **F12**: 変換後別文字入力後のUndo、連続ブロック+インラインの連鎖Undo、Undo復元後の再トリガー、空スタックRedo、履歴上限
- **F13**: 変換後カーソル移動後のBackspace（通常削除）、選択範囲Backspace、復元直後Space再トリガー、連続Backspace、IME確定後
- **F14**: 行末変換直後のカーソル、インデックス0での変換、文中挿入時のカーソル、タイピング属性解除後、選択範囲上書き
- **F15**: readOnly=true での変換抑止、enabled=false の安定性、巨大文字列の初期化性能、外部 state.edit 同期、リコンポジション耐性
- **F16**: RichTextEditor での高速記号入力、Undo/Redo への干渉なし、既存ショートカット（Cmd+B/I）の動作、EnterKeyStrategy 互換、同一画面併用

### Tier 3: Cross-Feature Combinations (ペアワイズ・ネスト検証, 15件)
- **C1**: リスト項目内の太字 (`- Item with **bold** word`)
- **C2**: リスト項目内のインラインコード (`- Item with `code` syntax`)
- **C3**: 見出し内のインラインコード (`# Heading with `inline code``)
- **C4**: 見出し内の太字 (`## Heading with **bold** text`)
- **C5**: 引用ブロック内の取り消し線 (`> Quote with ~strikethrough~ text`)
- **C6**: 引用ブロック内の太字と斜体 (`> **Bold** and *italic* in quote`)
- **C7**: 同一行での太字・アスタリスク斜体・アンダースコア斜体の混在
- **C8**: 番号付きリスト内の太字とインラインコード (`1. Step 1: run `install` and **verify**`)
- **C9**: 複数行にわたる異なるブロック記号（H1 -> Bullet -> Ordered -> Quote）の順次変換
- **C10**: インライン変換直後の改行ブロック変換
- **C11**: ブロック変換直後のインライン変換
- **C12**: ネスト装飾（`***bold italic***`）の適用優先度
- **C13**: リスト項目内でのインライン装飾 Undo（リスト属性維持）
- **C14**: リスト項目内でのインライン装飾 Backspace 復元（リスト属性維持）
- **C15**: 複合ドキュメント全体の一連の Undo / Redo 連鎖検証

### Tier 4: Real-World Application Scenarios (実世界シナリオ, 5大シナリオ)
- **Scenario 1: テクニカルブログ記事の執筆**
  - タイトルH1、概要Blockquote、特徴BulletList、インストール手順OrderedList、インラインコードと太字、Backspace修正
- **Scenario 2: スプリント計画ミーティング議事録**
  - H2アジェンダ、参加者リスト（太字・斜体）、決定事項Quote、タスク番号リスト、Undoでの誤変換取り消し
- **Scenario 3: 開発者向けリリースチェックリスト**
  - H1タイトル、完了タスクの取り消し線、未完了タスクの太字、インラインコードバージョン表記、Undo復元
- **Scenario 4: 学術・技術論文サマリー**
  - H1/H2/H3階層構造、数式・変数インラインコード、スネークケース保護、強調斜体、引用文
- **Scenario 5: 試行錯誤を伴う高度なインタラクティブ編集**
  - 見出し作成 -> Backspace復元 -> 再入力 -> リスト作成 -> Undo復元 -> 太字入力 -> Undo訂正 -> 最終ドキュメント整合性

---

## 4. カバレッジ目標と品質基準 (Coverage Targets & Quality Gates)

| 指標 | 目標値 | 検証方法 |
|------|--------|----------|
| **Feature Coverage (F1〜F16)** | 100% (80+ cases) | `Tier1FeatureCoverageTest` 全件パス |
| **Boundary & False Positive** | 100% (80+ cases) | `Tier2BoundaryCornerCaseTest` 全件パス |
| **Cross-Feature Combinations**| 100% (15 cases) | `Tier3CrossFeatureCombinationTest` 全件パス |
| **Real-World Scenarios**      | 100% (5 scenarios)| `Tier4RealWorldScenarioTest` 全件パス |
| **RichTextEditor Non-regression** | 100% (F16) | 既存テスト (`RichTextStateTest` 等) および F16 テスト全件パス |
| **コードスタイル・静的解析**  | 0 violations | `./gradlew spotlessCheck` パス |

---

## 5. 実行方法と検証コマンド (Execution & Verification)

### テスト実行コマンド
```bash
# 全E2Eテストスイートの実行
./gradlew :richtext-editor:jvmTest

# 特定のTierの実行
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier1FeatureCoverageTest"
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier2BoundaryCornerCaseTest"
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier3CrossFeatureCombinationTest"
./gradlew :richtext-editor:jvmTest --tests "dev.mkeeda.arranger.richtext.editor.wysiwyg.Tier4RealWorldScenarioTest"

# フォーマット・静的解析チェック
./gradlew spotlessCheck
```
