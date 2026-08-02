---
trigger: glob: "**/*.kt"
---

# Arranger Architecture Principles

Arrangerにおけるアーキテクチャ設計および実装に関する根本原則です。エージェントはコードの提案および実装時に常にこれらの原則を厳守してください。

---

## 1. 状態管理と State Holder パターン（Jetpack Compose API Guidelines 準拠）
- **[人間が求める理想の実装]:** 
  公式の [Compose API Guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md) および [Compose Component API Guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md) に厳密に従う。
  `RichTextState` を単一の真実の源（Single Source of Truth）兼 State Holder とし、`rememberRichTextState()` 等で状態を保持・昇格（State Hoisting）させる。UIコンポーネント（`RichTextEditor`）自体は無状態（Stateless）または State Holder を受け取る構造とし、渡された状態と Modifier に基づいて純粋かつ宣言的に描画を行う。
- **[拒否されるアンチパターン]:** 
  UIコンポーネント内部で描画状態を直接保持・操作したり、状態とイベントの流れが双方向で追跡不能になる設計。Compose API Guidelines に反する命令的なView操作や不適切な引数設計。

---

## 2. コンポジションと拡張性の最大化 (Open-Closed Extensibility)
- **[人間が求める理想の実装]:** 
  モノリシックな巨大クラスを避け、小さな責任を持つコンポーネントやポリモーフィックな属性定義の組み合わせ（Composition over Inheritance）で機能を構築する。利用者がコアコードを修正することなく、カスタム属性やスタイルを自由に追加・カスタマイズできる疎結合設計とする。
- **[拒否されるアンチパターン]:** 
  継承を多用した硬直したクラス設計や、特定の固定されたスタイル（太字、斜体など）しか扱えないハードコードされた属性設計。

---

## 3. Jetpack Compose / Compose Multiplatform パフォーマンス最適化
- **[人間が求める理想の実装]:** 
  不要な再描画（Recomposition）を徹底的に防止する。`@Stable` / `@Immutable` アノテーションを適切に付与し、状態の派生計算には `derivedStateOf` や `remember` を活用して、高頻度なテキスト解析（Spans計算や属性マージ）の再実行を最小限に抑える。
- **[拒否されるアンチパターン]:** 
  Recompositionのたびに重い計算ロジック（例: 全テキストの正規表現検索やスタイル計算）を実行したり、不安定な型（`MutableList` や生キーの `Map` など）をComposeプロパティとして露出すること。

---

## 4. 型安全（Type-Safe）な属性・ドメイン設計
- **[人間が求める理想の実装]:** 
  属性（Attributes）の管理は単なる文字列キーの Map ではなく、`AttributeKey<T>` インターフェースを用いた厳格な型安全性を保証する。各属性キーはデフォルト値を持ち、値の型 `T` がコンパイラによって検証されることで、実行時エラーを防ぐ。
- **[拒否されるアンチパターン]:** 
  `Map<String, Any>` のような型情報が喪失するデータ構造を公開APIとして用いることや、利用側でダウンキャスト（`as T`）を強制し、型チェックの責任を押し付ける設計。

---

## 5. 不変性（Immutability）とアトミックな状態更新
- **[人間が求める理想の実装]:** 
  リッチテキストのドメインモデル（`RichString` 等）は不変（Immutable）な値オブジェクトとし、状態の変更はカプセル化された状態変更メソッド（Atomic Mutations や `editAll`）経由で行う。これにより予測可能で安全なUndo/Redo履歴管理や状態再現を保証する。
- **[拒否されるアンチパターン]:** 
  外部からドメインモデル内部の可変コレクションや文字データを直接書き換えるアプローチ。

---

## 6. KMP（Kotlin Multiplatform）としての責務分離とプラットフォーム遮断
- **[人間が求める理想の実装]:** 
  リッチテキストの「データ構造（モデル）」「状態管理」「属性ロジック」はすべて `commonMain`（プラットフォーム非依存）に配置する。プラットフォーム固有の型（Androidの `SpannableString` / `Typeface` や iOSの `NSAttributedString` / `UIFont` 等）がコアロジックや公開APIに漏洩することを防ぎ、描画レイヤーの最外郭にカプセル化する。
- **[拒否されるアンチパターン]:** 
  ビジネスロジックや状態管理に特定のプラットフォーム概念を安易に持ち込み、コードの共通化や多プラットフォーム展開を阻害すること。

---

## 7. 高頻度処理におけるアロケーションとパフォーマンスの意識
- **[人間が求める理想の実装]:** 
  ユーザーのタイピングやテキスト編集はミリ秒単位で高頻度に発生するため、テキスト差分計算やスタイリング計算において無駄な中間オブジェクト（中間文字列やリストの不必要な生成）のアロケーションを最小限に抑える設計を行う。
- **[拒否されるアンチパターン]:** 
  1文字入力されるたびに全テキストの複製や大規模な文字列切り出しを伴う重いアロケーションを行うこと。

---

## 8. 過度な抽象化の排除（Simplicity & YAGNI）
- **[人間が求める理想の実装]:** 
  適切な抽象化レベルを維持し、将来使うかわからない複雑なフレームワークや過度なレイヤー追加を避ける。シンプルで直感的に理解・メンテナンスできるコード構造を保つ。
- **[拒否されるアンチパターン]:** 
  1回しか使われない無意味なインターフェースの濫用や、目的の不明瞭な中間アダプター層の追加などのオーバーエンジニアリング。

---

## 9. コードドキュメントとコメントの自然言語ルール
- **[標準規範]:** 
  コード内のKDocコメント、インラインコメント、型・関数定義、およびコミット/PRの説明は**すべて英語**で記述する。
