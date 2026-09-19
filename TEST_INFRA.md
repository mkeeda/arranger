# Test Infrastructure Specification: Arranger Documentation Site

## 1. Overview & Objectives

本ドキュメントは、Compose Multiplatform 向けリッチテキストエディタエンジン「Arranger」の公式ドキュメントサイト（Material for MkDocs 採用、公開URL: `https://mkeeda.github.io/arranger/`）および関連インフラストラクチャに対する **E2E 自動テストハーネスの設計仕様と運用ガイド** を定義するものです。

Dual Track（Implementation Track と E2E Testing Track）の分離原則に基づき、本テストスイートは実装コードに依存しないブラックボックス（要件駆動）なテストとして設計されています。

---

## 2. 4-Tier Verification Architecture

テストスイートは `tests/test_docs.py` に配置され、以下の 4-Tier モデルに従って構造化されています。

```
+-------------------------------------------------------------------------+
|                  4-Tier Documentation Verification Model                 |
+-------------------------------------------------------------------------+
| Tier 1: Feature Coverage (全27ページ実在性, H1/Frontmatter, nav一致)     |
| Tier 2: Boundary & Corner Cases (相対リンク解決, 画像・GIF実在性, 孤立検知)|
| Tier 3: Cross-Feature Combinations (コードタブ記法, 言語タグ, パレット, 構文) |
| Tier 4: Real-World Acceptance Scenarios (CIワークフロー, README, strictビルド)|
+-------------------------------------------------------------------------+
```

### Tier 1: Feature Coverage (機能網羅)
- **対象**: `PROJECT.md` § Documentation Site Information Architecture に定義された全8カテゴリ・27Markdownファイル (`index.md` + 26個別ページ)。
- **検証項目**:
  - `docs/` 配下に全27ファイルが存在し、0バイトでないこと。
  - 各ファイルに有効な H1 見出し（`# タイトル`）または YAML frontmatter `title:` が定義されていること。
  - `mkdocs.yml` の `nav:` 宣言に全8カテゴリおよび全27ページが完全一致でマッピングされていること。
  - `requirements.txt` に `mkdocs-material` が定義されていること。
  - `.gitignore` にビルド成果物（`/site/`, `.venv/`, `.cache/`）の無視設定が含まれていること。

### Tier 2: Boundary & Corner Cases (境界値・整合性)
- **対象**: 全Markdownファイル内のリンク・メディア参照およびファイルシステム整合性。
- **検証項目**:
  - **相対リンク解決性**: ドキュメント内のすべての内部Markdownリンク (`[text](target.md)`) が実在するファイルへ解決されること（404・デッドリンクなし）。
  - **アンカー解決性**: リンク先がアンカーを含む場合 (`target.md#heading-slug`)、リンク先ファイル内に該当する見出しスラッグが実在すること。
  - **画像・デモGIF参照**: `docs/images/` への全メディア参照がファイルシステム上に実在すること。
  - **大文字小文字の一致 (Adversarial Check)**: macOS（通常大文字小文字非区別）での検証時でも、Linux CI（Ubuntu 等）でのファイルパス大文字小文字不一致によるビルド破壊を防止するため、親ディレクトリのエントリ一覧との厳密な大文字小文字比較を実施。
  - **孤立画像検知**: `docs/images/` 内の全17アセットについて、Markdownからの参照状況を追跡・検知。
  - **空リンク・空画像検知**: `[]()`, `[text]()`, `![]()`, `<img src="">` 等の空タグが存在しないこと。

### Tier 3: Cross-Feature Combinations (複合機能・シンタックス)
- **対象**: Material for MkDocs の拡張Markdown構文およびテーマ設定。
- **検証項目**:
  - **コードタブ構文**: Pymdownx Tabbed 構文（`=== "Tab Title"`）において、タイトルがクォートされており、後続の本文が 4 スペースで適切にインデントされていること。
  - **コードブロック言語タグ**: フェンスコードブロック（````lang`）が適切に閉じられており、空やタイポではなく有効な言語識別子（`kotlin`, `yaml`, `bash`, `mermaid` 等）が指定されていること。
  - **テーマパレット切り替え**: `mkdocs.yml` の `theme.palette` にライトモード（`scheme: default`）とダークモード（`scheme: slate`）の双方が定義され、トグルアイコン（`icon:`）と名前（`name:`）が設定されていること。
  - **Admonition 構文**: `!!! note` や `???+ tip` 等のコールアウト構文において、有効な種別名（`note`, `tip`, `warning` 等）が使用され、本文が 4 スペースでインデントされていること。
  - **Mermaid 構文**: ````mermaid` ブロックが有効な図表定義（`graph`, `flowchart`, `sequenceDiagram` 等）で開始されていること。

### Tier 4: Real-World Acceptance Scenarios (受入シナリオ・実行検証)
- **対象**: 実際のビルド実行、出力HTML成果物、CIパイプライン設定、ルートREADMEの整合性。
- **検証項目**:
  - **CI ワークフロー設定**: `.github/workflows/docs.yml` が存在し、`pull_request` 時の `mkdocs build --strict` 検証、および `main` ブランチ push 時の GitHub Pages 自動デプロイ（`actions/upload-pages-artifact@v3`, `actions/deploy-pages@v4`, `permissions: pages: write, id-token: write`）が正確に定義されていること。
  - **Root README 契約**: ルート `README.md` に公式ドキュメントサイトURL（`https://mkeeda.github.io/arranger/`）へのリンクが含まれ、かつ冗長記述が解消されてスリム化（< 350行、目標 ~160行）されていること。
  - **`mkdocs build --strict` 実行**: 環境内の MkDocs（`.venv` またはシステム環境）を用いて厳格ビルドを実行し、警告・エラーがゼロ（終了コード 0）で静的HTMLが生成されること。
  - **生成アーティファクト検証**: `site/index.html`, `site/404.html`, `site/getting-started/quickstart/index.html` 等の重要HTMLおよび `site/sitemap.xml` が正常に出力されていること。

---

## 3. Test Cases Inventory

| Tier | Test Method | Target | Description |
|:---:|---|---|---|
| **1** | `test_all_27_pages_exist` | `docs/**/*.md` | PROJECT.md 定義の全27ページの実体存在と非0バイト検証 |
| **1** | `test_page_titles_and_frontmatter` | `docs/**/*.md` | 全ページの H1 見出し（または frontmatter title）検証 |
| **1** | `test_mkdocs_yml_exists_and_configured` | `mkdocs.yml` | サイト名、サイトURL、テーマ、docs_dir 設定検証 |
| **1** | `test_mkdocs_nav_complete_match` | `mkdocs.yml` | 8カテゴリおよび全27ページの nav 宣言完全一致検証 |
| **1** | `test_requirements_txt_dependencies` | `requirements.txt` | `mkdocs-material` 依存定義の存在検証 |
| **1** | `test_gitignore_rules` | `.gitignore` | `/site/`, `.venv/`, `.cache/` の無視設定検証 |
| **2** | `test_internal_markdown_links_resolution` | `docs/**/*.md`, `README.md` | 相対Markdownリンクおよび `#anchor` スラッグ解決検証 |
| **2** | `test_image_and_gif_asset_paths` | `docs/**/*.md`, `README.md` | 画像・GIF相対パス解決および厳密な大文字小文字検証 |
| **2** | `test_all_17_images_exist_and_no_orphan_images` | `docs/images/*` | 全17アセットの実体存在および参照状況追跡 |
| **2** | `test_no_empty_links_or_images` | `docs/**/*.md`, `README.md` | 空リンク `[]()` や空画像 `![]()` の不存在検証 |
| **3** | `test_code_tabs_syntax_and_indentation` | `docs/**/*.md` | `=== "..."` 構文および4スペースインデント検証 |
| **3** | `test_code_block_language_tags` | `docs/**/*.md` | コードブロック閉じ確認および有効言語タグ検証 |
| **3** | `test_theme_palette_definition` | `mkdocs.yml` | ライト/ダークモードパレットトグル設定検証 |
| **3** | `test_admonitions_syntax` | `docs/**/*.md` | `!!! type` 構文、種別妥当性、インデント検証 |
| **3** | `test_mermaid_diagrams_syntax` | `docs/**/*.md` | ````mermaid` 図表種別および構文妥当性検証 |
| **4** | `test_ci_workflow_specification` | `.github/workflows/docs.yml` | PR厳格検証ジョブ・Pages自動デプロイ定義検証 |
| **4** | `test_root_readme_docs_link` | `README.md` | ドキュメントサイト公式URL (`https://mkeeda.github.io/arranger/`) リンク検証 |
| **4** | `test_root_readme_slimming` | `README.md` | ルートREADMEのスリム化（<= 350行）検証 (M4スコープ) |
| **4** | `test_mkdocs_build_strict_execution` | Subprocess | `mkdocs build --strict` 終了コード0および警告ゼロ検証 |
| **4** | `test_generated_site_artifacts` | `site/**` | `site/index.html` 等の出力成果物構造検証 |

---

## 4. How to Run the Tests

テストスイート `tests/test_docs.py` は **Python 3 標準ライブラリのみで完結して実行可能** です。外部 pip パッケージがインストールされていないクリーンな環境でも Tier 1〜Tier 3 および Tier 4 の静的検証を即座に実行できます。

### 4.1 全テストの実行（サマリーレポート出力付き）
```bash
python3 tests/test_docs.py
```

### 4.2 特定 Tier のみの個別実行
```bash
# Tier 1 のみ実行
python3 tests/test_docs.py --tier 1

# Tier 2 のみ実行
python3 tests/test_docs.py --tier 2

# Tier 3 のみ実行
python3 tests/test_docs.py --tier 3

# Tier 4 のみ実行
python3 tests/test_docs.py --tier 4
```

### 4.3 標準 unittest Discovery による実行
```bash
python3 -m unittest discover tests
```

### 4.4 仮想環境での完全実行（MkDocs strict ビルド含む）
```bash
# 仮想環境のセットアップと依存インストール（初回のみ）
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# フルテスト実行
python3 tests/test_docs.py
```

---

## 5. Progressive Testability & Milestone Lifecycle

各マイルストーンにおけるテスト合格期待値:

- **Milestone 1 (インフラ & プレースホルダー)**:
  - Tier 1: 100% 合格 (全27ページ配置、`mkdocs.yml`、`requirements.txt`、`.gitignore`)
  - Tier 2: 100% 合格 (相対パス整合性、全17画像存在)
  - Tier 3: 100% 合格 (パレット設定、プレースホルダー構文妥当性)
  - Tier 4: `test_mkdocs_build_strict_execution` および CI ワークフロー定義が合格。
    ※ `test_root_readme_docs_link` および `test_root_readme_slimming` は M4 完了時に合格予定。
- **Milestone 2〜3 (コンテンツ執筆)**:
  - Tier 1〜3 が継続して 100% 合格を維持（新しいリンク・画像・タブが追加されても破損がないこと）。
- **Milestone 4 (APIリファレンス & READMEスリム化)**:
  - `README.md` がスリム化されドキュメントURLが追加されることで、Tier 4 を含む全テストが 100% 合格。
- **Milestone 5 (受入 & Adversarial Hardening)**:
  - 全 20 テストケースが 100% 合格し、アドバーサリアルストレステストをパスすること。
