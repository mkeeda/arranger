---
trigger: always_on
---

# Arranger Autonomous Development Workflow Rules

AntigravityエージェントがArrangerプロジェクトの開発を自律的かつ長期持続可能に進めるための行動原則および開発ガバナンス規範です。

---

## 1. 自然言語とコミュニケーションに関する原則

- **ユーザーとのコミュニケーション**: 会話、思考プロセス、およびローカル Artifact（`implementation_plan.md` や `walkthrough.md` 等）の記載はすべて**日本語**で行うこと。
- **GitHub・コード上の記述**: OSSプロジェクトであるため、GitHub上に公開・表示されるすべての情報（コミットメッセージ、Pull Request、GitHub Issue、コード内コメント、KDocドキュメント、テスト関数名、コード変更）はすべて**英語**で記述すること。

---

## 2. 独立サブエージェントによる役割分離の徹底 (Mandatory Subagent Delegation)

役割ごとのコンテキスト分離と客観的な品質維持のため、各フェーズにおいて必ず `invoke_subagent` を用いて独立したサブエージェントをディスパッチし、作業を分担すること。

- **プランニング**: 独立した `planner` サブエージェントをディスパッチする。
- **実装・TDD**: 独立した `developer` サブエージェントをディスパッチする。
- **品質検証**: 独立した `qa-engineer` サブエージェントをディスパッチする。
- **コード査定**: 独立した `reviewer` サブエージェントをディスパッチする。

---

## 3. 開発思想と行動規範

### 1. テストファースト (TDD)
- 実装コードを書く前に、必ず意図する振る舞いを表現したテストコード（`commonTest` または Compose UI テスト）を作成すること。

### 2. 相互フィードバック駆動イテレーション
- ウォーターフォール型の単方向実行は厳禁。実装やテスト作成の過程で「設計の不備」「未知のエッジケース」「APIの使いにくさ」等の新しい知見が得られた場合は、**即座に上位の設計（Plan）やQA戦略にフィードバックし、イテレーションと計画を柔軟に更新**すること。

### 3. 動的モデル選定 (Model Selection Principle)
- サブエージェントディスパッチ時はスピードと推論深度のバランスを動的に最適化する。
  - 通常の実装・QA・レビュー: 親モデル（Gemini 3.6 Flash High等, `'inherit'`）でスピード重視で実行。
  - 複雑なアーキテクチャ設計・難易度の高いバグ解析・大型リファクタリング: 推論特化モデル（`'pro'`）を指定してディスパッチする。

---

## 4. 必須品質ゲート (Quality Gates)

すべての変更は以下の品質チェックを通過しなければならない：

1. **単体テスト**: `./gradlew test` がエラーなく全件パスすること。
2. **ビジュアル検証**: Roborazzi によるスナップショットテスト (`./gradlew verifyRoborazziDebug`) がパスすること。
3. **コード整形**: `./gradlew spotlessCheck` がエラーなしでパスすること。

---

## 5. マルチエージェント体制の概念図

全体統括を **Orchestrator** が担当し、専門タスクを `.agents/subagents/` に定義されたサブエージェント（`planner`, `developer`, `qa-engineer`, `reviewer`）へディスパッチして進めます。

```
[Orchestrator]
   ├── 1. Planner      : 壁打ち・ゴール明確化 (Model: 'pro' or 'inherit') ──> implementation_plan.md
   ├── 2. Developer    : TDD ──> commonTest作成 ──> commonMain実装 (Model: 'inherit' or 'pro')
   ├── 3. QA Engineer  : ./gradlew test + Roborazzi + a11y/Perf検証 (Model: 'inherit')
   ├── 4. Reviewer     : コードスタイル・設計原則の査定評価 (Model: 'inherit')
   ├── 5. Orchestrator : pr-creator スキルによる Pull Request 作成（英語）
   └── 6. Orchestrator : Walkthrough (MD/HTML) 提示 & gh issue create による英語タスク起票
```
