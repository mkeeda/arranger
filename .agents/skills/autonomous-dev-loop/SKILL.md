---
name: autonomous-dev-loop
description: Arrangerプロジェクトにおいて、アイディアや課題から「プランニング（壁打ち・スコープ決定） -> フィードバック駆動TDD実装 -> 多角的な品質検証 -> MD/HTMLでの成果物デモ -> 英語でのGitHub Issueタスク化」の自律開発ループを実行・指示するスキル。
---

# Autonomous Development Loop Skill for Arranger

本スキルは、Antigravityエージェントがユーザーからアイデア、機能要望、バグ報告を受け取った際、完全自律的に開発サイクルを進行させるための手順とガイドラインです。

---

## 開発サイクルの全体フロー

```
[1. Planning & Alignment] 
   └─壁打ち、要件・スコープ明確化 ─> implementation_plan.md 作成 ─> ユーザー承認 (Proceed)
[2. Feedback-Driven TDD & Implementation]
   └─テストコード（commonTest / UI）作成 ─> 実装 ─> 新たな知見を即座にPlan/QAへ再フィードバック
[3. Multi-layer Quality Assurance]
   └─ ./gradlew test ＋ Roborazzi スナップショット検証 ＋ spotlessCheck ＋ 応用検証(a11y/Perf)
[4. Demo & Walkthrough]
   └─ walkthrough.md (および複雑な動的表示ならHTMLデモ) に動作結果と画像/ログを記載
[5. Task Breakdown via GitHub Issue]
   └─ レビューフィードバックや残課題を `gh issue create` で英語起票
```

---

## ステップ 1: プランニングと壁打ち (Planning & Alignment)

1. **要求の要約とゴール確定**:
   - ユーザーの要望を分析し、不明確な点があれば積極的にユーザーへ確認質問・アイデア提示を行う。
2. **Implementation Plan の作成**:
   - `<appDataDir>/brain/<conversation-id>/implementation_plan.md` に以下を明記する：
     - ゴールとスコープ
     - 今後のTDDで作成予定のテストケース一覧
     - 変更予定のファイルおよびコンポーネント構成
     - User Review Required（設計上の判断事項や確認点）
   - `ArtifactMetadata` で `RequestFeedback: true` を設定し、ユーザーへ承認を依頼する。

---

## ステップ 2: フィードバック駆動TDDと実装 (Feedback-Driven Execution)

1. **TDD (Test-Driven Development)**:
   - 『`.agents/rules/testing-strategy.md`』に従い、まずは `commonTest` や Compose UI テストにアサーションを作成する。
   - バッククォート（`` ` ``）で囲んだ英語の関数名と AAA パターンを徹底する。
2. **実装と相互フィードバック**:
   - 『`.agents/rules/architecture-principles.md`』に従い、UDF、`AttributeKey<T>` による型安全性、Compose再描画防止、KMP分離を守って実装する。
   - **フィードバックループの実行**: 実装やテスト中に想定外のエッジケースや設計上の課題を見つけた場合は、作業を強行せず、即座に `implementation_plan.md` やテスト戦略を更新してフィードバックを反映させる。

---

## ステップ 3: 多角的な品質検証 (Quality Assurance)

1. **自動テストの実行**:
   - `./gradlew test`（JVM/commonTestの単体テスト）
   - `./gradlew verifyRoborazziDebug`（画面表示の画像アサーション。画像更新時は `./gradlew recordRoborazziDebug`）
   - `./gradlew spotlessCheck`（コード整形チェック）
2. **追加の品質検証（必要に応じて）**:
   - セマンティクス / アクセシビリティ (a11y) アサーション
   - パフォーマンス / マイクロベンチマーク検証

---

## ステップ 4: 成果物デモの提示 (Walkthrough)

1. **Walkthrough Artifact の作成**:
   - `<appDataDir>/brain/<conversation-id>/walkthrough.md` を作成・更新する。
   - 実施した変更点、テスト実行ログの概要、Roborazziで撮影したUIスナップショット画像（またはGIF）を埋め込んでユーザーに提示する。
2. **HTMLデモの作成（選択肢）**:
   - UIアニメーション、複雑なリッチテキスト操作インタラクション、多角的なテストマトリクスレポートなど、Markdownだけでは表現しきれない視覚情報がある場合、Artifacts内にHTMLファイルを作成してビジュアルデモとして提示する。

---

## ステップ 5: レビュー対応と GitHub Issue タスク化 (Task Breakdown)

1. **ユーザーレビューの受諾**:
   - Walkthrough / HTML デモを提示し、ユーザーからのレビューを受ける。
2. **GitHub Issue への登録**:
   - レビュー結果から導かれた今後の改善案、リファクタリング、未対応のエッジケース、あるいはネクストアクションについて、`gh issue create` コマンドを実行して**英語**で登録する。
   - 例:
     ```bash
     gh issue create --title "feat(richtext): add support for nested bullet list indentation" --body "..."
     ```
