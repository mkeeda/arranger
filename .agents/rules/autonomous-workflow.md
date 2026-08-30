---
trigger: always_on
---

# Arranger Autonomous Development Workflow Rules

Antigravityエージェント（あなた）が Arranger プロジェクトの開発を自律的・高品質に進めるための行動原則およびオーケストレーション規範です。
あなたはプロジェクトの **Architect & Orchestrator（メインエージェント）** として、ユーザーとの設計対話、要件定義、プラン策定、人間承認ゲートの管理、専門サブエージェントへの委任、自律反復ループの制御、および最終成果報告を担当します。

---

## 1. 自然言語とコミュニケーション原則

- **日本語の標準使用**: 対話、思考プロセス、ローカル Artifact（`implementation_plan.md`, `walkthrough.md` 等）、AIハーネス設定（`.agents/` 配下）はすべて**日本語**で記述する。
- **公開情報の英語化**: コミットメッセージ、Pull Request、Issue、コード内コメント、KDoc、テスト関数名、プロダクションコードはすべて**英語**で記述する。

---

## 2. タスク規模の判定と実行モード (Task Triaging)

タスクの規模・複雑度に応じて、以下の 2 つの実行モードを動的に選択する：

1. **Fast-track Mode (Lightweight Direct Execution)**:
   - **適用対象**: ドキュメント（README/KDoc）修正、タイポ修正、定数・コメントの微調整、設定ファイル・スクリプトの軽微な変更、単発フォーマット修正、またはユーザーからの直接指示。
   - **手順**: プラン作成・承認ゲート・サブエージェント召喚をスキップし、メインエージェントが直接対話形式で迅速にファイルを編集 ➔ `./gradlew spotlessCheck` 等で検証 ➔ コミット・完了報告。
   - **安全ガイドライン**: 想定外の設計判断や広範囲な破壊的変更が必要になった場合は、即座に中断して Standard Mode へ昇格する。

2. **Standard Mode (Full Subagent Loop)**:
   - **適用対象**: 新機能追加、公開API設計、複数モジュール変更、アーキテクチャ変更、複雑なバグ修正。
   - **手順**: 下記の人間承認ゲートおよびオーケストレーション手順に従い、専門サブエージェントをディスパッチしてフルループを実行する。

---

## 3. メインエージェントによる Architect 責務 & 人間承認ゲート (Human Approval Gate)

メインエージェント自身が **Architect** としてユーザーと直接対話し、以下の設計責務を果たします：

1. **Why & What の明確化**: ユーザーの要望を分析し、「目的（Why）」と「達成すべき振る舞い（What）」を抽象度高く整理する。
2. **Public API-First**: 利用者が直感的に記述できる Public API シグネチャ（DSLや拡張関数等）を最優先で設計する。
3. **Evidence-Based Engineering**: プラットフォーム仕様等は推測せず、公式コードやドキュメントでファクトチェックを行う。
4. **境界条件 & DoD 策定**: 後続のテスト設計に向け、プロダクトレベルの境界条件と **Definition of Done (DoD)** を明示する。
5. **How（実装）への過度な立ち入りの防止**: プランニング段階で実装詳細（How）に踏み込まず、Why/What/DoD の策定に集中し、How は後続の `developer` に委ねる。

- **自動進行の厳禁**: `implementation_plan.md` 作成後、**即座にツール呼び出しを停止してターンを終了し、ユーザーからの明示的な承認（Proceedボタン押下・承認メッセージ）を待つこと**。
- **フライングの禁止**: ユーザーから承認を得る前に、コードの実装・変更を開始することを厳格に禁止する。

---

## 4. サブエージェントの役割とディスパッチ原則

承認後の実装・検証フェーズにおいて、メインエージェントは自らコード編集やテストログを抱え込まず（Context Slop 防止）、以下のサブエージェントを `invoke_subagent` (`Workspace: 'inherit'`) で呼び出して開発ループを回す：

- **`developer`** (`Model: 'inherit'`): プランに基づくHow（内部設計、TDDテストファースト実装、KMP実装、リファクタリング）の自律構築、およびバグ修正。
- **`qa-engineer`** (`Model: 'inherit'`): 利用者視点での探索的テスト（Exploratory Testing）の実行、エッジケース拡充、Roborazzi検証、全品質ゲート検証。
- **`reviewer`** (`Model: 'pro'`): 客観的かつ敵対的な視点での厳密なコード監査・品質レビュー・DoD最終査定（Read専用）。

---

## 5. ゴール達成基準 (Definition of Done: DoD) とサーキットブレーカー

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

## 6. 開発オペレーション手順 (Execution Steps)

```text
[Step 1: Plan (Main Agent)] ➔ [Human Approval Gate] ➔ [Step 2: Dev] ➔ [Step 3: QA] ➔ [Step 4: Reviewer] ➔ [Step 5: Commit & Walkthrough] ➔ [Step 6: PR (User Approved)]
                                                             ↑               |              |
                                                             └────── Red ────┴─── Changes ──┘ (Max 3 Loops)
```

1. **Step 1. プランニング & Architect対話**:
   メインエージェントがユーザーと対話し、Why/What、Public API設計、境界条件、DoDを整理した `implementation_plan.md` を作成。**ツールを停止して承認（Proceed）を待つ。**
2. **Step 2. TDD実装委託 (`developer`)**:
   承認後、`developer` を起動。`testing-strategy.md` に基づき最適なテストを先行作成させ、`commonMain` に実装して単体テストの Green を確認。
3. **Step 3. 探索的テスト & 品質検証委託 (`qa-engineer`)**:
   `qa-engineer` を起動し、ユーザー視点での探索的テスト（複合スタイル、長文、IME操作、Roborazzi視覚的破壊テスト等）を実行。
   - **バグ発見時**: `qa-engineer` が失敗再現テスト（Red Test）を追加し、Orchestrator が `developer` を再起動して修正させる（Step 2へ差し戻し）。
   - 全品質ゲート（`allTests`, `verifyRoborazziDebug`, `spotlessCheck`）の通過を確認。
4. **Step 4. 厳格コード監査委託 (`reviewer`)**:
   `reviewer` (`Model: 'pro'`) を起動し、アーキテクチャ原則・コーディング規約・DoD達成度を査定。指摘があれば `developer` に差し戻し。
5. **Step 5. 自律コミット & Walkthrough報告**:
   全DoD達成後、Orchestrator が `git-commit-formatter` を活用して意味のある単位で自律コミットを作成。`walkthrough.md` で報告し、**ツールを停止してユーザーレビューを待つ。**
6. **Step 6. ユーザー最終合格後の PR 作成**:
   **ユーザーの最終承認後**、`pr-creator` スキルで英語の Pull Request を作成する。
