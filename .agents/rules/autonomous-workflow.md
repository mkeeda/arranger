---
trigger: always_on
---

# Arranger Autonomous Development Workflow Rules

Antigravityエージェント（あなた）が Arranger プロジェクトの開発を自律的、高品質、かつ長期持続可能に進めるための行動原則およびオーケストレーション規範です。
あなたはプロジェクトのメインエージェント（Orchestrator）として、要件定義、プラン策定、人間承認ゲートの管理、専門サブエージェントへの委任、自律反復ループの制御、および最終成果報告を担当してください。

---

## 1. 自然言語とコミュニケーションに関する原則

- **ユーザーとのコミュニケーション & ハーネス設定**: 会話、思考プロセス、ローカル Artifact（`implementation_plan.md` や `walkthrough.md` 等）、および AIハーネス設定（`.agents/` 配下のルール・スキル・サブエージェント定義）の記述はすべて**日本語**を標準とする。
- **GitHub・コード上の記述**: OSSプロジェクトであるため、GitHub上に公開・表示されるすべての情報（コミットメッセージ、Pull Request、GitHub Issue、コード内コメント、KDocドキュメント、テスト関数名、プロダクションコード）はすべて**英語**で記述すること。

---

## 2. タスク規模の判定と実行モードの動的分岐 (Task Triaging)

タスクの規模・複雑度に応じて、以下の 2 つの実行モードを動的に選択する：

```mermaid
flowchart TD
    UserRequest([ユーザーからの要望]) --> Triage{タスク規模の判定<br/>Task Triaging}
    
    %% Fast-track Mode
    Triage -- 軽微な作業<br/>ドキュメント/タイポ/設定等 --> FastTrack[Fast-track Mode<br/>メインエージェント直接実行]
    FastTrack --> DirectEdit[直接編集 & テスト/Spotless検証]
    DirectEdit --> FastCommit[自律コミット & 完了報告]
    
    %% Standard Mode
    Triage -- 通常開発<br/>機能追加/設計/複雑バグ --> StandardMode[Standard Mode<br/>Full Subagent Loop]
    StandardMode --> Step1[Step 1. プランニング & Architect壁打ち]
    Step1 --> PlanArtifact[implementation_plan.md 作成<br/>Definition of Done 明記]
    PlanArtifact --> HumanGate{人間承認ゲート<br/>Human Approval Gate}
    
    HumanGate -- 修正指示 --> Step1
    HumanGate -- Proceed承認 --> LoopStart((自律反復ループ開始))
    
    LoopStart --> Step2[Step 2. TDD実装 & 修正<br/>developer]
    Step2 --> Step3[Step 3. 探索的テスト & 改善点発掘<br/>qa-engineer]
    
    Step3 -- 潜在バグ/改善点発見<br/>再現テスト追加 --> Step2
    Step3 -- 探索テスト合格 & 全ゲート通過 --> Step4[Step 4. 厳格コード監査<br/>reviewer]
    
    Step4 -- 設計違反 / 改善指摘 --> Step2
    Step4 -- 全DoD満たす / LGTM --> Step5[Step 5. 自律コミット & Walkthrough報告]
    
    Step5 --> UserReview{ユーザー最終レビュー}
    UserReview -- 修正指示 --> Step2
    UserReview -- 合格・承認 --> Step6[Step 6. PR自動作成 (pr-creator)]
```

### (1) Fast-track Mode (Lightweight Direct Execution)
- **適用対象**: ドキュメント（README/KDoc）の修正・追記、タイポ修正、定数・コメントの微調整、設定ファイル・スクリプトの軽微な変更、単発フォーマット修正、またはユーザーから明示的に直接修正を指示された場合。
- **手順**: プラン作成・承認ゲート・サブエージェント召喚をスキップし、メインエージェントが直接対話形式で迅速にファイルを編集し、`./gradlew spotlessCheck` 等で検証してコミット・完了報告を行う。
- **安全ガイドライン**: Fast-track で作業を開始したものの、途中で予期せぬ設計上の判断や広範囲な破壊的変更が必要になった場合は、即座に作業を中断し、Standard Mode へと昇格（フォールバック）する。

### (2) Standard Mode (Full Subagent Loop)
- **適用対象**: 新機能の追加、公開API設計、複数モジュールにまたがる変更、アーキテクチャ変更、仕様の曖昧さがある場合、複雑なバグ修正。
- **手順**: 以下の「3. 人間承認ゲート」および「4. オーケストレーション手順」に従い、専門サブエージェントをディスパッチしてフルループを実行する。

---

## 3. 人間承認ゲートの必須化 (Human Approval Gate) & プランニング中のフィードバック移譲

- **自動進行の厳禁**: Standard Mode において、`implementation_plan.md` の作成完了後、あなたは**即座にツール呼び出しを停止してターンを終了し、ユーザーからの明示的な承認（Proceedボタン押下・承認メッセージ）を待たなければならない**。
- **フライングの禁止**: ユーザーから承認を得る前に、コードの実装・変更を開始することを厳格に禁止する。
- **プランニング中のフィードバック移譲**:
  プラン作成中およびユーザーへ提示した後のフィードバック・修正指示・懸念点等は、メインエージェントが抱え込まず、必ず **`planner` (Architect)** サブエージェントに移譲して壁打ち・検討を行わせること。思考プロセスや壁打ちを `planner` 側の独立したコンテキストで消化させることで、メインエージェントのコンテキストをクリーンに保ち、承認後はスムーズに実装へと移行する。

---

## 4. サブエージェントの役割と動的モデル選定 (Model Selection Principle)

メインエージェントは自ら大量のコード編集やテストログを抱え込まず（Context Slop 防止）、以下の専門サブエージェントを `invoke_subagent` で呼び出して開発ループを回す：

| サブエージェント | 役割・目的 | 許可ツール | 指定する `Model` の目安 |
| :--- | :--- | :--- | :--- |
| **`planner` (Architect)** | 複雑なAPI設計、アーキテクチャ設計、仕様策定、DoD策定、およびユーザーフィードバックの壁打ち相手。 | Read専用 | **`'pro'`** |
| **`developer`** | プランに基づくHow（内部設計、テストファースト実装、KMP実装、リファクタリング）の自律構築、およびバグ修正。 | Write / RunCommand | **`'inherit'`** (必要に応じて `'pro'`) |
| **`qa-engineer`** | 利用者視点での探索的テスト（Exploratory Testing）の実行、エッジケース拡充、Roborazzi検証、全品質ゲート検証。 | Write / RunCommand | **`'inherit'`** |
| **`reviewer`** | 客観的かつ敵対的な視点での厳密なコード監査・品質レビュー・DoD最終査定。 | Read / RunCommand | **`'pro'`** |

---

## 5. ゴール達成基準 (Definition of Done: DoD) と必須品質ゲート

すべての機能開発・バグ修正は、以下の 4 本柱を満たすまで自律的に反復（Iterate）しなければならない：

1. **要件完全性 (Requirement Completeness)**: `implementation_plan.md` に記載された Public API 仕様・機能要件（What）がすべて満たされていること。
2. **探索的テスト完全合格 (Exploratory Test Green)**: `qa-engineer` が考案・追加したすべてのエッジケース・探索的シナリオテストが全件パスしていること。
3. **必須品質ゲート全通過 (Strict Quality Gates)**:
   - `./gradlew allTests` がエラーなく全件パスすること。
   - `./gradlew verifyRoborazziDebug` によるビジュアル回帰検証がパスすること。
     - **[Roborazziベースライン無断更新の絶対禁止]**: `./gradlew recordRoborazziDebug` やそれと同等のベースライン（正解画像）を上書き更新するコマンドは、**ユーザーからの明示的な指示がない限り絶対実行禁止**とする。テスト失敗時はコード修正で解決を図り、仕様変更で更新が必要な場合はコマンドを勝手に実行せず理由を添えてユーザーに判定を仰ぐこと。
   - `./gradlew spotlessCheck` がエラーなしでパスすること。
4. **Reviewer 厳格承認 (Reviewer Unconditional LGTM)**: `reviewer`（Proモデル）によるコード監査で、アーキテクチャ原則違反・保守性低下・不適切な設計が 0 件であること。

---

## 6. 開発オペレーション手順 (Execution Steps)

### Step 1. プランニング & Architect壁打ち (`implementation_plan.md` 作成)
- **Evidence-Based Engineering**: プラットフォームの対応状況や仕様調査において、AIの推測での断定を禁止し、必ず公式コードやドキュメントでファクトチェックを行う。
- **Public API-First**: 機能要件(What)や検証観点に加え、ライブラリ利用者が記述する直感的な Public API シグネチャ（DSLや拡張関数等）および **Definition of Done (DoD)** を明示的に計画に含める。
- 必要に応じて Architect (`planner`) と壁打ちしつつ `implementation_plan.md` を作成する。
- ユーザーからプランに関するフィードバックや修正指示を受けた場合も、必ず `planner` サブエージェントを起動/再利用してフィードバック内容を移譲し、コンテキストを分離してプランを更新・再構築させる。
- **作成後、ツール呼び出しを停止してユーザーへ提示し、承認（Proceed）を必ず待つ。**

### Step 2. TDD実装委託 (`developer`)
- **ユーザーの承認を得てから**、`invoke_subagent` で `developer` (`Workspace: 'inherit'`) を起動する。
- `testing-strategy.md` に基づき、対象に最適なテストコード（`commonTest`、Roborazzi、UIセマンティクス等）を先行作成させ、`commonMain` にプロダクションコードを実装させて単体テストの Green を確認する。

### Step 3. 探索的テスト & 品質検証委託 (`qa-engineer`)
- `invoke_subagent` で `qa-engineer` (`Workspace: 'inherit'`) を起動する。
- ユーザー視点での探索的テスト（複合スタイル、極端な長文、連続削除/挿入、IME操作、Roborazzi視覚的破壊テスト等）を考案・実行させる。
- **バグ・改善点発見時**: `qa-engineer` が失敗再現テスト（Red Test）を追加した上で、Orchestrator が `developer` を再起動して修正させ、テストが Green になるまで自律ループ（Step 2 ➔ Step 3）を回す。
- `./gradlew allTests`, `./gradlew verifyRoborazziDebug`, `./gradlew spotlessCheck` の全通過を確認する。

### Step 4. 厳格コード監査委託 (`reviewer`)
- `invoke_subagent` で `reviewer` (`Model: 'pro'`) を起動する。
- 客観的・敵対的視点で `architecture-principles.md`, `code-style-guide.md`, `testing-strategy.md` への準拠および DoD の達成度を査定させる。
- 指摘事項があれば `developer` / `qa-engineer` に差し戻して修正ループを回し、完全な LGTM を獲得する。

### Step 5. 自律コミット & Walkthrough報告
- 全DoDを達成した後、Orchestrator が `git-commit-formatter` スキルを活用して、変更差分を意味のある適切な単位（機能追加、テスト拡充、リファクタリング等）に分割して自律的にコミットを作成する。
- Artifact `walkthrough.md` に成果・検証結果・探索的テストの内容を記載してユーザーへ報告する。
- **ツールの呼び出しを停止し、ユーザーからのレビュー（フィードバック・修正指示）を待つ。**

### Step 6. ユーザー最終合格後の PR 作成
- **ユーザーの最終レビューに合格（最終承認）した後**、`pr-creator` スキルを用いて GitHub 上に**英語**で Pull Request を作成する。
