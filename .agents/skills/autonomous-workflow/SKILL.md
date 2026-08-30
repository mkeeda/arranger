---
name: autonomous-workflow
description: Arrangerプロジェクトで新機能開発、アーキテクチャ変更、複数モジュール改修、または仕様策定を伴うタスクを行う際に、Architect設計対話・人間承認ゲート・サブエージェント自律反復ループ（developer ➔ qa-engineer ➔ reviewer）を実行するためのオーケストレーション手順書です。
---

# Arranger Autonomous Development Workflow Skill

このスキルは、Arranger プロジェクトにおいて新機能開発や設計変更を自律的・高品質に進めるためのオーケストレーション手順書です。
メインエージェント自身が **Architect & Orchestrator** としてユーザーと直接対話し、要件定義・プラン策定・人間承認ゲートを管理した上で、専門サブエージェント（`developer`, `qa-engineer`, `reviewer`）をディスパッチして自律反復ループを回します。

---

## 1. 自然言語とコミュニケーション原則

- **日本語の標準使用**: 対話、思考プロセス、ローカル Artifact（`implementation_plan.md`, `walkthrough.md` 等）、AIハーネス設定（`.agents/` 配下）はすべて**日本語**で記述する。
- **公開情報の英語化**: コミットメッセージ、Pull Request、Issue、コード内コメント、KDoc、テスト関数名、プロダクションコードはすべて**英語**で記述する。

---

## 2. メインエージェントによる Architect 責務

メインエージェント自身が **Architect** としてユーザーと直接対話し、以下の設計責務を果たします：

1. **Why & What の明確化**: ユーザーの要望を分析し、「目的（Why）」と「達成すべき振る舞い（What）」を抽象度高く整理する。
2. **Public API-First**: 利用者が直感的に記述できる Public API シグネチャ（DSLや拡張関数等）を最優先で設計する。
3. **Evidence-Based Engineering**: プラットフォーム仕様等は推測せず、公式コードやドキュメントでファクトチェックを行う。
4. **境界条件 & DoD 策定**: 後続のテスト設計に向け、プロダクトレベルの境界条件と **Definition of Done (DoD)** を明示する。
5. **How（実装）への過度な立ち入りの防止**: プランニング段階で実装詳細（How）に踏み込まず、Why/What/DoD の策定に集中し、How は後続の `developer` に委ねる。

---

## 3. ゴール達成基準 (Definition of Done: DoD) とサーキットブレーカー

### (1) ゴール達成基準 (DoD) の 4 本柱
すべての機能開発・バグ修正は、以下の 4 条件を満たすまで自律的に反復（Iterate）しなければならない：
1. **要件完全性**: `implementation_plan.md` に記載された Public API 仕様・機能要件がすべて満たされていること。
2. **探索的テスト完全合格**: `qa-engineer` が考案・追加したすべてのエッジケース・探索的シナリオテストが全件パスしていること。
3. **必須品質ゲート全通過**:
   - `./gradlew allTests`（全ターゲット単体テスト 100% Pass）
   - `./gradlew verifyRoborazziDebug`（ビジュアル回帰ゼロ / ※ベースライン無断更新は絶対禁止）
   - `./gradlew spotlessCheck`（コード整形エラーゼロ）
4. **Reviewer 厳格承認**: `reviewer`（Proモデル）によるコード監査で、アーキテクチャ原則違反・保守性低下・不適切な設計が 0 件（Unconditional LGTM）であること。

### (2) サーキットブレーカー (Circuit Breaker)
- **最大反復上限**: 同一タスクにおける自律修正ループ（Dev ➔ QA ➔ Reviewer ➔ Dev）は**最大 3 回まで**とする。
- **エスカレーション**: 3 回ループしてもテストがパスしない、または Reviewer の指摘が解消しない場合は、自律ループを即座に中断し、問題の根本原因・試行内容・トレードオフを整理してユーザーに報告し、判断を仰ぐこと。

---

## 4. 開発オペレーション手順 (Execution Steps)

```text
[Step 1: Plan (Main Agent)] ➔ [Human Approval Gate] ➔ [Step 2: Dev] ➔ [Step 3: QA] ➔ [Step 4: Reviewer] ➔ [Step 5: Commit & Walkthrough] ➔ [Step 6: PR (User Approved)]
                                                             ↑               |              |
                                                             └────── Red ────┴─── Changes ──┘ (Max 3 Loops)
```

### Step 1. プランニング & Architect対話 (`implementation_plan.md` 作成)
- メインエージェントがユーザーと直接対話し、Why/What、Public API設計、境界条件、および **Definition of Done (DoD)** を整理した `implementation_plan.md` を作成する。
- **作成後、即座にツール呼び出しを停止してターンを終了し、ユーザーからの承認（Proceed）を必ず待つ。**

### Step 2. TDD実装委託 (`developer`)
- **ユーザーの承認を得てから**、`invoke_subagent` で `developer` (`Workspace: 'inherit'`) を起動する。
- `testing-strategy.md` に基づき、対象に最適なテストコード（`commonTest`、Roborazzi、UIセマンティクス等）を先行作成させ、`commonMain` に実装して単体テストの Green を確認させる。

### Step 3. 探索的テスト & 品質検証委託 (`qa-engineer`)
- `invoke_subagent` で `qa-engineer` (`Workspace: 'inherit'`) を起動する。
- ユーザー視点での探索的テスト（複合スタイル、長文、IME操作、Roborazzi視覚的破壊テスト等）を実行させる。
- **バグ発見時**: `qa-engineer` が失敗再現テスト（Red Test）を追加した上で、Orchestrator が `developer` を再起動して修正させ、テストが Green になるまで自律ループ（Step 2 ➔ Step 3）を回す。
- `./gradlew allTests`, `./gradlew verifyRoborazziDebug`, `./gradlew spotlessCheck` の全通過を確認する。

### Step 4. 厳格コード監査委託 (`reviewer`)
- `invoke_subagent` で `reviewer` (`Model: 'pro'`) を起動する。
- 客観的・敵対的視点で `architecture-principles.md`, `code-style-guide.md`, `testing-strategy.md` への準拠および DoD の達成度を査定させる。
- 指摘事項があれば `developer` に差し戻して修正ループを回し、完全な LGTM を獲得する。

### Step 5. 自律コミット & Walkthrough報告
- 全DoDを達成した後、Orchestrator が `git-commit-formatter` スキルを活用して、変更差分を意味のある適切な単位（機能追加、テスト拡充、リファクタリング等）に分割して自律的にコミットを作成する。
- Artifact `walkthrough.md` に成果・検証結果・探索的テストの内容を記載してユーザーへ報告する。
- **ツールの呼び出しを停止し、ユーザーからのレビュー（フィードバック・修正指示）を待つ。**

### Step 6. ユーザー最終合格後の PR 作成
- **ユーザーの最終レビューに合格（最終承認）した後**、`pr-creator` スキルを用いて GitHub 上に**英語**で Pull Request を作成する。
