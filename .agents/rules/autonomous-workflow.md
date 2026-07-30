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

## 2. 開発思想と行動規範

### 1. テストファースト (TDD)
- 実装コードを書く前に、必ず意図する振る舞いを表現したテストコード（`commonTest` または Compose UI テスト）を作成すること。

### 2. 相互フィードバック駆動イテレーション
- ウォーターフォール型の単方向実行は厳禁。実装やテスト作成の過程で「設計の不備」「未知のエッジケース」「APIの使いにくさ」等の新しい知見が得られた場合は、**即座に上位の設計（Plan）やQA戦略にフィードバックし、イテレーションと計画を柔軟に更新**すること。

### 3. 長期的な保守性と根本解決
- 単なる対向的な対症療法（Try-Catchでの例外隠蔽やダミー値返却）を禁止し、根本原因を解決する長期的に保守可能な設計を行うこと。

---

## 3. 必須品質ゲート (Quality Gates)

すべての変更は以下の品質チェックを通過しなければならない：

1. **単体テスト**: `./gradlew test` がエラーなく全件パスすること。
2. **ビジュアル検証**: Roborazzi によるスナップショットテスト (`./gradlew verifyRoborazziDebug`) がパスすること。
3. **コード整形**: `./gradlew spotlessCheck` がエラーなしでパスすること。

---

## 4. マルチエージェント体制の概念図

全体統括を **Orchestrator** が担当し、専門タスクを `.agents/subagents/` に定義されたサブエージェント（`planner`, `developer`, `qa-engineer`, `reviewer`）へディスパッチして進めます。

```
[Orchestrator]
   ├── 1. Planner      : 壁打ち・ゴール明確化 ──> implementation_plan.md
   ├── 2. Developer    : TDD ──> commonTest作成 ──> commonMain実装
   ├── 3. QA Engineer  : ./gradlew test + Roborazzi + a11y/Perf検証
   ├── 4. Reviewer     : コードスタイル・設計原則の査定評価
   ├── 5. Orchestrator : pr-creator スキルによる Pull Request 作成（英語）
   └── 6. Orchestrator : Walkthrough (MD/HTML) 提示 & gh issue create による英語タスク起票
```
