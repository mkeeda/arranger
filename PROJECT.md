# Project: Arranger Documentation Site

## Architecture

### System Architecture & Pipeline
Arranger は Compose Multiplatform 向けのリッチテキストエディタエンジンです。本プロジェクトでは、公式ドキュメントサイトを Material for MkDocs を用いて構築し、全モジュールの公開APIを網羅的に文書化し、GitHub Pages へ自動デプロイするCI/CDパイプラインを確立します。

```
[Arranger Codebase] ──(KDoc Annotation)──────► [Dokka (Kotlin 2.x / KMP)]
        │                                              │ (dokkaHtmlMultiModule)
        ▼                                              ▼
[docs/**/*.md] ──────(Guides & Architecture)─► [Material for MkDocs Engine]
[docs/images/*] ────(Visual Assets)────────┤      │ (mkdocs build --strict)
[mkdocs.yml] ────────(Site Config & Nav)───┘      ▼
                                             [site/ (Static HTML)]
                                                  │ (Dokka HTML merged to site/api/)
                                                  ▼
                            [GitHub Actions: .github/workflows/docs.yml]
                                                  │
                                    (main push) ─┴─► [GitHub Pages Deployment]
                                                     (https://mkeeda.github.io/arranger/)
```

### Documentation Site Information Architecture (8 Categories / 26 Pages + Dokka Reference)
1. **Home / Getting Started**
   - `docs/index.md` (概要、特徴、クイックスタート、デモ)
   - `docs/getting-started/installation.md` (Gradle依存設定、KMPターゲット対応状況)
   - `docs/getting-started/quickstart.md` (最小限のRichTextEditor導入コード)
2. **Editor Basics**
   - `docs/editor-basics/rich-text-editor.md` (RichTextEditorコンポーネント基本、パラメータ詳解、basic-usage.png)
   - `docs/editor-basics/wysiwyg-editor.md` (WysiwygEditor、Markdownショートカット、即時バックスペース解除)
   - `docs/editor-basics/state-management.md` (RichTextState, typingAttributes, currentAttributes論理積, undo/redo)
3. **Styling & Formatting**
   - `docs/styling/spans-and-paragraphs.md` (Span属性 vs Paragraph属性の概念と適用)
   - `docs/styling/built-in-attributes.md` (太字、斜体、下線、打消し、インラインコード、色、フォントサイズ、リンク、見出し、配置、引用、リスト)
   - `docs/styling/custom-attributes.md` (独自AttributeKeyとAttributeEditScope拡張の実装方法)
   - `docs/styling/theming-and-m3.md` (AttributeStyleResolver, DefaultAttributeStyleResolver, Material3統合)
4. **Advanced Behaviors**
   - `docs/advanced-behaviors/enter-key-strategies.md` (EnterKeyStrategy, Inherit, Heading, List戦略、独自戦略)
   - `docs/advanced-behaviors/list-handling.md` (箇条書き・番号付きリスト、ListMarkerResolver、インデント制御)
   - `docs/advanced-behaviors/span-merging.md` (Sweep-line区間分割アルゴリズム、resnapParagraphSpans)
5. **Interactive Features**
   - `docs/interactions/autocomplete.md` (AutocompleteTrigger, detectAutocomplete, createPopupPositionProvider, サジェストUI)
   - `docs/interactions/span-clicks.md` (onSpanClick, SpanClickEvent, consume()によるカーソル移動抑止)
   - `docs/interactions/toolbars.md` (RichTextStateFormatExt高水準API, Modifier.focusPropertiesフォーカス保護)
6. **Interoperability & Formats**
   - `docs/interop/markdown.md` (toMarkdown, fromMarkdown, GFM仕様、InlineCodeKey現行制約)
   - `docs/interop/html.md` (toHtml, fromHtml, インラインCSS対応)
   - `docs/interop/custom-formats.md` (RichTextFormat, RichTextImporter, RichTextExporter拡張)
7. **Architecture & Internal Design**
   - `docs/architecture/overview.md` (レイヤードアーキテクチャ: コア純粋KMP vs Compose UIバインディング)
   - `docs/architecture/state-lifecycle.md` (TextFieldState連携、不変RichStringとミュータブルバッファ)
8. **API Reference**
   - `docs/api/overview.md` (全モジュール一覧、API概要マップ、Dokkaインタラクティブリファレンスへの導線)
   - `docs/api/richtext.md` (`:richtext` モジュール概要・Dokka導線)
   - `docs/api/richtext-editor.md` (`:richtext-editor` モジュール概要・Dokka導線)
   - `docs/api/richtext-editor-material3.md` (`:richtext-editor-material3` モジュール概要・Dokka導線)
   - `docs/api/richtext-markdown.md` (`:richtext-markdown` モジュール概要・Dokka導線)
   - `docs/api/richtext-html.md` (`:richtext-html` モジュール概要・Dokka導線)
   - **Dokka KDoc API Reference**: 全公開APIのHTMLドキュメント（`site/api/` に統合配置）

---

## Feature Inventory

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | MkDocs Base Setup | `mkdocs.yml`, `requirements.txt`, `.gitignore` の基本構成定義 | M1 | Survey 2 |
| 2 | MkDocs Material Theme & Extensions | パレット切り替え、JetBrains Mono、検索、コードタブ、Mermaid、strict検証設定 | M1 | Survey 2 |
| 3 | GitHub Actions CI/CD Pipeline | `.github/workflows/docs.yml` (PR strict検証, main push 自動デプロイ) | M1 | Survey 2 |
| 4 | Asset Path Standard & Placeholder Layout | `docs/images/*` 相対パス規約確立および全26ページのプレースホルダー先行配置 | M1 | Survey 2, 3 |
| 5 | Getting Started Docs | Index, Installation, Quickstart ページの執筆 | M2 | Survey 3 |
| 6 | Editor Basics Docs | RichTextEditor, WysiwygEditor, State Management (typing/current/undo) ページの執筆 | M2 | Survey 1, 3 |
| 7 | Styling & Formatting Docs | Span/Paragraph, Built-in Attributes, Custom Attributes, Theming/M3 ページの執筆 | M2 | Survey 1, 3 |
| 8 | Advanced Behaviors Docs | EnterKeyStrategies, List Handling, Span Merging ページの執筆 | M3 | Survey 1, 3 |
| 9 | Interactive Features Docs | Autocomplete, Span Clicks, Toolbars ページの執筆 | M3 | Survey 1, 3 |
| 10 | Interoperability Docs | Markdown (GFM/制限事項), HTML, Custom Formats ページの執筆 | M3 | Survey 1, 3 |
| 11 | Architecture & Design Docs | Overview, State Lifecycle ページの執筆 | M3 | Survey 1, 3 |
| 12 | Dokka KMP Setup & Multi-Module Task | Dokkaプラグイン導入、KDocからのHTML APIリファレンス生成 (`dokkaHtmlMultiModule`) | M4 | ユーザー要望 |
| 13 | Dokka MkDocs & CI Integration | CI上でDokka成果物を `site/api/` に配置・統合しGitHub Pagesへデプロイ | M4 | ユーザー要望 |
| 14 | API Reference Markdown Guides | 各モジュールの概要・主要型解説とDokkaリファレンスへのリンク整備 | M4 | ユーザー要望, Survey 1 |
| 15 | Root README.md Slimming | プロジェクト概要、主要機能、クイックスタート、新ドキュメントサイトへの誘導を含むスリム化 (~160行) | M4 | Survey 3 |
| 16 | E2E Documentation Test Suite | `mkdocs build --strict`、リンク整合性、全画像表示、ナビゲーション網羅性のE2Eテスト検証 | M5 | E2E Testing Track |
| 17 | Adversarial Coverage Hardening | Tier 5: エッジケース、孤立ページ、壊れたアンカー、不正Markdownの網羅的ストレステスト | M5 | E2E Testing Track |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | MkDocs Site Infrastructure & CI/CD | `mkdocs.yml`, `requirements.txt`, `.gitignore`, `.github/workflows/docs.yml`, 全26ページのプレースホルダー配置 | none | DONE |
| M2 | Core Guides (Getting Started, Editor Basics, Styling) | `docs/index.md`, `docs/getting-started/*.md`, `docs/editor-basics/*.md`, `docs/styling/*.md` の完全執筆 | M1 | DONE |
| M3 | Advanced, Interactive, Interop & Architecture Docs | `docs/advanced-behaviors/*.md`, `docs/interactions/*.md`, `docs/interop/*.md`, `docs/architecture/*.md` の完全執筆 | M1 | DONE |
| M4 | Dokka Integration, API Guides & Root README Slimming | Dokka設定・生成、DokkaとMkDocs/CI統合、`docs/api/*.md` 執筆、`README.md` のスリム化更新 | M2, M3 | DONE |
| M5 | Final Acceptance & Adversarial Hardening | Phase 1: E2E テストスイート 100% 合格 (`mkdocs build --strict` 完全通過、Dokkaビルド確認)<br>Phase 2: Adversarial Coverage Hardening | M4, E2E Testing Track | DONE |

---

## Dual Track: Implementation & E2E Testing

1. **Implementation Track**:
   - 上記 M1 ➔ M2 ➔ M3 ➔ M4 ➔ M5 の順序で推進。
   - 各マイルストーンはサブオーケストレーターに委譲し、自律的反復ループ (Explorer ➔ Worker ➔ Reviewer ➔ Challenger ➔ Auditor) によりゲート通過を検証。

2. **E2E Testing Track**:
   - 並行して E2E Testing Orchestrator をディスパッチ。
   - 要件・Feature Inventory に基づく網羅的検証テストハーネス (Tier 1〜Tier 4) を設計・構築。
   - `TEST_INFRA.md` および `TEST_READY.md` を発行。

---

## Interface Contracts

### 1. MkDocs Navigation Contract (`mkdocs.yml` ↔ `docs/**/*.md`)
- `mkdocs.yml` の `nav:` に宣言されたすべての `.md` パスは、`docs/` 直下に実体ファイルとして存在しなければならない（未作成時は見出し付きプレースホルダーを配置）。
- `site_url`: `https://mkeeda.github.io/arranger/`

### 2. Asset Path Contract (`docs/**/*.md` ↔ `docs/images/*`)
- `docs/index.md` からの画像参照: `images/<filename>`
- `docs/*/*.md`（第1階層）からの画像参照: `../images/<filename>`
- ルート `README.md` からの画像参照: `./docs/images/<filename>`
- すべての画像参照は実在するファイル名と大文字小文字を含め完全一致すること。

### 3. CI/CD Contract (`.github/workflows/docs.yml`)
- イベント: `pull_request`（全ブランチ）、`push`（`main` ブランチのみ）
- 検証ステップ: `python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt && mkdocs build --strict`
- デプロイステップ: `actions/upload-pages-artifact@v3` + `actions/deploy-pages@v4`（`main` push 時のみ）

### 4. Root README Contract (`README.md`)
- ドキュメントサイトへの誘導URL `https://mkeeda.github.io/arranger/` を明記。
- 既存の冗長な詳細API記述は新ドキュメントサイトへ委譲し、約160行前後の簡潔な概要に整理。

---

## Code Layout

```
.
├── .github/
│   └── workflows/
│       ├── ci.yml                 # 既存CIワークフロー
│       └── docs.yml               # [新規] ドキュメントビルド検証 & GitHub Pages デプロイ
├── docs/
│   ├── index.md                   # サイトトップ
│   ├── getting-started/
│   │   ├── installation.md
│   │   └── quickstart.md
│   ├── editor-basics/
│   │   ├── rich-text-editor.md
│   │   ├── wysiwyg-editor.md
│   │   └── state-management.md
│   ├── styling/
│   │   ├── spans-and-paragraphs.md
│   │   ├── built-in-attributes.md
│   │   ├── custom-attributes.md
│   │   └── theming-and-m3.md
│   ├── advanced-behaviors/
│   │   ├── enter-key-strategies.md
│   │   ├── list-handling.md
│   │   └── span-merging.md
│   ├── interactions/
│   │   ├── autocomplete.md
│   │   ├── span-clicks.md
│   │   └── toolbars.md
│   ├── interop/
│   │   ├── markdown.md
│   │   ├── html.md
│   │   └── custom-formats.md
│   ├── architecture/
│   │   ├── overview.md
│   │   └── state-lifecycle.md
│   ├── api/
│   │   ├── overview.md
│   │   ├── richtext.md
│   │   ├── richtext-editor.md
│   │   ├── richtext-editor-material3.md
│   │   ├── richtext-markdown.md
│   │   └── richtext-html.md
│   └── images/                    # 既存画像・GIFアセット (17点)
├── mkdocs.yml                     # [新規] Material for MkDocs 設定ファイル
├── requirements.txt               # [新規] Python 依存関係定義
├── .gitignore                     # [更新] /site/, .cache/, .venv/ 追加
└── README.md                      # [更新] ドキュメントサイトへのリンクを含むスリム化版
```
