#!/usr/bin/env python3
"""
Adversarial Site Integrity Checker (Arranger Documentation & Dokka API Reference)

Milestone 5 Adversarial Verification Harness:
- Crawls 100% of HTML files in `site/` (all MkDocs guide pages and Dokka API reference pages).
- Validates 100% of `<a href="...">` links (internal HTML targets, anchor hashes).
- Validates 100% of `<img src="...">` images (disk existence, exact casing).
- Guarantees zero broken links, zero broken anchors, zero missing images.
"""

import os
import sys
import urllib.parse
from html.parser import HTMLParser
from pathlib import Path
from typing import Dict, List, Set, Tuple

REPO_ROOT = Path(__file__).resolve().parent.parent
SITE_DIR = REPO_ROOT / "site"
BASE_URL = "https://mkeeda.github.io/arranger/"


class HTMLPageScanner(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids: Set[str] = set()
        self.links: List[Tuple[str, int]] = []
        self.images: List[Tuple[str, int]] = []

    def handle_starttag(self, tag: str, attrs: List[Tuple[str, str]]):
        attrs_dict = dict(attrs)
        line_num = self.getpos()[0]

        # Element IDs
        if "id" in attrs_dict and attrs_dict["id"]:
            self.ids.add(attrs_dict["id"])

        # <a name="..."> is also an anchor target
        if tag == "a":
            if "name" in attrs_dict and attrs_dict["name"]:
                self.ids.add(attrs_dict["name"])
            if "href" in attrs_dict and attrs_dict["href"] is not None:
                self.links.append((attrs_dict["href"], line_num))

        elif tag == "img":
            if "src" in attrs_dict and attrs_dict["src"] is not None:
                self.images.append((attrs_dict["src"], line_num))


def is_case_sensitive_match(file_path: Path) -> bool:
    """Verify that file_path matches exact filesystem casing to prevent Linux CI failures."""
    if not file_path.exists():
        return False
    curr = file_path.resolve()
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


def crawl_and_verify_site() -> Tuple[bool, Dict[str, int], List[str]]:
    if not SITE_DIR.exists():
        print(f"Error: Site directory does not exist at {SITE_DIR}")
        return False, {}, ["site/ directory not found"]

    # Filter for files only, as package dirs in Dokka may end with .html (e.g. dev.mkeeda.arranger.richtext.html)
    html_files = sorted([p for p in SITE_DIR.rglob("*.html") if p.is_file()])
    print(f"Discovered {len(html_files)} HTML files in {SITE_DIR}")

    # Step 1: Pre-parse all HTML files to index their element IDs / anchor targets
    page_dom_ids: Dict[Path, Set[str]] = {}
    page_scanners: Dict[Path, HTMLPageScanner] = {}

    for html_file in html_files:
        scanner = HTMLPageScanner()
        try:
            content = html_file.read_text(encoding="utf-8", errors="replace")
            scanner.feed(content)
        except Exception as e:
            print(f"Warning: Failed to parse {html_file}: {e}")
        page_dom_ids[html_file.resolve()] = scanner.ids
        page_scanners[html_file] = scanner

    # Step 2: Adversarial Link and Image Verification
    stats = {
        "html_pages": len(html_files),
        "total_links": 0,
        "internal_links": 0,
        "external_links": 0,
        "anchor_checks": 0,
        "total_images": 0,
        "internal_images": 0,
        "broken_links": 0,
        "broken_anchors": 0,
        "broken_images": 0,
    }

    errors: List[str] = []

    for html_file, scanner in page_scanners.items():
        rel_html = html_file.relative_to(REPO_ROOT)
        curr_dir = html_file.parent

        # Verify links
        for href, line_num in scanner.links:
            stats["total_links"] += 1
            href_clean = href.strip()

            if not href_clean or href_clean.startswith("javascript:") or href_clean.startswith("mailto:"):
                continue

            # Handle full base URL pointing to this site
            is_internal_base_url = href_clean.startswith(BASE_URL)
            is_root_relative_url = href_clean.startswith("/arranger/") or href_clean == "/arranger"

            if href_clean.startswith("http://") or href_clean.startswith("https://"):
                if not is_internal_base_url:
                    stats["external_links"] += 1
                    continue

            stats["internal_links"] += 1

            # Determine target path and anchor
            if is_internal_base_url:
                rel_to_site = href_clean[len(BASE_URL):]
                if "#" in rel_to_site:
                    path_part, anchor_part = rel_to_site.split("#", 1)
                else:
                    path_part, anchor_part = rel_to_site, None
                path_part = path_part.split("?")[0]
                target_path = (SITE_DIR / path_part).resolve()
            elif is_root_relative_url:
                # e.g. /arranger/. or /arranger/getting-started/
                rel_to_site = href_clean[len("/arranger/"):] if href_clean.startswith("/arranger/") else ""
                if "#" in rel_to_site:
                    path_part, anchor_part = rel_to_site.split("#", 1)
                else:
                    path_part, anchor_part = rel_to_site, None
                path_part = path_part.split("?")[0]
                if path_part == "." or path_part == "":
                    target_path = (SITE_DIR / "index.html").resolve()
                else:
                    target_path = (SITE_DIR / path_part).resolve()
            else:
                if "#" in href_clean:
                    path_part, anchor_part = href_clean.split("#", 1)
                else:
                    path_part, anchor_part = href_clean, None
                path_part = path_part.split("?")[0]

                if not path_part:
                    # Same-page anchor: e.g. '#heading'
                    target_path = html_file.resolve()
                else:
                    target_path = (curr_dir / path_part).resolve()

            # Resolve directory to index.html if needed
            target_html_file = None
            if target_path.is_dir():
                cand = target_path / "index.html"
                if cand.is_file():
                    target_html_file = cand
            elif target_path.is_file():
                target_html_file = target_path
            else:
                # Might be directory without trailing slash: e.g. ../getting-started
                cand = target_path.parent / target_path.name / "index.html"
                if cand.is_file():
                    target_html_file = cand

            if not target_html_file or not target_html_file.exists():
                stats["broken_links"] += 1
                errors.append(
                    f"{rel_html}:L{line_num}: Broken link '{href}' -> unresolved path '{target_path}'"
                )
                continue

            # Case sensitivity check
            if not is_case_sensitive_match(target_html_file):
                stats["broken_links"] += 1
                errors.append(
                    f"{rel_html}:L{line_num}: Link casing mismatch on disk: '{href}' -> '{target_html_file}'"
                )
                continue

            # Anchor check
            if anchor_part:
                stats["anchor_checks"] += 1
                decoded_anchor = urllib.parse.unquote(anchor_part)
                known_ids = page_dom_ids.get(target_html_file.resolve(), set())

                # Dokka or MkDocs might use raw or decoded or slugified anchors
                if anchor_part not in known_ids and decoded_anchor not in known_ids:
                    stats["broken_anchors"] += 1
                    errors.append(
                        f"{rel_html}:L{line_num}: Broken anchor '#{anchor_part}' in '{target_html_file.relative_to(REPO_ROOT)}'"
                    )

        # Verify images
        for src, line_num in scanner.images:
            stats["total_images"] += 1
            src_clean = src.strip()
            if not src_clean or src_clean.startswith("data:"):
                continue

            if src_clean.startswith("http://") or src_clean.startswith("https://"):
                if not src_clean.startswith(BASE_URL):
                    continue
                rel_to_site = src_clean[len(BASE_URL):].split("?")[0].split("#")[0]
                img_path = (SITE_DIR / rel_to_site).resolve()
            else:
                rel_part = src_clean.split("?")[0].split("#")[0]
                img_path = (curr_dir / rel_part).resolve()

            stats["internal_images"] += 1

            if not img_path.exists():
                stats["broken_images"] += 1
                errors.append(
                    f"{rel_html}:L{line_num}: Missing image '{src}' -> unresolved '{img_path}'"
                )
            elif not is_case_sensitive_match(img_path):
                stats["broken_images"] += 1
                errors.append(
                    f"{rel_html}:L{line_num}: Image casing mismatch: '{src}'"
                )

    success = (len(errors) == 0)
    return success, stats, errors


if __name__ == "__main__":
    print("=" * 70)
    print(" ADVERSARIAL SITE INTEGRITY VERIFICATION (MKDOCS + DOKKA)")
    print("=" * 70)
    success, stats, errors = crawl_and_verify_site()

    print("\n" + "=" * 70)
    print(" ADVERSARIAL VERIFICATION SUMMARY REPORT")
    print("=" * 70)
    for k, v in stats.items():
        print(f" {k:<25}: {v}")
    print("=" * 70)

    if not success:
        print(f"\nFAILURE: Found {len(errors)} site integrity defect(s):")
        for err in errors[:50]:
            print(f"  - {err}")
        if len(errors) > 50:
            print(f"  ... and {len(errors) - 50} more")
        sys.exit(1)
    else:
        print("\nSUCCESS: 100% of internal links, anchors, and images verified successfully!")
        print("Zero 404s, zero broken images, zero broken links detected.")
        sys.exit(0)
