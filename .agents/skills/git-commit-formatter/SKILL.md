---
name: git-commit-formatter
description: Conventional Commits 仕様に従って git のコミットメッセージを生成します。ユーザーから指示された時のほか、実装作業が一段落し、エージェント自身の判断で変更差分を意味のある単位に分割して自律的にコミットを作成したい時に使用してください。
---

# Git Commit Formatter Skill

Gitのコミットメッセージを作成する際は、必ず以下の **Conventional Commits** 仕様に従ってください。
また、**コミットメッセージは必ず英語で記述すること。**
## コミット粒度と分割のルール (Meaningful Commit Strategy)

- **コミットの分割・単位**: 差分は必ず意味のある適切な単位（Atomic / Meaningful Units）で小分けにしてコミットすること。
- **アンチパターンの禁止**: 異なる目的の作業や複数のファイル変更を一括で1つのコミットにまとめたり、直前のコミットへ安易に `amend` して差分を崩すことを禁止する。

## フォーマット
`<type>[optional scope]: <description>`

## 使用可能な Type
- **feat**: A new feature（新機能）
- **fix**: A bug fix（バグ修正）
- **docs**: Documentation only changes（ドキュメントの変更のみ）
- **style**: Changes that do not affect the meaning of the code（コードの動作に影響しない修正：空白、フォーマット、セミコロンなど）
- **refactor**: A code change that neither fixes a bug nor adds a feature（リファクタリング）
- **perf**: A code change that improves performance（パフォーマンス改善）
- **test**: Adding missing tests or correcting existing tests（テストの追加・修正）
- **chore**: Changes to the build process or auxiliary tools and libraries（ビルドプロセスや補助ツールの変更）

## 指示事項
1. 変更内容を分析し、最適な `type` を選択すること。
2. 必要に応じて、変更範囲を示す `scope` を特定すること（例: 特定のコンポーネント名やファイル名）。
3. **英語で** 簡潔な `description` を書くこと。その際、命令形（例: "add feature" とし、"added" や "adds" は避ける）を用いること。
4. 破壊的変更がある場合は、フッターに `BREAKING CHANGE:` で始まる説明を加えること。

## 出力例
`feat(auth): implement login with google`