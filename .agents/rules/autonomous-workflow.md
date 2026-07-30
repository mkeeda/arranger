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

## 2. 責務の分離原則: Why/What vs How (Separation of Concerns)

各サブエージェントは明確な責務の境界を保持し、後続エージェントの自律的な思考や判断を奪ってはならない。

- **Planner (Why & What に特化)**:
  - 目的（Why）、達成すべき振る舞い・スコープ（What）、およびプロダクトレベルの検証観点を定義する。
  - 具体的な関数の実装詳細や具体的なテストコードの書き方（How）には過度に踏み込まない。
- **Developer & QA Engineer (How を主体的に判断・設計)**:
  - **Developer**: Plannerの Why/What を受け、最適なデータ構造・内部アルゴリズム・具体テストコード（How）を主体的に設計・実装する。
  - **QA Engineer**: Plannerの「検証観点」を受け、境界値・エッジケーステストの拡充、Roborazzi画面検証、a11y/Perfアサーション（How）を自律的に設計・実行する。

---

## 3. コミット粒度と分割のルール (Meaningful Commit Strategy)

- **コミットの分割・単位**: 差分は必ず意味のある適切な単位（Atomic / Meaningful Units）で小分けにしてコミットすること。
- **アンチパターンの禁止**: 異なる目的の作業や複数のファイル変更を一括で1つのコミットにまとめたり、直前のコミットへ安易に `amend` して差分を崩すことを禁止する。
- **コミット形式**: コミットメッセージは Conventional Commits 仕様に準拠し、英語で記述すること（例: `feat(editor): ...`, `test(richtext): ...`）。

---

## 4. 独立サブエージェントによる役割分離の徹底 (Mandatory Subagent Delegation)

役割ごとのコンテキスト分離と客観的な品質維持のため、親エージェント（Orchestrator）自身が単体でコード実装やレビューを一気通貫で行ってはならない。
各フェーズにおいて必ず `invoke_subagent` を用いて独立したサブエージェントをディスパッチし、作業を分担すること。

- **プランニング**: 独立した `planner` サブエージェントを `invoke_subagent` でディスパッチする。
- **実装・TDD**: 独立した `developer` サブエージェントを `invoke_subagent` でディスパッチする。
- **品質検証**: 独立した `qa-engineer` サブエージェントを `invoke_subagent` でディスパッチする。
- **コード査定**: 独立した `reviewer` サブエージェントを `invoke_subagent` でディスパッチする。

---

## 5. サブエージェントの動的モデル選定 (Model Selection Principle)

サブエージェントを `invoke_subagent` で起動する際は、タスクの難易度に応じて `Model` パラメータを切り替えること：

| タスクの性質 | サブエージェント | 指定する `Model` | 目的・理由 |
| :--- | :--- | :--- | :--- |
| **通常の機能追加・バグ修正・TDD実装** | `developer`, `qa-engineer`, `reviewer` | **`'inherit'`** | メインモデル（Gemini 3.6 Flash High等）を継承しスピード重視で実行 |
| **複雑な設計・仕様策定・壁打ち** | `planner` | **`'pro'`** | Gemini 3.1 Pro High等の高度推論モデルを活用し、漏れのない設計立案 |
| **難易度の高い深層バグ解析・大規模構造刷新** | `developer` / `planner` | **`'pro'`** | 高度な多段階推論とコード解析能力をフル活用 |
| **ユーザーからの明確なモデル指定時** | 該当サブエージェント | ユーザー指定モデル | 例: 「Proでじっくり考えて」→ `'pro'`、「Flashで素早く」→ `'inherit'` |

---

## 6. 必須品質ゲート (Quality Gates)

すべての変更は以下の品質チェックを通過しなければならない：

1. **単体テスト**: `./gradlew test` がエラーなく全件パスすること。
2. **ビジュアル検証**: Roborazzi によるスナップショットテスト (`./gradlew verifyRoborazziDebug`) がパスすること。
3. **コード整形**: `./gradlew spotlessCheck` がエラーなしでパスすること。

---

## 7. Orchestrator のオペレーション手順 (Execution Steps)

```
[Orchestrator]
   ├── 1. Planner      : 壁打ち・Why/What定義・検証観点提示 (Model: 'pro' or 'inherit') ──> implementation_plan.md
   ├── 2. Developer    : Howの設計・TDD実装 ──> commonTest作成 ──> commonMain実装 ──> 分割コミット (Model: 'inherit' or 'pro')
   ├── 3. QA Engineer  : Howのテスト拡充・./gradlew test + Roborazzi + a11y/Perf検証 (Model: 'inherit')
   ├── 4. Reviewer     : コードスタイル・設計原則の査定評価 (Model: 'inherit')
   ├── 5. Orchestrator : pr-creator スキルによる Pull Request 作成（英語）
   └── 6. Orchestrator : Walkthrough (MD/HTML) 提示 & gh issue create による英語タスク起票
```

### Step 1. プランニングの委任 (`invoke_subagent`)
- `invoke_subagent` で Subagent `planner` を起動（複雑な設計時は `Model: 'pro'` を指定）。Why/Whatと検証観点を整理した `implementation_plan.md` を作成。
- 作成された計画をユーザーに提示し、承認（Proceed）を得る。

### Step 2. TDD実装の委任 (`invoke_subagent`)
- 承認後、`invoke_subagent` で Subagent `developer` を起動（通常は `Model: 'inherit'`、複雑リファクタリング時は `Model: 'pro'`）。
- Plannerの観点を受けてHowを主体的に設計し、`commonTest` の追加と `commonMain` への実装を実行。
- **変更差分は意味のある単位で適切にコミット分割**すること。

### Step 3. 品質保証の委任 (`invoke_subagent`)
- `invoke_subagent` で Subagent `qa-engineer` を起動（`Model: 'inherit'`）。テストシナリオの拡充・エッジケース追加を行い、`./gradlew test` / Roborazzi / `spotlessCheck` を実行させて結果を検証。

### Step 4. コード査定の委任 (`invoke_subagent`)
- `invoke_subagent` で Subagent `reviewer` を起動（`Model: 'inherit'`）。
- `architecture-principles.md` および `code-style-guide.md` に対する査定レポートを作成させる。

### Step 5. Pull Request の起票
- `pr-creator` スキルを呼び出し、コミット内容とテスト結果をまとめた英語の Pull Request を作成。

### Step 6. 報告と Issue 起票
- Artifact `walkthrough.md` (必要に応じてHTMLデモ) に成果・動作ログ・PR URL を記載してユーザーに報告。
- レビュー結果や残課題を取りまとめ、`gh issue create` コマンドで GitHub 上に英語で Issue を登録。
