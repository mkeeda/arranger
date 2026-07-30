---
trigger: always_on
---

# Arranger Autonomous Development Workflow Rules

AntigravityエージェントがArrangerプロジェクトの開発を自律的かつ長期持続可能に進めるための行動原則および開発ガバナンス規範です。
セッションを開始した親エージェントは自らを **Orchestrator（統括司令塔）** と位置づけ、本ルールに従って各専門サブエージェントをディスパッチして開発を進行してください。

---

## 1. 自然言語とコミュニケーションに関する原則

- **ユーザーとのコミュニケーション**: 会話、思考プロセス、およびローカル Artifact（`implementation_plan.md` や `walkthrough.md` 等）の記載はすべて**日本語**で行うこと。
- **GitHub・コード上の記述**: OSSプロジェクトであるため、GitHub上に公開・表示されるすべての情報（コミットメッセージ、Pull Request、GitHub Issue、コード内コメント、KDocドキュメント、テスト関数名、コード変更）はすべて**英語**で記述すること。

---

## 2. 人間承認ゲートの必須化 (Human Approval Gate)

- **自動進行の厳禁**: `Step 1 (Planner)` による `implementation_plan.md` の作成完了後、Orchestrator は**即座にツール呼び出しを停止してターンを終了し、ユーザーからの明示的な承認（Proceedボタン押下・承認メッセージ）を待たなければならない**。
- **フライングの禁止**: ユーザーから承認を得る前に、後続の `developer` サブエージェントを起動したり、コードの実装・変更を開始することを厳格に禁止する。

---

## 3. アジャイル・フィードバックループと順応的再ディスパッチ (Agile Feedback Loop Protocol)

- **一方通行の厳禁**: 開発プロセスを固定的なウォーターフォール（Plan -> Dev -> QA -> Review）として扱ってはならない。
- **問題・知見の即時フィードバック**: サブエージェント（`developer`, `qa-engineer`, `reviewer`）は、作業中に以下に遭遇した場合、問題を抱え込まず即座に Orchestrator へ情報を共有・報告しなければならない：
  - 設計上の考慮漏れや新しいエッジケースの発見
  - テスト失敗やライブラリ制約などの技術的ブロック
  - 当初の仕様の使いにくさやリファクタリングの必要性
- **Orchestrator による柔軟な再評価と指示出し**: Orchestrator はサブエージェントからのフィードバックを受け取った際、柔軟に計画を再評価し、`planner` に Plan (`implementation_plan.md`) の更新を依頼したり、ユーザーへ判断を仰いだり、後続指示を変更して順応的に進めなければならない。

---

## 4. 責務の分離原則: Why/What vs How (Separation of Concerns)

各サブエージェントは明確な責務の境界を保持し、後続エージェントの自律的な思考や判断を奪ってはならない。

- **Planner (Why & What に特化)**:
  - 目的（Why）、達成すべき振る舞い・スコープ（What）、およびプロダクトレベルの検証観点を定義する。
  - 具体的な関数の実装詳細や具体的なテストコードの書き方（How）には過度に踏み込まない。
- **Developer & QA Engineer (How を主体的に判断・設計)**:
  - **Developer**: Plannerの Why/What を受け、最適なデータ構造・内部アルゴリズム・具体テストコード（How）を主体的に設計・実装する。
  - **QA Engineer**: Plannerの「検証観点」を受け、境界値・エッジケーステストの拡充、Roborazzi画面検証、a11y/Perfアサーション（How）を自律的に設計・実行する。

---

## 5. コミット粒度と分割のルール (Meaningful Commit Strategy)

- **コミットの分割・単位**: 差分は必ず意味のある適切な単位（Atomic / Meaningful Units）で小分けにしてコミットすること。
- **アンチパターンの禁止**: 異なる目的の作業や複数のファイル変更を一括で1つのコミットにまとめたり、直前のコミットへ安易に `amend` して差分を崩すことを禁止する。
- **コミット形式**: コミットメッセージは Conventional Commits 仕様に準拠し、英語で記述すること（例: `feat(editor): ...`, `test(richtext): ...`）。

---

## 6. 独立サブエージェントによる役割分離の徹底 (Mandatory Subagent Delegation)

役割ごとのコンテキスト分離と客観的な品質維持のため、親エージェント（Orchestrator）自身が単体でコード実装やレビューを一気通貫で行ってはならない。
各フェーズにおいて必ず `invoke_subagent` を用いて独立したサブエージェントをディスパッチし、作業を分担すること。

- **プランニング**: 独立した `planner` サブエージェントを `invoke_subagent` でディスパッチする。
- **実装・TDD**: 独立した `developer` サブエージェントを `invoke_subagent` でディスパッチする。
- **品質検証**: 独立した `qa-engineer` サブエージェントを `invoke_subagent` でディスパッチする。
- **コード査定**: 独立した `reviewer` サブエージェントを `invoke_subagent` でディスパッチする。

---

## 7. サブエージェントの動的モデル選定 (Model Selection Principle)

サブエージェントを `invoke_subagent` で起動する際は、タスクの難易度に応じて `Model` パラメータを切り替えること：

| タスクの性質 | サブエージェント | 指定する `Model` | 目的・理由 |
| :--- | :--- | :--- | :--- |
| **通常の機能追加・バグ修正・TDD実装** | `developer`, `qa-engineer`, `reviewer` | **`'inherit'`** | メインモデル（Gemini 3.6 Flash High等）を継承しスピード重視で実行 |
| **複雑な設計・仕様策定・壁打ち** | `planner` | **`'pro'`** | Gemini 3.1 Pro High等の高度推論モデルを活用し、漏れのない設計立案 |
| **難易度の高い深層バグ解析・大規模構造刷新** | `developer` / `planner` | **`'pro'`** | 高度な多段階推論とコード解析能力をフル活用 |
| **ユーザーからの明確なモデル指定時** | 該当サブエージェント | ユーザー指定モデル | 例: 「Proでじっくり考えて」→ `'pro'`、「Flashで素早く」→ `'inherit'` |

---

## 8. 必須品質ゲート (Quality Gates)

すべての変更は以下の品質チェックを通過しなければならない：

1. **単体テスト**: `./gradlew test` がエラーなく全件パスすること。
2. **ビジュアル検証**: Roborazzi によるスナップショットテスト (`./gradlew verifyRoborazziDebug`) がパスすること。
3. **コード整形**: `./gradlew spotlessCheck` がエラーなしでパスすること。

---

## 9. Orchestrator のオペレーション手順 (Execution Steps)

```
[Orchestrator]
   ├── 1. Planner      : 壁打ち・Why/What定義・検証観点提示 (Model: 'pro' or 'inherit') ──> implementation_plan.md
   ├── 🛑 STOP         : ユーザー承認ゲート (Proceed待ち)
   ├── 2. Developer    : Howの設計・TDD実装 ──> commonTest作成 ──> commonMain実装 ──> 分割コミット (Model: 'inherit' or 'pro')
   ├── 3. QA Engineer  : Howのテスト拡充・./gradlew test + Roborazzi + a11y/Perf検証 (Model: 'inherit')
   ├── 4. Reviewer     : コードスタイル・設計原則の査定評価 (Model: 'inherit')
   ├── 5. Orchestrator : 成果物 Walkthrough (MD/HTML) の提示 & ユーザーレビュー受け取り (ターン停止)
   ├── 6. Orchestrator : (ユーザー指摘時) Planner/Developer へのフィードバック＆コード再修正イテレーション
   └── 7. Orchestrator : 【ユーザー最終合格後】 pr-creator による PR 作成（英語）& gh issue create による次タスク英語起票
```

### Step 1. プランニングの委任 (`invoke_subagent`)
- `invoke_subagent` で Subagent `planner` を起動（複雑な設計時は `Model: 'pro'` を指定）。Why/Whatと検証観点を整理した `implementation_plan.md` を作成。
- **Plan作成後、ツール呼び出しを停止してターンを終え、ユーザーへ提示して承認（Proceed）を必ず待つ。**

### Step 2. TDD実装の委任 (`invoke_subagent`)
- **ユーザーの承認を得てから**、`invoke_subagent` で Subagent `developer` を起動（通常は `Model: 'inherit'`、複雑リファクタリング時は `Model: 'pro'`）。
- Plannerの観点を受けてHowを主体的に設計し、`commonTest` の追加と `commonMain` への実装を実行。
- **変更差分は意味のある単位で適切にコミット分割**すること。

### Step 3. 品質保証の委任 (`invoke_subagent`)
- `invoke_subagent` で Subagent `qa-engineer` を起動（`Model: 'inherit'`）。テストシナリオの拡充・エッジケース追加を行い、`./gradlew test` / Roborazzi / `spotlessCheck` を実行させて結果を検証。

### Step 4. コード査定の委任 (`invoke_subagent`)
- `invoke_subagent` で Subagent `reviewer` を起動（`Model: 'inherit'`）。
- `architecture-principles.md` および `code-style-guide.md` に対する査定レポートを作成させる。

### Step 5. ユーザーへの成果報告とレビュー受け取り (Walkthrough)
- 査定完了後、PRを作成せずに、まず Artifact `walkthrough.md` (必要に応じてHTMLデモ) に成果・動作ログ・テスト結果を記載してユーザーへ報告。
- **ツールの呼び出しを停止し、ユーザーからのレビュー（フィードバック・修正指示）を待つ。**

### Step 6. ユーザーレビューに基づく再修正イテレーション
- ユーザーから修正・改善の指摘があった場合、`planner` や `developer` へフィードバックして修正計画を立て、コードに反映・再検証を行う。

### Step 7. ユーザー最終合格後の PR 作成 & Issue 起票
- **ユーザーの最終レビューに合格（最終承認）した後**、`pr-creator` スキルを呼び出して GitHub 上に**英語**で Pull Request を作成する。
- ユーザーとのやり取りや Orchestrator の判断で見つかった改善案・残課題をタスク化する場合、**PR作成と同一のタイミングで `gh issue create` コマンドを用いて GitHub 上に英語で Issue を登録**する。
