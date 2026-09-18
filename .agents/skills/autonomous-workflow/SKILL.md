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
- **公開成果物の英語徹底**: コミットメッセージ、Pull Request、Issue、コード内コメント、KDoc、テスト関数名、プロダクションコード、公開Markdown（README等）はすべて**英語**で記述する。

---

## 2. メインエージェントによる Architect 責務

メインエージェント自身が **Architect** としてユーザーと直接対話し、以下の設計責務を果たします：

1. **Why & What の明確化**: ユーザーの要望を分析し、「目的（Why）」と「達成すべき振る舞い（What）」を抽象度高く整理する。
2. **DX-First & Public API-First**: 利用者が直感的に最小のコードで目的を達成できる高レベルAPI（DSLや拡張関数、PositionProvider等）を最優先で設計する。内部実装や生座標（`Rect`等）を安易に露出させない。
3. **価値志向の Definition of Done (DoD) 策定**: 後続のテスト設計に向け、「計画書の消化」ではなく**「ユーザー・開発者が得られる具体的な価値・体験」**を基準として DoD を定義する。
4. **How（実装）への過度な立ち入りの防止**: プランニング段階で内部実装詳細（How）に踏み込まず、Why/What/DoD の策定に集中し、How は後続の `developer` に委ねる。

---

## 3. ゴール達成基準 (Definition of Done: DoD) とサーキットブレーカー

### (1) ゴール達成基準 (DoD) の 5 本柱
すべての機能開発・バグ修正は、以下の 5 条件を満たすまで自律的に反復（Iterate）しなければならない：
1. **価値・要件完全性**: 計画書で定義された開発者体験・機能価値・Public API仕様がすべて満たされていること。
2. **探索的テスト合格と回帰スイート統合**: `qa-engineer` による探索的テストが完了し、**回帰テストとして残すべき有意義なシナリオが既存の恒久テストスイートへと完全に統合・マージされていること**（孤立した使い捨てテストファイルが残っていないこと）。
3. **必須品質ゲート全通過**:
   - `./gradlew allTests`（全ターゲット単体テスト 100% Pass）
   - `./gradlew verifyRoborazziDebug`（ビジュアル回帰ゼロ / ※ベースライン無断更新は絶対禁止）
   - `./gradlew spotlessCheck`（コード整形エラーゼロ）
4. **Reviewer 厳格承認**: `reviewer`（Proモデル）によるコード監査で、アーキテクチャ原則違反・保守性低下・不適切な設計・無価値テスト・一時ファイル混入が 0 件（Unconditional LGTM）であること。
5. **ドキュメント・ROADMAP更新**: READMEのキュレーション（グループ化・折りたたみ）、高品質サンプルコード、およびROADMAPのステータス更新が完了していること。

### (2) サーキットブレーカー (Circuit Breaker)
- **最大反復上限**: 同一タスクにおける自律修正ループ（Dev ➔ QA ➔ Reviewer ➔ Dev）は**最大 3 回まで**とする。
- **エスカレーション**: 3 回ループしてもテストがパスしない、または Reviewer の指摘が解消しない場合は、自律ループを即座に中断し、問題の根本原因・試行内容・トレードオフを整理してユーザーに報告し、判断を仰ぐこと。

---

## 4. 開発オペレーション手順 (Execution Steps)

```text
[Step 1: Plan & Sync] ➔ [Human Approval Gate] ➔ [Step 2: Dev] ➔ [Step 3: QA & Integrate] ➔ [Step 4: Reviewer] ➔ [Step 5: Clean & Commit] ➔ [Step 6: PR]
                                                         ↑                    |                     |
                                                         └──────── Red ───────┴────── Changes ──────┘ (Max 3 Loops)
```

### Step 1. Git最新同期 & プランニング (`implementation_plan.md` 作成)
- **Git同期**: 作業開始前に必ず最新のリモート `main` を pull し、ローカルの `main` を同期した上で作業ブランチを作成する。
- メインエージェントがユーザーと直接対話し、Why/What、DX-FirstなPublic API設計、および価値基準の **Definition of Done (DoD)** を整理した `implementation_plan.md` を作成する。
- **作成後、即座にツール呼び出しを停止してターンを終了し、ユーザーからの承認（Proceed）を必ず待つ。**

### Step 2. TDD実装委託 (`developer`)
- **ユーザーの承認を得てから**、`invoke_subagent` で `developer` (`Workspace: 'inherit'`) を起動する。
- `testing-strategy.md` に基づき、振る舞いを保証するテストコード（`commonTest`、Roborazzi等）を先行作成させ、`commonMain` に実装して単体テストの Green を確認させる。
- `// Arrange` などの無価値なコメントは書かせず、Kotest標準マッチャー（`shouldNotBeNull()` 等）を用いさせる。

### Step 3. 探索的テスト & 回帰スイート統合委託 (`qa-engineer`)
- `invoke_subagent` で `qa-engineer` (`Workspace: 'inherit'`) を起動する。
- ユーザー視点での探索的テスト（複合スタイル、長文、IME操作、Roborazzi視覚的破壊テスト等）をバランスよく実行させる。
- **バグ発見時**: `qa-engineer` が失敗再現テスト（Red Test）を追加し、Orchestrator が `developer` に修正させてテストが Green になるまで自律ループ（Step 2 ➔ Step 3）を回す。
- **回帰スイートへの統合**: **探索的テストで考案した有意義なテストシナリオを、既存の恒久テストスイート（`RichTextEditorTest.kt` 等）へと統合・マージし、使い捨ての一時ファイル（`*ExploratoryTest.kt`等）を綺麗に削除・整理する。**
- `./gradlew allTests`, `./gradlew verifyRoborazziDebug`, `./gradlew spotlessCheck` の全通過を確認する。

### Step 4. 厳格コード監査委託 (`reviewer`)
- `invoke_subagent` で `reviewer` (`Model: 'pro'`) を起動する。
- 客観的・敵対的視点で `architecture-principles.md` (DX, Composeイディオム), `testing-strategy.md` (価値主導テスト、探索的テスト統合), `code-style-guide.md` (パラメータラベル、英語徹底), `documentation-guidelines.md` への準拠および DoD の達成度を査定させる。
- 指摘事項があれば `developer` に差し戻して修正ループを回し、完全な LGTM を獲得する。

### Step 5. ワークスペースクリーンアップ & 自律コミット
- **中間成果物のクリーンアップ**: エージェントの思考用ファイル（`prompt_draft.md`, `PROJECT.md`, `TEST_INFRA.md` 等）やIDE生成物（`.idea/artifacts` 等）がGit管理下に混入していないか確認し、削除または除外する。
- 全DoDを達成した後、Orchestrator が `git-commit-formatter` スキルを活用して、変更差分を意味のある適切な単位（機能追加、テスト拡充、リファクタリング等）に分割して自律的にコミットを作成する。
- Artifact `walkthrough.md` に成果・検証結果・探索的テストの統合内容を記載してユーザーへ報告する。
- **ツールの呼び出しを停止し、ユーザーからのレビュー（フィードバック・修正指示）を待つ。**

### Step 6. ユーザー最終合格後の PR 作成
- **ユーザーの最終レビューに合格（最終承認）した後**、最新の `main` との差分を確認し、`pr-creator` スキルを用いて GitHub 上に**英語**で Pull Request を作成する。
