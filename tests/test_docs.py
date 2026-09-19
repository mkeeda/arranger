#!/usr/bin/env python3
"""
Arranger Documentation E2E Test Suite (4-Tier Architecture)

Authoritative Requirements & Specifications Source:
- ORIGINAL_REQUEST.md (R1-R4, Acceptance Criteria)
- PROJECT.md (Architecture, Feature Inventory #1-#17, Interface Contracts #1-#4)

Tiers:
- Tier 1: Feature Coverage (All 27 pages existence, H1/frontmatter titles, mkdocs.yml nav declaration, requirements.txt, .gitignore)
- Tier 2: Boundary & Corner Cases (Markdown relative link resolution with slug matching, image asset paths with exact casing, orphan image detection)
- Tier 3: Cross-Feature Combinations (Code tabs syntax & indentation, code block language tags, dark/light palette toggles, admonition syntax, Mermaid diagram syntax)
- Tier 4: Real-World Acceptance Scenarios (CI docs.yml workflow specification, Root README.md link & slimming, mkdocs build --strict execution, site output validation)
"""

import os
import re
import sys
import shutil
import subprocess
import unittest
import urllib.parse
from pathlib import Path
from typing import Dict, List, Set, Tuple, Optional

# Root directory of the repository
REPO_ROOT = Path(__file__).resolve().parent.parent
DOCS_DIR = REPO_ROOT / "docs"
IMAGES_DIR = DOCS_DIR / "images"
MKDOCS_YML = REPO_ROOT / "mkdocs.yml"
REQUIREMENTS_TXT = REPO_ROOT / "requirements.txt"
GITIGNORE_FILE = REPO_ROOT / ".gitignore"
WORKFLOW_DOCS_YML = REPO_ROOT / ".github" / "workflows" / "docs.yml"
ROOT_README = REPO_ROOT / "README.md"
SITE_DIR = REPO_ROOT / "site"

# Expected Canonical Specification (Derived from PROJECT.md § Documentation Site Information Architecture)
EXPECTED_PAGES: List[str] = [
    "index.md",
    "getting-started/installation.md",
    "getting-started/quickstart.md",
    "editor-basics/rich-text-editor.md",
    "editor-basics/wysiwyg-editor.md",
    "editor-basics/state-management.md",
    "styling/spans-and-paragraphs.md",
    "styling/built-in-attributes.md",
    "styling/custom-attributes.md",
    "styling/theming-and-m3.md",
    "advanced-behaviors/enter-key-strategies.md",
    "advanced-behaviors/list-handling.md",
    "advanced-behaviors/span-merging.md",
    "interactions/autocomplete.md",
    "interactions/span-clicks.md",
    "interactions/toolbars.md",
    "interop/markdown.md",
    "interop/html.md",
    "interop/custom-formats.md",
    "architecture/overview.md",
    "architecture/state-lifecycle.md",
    "api/overview.md",
    "api/richtext.md",
    "api/richtext-editor.md",
    "api/richtext-editor-material3.md",
    "api/richtext-markdown.md",
    "api/richtext-html.md",
]

EXPECTED_CATEGORIES: List[str] = [
    "Home / Getting Started",
    "Editor Basics",
    "Styling & Formatting",
    "Advanced Behaviors",
    "Interactive Features",
    "Interoperability & Formats",
    "Architecture & Internal Design",
    "API Reference",
]

EXPECTED_IMAGES: Set[str] = {
    "advanced-formatting.png",
    "attribute-batch-edit.gif",
    "basic-usage.png",
    "bullet-list.png",
    "custom-attribute.png",
    "custom-list-marker.png",
    "document-editor.png",
    "dynamic-typing-demo.gif",
    "enter-key-strategy-heading.gif",
    "enter-key-strategy-inherit.gif",
    "enter-key-strategy-list.gif",
    "hashtag-highlight.gif",
    "mention-autocomplete.png",
    "ordered-list.png",
    "rich-text-editor-demo.gif",
    "undo-redo.gif",
    "wysiwyg.gif",
}

EXPECTED_SITE_NAME = "Arranger"
EXPECTED_SITE_URL = "https://mkeeda.github.io/arranger/"

VALID_CODE_LANGUAGES: Set[str] = {
    "kotlin",
    "groovy",
    "xml",
    "yaml",
    "yml",
    "bash",
    "sh",
    "shell",
    "text",
    "markdown",
    "md",
    "json",
    "html",
    "diff",
    "mermaid",
    "properties",
    "gradle",
    "toml",
}

VALID_ADMONITION_TYPES: Set[str] = {
    "note",
    "abstract",
    "info",
    "tip",
    "success",
    "question",
    "warning",
    "failure",
    "danger",
    "bug",
    "example",
    "quote",
    "seealso",
    "tldr",
    "hint",
    "important",
    "caution",
    "attention",
    "error",
}

VALID_MERMAID_TYPES: Set[str] = {
    "graph",
    "flowchart",
    "sequencediagram",
    "classdiagram",
    "statediagram",
    "statediagram-v2",
    "erdiagram",
    "gantt",
    "pie",
    "gitgraph",
    "mindmap",
    "timeline",
    "quadrantchart",
    "sankey-beta",
    "xychart-beta",
}


# ============================================================================
# Helpers: Markdown & Config Parsing (Self-contained, Stdlib only)
# ============================================================================

def parse_yaml_sections(file_path: Path) -> Dict[str, str]:
    """Parse top-level YAML keys into sections without requiring external PyYAML."""
    if not file_path.exists():
        return {}
    sections: Dict[str, str] = {}
    current_key: Optional[str] = None
    buffer: List[str] = []

    for line in file_path.read_text(encoding="utf-8").splitlines():
        # Top-level key: no leading space, identifier followed by colon
        top_match = re.match(r"^([a-zA-Z0-9_-]+):\s*(.*)$", line)
        if top_match and not line.startswith(" "):
            if current_key is not None:
                sections[current_key] = "\n".join(buffer)
            current_key = top_match.group(1)
            val = top_match.group(2).strip()
            buffer = [val] if val else []
        else:
            buffer.append(line)
    if current_key is not None:
        sections[current_key] = "\n".join(buffer)
    return sections


def extract_scalar_yaml_value(content: str, key: str) -> Optional[str]:
    """Extract scalar value for a given key from YAML string."""
    m = re.search(rf"^{key}:\s*(.+)$", content, re.MULTILINE)
    if m:
        return m.group(1).strip().strip("'\"")
    return None


def extract_mkdocs_nav(mkdocs_path: Path) -> Tuple[List[str], List[str]]:
    """
    Extract categories and markdown file paths declared in mkdocs.yml 'nav:'.
    Returns (categories, nav_paths).
    """
    sections = parse_yaml_sections(mkdocs_path)
    nav_text = sections.get("nav", "")
    if not nav_text:
        return [], []

    # Categories in nav have 2-space indentation: '  - Category Name:'
    categories = re.findall(r"^[ ]{2}-\s+([^:\n]+):\s*$", nav_text, re.MULTILINE)
    # File paths end with .md
    paths = re.findall(r"-\s+[^:\n]+:\s+([a-zA-Z0-9_\-\.\/]+\.md)", nav_text)
    return categories, paths


def slugify_heading(text: str) -> str:
    """Generate Markdown anchor slug following Python-Markdown / MkDocs convention."""
    text = re.sub(r"[`\*_~\[\]]", "", text)
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[-\s]+", "-", text)
    return text.strip("-")


def extract_headings_from_markdown(content: str) -> List[Tuple[int, str, str]]:
    """
    Extract headings from markdown.
    Returns list of (level, raw_title, slug).
    """
    headings = []
    in_code_block = False
    for line in content.splitlines():
        if line.strip().startswith("```"):
            in_code_block = not in_code_block
            continue
        if in_code_block:
            continue
        m = re.match(r"^(#{1,6})\s+(.+)$", line)
        if m:
            level = len(m.group(1))
            title = m.group(2).strip()
            slug = slugify_heading(title)
            headings.append((level, title, slug))
    return headings


def get_case_sensitive_path(path: Path) -> bool:
    """
    Verify if a path exists with EXACT case matching on filesystem.
    Critical for catching casing defects on macOS that would break on Linux CI.
    """
    if not path.exists():
        return False
    curr = path.resolve()
    while curr != curr.parent:
        parent = curr.parent
        if not parent.exists():
            return False
        try:
            if curr.name not in os.listdir(parent):
                return False
        except OSError:
            return False
        if curr == REPO_ROOT:
            break
        curr = parent
    return True


# ============================================================================
# Tier 1: Feature Coverage Tests
# ============================================================================

class Tier1FeatureCoverageTests(unittest.TestCase):
    """
    Tier 1: Verify presence and validity of all 26 pages (+ index.md),
    title/frontmatter integrity, mkdocs.yml nav declaration, requirements.txt, and .gitignore.
    """

    def test_all_27_pages_exist(self):
        """Assert all 27 markdown pages defined in PROJECT.md exist and are non-empty."""
        missing_pages = []
        empty_pages = []
        for rel_path in EXPECTED_PAGES:
            full_path = DOCS_DIR / rel_path
            if not full_path.exists():
                missing_pages.append(rel_path)
            elif full_path.stat().st_size == 0:
                empty_pages.append(rel_path)

        self.assertEqual(
            missing_pages,
            [],
            f"Missing required documentation pages ({len(missing_pages)}/{len(EXPECTED_PAGES)}):\n"
            + "\n".join(f"  - docs/{p}" for p in missing_pages)
        )
        self.assertEqual(
            empty_pages,
            [],
            f"Documentation pages must not be 0 bytes ({len(empty_pages)} found):\n"
            + "\n".join(f"  - docs/{p}" for p in empty_pages)
        )

    def test_page_titles_and_frontmatter(self):
        """Assert every documentation page has a valid H1 header or title frontmatter."""
        missing_titles = []
        for rel_path in EXPECTED_PAGES:
            full_path = DOCS_DIR / rel_path
            if not full_path.exists():
                continue
            content = full_path.read_text(encoding="utf-8")
            headings = extract_headings_from_markdown(content)
            h1_headings = [h for h in headings if h[0] == 1]

            # Check frontmatter title if H1 is absent
            has_frontmatter_title = False
            if content.startswith("---"):
                fm_match = re.search(r"^title:\s*(.+)$", content[:500], re.MULTILINE)
                if fm_match and fm_match.group(1).strip():
                    has_frontmatter_title = True

            if not h1_headings and not has_frontmatter_title:
                missing_titles.append(rel_path)

        self.assertEqual(
            missing_titles,
            [],
            f"Documentation pages missing an H1 header (# Title) or frontmatter title:\n"
            + "\n".join(f"  - docs/{p}" for p in missing_titles)
        )

    def test_mkdocs_yml_exists_and_configured(self):
        """Assert mkdocs.yml exists, sets site identity, and points to docs/."""
        self.assertTrue(MKDOCS_YML.exists(), "mkdocs.yml must exist at project root")
        raw_text = MKDOCS_YML.read_text(encoding="utf-8")

        site_name = extract_scalar_yaml_value(raw_text, "site_name")
        site_url = extract_scalar_yaml_value(raw_text, "site_url")
        docs_dir = extract_scalar_yaml_value(raw_text, "docs_dir")

        self.assertEqual(site_name, EXPECTED_SITE_NAME, f"site_name must be '{EXPECTED_SITE_NAME}'")
        self.assertEqual(site_url, EXPECTED_SITE_URL, f"site_url must be '{EXPECTED_SITE_URL}'")
        self.assertEqual(docs_dir or "docs", "docs", "docs_dir must be 'docs'")
        self.assertIn("theme:", raw_text, "theme definition must exist in mkdocs.yml")
        self.assertIn("name: material", raw_text, "theme.name must be 'material'")

    def test_mkdocs_nav_complete_match(self):
        """Assert mkdocs.yml nav declaration matches all 27 pages and 8 categories."""
        self.assertTrue(MKDOCS_YML.exists(), "mkdocs.yml must exist at project root")
        categories, nav_paths = extract_mkdocs_nav(MKDOCS_YML)

        # Check all 8 categories exist
        missing_cats = [c for c in EXPECTED_CATEGORIES if c not in categories]
        self.assertEqual(
            missing_cats,
            [],
            f"Missing categories in mkdocs.yml nav:\n" + "\n".join(f"  - {c}" for c in missing_cats)
        )

        # Check all 27 pages are included in nav
        missing_in_nav = [p for p in EXPECTED_PAGES if p not in nav_paths]
        self.assertEqual(
            missing_in_nav,
            [],
            f"Pages defined in PROJECT.md missing from mkdocs.yml nav:\n"
            + "\n".join(f"  - {p}" for p in missing_in_nav)
        )

        # Check all files declared in nav actually exist in docs/
        nav_missing_on_disk = [p for p in nav_paths if not (DOCS_DIR / p).exists()]
        self.assertEqual(
            nav_missing_on_disk,
            [],
            f"Files declared in mkdocs.yml nav that do not exist in docs/:\n"
            + "\n".join(f"  - docs/{p}" for p in nav_missing_on_disk)
        )

    def test_requirements_txt_dependencies(self):
        """Verify requirements.txt exists and specifies mkdocs-material."""
        self.assertTrue(REQUIREMENTS_TXT.exists(), "requirements.txt must exist at project root")
        content = REQUIREMENTS_TXT.read_text(encoding="utf-8")
        self.assertIn("mkdocs-material", content, "requirements.txt must contain 'mkdocs-material'")

    def test_gitignore_rules(self):
        """Verify .gitignore ignores MkDocs and virtualenv artifacts (/site/, .venv/, .cache/)."""
        self.assertTrue(GITIGNORE_FILE.exists(), ".gitignore must exist at project root")
        content = GITIGNORE_FILE.read_text(encoding="utf-8")
        self.assertIn("site/", content, ".gitignore must ignore site/ build output")
        self.assertIn(".venv", content, ".gitignore must ignore .venv")
        self.assertIn(".cache", content, ".gitignore must ignore .cache")


# ============================================================================
# Tier 2: Boundary & Corner Cases Tests
# ============================================================================

class Tier2BoundaryCornerCaseTests(unittest.TestCase):
    """
    Tier 2: Relative markdown link resolution, image asset path contract,
    casing accuracy (preventing Linux CI breakage), and orphan image detection.
    """

    def _collect_markdown_files(self) -> List[Path]:
        files = []
        if ROOT_README.exists():
            files.append(ROOT_README)
        if DOCS_DIR.exists():
            files.extend(list(DOCS_DIR.rglob("*.md")))
        return files

    def test_internal_markdown_links_resolution(self):
        """Verify all internal markdown links resolve to valid files and anchors."""
        broken_links: List[str] = []
        md_files = self._collect_markdown_files()

        # Pre-cache slugs for all existing markdown files
        file_slugs: Dict[Path, Set[str]] = {}
        for f in md_files:
            try:
                content = f.read_text(encoding="utf-8")
                headings = extract_headings_from_markdown(content)
                file_slugs[f.resolve()] = {h[2] for h in headings}
            except Exception:
                file_slugs[f.resolve()] = set()

        for f in md_files:
            content = f.read_text(encoding="utf-8")
            # Exclude code blocks when searching for links
            cleaned_lines = []
            in_code = False
            for line in content.splitlines():
                if line.strip().startswith("```"):
                    in_code = not in_code
                    continue
                if not in_code:
                    cleaned_lines.append(line)
            cleaned_content = "\n".join(cleaned_lines)

            # Match [link text](destination)
            link_matches = re.findall(r"(?<!!)\[([^\]]+)\]\(([^)]+)\)", cleaned_content)
            for text, target in link_matches:
                target = urllib.parse.unquote(target.strip())
                # Skip external links, mailto, javascript, or blank targets
                if re.match(r"^(https?://|mailto:|javascript:)", target):
                    continue

                # Parse path and anchor
                if "#" in target:
                    path_part, anchor_part = target.split("#", 1)
                else:
                    path_part, anchor_part = target, None

                # Same-page anchor: e.g. '#installation'
                if not path_part:
                    if anchor_part:
                        known_slugs = file_slugs.get(f.resolve(), set())
                        if anchor_part not in known_slugs:
                            broken_links.append(f"{f.relative_to(REPO_ROOT)}: anchor '#{anchor_part}' not found in file")
                    continue

                # Clean query strings if any
                path_part = path_part.split("?")[0]
                target_path = (f.parent / path_part).resolve()

                # If target is a markdown link
                if path_part.endswith(".md"):
                    if not target_path.exists():
                        broken_links.append(f"{f.relative_to(REPO_ROOT)}: link '{target}' -> '{path_part}' not found")
                    elif not get_case_sensitive_path(target_path):
                        broken_links.append(f"{f.relative_to(REPO_ROOT)}: link '{target}' has casing mismatch on disk")
                    elif anchor_part:
                        known_slugs = file_slugs.get(target_path, set())
                        if anchor_part not in known_slugs:
                            broken_links.append(
                                f"{f.relative_to(REPO_ROOT)}: link '{target}' anchor '#{anchor_part}' not found in {path_part}"
                            )

        self.assertEqual(
            broken_links,
            [],
            f"Broken internal markdown links found ({len(broken_links)}):\n"
            + "\n".join(f"  - {err}" for err in broken_links)
        )

    def test_image_and_gif_asset_paths(self):
        """Verify all image references resolve to existing files with strict casing match."""
        broken_images: List[str] = []
        md_files = self._collect_markdown_files()

        for f in md_files:
            content = f.read_text(encoding="utf-8")
            # Match markdown images: ![alt](path)
            md_imgs = re.findall(r"!\[([^\]]*)\]\(([^)]+)\)", content)
            # Match HTML images: <img ... src="path" ...>
            html_imgs = re.findall(r'<img[^>]+src=["\']([^"\']+)["\']', content)

            all_refs = [src.strip() for _, src in md_imgs] + [src.strip() for src in html_imgs]
            for ref in all_refs:
                ref = urllib.parse.unquote(ref)
                if re.match(r"^(https?://|data:)", ref):
                    continue
                ref_clean = ref.split("?")[0].split("#")[0]
                target_path = (f.parent / ref_clean).resolve()

                if not target_path.exists():
                    broken_images.append(f"{f.relative_to(REPO_ROOT)}: image reference '{ref}' not found on disk")
                elif not get_case_sensitive_path(target_path):
                    broken_images.append(f"{f.relative_to(REPO_ROOT)}: image reference '{ref}' casing mismatch on disk")

        self.assertEqual(
            broken_images,
            [],
            f"Broken image/GIF references found ({len(broken_images)}):\n"
            + "\n".join(f"  - {err}" for err in broken_images)
        )

    def test_all_17_images_exist_and_no_orphan_images(self):
        """
        Verify that all 17 expected images exist in docs/images/
        and track orphan (unreferenced) image status.
        """
        self.assertTrue(IMAGES_DIR.exists(), f"docs/images directory must exist at {IMAGES_DIR}")
        actual_images = {p.name for p in IMAGES_DIR.iterdir() if p.is_file()}

        # Verify all 17 expected assets exist
        missing_assets = EXPECTED_IMAGES - actual_images
        self.assertEqual(
            missing_assets,
            set(),
            f"Missing expected image/GIF assets ({len(missing_assets)}):\n"
            + "\n".join(f"  - docs/images/{img}" for img in sorted(missing_assets))
        )

        # Collect all image filenames referenced in markdown
        md_files = self._collect_markdown_files()
        referenced_image_names: Set[str] = set()
        for f in md_files:
            content = f.read_text(encoding="utf-8")
            refs = re.findall(r"!\[[^\]]*\]\(([^)]+)\)", content) + re.findall(r'<img[^>]+src=["\']([^"\']+)["\']', content)
            for r in refs:
                name = Path(urllib.parse.unquote(r.split("?")[0].split("#")[0])).name
                referenced_image_names.add(name)

        orphan_images = actual_images - referenced_image_names
        # In final documentation site, all images should be referenced.
        if orphan_images:
            print(f"\n[INFO Tier 2] Currently unreferenced images in docs/images ({len(orphan_images)}):")
            for img in sorted(orphan_images):
                print(f"  - docs/images/{img}")

    def test_no_empty_links_or_images(self):
        """Verify markdown does not contain empty links [](), [text](), ![](), or <img src="">."""
        empty_tags: List[str] = []
        for f in self._collect_markdown_files():
            content = f.read_text(encoding="utf-8")
            lines = []
            in_code = False
            for line in content.splitlines():
                if line.strip().startswith("```"):
                    in_code = not in_code
                    continue
                if not in_code:
                    lines.append(line)
            clean = "\n".join(lines)

            # Check empty links: [text]() or []()
            if re.search(r"\[[^\]]*\]\(\s*\)", clean):
                empty_tags.append(f"{f.relative_to(REPO_ROOT)}: empty link '[]()'")
            # Check empty images: ![]()
            if re.search(r"!\[[^\]]*\]\(\s*\)", clean):
                empty_tags.append(f"{f.relative_to(REPO_ROOT)}: empty image '![]()'")
            # Check empty img src
            if re.search(r'<img[^>]+src=["\']\s*["\']', clean):
                empty_tags.append(f"{f.relative_to(REPO_ROOT)}: empty <img src=''>")

        self.assertEqual(
            empty_tags,
            [],
            f"Found empty link/image tags:\n" + "\n".join(f"  - {t}" for t in empty_tags)
        )


# ============================================================================
# Tier 3: Cross-Feature Combinations Tests
# ============================================================================

class Tier3CrossFeatureCombinationTests(unittest.TestCase):
    """
    Tier 3: Verify MkDocs Material code tab indentation, code block language tag validity,
    dark/light mode palette toggle configuration, admonitions syntax, and Mermaid diagrams.
    """

    def _collect_markdown_files(self) -> List[Path]:
        if not DOCS_DIR.exists():
            return []
        return list(DOCS_DIR.rglob("*.md"))

    def test_code_tabs_syntax_and_indentation(self):
        """Verify code tabs (=== \"Title\") have quoted titles and proper 4-space indentation."""
        errors: List[str] = []
        for f in self._collect_markdown_files():
            lines = f.read_text(encoding="utf-8").splitlines()
            for i, line in enumerate(lines):
                if line.strip().startswith("==="):
                    if not re.match(r'^\s*===\s+["\'][^"\']+["\']', line):
                        errors.append(
                            f"{f.relative_to(REPO_ROOT)}:L{i+1}: Invalid tab syntax '{line.strip()}'. Expected '=== \"Title\"'"
                        )
                    # Next non-empty line must be indented by at least 4 spaces or be another tab header
                    j = i + 1
                    while j < len(lines) and lines[j].strip() == "":
                        j += 1
                    if j < len(lines):
                        next_line = lines[j]
                        if not next_line.startswith("    ") and not next_line.strip().startswith("==="):
                            errors.append(
                                f"{f.relative_to(REPO_ROOT)}:L{j+1}: Content under tab must be indented with 4 spaces. Got: '{next_line}'"
                            )

        self.assertEqual(
            errors,
            [],
            f"Code tab syntax errors found ({len(errors)}):\n" + "\n".join(f"  - {e}" for e in errors)
        )

    def test_code_block_language_tags(self):
        """Verify all fenced code blocks specify recognized languages and are properly closed."""
        errors: List[str] = []
        for f in self._collect_markdown_files():
            lines = f.read_text(encoding="utf-8").splitlines()
            in_code = False
            open_line = 0
            open_lang = ""

            for i, line in enumerate(lines):
                stripped = line.strip()
                if stripped.startswith("```"):
                    if not in_code:
                        in_code = True
                        open_line = i + 1
                        open_lang = stripped[3:].strip()
                        if open_lang:
                            lang_key = open_lang.split()[0].lower()
                            if lang_key not in VALID_CODE_LANGUAGES:
                                errors.append(
                                    f"{f.relative_to(REPO_ROOT)}:L{open_line}: Unrecognized code language '{open_lang}'"
                                )
                        else:
                            errors.append(
                                f"{f.relative_to(REPO_ROOT)}:L{open_line}: Missing code block language tag"
                            )
                    else:
                        in_code = False

            if in_code:
                errors.append(
                    f"{f.relative_to(REPO_ROOT)}:L{open_line}: Unclosed code block (opened with ```{open_lang})"
                )

        self.assertEqual(
            errors,
            [],
            f"Code block syntax/language errors ({len(errors)}):\n" + "\n".join(f"  - {e}" for e in errors)
        )

    def test_theme_palette_definition(self):
        """Verify mkdocs.yml has both light and dark palette definitions with toggles."""
        self.assertTrue(MKDOCS_YML.exists(), "mkdocs.yml must exist")
        text = MKDOCS_YML.read_text(encoding="utf-8")

        self.assertIn("scheme: default", text, "mkdocs.yml theme.palette must have 'scheme: default' (Light mode)")
        self.assertIn("scheme: slate", text, "mkdocs.yml theme.palette must have 'scheme: slate' (Dark mode)")

        # Both must have toggle definitions
        default_block = text[text.find("scheme: default"):text.find("scheme: slate")] if "scheme: slate" in text else ""
        self.assertIn("toggle:", default_block, "Default palette must have 'toggle:' block")
        self.assertIn("icon:", default_block, "Default palette must have toggle icon")

        slate_block = text[text.find("scheme: slate"):]
        self.assertIn("toggle:", slate_block, "Slate palette must have 'toggle:' block")
        self.assertIn("icon:", slate_block, "Slate palette must have toggle icon")

    def test_admonitions_syntax(self):
        """Verify admonitions (!!! type \"Title\") use valid types and proper indentation."""
        errors: List[str] = []
        for f in self._collect_markdown_files():
            lines = f.read_text(encoding="utf-8").splitlines()
            for i, line in enumerate(lines):
                m = re.match(r"^\s*(\!{3}|\?{3}\+?)\s+([a-zA-Z_-]+)", line)
                if m:
                    adm_type = m.group(2).lower()
                    if adm_type not in VALID_ADMONITION_TYPES:
                        errors.append(
                            f"{f.relative_to(REPO_ROOT)}:L{i+1}: Invalid admonition type '{adm_type}'"
                        )
                    # Check next line indentation
                    j = i + 1
                    while j < len(lines) and lines[j].strip() == "":
                        j += 1
                    if j < len(lines):
                        next_line = lines[j]
                        if not next_line.startswith("    "):
                            errors.append(
                                f"{f.relative_to(REPO_ROOT)}:L{j+1}: Admonition body must be indented 4 spaces. Got: '{next_line}'"
                            )

        self.assertEqual(
            errors,
            [],
            f"Admonition syntax errors ({len(errors)}):\n" + "\n".join(f"  - {e}" for e in errors)
        )

    def test_mermaid_diagrams_syntax(self):
        """Verify Mermaid code blocks start with valid diagram definitions."""
        errors: List[str] = []
        for f in self._collect_markdown_files():
            content = f.read_text(encoding="utf-8")
            blocks = re.findall(r"```mermaid\s*\n(.*?)\n```", content, re.DOTALL)
            for block in blocks:
                lines = [l.strip() for l in block.splitlines() if l.strip() and not l.strip().startswith("%%")]
                if not lines:
                    continue
                first_word = lines[0].split()[0].lower()
                if first_word not in VALID_MERMAID_TYPES:
                    errors.append(
                        f"{f.relative_to(REPO_ROOT)}: Mermaid diagram starts with unknown type '{first_word}'"
                    )

        self.assertEqual(
            errors,
            [],
            f"Mermaid syntax errors ({len(errors)}):\n" + "\n".join(f"  - {e}" for e in errors)
        )


# ============================================================================
# Tier 4: Real-World Acceptance Scenarios Tests
# ============================================================================

class Tier4AcceptanceScenarioTests(unittest.TestCase):
    """
    Tier 4: Verification of CI workflow (.github/workflows/docs.yml),
    root README.md link & slimming, mkdocs build --strict live execution, and output artifacts.
    """

    def test_ci_workflow_specification(self):
        """Verify .github/workflows/docs.yml has required triggers, strict build, and pages deploy."""
        self.assertTrue(
            WORKFLOW_DOCS_YML.exists(),
            f".github/workflows/docs.yml must exist at {WORKFLOW_DOCS_YML}"
        )
        content = WORKFLOW_DOCS_YML.read_text(encoding="utf-8")

        # 1. Triggers
        self.assertIn("pull_request", content, "CI workflow must trigger on pull_request")
        self.assertIn("push", content, "CI workflow must trigger on push")
        self.assertIn("main", content, "CI workflow must trigger on main branch push")

        # 2. Strict build verification
        self.assertIn(
            "mkdocs build --strict",
            content,
            "CI workflow must run 'mkdocs build --strict'"
        )

        # 3. GitHub Pages deployment configuration
        self.assertIn("actions/upload-pages-artifact@v3", content, "CI workflow must use upload-pages-artifact@v3")
        self.assertIn("actions/deploy-pages@v4", content, "CI workflow must use deploy-pages@v4")
        self.assertIn("pages: write", content, "Deploy job must have 'pages: write' permission")
        self.assertIn("id-token: write", content, "Deploy job must have 'id-token: write' permission")

    def test_root_readme_docs_link(self):
        """Verify root README.md contains link to official documentation site."""
        self.assertTrue(ROOT_README.exists(), "Root README.md must exist")
        content = ROOT_README.read_text(encoding="utf-8")
        has_doc_link = EXPECTED_SITE_URL in content
        self.assertTrue(
            has_doc_link,
            f"Root README.md must include link to official documentation site: {EXPECTED_SITE_URL} (Contract #4)"
        )

    def test_root_readme_slimming(self):
        """
        Verify root README.md line count is streamlined (< 350 lines, target ~160 lines vs original 914 lines).
        Note: Assigned to Milestone 4.
        """
        self.assertTrue(ROOT_README.exists(), "Root README.md must exist")
        lines = ROOT_README.read_text(encoding="utf-8").splitlines()
        self.assertLessEqual(
            len(lines),
            350,
            f"Root README.md is currently {len(lines)} lines. It must be slimmed to ~160 lines (< 350 lines) in Milestone 4."
        )

    def test_mkdocs_build_strict_execution(self):
        """
        Execute 'mkdocs build --strict' and verify zero warnings/errors.
        If mkdocs is not in current PATH or virtual environment, skip with instructions.
        """
        mkdocs_cmd = None
        venv_mkdocs = REPO_ROOT / ".venv" / "bin" / "mkdocs"
        if venv_mkdocs.exists():
            mkdocs_cmd = str(venv_mkdocs)
        elif shutil.which("mkdocs"):
            mkdocs_cmd = "mkdocs"

        if not mkdocs_cmd:
            raise unittest.SkipTest(
                "mkdocs is not installed in current environment. "
                "To run live build test: python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt"
            )

        # Execute mkdocs build --strict
        result = subprocess.run(
            [mkdocs_cmd, "build", "--strict"],
            cwd=str(REPO_ROOT),
            capture_output=True,
            text=True
        )

        self.assertEqual(
            result.returncode,
            0,
            f"'mkdocs build --strict' failed with exit code {result.returncode}.\n"
            f"STDOUT:\n{result.stdout}\n"
            f"STDERR:\n{result.stderr}"
        )
        self.assertIn("Documentation built in", result.stderr + result.stdout)

        # Preserve Dokka artifacts in site/api/dokka if build/dokka/html exists
        dokka_html_dir = REPO_ROOT / "build" / "dokka" / "html"
        if dokka_html_dir.exists():
            site_dokka_dir = SITE_DIR / "api" / "dokka"
            site_dokka_dir.mkdir(parents=True, exist_ok=True)
            shutil.copytree(dokka_html_dir, site_dokka_dir, dirs_exist_ok=True)

    def test_generated_site_artifacts(self):
        """Verify generated site/ artifacts if site directory exists."""
        if not SITE_DIR.exists():
            raise unittest.SkipTest("site/ directory does not exist yet. Run mkdocs build first.")

        critical_artifacts = [
            SITE_DIR / "index.html",
            SITE_DIR / "404.html",
            SITE_DIR / "sitemap.xml",
            SITE_DIR / "getting-started" / "installation" / "index.html",
            SITE_DIR / "getting-started" / "quickstart" / "index.html",
            SITE_DIR / "api" / "overview" / "index.html",
        ]

        missing = [str(p.relative_to(REPO_ROOT)) for p in critical_artifacts if not p.exists()]
        self.assertEqual(
            missing,
            [],
            f"Critical generated HTML artifacts missing in site/:\n" + "\n".join(f"  - {m}" for m in missing)
        )


# ============================================================================
# Test Runner & CLI Reporting
# ============================================================================

def run_suite(tier: Optional[int] = None) -> bool:
    suite = unittest.TestSuite()
    loader = unittest.TestLoader()

    tier_classes = {
        1: ("Tier 1: Feature Coverage", Tier1FeatureCoverageTests),
        2: ("Tier 2: Boundary & Corner Cases", Tier2BoundaryCornerCaseTests),
        3: ("Tier 3: Cross-Feature Combinations", Tier3CrossFeatureCombinationTests),
        4: ("Tier 4: Real-World Acceptance Scenarios", Tier4AcceptanceScenarioTests),
    }

    print("=" * 70)
    print(" ARRANGER DOCUMENTATION E2E TEST SUITE (4-TIER ARCHITECTURE)")
    print("=" * 70)

    results_summary = []
    overall_success = True

    tiers_to_run = [tier] if tier in tier_classes else [1, 2, 3, 4]

    for t_num in tiers_to_run:
        title, cls = tier_classes[t_num]
        t_suite = loader.loadTestsFromTestCase(cls)
        runner = unittest.TextTestRunner(verbosity=1)
        print(f"\n--- [{title}] ---")
        t_res = runner.run(t_suite)

        passed = t_res.testsRun - len(t_res.failures) - len(t_res.errors) - len(t_res.skipped)
        total = t_res.testsRun
        fails = len(t_res.failures) + len(t_res.errors)
        skipped = len(t_res.skipped)

        status_tag = "PASS" if fails == 0 else "FAIL"
        if fails > 0:
            overall_success = False

        results_summary.append((title, status_tag, passed, fails, skipped, total))

    print("\n" + "=" * 70)
    print(" TEST EXECUTION SUMMARY REPORT")
    print("=" * 70)
    for title, tag, p, f, s, tot in results_summary:
        skip_str = f" ({s} skipped)" if s > 0 else ""
        print(f" {title:<42} [{tag}] {p}/{tot} passed, {f} failed{skip_str}")
    print("=" * 70)

    return overall_success


if __name__ == "__main__":
    tier_arg = None
    if "--tier" in sys.argv:
        idx = sys.argv.index("--tier")
        if idx + 1 < len(sys.argv):
            try:
                tier_arg = int(sys.argv[idx + 1])
            except ValueError:
                print(f"Invalid tier argument: {sys.argv[idx + 1]}")
                sys.exit(1)

    success = run_suite(tier_arg)
    sys.exit(0 if success else 1)
