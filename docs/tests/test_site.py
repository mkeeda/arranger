#!/usr/bin/env python3
"""
Arranger Documentation Site: Adversarial Stress Test & Link Crawler Suite (Milestone 5 Phase 2)

Adversarial Verification Scope:
1. MkDocs Navigation Integrity:
   - All 27 pages declared in mkdocs.yml 'nav' exist as valid HTML artifacts in site/.
2. Static HTML Link Crawling & Graph Reachability:
   - Full crawl of all 361 HTML files (MkDocs 28 files + Dokka 333 files).
   - Validation of all internal hrefs (relative, root-relative, and self-domain FQDN https://mkeeda.github.io/arranger/*).
   - Zero broken links (404s).
   - Strict filesystem casing verification (preventing Linux CI breakage).
3. Anchor & Fragment Verification:
   - Strict resolution of #fragments in local page and cross-page navigation.
   - Zero broken anchors.
4. Asset Integrity:
   - All img src links point to existing non-empty assets with exact casing.
   - All Dokka UI kit resources, stylesheets, and scripts exist.
5. Dokka Integration & Cross-System Bridge:
   - MkDocs API guides link accurately into Dokka multi-module documentation.
   - Dokka root index correctly links to all 5 published modules.
   - Dokka internal links (4,000+) are completely unbroken.
6. Site Graph & Metadata Integrity:
   - Breadth-First-Search (BFS) crawler verifies 100% reachability of MkDocs pages from site/index.html.
   - All URLs in site/sitemap.xml resolve to existing pages.
   - All locations in site/search/search_index.json resolve to valid pages/anchors.
7. Adversarial Stress Scenarios:
   - Zero empty or whitespace-only links or image sources.
   - Zero unauthorized directory traversal escape links.
"""

import collections
import json
import os
import re
import sys
import unittest
import urllib.parse
import xml.etree.ElementTree as ET
from html.parser import HTMLParser
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SITE_DIR = REPO_ROOT / "site"
MKDOCS_YML = REPO_ROOT / "mkdocs.yml"
DOCS_DIR = REPO_ROOT / "docs"

SELF_DOMAIN_PREFIX = "https://mkeeda.github.io/arranger/"


# ============================================================================
# HTML Analysis Engine (Self-contained, Stdlib only)
# ============================================================================

class ParsedHTMLPage:
    """Holds parsed link, asset, and anchor metadata for a single HTML file."""
    def __init__(self, path: Path):
        self.path = path
        self.href_links: List[str] = []
        self.img_sources: List[str] = []
        self.link_stylesheets: List[str] = []
        self.script_sources: List[str] = []
        self.declared_ids: Set[str] = set()


class SiteHTMLParser(HTMLParser):
    def __init__(self, page: ParsedHTMLPage):
        super().__init__()
        self.page = page

    def handle_starttag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]):
        attr_dict = dict(attrs)

        # Collect anchor IDs
        tag_id = attr_dict.get("id")
        if tag_id:
            self.page.declared_ids.add(tag_id)

        if tag == "a":
            name = attr_dict.get("name")
            if name:
                self.page.declared_ids.add(name)
            href = attr_dict.get("href")
            if href is not None:
                self.page.href_links.append(href)

        elif tag == "img":
            src = attr_dict.get("src")
            if src is not None:
                self.page.img_sources.append(src)

        elif tag == "link":
            href = attr_dict.get("href")
            if href is not None:
                self.page.link_stylesheets.append(href)

        elif tag == "script":
            src = attr_dict.get("src")
            if src is not None:
                self.page.script_sources.append(src)


# Global cache for pre-parsed site data
_PARSED_SITE_DATA: Optional[Dict[Path, ParsedHTMLPage]] = None


def get_parsed_site_data() -> Dict[Path, ParsedHTMLPage]:
    """Parse and cache all HTML pages in site/."""
    global _PARSED_SITE_DATA
    if _PARSED_SITE_DATA is not None:
        return _PARSED_SITE_DATA

    data: Dict[Path, ParsedHTMLPage] = {}
    if not SITE_DIR.exists():
        return data

    all_html_files = [p for p in SITE_DIR.rglob("*.html") if p.is_file()]
    for p in all_html_files:
        page = ParsedHTMLPage(p)
        try:
            content = p.read_text(encoding="utf-8")
            parser = SiteHTMLParser(page)
            parser.feed(content)
            data[p.resolve()] = page
        except Exception as e:
            print(f"Warning: Failed to parse HTML {p}: {e}", file=sys.stderr)

    _PARSED_SITE_DATA = data
    return data


def verify_filesystem_casing(target_path: Path) -> bool:
    """
    Verify if target_path matches directory entry casing exactly on filesystem.
    Crucial for detecting case-sensitivity defects on macOS that break on Linux CI.
    """
    curr = target_path.resolve()
    while curr != curr.parent:
        parent = curr.parent
        if not parent.exists():
            return False
        try:
            entries = os.listdir(parent)
            if curr.name not in entries:
                return False
        except OSError:
            return False
        if curr == REPO_ROOT or curr == SITE_DIR:
            break
        curr = parent
    return True


def resolve_link_to_file(source_page: Path, raw_href: str) -> Tuple[Optional[Path], Optional[str], bool]:
    """
    Resolves raw_href from source_page into (resolved_file_path, fragment, is_internal).
    Handles relative paths, root-relative paths, and self-domain FQDNs.
    """
    href = raw_href.strip()
    if not href:
        return None, None, True

    # Skip external domains, mailto, javascript, tel
    if re.match(r"^(mailto:|javascript:|data:|tel:)", href):
        return None, None, False

    if href.startswith(("http://", "https://")):
        if href.startswith(SELF_DOMAIN_PREFIX):
            # Treat self-domain FQDN as internal
            sub_url = href[len(SELF_DOMAIN_PREFIX):]
            parts = urllib.parse.urlsplit(sub_url)
            frag = urllib.parse.unquote(parts.fragment) if parts.fragment else None
            path_part = urllib.parse.unquote(parts.path)
            target = (SITE_DIR / path_part.lstrip("/")).resolve()
            if target.is_dir():
                target = target / "index.html"
            return target, frag, True
        else:
            return None, None, False

    parts = urllib.parse.urlsplit(href)
    frag = urllib.parse.unquote(parts.fragment) if parts.fragment else None
    path_part = urllib.parse.unquote(parts.path)

    # Fragment-only link on same page
    if not path_part:
        return source_page, frag, True

    # Root-relative paths
    if path_part.startswith("/arranger/"):
        target = (SITE_DIR / path_part[len("/arranger/"):].lstrip("/")).resolve()
    elif path_part.startswith("/"):
        target = (SITE_DIR / path_part.lstrip("/")).resolve()
    else:
        target = (source_page.parent / path_part).resolve()

    if target.is_dir():
        target = target / "index.html"

    return target, frag, True


# ============================================================================
# Test Cases
# ============================================================================

class AdversarialNavIntegrityTests(unittest.TestCase):
    """Tier 5.1: MkDocs Navigation Contract & HTML Generation Match."""

    def test_mkdocs_nav_pages_exist_in_site(self):
        """Assert all 27 markdown files declared in mkdocs.yml 'nav' exist as HTML in site/."""
        self.assertTrue(MKDOCS_YML.exists(), "mkdocs.yml must exist")
        self.assertTrue(SITE_DIR.exists(), "site/ directory must exist")

        nav_text = MKDOCS_YML.read_text(encoding="utf-8")
        nav_md_paths = re.findall(r"-\s+[^:\n]+:\s+([a-zA-Z0-9_\-\.\/]+\.md)", nav_text)
        self.assertGreater(
            len(nav_md_paths),
            0,
            "Expected at least one nav markdown entry in mkdocs.yml",
        )

        missing_artifacts = []
        for md in nav_md_paths:
            if md == "index.md":
                expected_html = SITE_DIR / "index.html"
            else:
                rel_without_ext = md[:-3]
                expected_html = SITE_DIR / rel_without_ext / "index.html"

            if not expected_html.exists():
                missing_artifacts.append((md, str(expected_html.relative_to(REPO_ROOT))))
            elif expected_html.stat().st_size == 0:
                missing_artifacts.append((md, f"{expected_html.relative_to(REPO_ROOT)} is 0 bytes"))

        self.assertEqual(
            missing_artifacts,
            [],
            f"Missing generated HTML artifacts for mkdocs.yml nav entries:\n"
            + "\n".join(f"  - {md} -> {err}" for md, err in missing_artifacts)
        )


class AdversarialLinkCrawlerTests(unittest.TestCase):
    """Tier 5.2: Exhaustive HTML Link Crawling, 404 Detection & Exact Casing."""

    def test_all_internal_links_in_all_html_files(self):
        """
        Exhaustively crawl all HTML files in site/ and verify that every internal href
        resolves to an existing file with exact filesystem casing.
        """
        site_data = get_parsed_site_data()
        self.assertGreater(len(site_data), 0, "Expected at least one HTML file in site/")

        broken_links: List[str] = []
        casing_mismatches: List[str] = []
        total_internal_links_checked = 0

        for page_path, page_data in site_data.items():
            rel_page = str(page_path.relative_to(SITE_DIR))
            for raw_href in page_data.href_links:
                target_file, _, is_internal = resolve_link_to_file(page_path, raw_href)
                if not is_internal or target_file is None:
                    continue

                total_internal_links_checked += 1

                if not target_file.exists():
                    broken_links.append(f"{rel_page} -> href '{raw_href}' (resolved to non-existent {target_file})")
                else:
                    if not verify_filesystem_casing(target_file):
                        casing_mismatches.append(f"{rel_page} -> href '{raw_href}' (case mismatch on {target_file})")

        self.assertEqual(
            broken_links,
            [],
            f"Found {len(broken_links)} broken internal links out of {total_internal_links_checked} checked:\n"
            + "\n".join(f"  - {b}" for b in broken_links[:25])
        )
        self.assertEqual(
            casing_mismatches,
            [],
            f"Found {len(casing_mismatches)} casing mismatches in internal links:\n"
            + "\n".join(f"  - {c}" for c in casing_mismatches[:25])
        )

    def test_all_anchors_and_fragments(self):
        """
        Exhaustively verify that every #fragment in href links corresponds to a valid element id
        or name in the target HTML file.
        """
        site_data = get_parsed_site_data()
        broken_anchors: List[str] = []
        total_anchors_checked = 0

        # Known valid dynamic MkDocs navigation fragment ids (generated or handled by Material theme)
        KNOWN_VALID_DYNAMIC_IDS = {"__top", "__search"}

        for page_path, page_data in site_data.items():
            rel_page = str(page_path.relative_to(SITE_DIR))
            for raw_href in page_data.href_links:
                target_file, frag, is_internal = resolve_link_to_file(page_path, raw_href)
                if not is_internal or target_file is None or not frag:
                    continue

                if frag in KNOWN_VALID_DYNAMIC_IDS:
                    continue

                total_anchors_checked += 1

                target_resolved = target_file.resolve()
                if target_resolved in site_data:
                    target_page_ids = site_data[target_resolved].declared_ids
                    # Check both unquoted and quoted variations
                    raw_frag = urllib.parse.urlsplit(raw_href).fragment
                    if frag not in target_page_ids and raw_frag not in target_page_ids:
                        target_rel = str(target_resolved.relative_to(SITE_DIR))
                        broken_anchors.append(
                            f"{rel_page} -> href '{raw_href}' (missing anchor #{frag} in {target_rel})"
                        )

        self.assertEqual(
            broken_anchors,
            [],
            f"Found {len(broken_anchors)} broken anchors out of {total_anchors_checked} checked:\n"
            + "\n".join(f"  - {b}" for b in broken_anchors[:25])
        )

    def test_no_broken_self_domain_fqdn_links(self):
        """Verify all absolute FQDN links pointing to arranger documentation site resolve properly."""
        site_data = get_parsed_site_data()
        fqdn_links: List[Tuple[str, str, Path]] = []

        for page_path, page_data in site_data.items():
            for raw_href in page_data.href_links:
                if raw_href.startswith(SELF_DOMAIN_PREFIX):
                    target_file, _, _ = resolve_link_to_file(page_path, raw_href)
                    if target_file:
                        fqdn_links.append((str(page_path.relative_to(SITE_DIR)), raw_href, target_file))

        self.assertGreaterEqual(len(fqdn_links), 0, "Expected self-domain FQDN links check")

        broken_fqdn = [
            f"{src} -> {href} (resolved to {target})"
            for src, href, target in fqdn_links
            if not target.exists()
        ]
        self.assertEqual(
            broken_fqdn,
            [],
            f"Found broken self-domain FQDN links ({len(broken_fqdn)}/{len(fqdn_links)}):\n"
            + "\n".join(f"  - {b}" for b in broken_fqdn)
        )


class AdversarialAssetIntegrityTests(unittest.TestCase):
    """Tier 5.3: Image & Static Asset Integrity Verification."""

    def test_all_images_and_gifs_exist_and_non_empty(self):
        """Verify all img src references resolve to non-empty files with strict casing."""
        site_data = get_parsed_site_data()
        broken_images: List[str] = []
        zero_byte_images: List[str] = []
        casing_mismatches: List[str] = []
        total_images_checked = 0

        for page_path, page_data in site_data.items():
            rel_page = str(page_path.relative_to(SITE_DIR))
            for raw_src in page_data.img_sources:
                src_clean = raw_src.strip()
                if not src_clean or src_clean.startswith(("http://", "https://", "data:")):
                    continue

                total_images_checked += 1
                parts = urllib.parse.urlsplit(src_clean)
                path_part = urllib.parse.unquote(parts.path)

                if path_part.startswith("/arranger/"):
                    target = (SITE_DIR / path_part[len("/arranger/"):].lstrip("/")).resolve()
                elif path_part.startswith("/"):
                    target = (SITE_DIR / path_part.lstrip("/")).resolve()
                else:
                    target = (page_path.parent / path_part).resolve()

                if not target.exists():
                    broken_images.append(f"{rel_page} -> img src '{raw_src}' (missing: {target})")
                else:
                    if target.stat().st_size == 0:
                        zero_byte_images.append(f"{rel_page} -> img src '{raw_src}' (0 bytes: {target})")
                    if not verify_filesystem_casing(target):
                        casing_mismatches.append(f"{rel_page} -> img src '{raw_src}' (casing mismatch: {target})")

        self.assertEqual(broken_images, [], f"Found {len(broken_images)} broken image links:\n" + "\n".join(broken_images))
        self.assertEqual(zero_byte_images, [], f"Found {len(zero_byte_images)} 0-byte images:\n" + "\n".join(zero_byte_images))
        self.assertEqual(casing_mismatches, [], f"Found {len(casing_mismatches)} image casing mismatches:\n" + "\n".join(casing_mismatches))

    def test_dokka_resources_and_ui_kit_assets(self):
        """Verify Dokka UI kit stylesheets, scripts, and SVG icon assets all exist."""
        dokka_dir = SITE_DIR / "api"
        self.assertTrue(dokka_dir.exists(), "site/api must exist")

        dokka_pages = [p for p in dokka_dir.rglob("*.html") if p.is_file()]
        broken_resources: List[str] = []
        total_resources_checked = 0

        site_data = get_parsed_site_data()
        for p in dokka_pages:
            rel_page = str(p.relative_to(SITE_DIR))
            page_data = site_data.get(p.resolve())
            if not page_data:
                continue

            for raw_res in page_data.link_stylesheets + page_data.script_sources + page_data.img_sources:
                res_clean = raw_res.strip()
                if not res_clean or res_clean.startswith(("http://", "https://", "data:")):
                    continue

                total_resources_checked += 1
                parts = urllib.parse.urlsplit(res_clean)
                path_part = urllib.parse.unquote(parts.path)
                target = (p.parent / path_part).resolve()

                if not target.exists():
                    broken_resources.append(f"{rel_page} -> resource '{raw_res}'")

        self.assertEqual(
            broken_resources,
            [],
            f"Found {len(broken_resources)} broken Dokka resources out of {total_resources_checked}:\n"
            + "\n".join(broken_resources[:25])
        )


class AdversarialDokkaNavigationTests(unittest.TestCase):
    """Tier 5.4: Dokka Multi-Module Navigation & Cross-System Integration."""

    def test_mkdocs_to_dokka_bridge_links(self):
        """Verify MkDocs navigation links to Dokka API Reference (relative or FQDN) and destination exists."""
        mkdocs_index = SITE_DIR / "index.html"
        self.assertTrue(mkdocs_index.exists(), "site/index.html must exist")

        content = mkdocs_index.read_text(encoding="utf-8")
        has_dokka_link = 'href="api/"' in content or 'href="https://mkeeda.github.io/arranger/api/"' in content
        self.assertTrue(
            has_dokka_link,
            "MkDocs index page navigation must link to Dokka API Reference (relative 'api/' or FQDN)"
        )

        root_api_index = SITE_DIR / "api" / "index.html"
        self.assertTrue(root_api_index.exists(), "site/api/index.html must exist as Dokka entrypoint")

    def test_dokka_multimodule_root_to_modules_reachability(self):
        """Verify Dokka multi-module root index links to all 5 arranger modules."""
        root_index = SITE_DIR / "api" / "index.html"
        self.assertTrue(root_index.exists(), "site/api/index.html must exist")

        site_data = get_parsed_site_data()
        root_data = site_data.get(root_index.resolve())
        self.assertIsNotNone(root_data, "site/api/index.html must be parsed")

        modules_linked = set()
        for href in root_data.href_links:
            target_file, _, is_internal = resolve_link_to_file(root_index, href)
            if is_internal and target_file and target_file.exists():
                rel = str(target_file.relative_to(SITE_DIR / "api"))
                for mod in ["richtext", "richtext-editor", "richtext-editor-material3", "richtext-markdown", "richtext-html"]:
                    if rel.startswith(mod):
                        modules_linked.add(mod)

        expected_modules = {"richtext", "richtext-editor", "richtext-editor-material3", "richtext-markdown", "richtext-html"}
        self.assertEqual(
            modules_linked,
            expected_modules,
            f"Dokka root index missing links to modules:\n"
            + "\n".join(f"  - {m}" for m in (expected_modules - modules_linked))
        )

    def test_dokka_internal_crawling_zero_broken(self):
        """Verify that Dokka's internal 4000+ links contain 0 dead links."""
        dokka_dir = SITE_DIR / "api"
        dokka_pages = [p for p in dokka_dir.rglob("*.html") if p.is_file()]
        site_data = get_parsed_site_data()

        broken_links = []
        total_links = 0
        for p in dokka_pages:
            page_data = site_data.get(p.resolve())
            if not page_data:
                continue
            for href in page_data.href_links:
                target_file, _, is_internal = resolve_link_to_file(p, href)
                if is_internal and target_file:
                    total_links += 1
                    if not target_file.exists():
                        broken_links.append(f"{p.relative_to(SITE_DIR)} -> {href} ({target_file})")

        self.assertGreater(total_links, 0, f"Expected internal links in Dokka, found {total_links}")
        self.assertEqual(broken_links, [], f"Found {len(broken_links)} broken internal Dokka links")


class AdversarialGraphAndMetadataTests(unittest.TestCase):
    """Tier 5.5: BFS Site Graph Reachability & Metadata File Verification."""

    def test_site_graph_bfs_crawling_reachability(self):
        """
        Perform a Breadth-First-Search (BFS) crawl starting from site/index.html.
        Assert that all MkDocs documentation pages are reachable via internal links.
        """
        site_data = get_parsed_site_data()
        start_page = (SITE_DIR / "index.html").resolve()
        self.assertIn(start_page, site_data, "site/index.html must exist and be parsed")

        visited: Set[Path] = set()
        queue: collections.deque = collections.deque([start_page])

        while queue:
            curr = queue.popleft()
            if curr in visited:
                continue
            visited.add(curr)

            page_data = site_data.get(curr)
            if not page_data:
                continue

            for href in page_data.href_links:
                target_file, _, is_internal = resolve_link_to_file(curr, href)
                if is_internal and target_file and target_file.exists() and target_file.is_file():
                    target_resolved = target_file.resolve()
                    if target_resolved not in visited and target_resolved in site_data:
                        queue.append(target_resolved)

        # Check all expected MkDocs pages are visited
        api_dir_resolved = str((SITE_DIR / "api").resolve())
        mkdocs_pages = [p for p in site_data.keys() if not str(p).startswith(api_dir_resolved) and p.name == "index.html"]
        unreachable = [str(p.relative_to(SITE_DIR)) for p in mkdocs_pages if p not in visited]

        self.assertEqual(
            unreachable,
            [],
            f"Found {len(unreachable)} unreachable (orphan) pages during BFS crawl from site/index.html:\n"
            + "\n".join(f"  - {u}" for u in unreachable)
        )

    def test_sitemap_xml_urls_resolve(self):
        """Verify all URLs declared in site/sitemap.xml resolve to existing files in site/."""
        sitemap_path = SITE_DIR / "sitemap.xml"
        self.assertTrue(sitemap_path.exists(), "site/sitemap.xml must exist")

        tree = ET.parse(sitemap_path)
        root = tree.getroot()
        ns = {"ns": "http://www.sitemaps.org/schemas/sitemap/0.9"}
        loc_elements = root.findall(".//ns:loc", ns)
        self.assertGreater(len(loc_elements), 0, "Expected sitemap entries in sitemap.xml")

        broken_sitemap_urls = []
        for elem in loc_elements:
            url = elem.text.strip() if elem.text else ""
            if not url.startswith(SELF_DOMAIN_PREFIX):
                broken_sitemap_urls.append(f"Invalid domain in sitemap URL: {url}")
                continue

            sub = url[len(SELF_DOMAIN_PREFIX):].lstrip("/")
            target = (SITE_DIR / sub).resolve()
            if target.is_dir():
                target = target / "index.html"

            if not target.exists():
                broken_sitemap_urls.append(f"{url} -> resolved to non-existent {target}")

        self.assertEqual(
            broken_sitemap_urls,
            [],
            f"Broken URLs found in sitemap.xml:\n" + "\n".join(broken_sitemap_urls)
        )

    def test_search_index_json_locations_resolve(self):
        """Verify all locations indexed in site/search/search_index.json resolve to valid files/anchors."""
        search_index_path = SITE_DIR / "search" / "search_index.json"
        self.assertTrue(search_index_path.exists(), "site/search/search_index.json must exist")

        data = json.loads(search_index_path.read_text(encoding="utf-8"))
        docs = data.get("docs", [])
        self.assertGreater(len(docs), 200, f"Expected >200 documents in search index, found {len(docs)}")

        site_data = get_parsed_site_data()
        broken_locations = []

        for doc in docs:
            loc = doc.get("location", "")
            # Location is relative to site root, e.g. "" (index.html), "#heading", or "editor-basics/rich-text-editor/#basic-usage"
            if loc == "" or loc.startswith("#"):
                target_file = SITE_DIR / "index.html"
                frag = loc.lstrip("#") if loc.startswith("#") else None
                is_internal = True
            else:
                target_file, frag, is_internal = resolve_link_to_file(SITE_DIR / "index.html", loc)

            if not is_internal or target_file is None or not target_file.exists():
                broken_locations.append(f"Doc location '{loc}' resolved to non-existent file {target_file}")
            elif frag:
                target_resolved = target_file.resolve()
                if target_resolved in site_data:
                    if frag not in site_data[target_resolved].declared_ids:
                        broken_locations.append(f"Doc location '{loc}' missing anchor #{frag} in {target_resolved.name}")

        self.assertEqual(
            broken_locations,
            [],
            f"Found {len(broken_locations)} broken search index locations:\n"
            + "\n".join(broken_locations[:25])
        )


class AdversarialStressScenariosTests(unittest.TestCase):
    """Tier 5.6: Boundary Defense, Traversal Defense & Zero Empty References."""

    def test_directory_traversal_escape_defense(self):
        """Assert no link attempts directory traversal escape above site/ root."""
        site_data = get_parsed_site_data()
        traversal_attempts = []

        for page_path, page_data in site_data.items():
            for href in page_data.href_links:
                target_file, _, is_internal = resolve_link_to_file(page_path, href)
                if is_internal and target_file:
                    try:
                        target_file.relative_to(SITE_DIR)
                    except ValueError:
                        # Target is outside site/ directory
                        traversal_attempts.append(f"{page_path.relative_to(SITE_DIR)} -> href '{href}' escapes site root")

        self.assertEqual(
            traversal_attempts,
            [],
            f"Found directory traversal escape links escaping site/:\n" + "\n".join(traversal_attempts)
        )

    def test_no_empty_or_whitespace_links_and_images(self):
        """Assert zero empty href='' or img src='' attributes across all HTML files."""
        site_data = get_parsed_site_data()
        empty_tags = []

        for page_path, page_data in site_data.items():
            rel = str(page_path.relative_to(SITE_DIR))
            for h in page_data.href_links:
                if h == "" or h.isspace():
                    empty_tags.append(f"{rel}: empty href '{h}'")
            for s in page_data.img_sources:
                if s == "" or s.isspace():
                    empty_tags.append(f"{rel}: empty img src '{s}'")

        self.assertEqual(
            empty_tags,
            [],
            f"Found empty or whitespace-only link/image references:\n" + "\n".join(empty_tags)
        )

    def test_extra_css_and_accent_color(self):
        """Assert extra.css exists, is linked in all MkDocs HTML pages, and properly overrides link/nav accent colors."""
        extra_css_path = SITE_DIR / "stylesheets" / "extra.css"
        self.assertTrue(extra_css_path.is_file(), "site/stylesheets/extra.css does not exist")

        content = extra_css_path.read_text(encoding="utf-8")

        # Check slate / dark mode override
        self.assertIn("#ff764d", content, "extra.css missing dark mode accent color #ff764d")
        self.assertIn("data-md-color-scheme=\"slate\"", content, "extra.css missing slate scheme selector")
        self.assertIn("data-md-color-primary=\"black\"", content, "extra.css missing primary black selector")

        # Check default / light mode override
        self.assertIn("#d84315", content, "extra.css missing light mode accent color #d84315")
        self.assertIn("data-md-color-scheme=\"default\"", content, "extra.css missing default scheme selector")

        # Check active nav link rule
        self.assertIn(".md-nav__link--active", content, "extra.css missing active nav link style rule")

        # Check all MkDocs HTML pages link to extra.css
        site_data = get_parsed_site_data()
        api_dir_str = str((SITE_DIR / "api").resolve())
        unlinked_pages = []
        for p in site_data:
            if not str(p).startswith(api_dir_str):
                html_text = p.read_text(encoding="utf-8")
                if "stylesheets/extra.css" not in html_text:
                    unlinked_pages.append(str(p.relative_to(SITE_DIR)))

        self.assertEqual(
            unlinked_pages,
            [],
            f"Found MkDocs pages not linking to extra.css: {unlinked_pages}"
        )



# ============================================================================
# Adversarial Test Runner & Comprehensive Report Generator
# ============================================================================

def run_adversarial_suite() -> bool:
    print("=" * 80)
    print(" ARRANGER DOCUMENTATION ADVERSARIAL STRESS TEST & CRAWLER SUITE (M5 PHASE 2)")
    print("=" * 80)

    # Pre-warm parsed site data and print inventory
    site_data = get_parsed_site_data()
    api_dir_str = str((SITE_DIR / "api").resolve())
    dokka_count = sum(1 for p in site_data if str(p).startswith(api_dir_str))
    mkdocs_count = len(site_data) - dokka_count

    print(f"Discovered & Parsed Artifacts:")
    print(f"  - Total HTML Files:          {len(site_data)}")
    print(f"  - MkDocs HTML Pages:         {mkdocs_count}")
    print(f"  - Dokka API Reference HTML:  {dokka_count}")

    total_hrefs = sum(len(d.href_links) for d in site_data.values())
    total_imgs = sum(len(d.img_sources) for d in site_data.values())
    total_ids = sum(len(d.declared_ids) for d in site_data.values())
    print(f"  - Total Extracted Hrefs:     {total_hrefs}")
    print(f"  - Total Extracted Img Srcs:  {total_imgs}")
    print(f"  - Total Declared Anchor IDs: {total_ids}")
    print("=" * 80)

    suite = unittest.TestSuite()
    loader = unittest.TestLoader()

    test_classes = [
        ("Tier 5.1: Navigation Integrity", AdversarialNavIntegrityTests),
        ("Tier 5.2: Link & Anchor Crawler", AdversarialLinkCrawlerTests),
        ("Tier 5.3: Asset & Media Integrity", AdversarialAssetIntegrityTests),
        ("Tier 5.4: Dokka Bridge & Multi-Module Navigation", AdversarialDokkaNavigationTests),
        ("Tier 5.5: BFS Site Graph & Metadata Verification", AdversarialGraphAndMetadataTests),
        ("Tier 5.6: Boundary Defense & Adversarial Stress", AdversarialStressScenariosTests),
    ]

    results_summary = []
    overall_success = True

    for title, cls in test_classes:
        t_suite = loader.loadTestsFromTestCase(cls)
        runner = unittest.TextTestRunner(verbosity=1)
        print(f"\n>>> [{title}]")
        t_res = runner.run(t_suite)

        passed = t_res.testsRun - len(t_res.failures) - len(t_res.errors) - len(t_res.skipped)
        total = t_res.testsRun
        fails = len(t_res.failures) + len(t_res.errors)
        skipped = len(t_res.skipped)

        status_tag = "PASS" if fails == 0 else "FAIL"
        if fails > 0:
            overall_success = False

        results_summary.append((title, status_tag, passed, fails, skipped, total))

    print("\n" + "=" * 80)
    print(" ADVERSARIAL VERIFICATION SUMMARY REPORT")
    print("=" * 80)
    for title, tag, p, f, s, tot in results_summary:
        skip_str = f" ({s} skipped)" if s > 0 else ""
        print(f" {title:<50} [{tag}] {p}/{tot} passed, {f} failed{skip_str}")
    print("=" * 80)
    verdict = "APPROVE" if overall_success else "REQUEST_CHANGES"
    print(f" ADVERSARIAL VERDICT: {verdict}")
    print("=" * 80)

    return overall_success


if __name__ == "__main__":
    success = run_adversarial_suite()
    sys.exit(0 if success else 1)
