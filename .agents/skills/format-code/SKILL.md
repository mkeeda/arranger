---
name: format-code
description: プロジェクト内のKotlinおよびComposeコードを一定のフォーマットに静的整形する。コミット前に必ず実行する。
---

# Kotlin Code Formatting Skill

このプロジェクトでは **Spotless** と **Ktlint**（および `.editorconfig`）を活用して、Google推奨の Jetpack Compose フォーマットを適用しています。
コードを変更した後や、コードのフォーマットに関するタスクを依頼された際は、以下のルールと手順に従ってフォーマットを実施してください。

### 実行コマンド
コードの静的解析および自動整形を行う場合は、プロジェクトルートで以下のコマンドを実行します。

```bash
./gradlew spotlessApply
```

### このSkillを使うべきタイミング
- Kotlinコードや `build.gradle.kts` スクリプトの新規作成・大幅な変更を行った後
- ユーザーから「コードを綺麗にして」「フォーマットを揃えて」といった依頼を受けた場合
- PR作成前やコミットの直前など、コード規約を遵守する必要がある時

### 動作の前提事項
- `.editorconfig` に `android_studio` スタイルと末尾カンマ (`Trailing commas`) の有効化などのルールが定義されており、これに基づいて自動フォーマットが適用されます。
- `arranger.android.spotless` プラグインが Convention Plugin 内で定義・提供されています。
