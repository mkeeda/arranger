---
trigger: always_on
---

# コードスタイルガイド

## ファイルに使用する自然言語

OSSで公開するコードのため、ファイルに含める自然言語は全て英語で記載する。
私との会話は日本語で行うが、生成するコードに含めるコメント、コードドキュメント等は英語で記述すること。

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