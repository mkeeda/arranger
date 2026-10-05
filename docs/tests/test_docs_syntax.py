#!/usr/bin/env python3
"""
Unit and Integration Tests for MkDocs & Material for MkDocs Syntax Verification.

Ensures that documentation complies with MkDocs Material specific extensions:
1. No GitHub Flavored Markdown (GFM) callouts (> [!NOTE]) -> Use '!!! note'
2. Valid Material Admonition types (note, tip, warning, etc.)
3. Strict 4-space indentation for Admonition bodies
4. No raw HTML tags inside Admonition titles (prevent broken rendering)
5. Valid Content Tabs syntax (=== "Title") and 4-space body indentation
6. Proper closure of fenced code blocks
"""

import os
import re
import unittest
from pathlib import Path
from typing import List, NamedTuple, Set

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
DOCS_DIR = REPO_ROOT / "docs"

# Material for MkDocs officially supported admonition types
VALID_ADMONITION_TYPES: Set[str] = {
    "note",
    "abstract",
    "summary",
    "tldr",
    "info",
    "todo",
    "tip",
    "hint",
    "important",
    "success",
    "check",
    "done",
    "question",
    "help",
    "faq",
    "warning",
    "caution",
    "attention",
    "failure",
    "fail",
    "missing",
    "danger",
    "error",
    "bug",
    "example",
    "quote",
    "cite",
    "seealso",
}


class SyntaxIssue(NamedTuple):
    file_path: Path
    line_number: int
    rule_id: str
    message: str


class MkDocsSyntaxChecker:
    """Validator for Material for MkDocs specific markdown syntax."""

    @staticmethod
    def check_content(content: str, file_path: Path = Path("unknown.md")) -> List[SyntaxIssue]:
        issues: List[SyntaxIssue] = []
        lines = content.splitlines()

        in_code_fence = False
        fence_char = ""
        fence_len = 0
        fence_start_line = 0

        for idx, line in enumerate(lines):
            line_num = idx + 1
            stripped = line.strip()

            # Track fenced code blocks (``` or ~~~)
            fence_match = re.match(r"^(\s*)(`{3,}|~{3,})", line)
            if fence_match:
                marker = fence_match.group(2)
                char = marker[0]
                length = len(marker)

                if not in_code_fence:
                    in_code_fence = True
                    fence_char = char
                    fence_len = length
                    fence_start_line = line_num
                    continue
                else:
                    if char == fence_char and length >= fence_len:
                        in_code_fence = False
                        continue

            # Ignore lines inside code blocks
            if in_code_fence:
                continue

            # Rule 1: No GFM callouts (> [!NOTE], etc.)
            gfm_match = re.match(r"^>\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]", stripped, re.IGNORECASE)
            if gfm_match:
                callout_type = gfm_match.group(1).lower()
                issues.append(
                    SyntaxIssue(
                        file_path=file_path,
                        line_number=line_num,
                        rule_id="no-gfm-callout",
                        message=(
                            f"GFM-style callout '> [!{gfm_match.group(1)}]' is not supported in MkDocs Material. "
                            f"Use '!!! {callout_type} \"Title\"' instead."
                        ),
                    )
                )

            # Rule 2: Admonition syntax validation
            # Matches: !!! type "Title" or ??? type "Title" or ???+ type "Title"
            adm_match = re.match(r"^(\s*)(!{3}|\?{3})\+?\s+([a-zA-Z0-9_-]+)(?:\s+(.*))?$", line)
            if adm_match:
                base_indent = len(adm_match.group(1))
                adm_type = adm_match.group(3).lower()
                title_part = (adm_match.group(4) or "").strip()

                # Rule 2a: Valid admonition type
                if adm_type not in VALID_ADMONITION_TYPES:
                    issues.append(
                        SyntaxIssue(
                            file_path=file_path,
                            line_number=line_num,
                            rule_id="admonition-invalid-type",
                            message=f"Unknown admonition type '{adm_type}'. Valid types: {sorted(VALID_ADMONITION_TYPES)}",
                        )
                    )

                # Rule 2b: Raw HTML tags in admonition title
                if title_part and re.search(r"<[a-zA-Z]+(?:\s+[^>]*)?>", title_part):
                    issues.append(
                        SyntaxIssue(
                            file_path=file_path,
                            line_number=line_num,
                            rule_id="admonition-raw-html-title",
                            message=f"Raw HTML tags detected in admonition title: '{title_part}'. Use code spans or text instead.",
                        )
                    )

                # Rule 2c: 4-space indentation for admonition body
                # Look ahead for the first non-empty line
                next_idx = idx + 1
                while next_idx < len(lines) and lines[next_idx].strip() == "":
                    next_idx += 1

                if next_idx < len(lines):
                    next_line = lines[next_idx]
                    next_stripped = next_line.strip()
                    # If next line is not another top-level admonition or fence
                    if not re.match(r"^(\s*)(!{3}|\?{3})\+?\s+", next_line) and not next_stripped.startswith("```"):
                        expected_indent = base_indent + 4
                        actual_indent = len(next_line) - len(next_line.lstrip(" "))
                        if actual_indent < expected_indent:
                            issues.append(
                                SyntaxIssue(
                                    file_path=file_path,
                                    line_number=next_idx + 1,
                                    rule_id="admonition-body-indentation",
                                    message=(
                                        f"Admonition body must be indented by at least {expected_indent} spaces (got {actual_indent}). "
                                        f"Insufficient indentation breaks admonition rendering in Material for MkDocs."
                                    ),
                                )
                            )

            # Rule 3: Content Tabs syntax validation (=== "Tab Title")
            tab_match = re.match(r"^(\s*)===\s+(.*)$", line)
            if tab_match:
                base_indent = len(tab_match.group(1))
                tab_title = tab_match.group(2).strip()

                # Tab title must be quoted
                if not (
                    (tab_title.startswith('"') and tab_title.endswith('"'))
                    or (tab_title.startswith("'") and tab_title.endswith("'"))
                ):
                    issues.append(
                        SyntaxIssue(
                            file_path=file_path,
                            line_number=line_num,
                            rule_id="content-tab-quotes",
                            message=f"Content tab title must be enclosed in quotes: '=== \"{tab_title}\"'",
                        )
                    )

                # Tab body indentation (must be indented by 4 spaces)
                next_idx = idx + 1
                while next_idx < len(lines) and lines[next_idx].strip() == "":
                    next_idx += 1

                if next_idx < len(lines):
                    next_line = lines[next_idx]
                    next_stripped = next_line.strip()
                    if not re.match(r"^(\s*)===\s+", next_line) and not next_stripped.startswith("```"):
                        expected_indent = base_indent + 4
                        actual_indent = len(next_line) - len(next_line.lstrip(" "))
                        if actual_indent < expected_indent:
                            issues.append(
                                SyntaxIssue(
                                    file_path=file_path,
                                    line_number=next_idx + 1,
                                    rule_id="content-tab-body-indentation",
                                    message=(
                                        f"Content tab body must be indented by at least {expected_indent} spaces (got {actual_indent})."
                                    ),
                                )
                            )

        # Check for unclosed code block
        if in_code_fence:
            issues.append(
                SyntaxIssue(
                    file_path=file_path,
                    line_number=fence_start_line,
                    rule_id="unclosed-code-fence",
                    message=f"Fenced code block opened at line {fence_start_line} was not closed before end of file.",
                )
            )

        return issues


class TestMkDocsSyntaxChecker(unittest.TestCase):
    """TDD Unit Tests for MkDocsSyntaxChecker."""

    def test_no_gfm_callouts_detected(self):
        content = "> [!NOTE] This is a GFM callout\n> It should be flagged."
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0].rule_id, "no-gfm-callout")
        self.assertIn("!!! note", issues[0].message)

    def test_standard_blockquote_allowed(self):
        content = "> This is a regular markdown blockquote.\n> It is completely valid."
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(issues, [])

    def test_admonition_valid_types_pass(self):
        content = (
            '!!! note "Valid Note"\n'
            '    Body with 4-space indent.\n\n'
            '???+ tip "Collapsible Tip"\n'
            '    Another valid body.\n'
        )
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(issues, [])

    def test_admonition_invalid_type_detected(self):
        content = '!!! invalid_type "Title"\n    Body content.'
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0].rule_id, "admonition-invalid-type")
        self.assertIn("invalid_type", issues[0].message)

    def test_admonition_insufficient_indentation_detected(self):
        content = '!!! warning "Title"\n  Body with only 2 spaces.'
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0].rule_id, "admonition-body-indentation")

    def test_admonition_raw_html_in_title_detected(self):
        content = '!!! warning "Inline Code (<code>) Tag"\n    Body content.'
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0].rule_id, "admonition-raw-html-title")

    def test_content_tabs_unquoted_title_detected(self):
        content = "=== Android\n    Platform instructions."
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0].rule_id, "content-tab-quotes")

    def test_content_tabs_valid_syntax_pass(self):
        content = '=== "Android"\n    Platform instructions.\n\n=== "iOS"\n    Platform instructions.'
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(issues, [])

    def test_unclosed_code_fence_detected(self):
        content = "# Title\n\n```kotlin\nval x = 1\n"
        issues = MkDocsSyntaxChecker.check_content(content)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0].rule_id, "unclosed-code-fence")

    def test_existing_documentation_files_pass_syntax_checks(self):
        """Integration regression test: Ensure all markdown files in docs/ pass MkDocsSyntaxChecker."""
        all_md_files = sorted(DOCS_DIR.rglob("*.md"))
        self.assertGreater(len(all_md_files), 0, "Expected markdown files in docs/")

        all_issues: List[SyntaxIssue] = []
        for md_path in all_md_files:
            content = md_path.read_text(encoding="utf-8")
            issues = MkDocsSyntaxChecker.check_content(content, file_path=md_path.relative_to(REPO_ROOT))
            all_issues.extend(issues)

        formatted_issues = [f"{iss.file_path}:{iss.line_number} [{iss.rule_id}]: {iss.message}" for iss in all_issues]
        self.assertEqual(
            all_issues,
            [],
            f"Found {len(all_issues)} MkDocs syntax issue(s) across documentation:\n" + "\n".join(formatted_issues),
        )


if __name__ == "__main__":
    unittest.main()
