---
trigger: glob: "**/*.kt"
---

# コードスタイルガイド

## 1. ソースコードおよび成果物内の自然言語ポリシー (Strict English Policy)

OSSとして公開するプロダクションコード、テストコード（`.kt`）、KDocドキュメント、インラインコメント、テスト関数名、コミットメッセージ、Pull Request、GitHub Issueへの返信、および公開Markdown（README等）は**すべて英語**で記述する。
日本語の混入は厳禁とする。

一方、ユーザーとの直接対話、ローカルArtifact（`implementation_plan.md`, `walkthrough.md` 等）、および AIハーネス内部設定（`.agents/` 内のスキル定義等）はチーム運用の円滑化のため**日本語**を使用する。

---

## 2. 引数渡しの表現とパラメータラベル (Parameter Labels)

可読性を担保するため、以下の場合には必ずパラメータラベルを明記して引数を渡すこと：

1. **クラスのコンストラクタ呼び出し**:
   ```kotlin
   RichString(text = "sample text")
   ```
2. **2つ以上の引数を持つ関数や、同一型の引数が並ぶ関数**:
   ```kotlin
   container.add(text = "heading", index = 0)
   ```
3. **Booleanフラグを渡す場合**:
   ```kotlin
   Modifier.spanTapHandler(enabled = true)
   ```
4. **サンプルコード（Practical Examples等）**:
   利用者がコード例を読んで引数の役割を即座に把握できるよう、主要な引数には明示的にラベルを付与する。

**[例外: ラベルを省略できる場合]:**
引数の意味が自明であり、Compose標準で1引数のみを受け取る一般的なComposable（例: `Text("Hello")`）など、省略しても誤読の余地がない場合のみ省略を許容する。

---

## 3. Compose API Guidelines の遵守

Composable 関数や修飾子（Modifier）の命名、引数順序（`modifier: Modifier = Modifier` の位置等）は公式の Compose API Guidelines に厳格に従うこと。
- https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md

---

## 4. テストケース名の表現

- テスト関数名は英語で記載する。
- バッククォート（`` ` ``）を使い、スペースを含めた完全な英文で振る舞いを記述する。
  ```kotlin
  @Test
  fun `typing on span in disabled editor does not trigger onSpanClick`() {
      // ...
  }
  ```