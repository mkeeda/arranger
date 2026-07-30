---
name: pr-reviewer
description: Pull Requestのコードレビューを行い、gh CLIコマンドを用いてGitHub上でレビューコメントを提出します。PR番号やURLが与えられた際に使用してください。
---

# PR Review Workflow Skill

このスキルは、`gh` CLI コマンドを用いてGitHubのPull Request (PR) に対するコードレビューを行うためのワークフロー定義です。

---

## ワークフロー

### 1. PRの情報を `gh` CLI で取得する
対象のPRに関する情報を `gh` CLI 経由で収集します。

- **PRメタデータの取得**: PRのタイトル、本文、作成者、ステータスを確認
  ```bash
  gh pr view <pr_number>
  ```
- **Diff (変更差分) の取得**: PRの変更内容を取得
  ```bash
  gh pr diff <pr_number>
  ```
- **CIステータスの確認**: テストやビルドの成否を確認
  ```bash
  gh pr checks <pr_number>
  ```

---

### 2. コードを分析・レビューする
取得したDiffをもとに、以下の観点でコードを精査します：

- **バグの有無**: 論理エラー、NullReference等の考慮漏れ、エッジケースへの対応ができているか。
- **プロジェクト規約の遵守**: `.agents/rules/` 内のルールファイル（`architecture-principles.md`, `code-style-guide.md`, `testing-strategy.md`）に従っているか。
- **パフォーマンスと設計**: 不要なアロケーションや Compose の Recomposition パフォーマンス影響はないか。
- **型安全性とKMP分離**: `AttributeKey<T>` や `commonMain` への適切な分離がなされているか。
- **テストの網羅性**: 必要な単体テストや Roborazzi ビジュアルテストが追加されているか。
- **ドキュメント・英語表記**: 公開APIのKDocやコメントが英語で正しく書かれているか。

---

### 3. レビュー内容の整理と方針決定
指摘事項や改善案、または賞賛（LGTMなど）をまとめます。

- **サマリー**: PR全体に対する所感や重要な指摘事項。
- *(推奨)* そのまま投稿する前に、「このような観点でレビューし、修正提案（またはApprove）を投稿しますが進めてよいですか？」とユーザーに提案し、**合意を得てから**次のステップへ進むと安全です。

---

### 4. `gh` CLI でレビューを提出する
ユーザーの承認を得たら、`gh pr review` コマンドを用いてレビューを送信します。
送信するコメントは **全て英語で** 記述し、本文の先頭に `(AI review)` プレフィックスを必ず付与してください。

- **問題がなく承認する場合 (`--approve`)**:
  ```bash
  gh pr review <pr_number> --approve --body "(AI review) LGTM! Excellent implementation of the feature."
  ```
- **修正を求める場合 (`--request-changes`)**:
  ```bash
  gh pr review <pr_number> --request-changes --body "(AI review) Overall looks good, but please address the following issues:

  1. Issue A...
  2. Issue B..."
  ```
- **コメント・質問のみの場合 (`--comment`)**:
  ```bash
  gh pr review <pr_number> --comment --body "(AI review) Thank you for the PR! A quick question regarding..."
  ```

※ 自分のアカウントで自分のPRにレビューを投稿する場合は、GitHubの仕様上 `--approve` や `--request-changes` が拒否されるため、常に `--comment` を使用してください。
