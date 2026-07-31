---
name: retrospective
description: Conducts a retrospective analysis of recent Antigravity session logs, abstracts user feedback and learnings into generalized development principles, refactors the AI harness (.agents/ rules, skills, subagents), and automatically creates a Pull Request.
---

# AI Harness Retrospective Skill

This skill provides an automated, end-to-end retrospective workflow to continuously optimize and refactor the AI harness (`.agents/` rules, skills, subagents, and workflows) based on empirical interaction logs from recent Antigravity sessions.

## Core Philosophy

1. **Deep Abstraction (Concrete ➔ Essence ➔ Generalization)**:
   Never copy raw user prompts directly into rules. Always perform root-cause analysis (e.g., context gap, rule conflict, tool misusage) and abstract the findings into broad, reusable development principles.
2. **Full Lifecycle Refactoring (Create, Modify, Delete, Consolidate)**:
   Do not just append new rules. Maintain a clean harness by removing outdated or redundant rules, consolidating overlapping skills, and introducing new specialized subagents when necessary.
3. **Antigravity Ecosystem Maximization**:
   Leverage the full spectrum of Antigravity capabilities: Rules (`.agents/rules/`), Skills (`.agents/skills/`), Subagents (`.agents/subagents/`), Artifacts, MCP tools, Timers/Cron, and Slash Commands.
4. **Human Approval Gate**:
   Always present a comprehensive refactoring proposal (`implementation_plan.md`) to the user and stop execution to wait for explicit user approval before applying file modifications.

---

## Workflow Steps

### Step 1. Discovery & Log Extraction
- Execute the session log analysis helper script:
  ```bash
  python3 .agents/skills/retrospective/scripts/analyze_logs.py --max-sessions 5
  ```
- Inspect recent session transcripts (`transcript.jsonl`) under `~/.gemini/antigravity/brain/` for:
  - Repeated user requests or directives.
  - Critical feedback, corrections, or rule violations noticed by the user.
  - Architectural decisions, troubleshooting insights, or domain knowledge gained during development.
- Review existing harness assets in `.agents/rules/`, `.agents/skills/`, and `.agents/subagents/`.

### Step 2. Deep Analysis & Abstraction (Invoke Pro Subagent)
- Delegate the analysis to a Pro model subagent (`invoke_subagent` with `Model: 'pro'`) to synthesize findings using the abstraction framework:
  1. **Identify Raw Signals**: Extract specific user inputs and agent mistakes.
  2. **Analyze Root Causes**: Determine why the issue occurred (e.g., missing context, conflicting rules, improper tool usage).
  3. **Elevate to Principles**: Transform specific findings into generalized, durable guidelines (e.g., state hoisting invariants, TDD verification bounds).
  4. **Determine Target Component**: Decide whether to update Rules, Skills, Subagents, or Workflows, including identifying candidates for **deletion** or **consolidation**.

### Step 3. Proposal Generation (`implementation_plan.md`)
- Create an `implementation_plan.md` artifact outlining:
  - **Abstracted Principles**: High-level engineering guidelines derived from logs.
  - **Harness Modification Matrix**:
    - `[NEW]`: New rules, skills, or subagent definitions.
    - `[MODIFY]`: Specific updates to existing harness files.
    - `[DELETE]`: Obsolete or redundant rules/skills to be removed.
    - `[CONSOLIDATE]`: Merging overlapping skills or rules.
- Set `RequestFeedback: true`, `UserFacing: true`, and provide a descriptive `Summary` in the artifact metadata.

### Step 4. Human Approval Gate
- **CRITICAL**: Stop tool calls and end your turn immediately after presenting the plan.
- Wait for explicit user approval (Proceed button or user approval message) before making any modifications to harness files.

### Step 5. Execution & Verification
- Once approved, checkout the latest default branch (`main` or equivalent), pull the latest changes, and create a fresh topic branch (e.g., `refactor/harness-retrospective-YYYYMMDD`).
- Apply the approved additions, modifications, deletions, and consolidations across `.agents/`.
- Ensure all comments, KDocs, skill files, and code in the repository adhere to project guidelines (e.g., English documentation in `.agents/`).
- Create `walkthrough.md` to summarize the changes made and the validation steps completed.

### Step 6. Automated PR Creation
- Invoke the `pr-creator` skill to automatically generate a Pull Request on GitHub.
- Write clear, professional English PR titles and descriptions detailing the rationale, abstracted principles, and harness modifications.
