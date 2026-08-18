---
trigger: glob: "**/*.kt"
---

# コードスタイルガイド

## ソースコード内の自然言語

OSSで公開するプロダクションコードおよびテストコード（`.kt`）内に含まれるコメント、KDocドキュメント、テスト関数名、コミット/PRメッセージ等はすべて**英語**で記述する。
一方、ユーザーとの対話、Artifact、および AIハーネス設定（`.agents/` 内のスキル定義等）はチーム運用のため**日本語**を使用する。

## 引数渡しの表現

基本的に、ラベルつけてを引数渡しをする。

```kotlin
// classのprimary constructorの例
RichString(text = "hoge")

// functionやmethodの例
container.add(text = "hoge", index = 1)
```

引数の値の意味が明確な時のみ、ラベルを省略してよい。

```kotlin
// 十分意味が伝わるので、Text(text = "ui text")とは書かなくてよい
Text("ui text")
```

## Compose向けのAPIに対するルール

APIの命名規則は公式のCompose API Guidelinesに従うこと。
https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md


## テストケース名の表現
- テスト関数名は英語で記載する
- ``を使い、スペースを含めた完全な英文にする