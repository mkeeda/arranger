---
name: pr-creator
description: GitHubにPull Requestを作成する際のワークフローとルールを定義します。gh CLIコマンドを用いてコードの変更をPRとして提出する際に使用してください。
---

# Pull Request Creator Skill

このスキルは、ユーザーから「Pull Request (PR) を作成して」と依頼された場合に、`gh` CLI コマンドを用いてどのようなフォーマットと手順でGitHub上にPRを作成するかを定めたワークフローです。

---

## 🚨 重要なルール (CRITICAL RULES)

1. **指定言語**: PRのタイトルおよび説明文(Description)は、**必ず全て英語**で記述してください。ユーザーとのチャットでのコミュニケーション自体は日本語で行う場合でも、GitHub上に記録されるPR自体からは日本語を排除してください。
2. **タイトルの形式**: PRのタイトルは `git-commit-formatter` スキルなどの仕様に準拠し、ルールに沿ったプレフィックスを使用してください（例: `feat(agents): add pr-creator skill`）。
3. **説明文の構成**: レビュアーに意図が伝わりやすいように、以下のMarkdown構造を目安に記載してください（すべて英語）。
   - **Overview**: 変更の目的や解決する課題の概要
   - **Implementation Details**: 実際にどのようなコード変更やファイル追加を行ったかの詳細
   - **Verification Results**: ビルドやテスト実行結果

---

## ワークフロー

### 1. 対象の把握
現在チェックアウトされているブランチや、直近のコミットログ、変更内容などを確認し、PRで説明すべき要点（何を実装したか、何のバグを修正したか）を整理します。

```bash
git status
git log -n 5 --oneline
```

### 2. PRのドラフト提案
いきなりPRを投稿するのではなく、まずはチャット上で以下の情報をユーザーに提示し、合意を得てください。
- ターゲットブランチ（`head` -> `base`）
- 提案するPRタイトル (英語)
- 提案するPR説明文 (英語)

### 3. `gh` CLI コマンドによるPR作成
ユーザーからの承認を得たら、`gh pr create` コマンドを用いてPRを作成します。

```bash
gh pr create \
  --title "feat(component): concise title in English" \
  --body "## Overview
Summary of changes...

## Implementation Details
- Detail 1
- Detail 2

## Verification Results
- All unit tests passed (`./gradlew test`)" \
  --base main
```

完了したら、生成されたPRのURLをユーザーに知らせてください。
