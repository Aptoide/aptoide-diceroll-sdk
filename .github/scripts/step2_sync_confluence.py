#!/usr/bin/env python3
"""Step 2 – Confluence Synchronization.

Read the local PROJECT_DOCUMENTATION.md (the source of truth written by step 1)
and upload it to the designated Confluence page.

This script performs NO AI generation. It is a strict synchronization tool.
"""

import os
import re
import sys
from pathlib import Path

import markdown
import requests
from requests.auth import HTTPBasicAuth

from dotenv import load_dotenv

LOCAL_DOC_PATH = Path("PROJECT_DOCUMENTATION.md")


# ---------------------------------------------------------------------------
# Markdown → Confluence Storage Format (XHTML) conversion
# ---------------------------------------------------------------------------


def markdown_to_confluence_xhtml(md_text: str) -> str:
    """Convert Markdown to Confluence Storage Format XHTML.

    Fenced code blocks (including ```mermaid```) are converted to the
    Confluence <ac:structured-macro ac:name="code"> macro so they render
    correctly.  All other Markdown is handled by the standard `markdown`
    library (headings, lists, bold/italic, tables).
    """
    code_blocks: list[tuple[str, str]] = []

    def _capture(match: re.Match) -> str:
        lang = (match.group(1) or "").strip()
        code = match.group(2)
        idx = len(code_blocks)
        code_blocks.append((lang, code))
        return f"\n\nCODE_BLOCK_PLACEHOLDER_{idx}\n\n"

    processed = re.sub(r"```(\w*)\n(.*?)```", _capture, md_text, flags=re.DOTALL)
    html = markdown.markdown(processed, extensions=["tables"])

    for idx, (lang, code) in enumerate(code_blocks):
        safe_code = code.replace("]]>", "]]]]><![CDATA[>")
        lang_param = f'<ac:parameter ac:name="language">{lang}</ac:parameter>' if lang else ""
        macro = (
            '<ac:structured-macro ac:name="code">'
            f"{lang_param}"
            f"<ac:plain-text-body><![CDATA[{safe_code}]]></ac:plain-text-body>"
            "</ac:structured-macro>"
        )
        html = html.replace(f"<p>CODE_BLOCK_PLACEHOLDER_{idx}</p>", macro)
        html = html.replace(f"CODE_BLOCK_PLACEHOLDER_{idx}", macro)

    return html


# ---------------------------------------------------------------------------
# Confluence helpers
# ---------------------------------------------------------------------------


def _confluence_url(base: str, path: str) -> str:
    return f"{base.rstrip('/')}/wiki/rest/api/{path}"


def find_page(base: str, auth: HTTPBasicAuth, space: str, title: str) -> dict | None:
    """Return the page dict (including body.storage) or None if not found."""
    resp = requests.get(
        _confluence_url(base, "content"),
        auth=auth,
        params={
            "type": "page",
            "spaceKey": space,
            "title": title,
            "expand": "version,body.storage",
        },
        timeout=30,
    )
    resp.raise_for_status()
    results = resp.json().get("results", [])
    return results[0] if results else None


def create_page(
    base: str, auth: HTTPBasicAuth, space: str, parent_id: str | None, title: str, body: str
) -> dict:
    payload: dict = {
        "type": "page",
        "title": title,
        "space": {"key": space},
        "body": {"storage": {"value": body, "representation": "storage"}},
    }
    if parent_id:
        payload["ancestors"] = [{"id": parent_id}]
    resp = requests.post(_confluence_url(base, "content"), auth=auth, json=payload, timeout=30)
    resp.raise_for_status()
    return resp.json()


def update_page(
    base: str, auth: HTTPBasicAuth, page_id: str, version: int, title: str, body: str
) -> dict:
    payload = {
        "type": "page",
        "title": title,
        "version": {"number": version + 1},
        "body": {"storage": {"value": body, "representation": "storage"}},
    }
    resp = requests.put(
        _confluence_url(base, f"content/{page_id}"), auth=auth, json=payload, timeout=30
    )
    resp.raise_for_status()
    return resp.json()


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------


def main() -> None:
    load_dotenv()
    base_url = os.environ["CONFLUENCE_BASE_URL"]
    username = os.environ["CONFLUENCE_USERNAME"]
    api_token = os.environ["CONFLUENCE_API_TOKEN"]
    space_key = os.environ["CONFLUENCE_SPACE_KEY"]
    parent_id = os.environ.get("CONFLUENCE_PARENT_PAGE_ID") or None
    page_title = os.environ.get("CONFLUENCE_PAGE_TITLE", "Unknown")

    auth = HTTPBasicAuth(username, api_token)

    # Read local source of truth
    if not LOCAL_DOC_PATH.exists():
        print(f"ERROR: {LOCAL_DOC_PATH} not found. Run step1_generate_docs.py first.")
        sys.exit(1)

    md_content = LOCAL_DOC_PATH.read_text(encoding="utf-8")
    print(f"Read {LOCAL_DOC_PATH} ({len(md_content)} chars).")

    # Convert to Confluence Storage Format
    print("Converting Markdown to Confluence Storage Format…")
    confluence_body = markdown_to_confluence_xhtml(md_content)

    # Create or update the Confluence page
    print(f"Looking up Confluence page '{page_title}'…")
    existing_page = find_page(base_url, auth, space_key, page_title)

    if existing_page:
        print("Page found – updating…")
        result = update_page(
            base_url,
            auth,
            existing_page["id"],
            existing_page["version"]["number"],
            page_title,
            confluence_body,
        )
        action = "Updated"
    else:
        print("Page not found – creating…")
        result = create_page(base_url, auth, space_key, parent_id, page_title, confluence_body)
        action = "Created"

    web_link = base_url.rstrip("/") + "/wiki" + result.get("_links", {}).get("webui", "")
    print(f"{action} Confluence page: {web_link}")
    print("Done.")


if __name__ == "__main__":
    main()
