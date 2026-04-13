#!/usr/bin/env python3
"""Step 1 – AI Documentation Generation.

Gather git context, call the Cursor Background Agent to create or update
PROJECT_DOCUMENTATION.md on the configured docs branch, then fetch the
resulting file into the local workspace so the CI workflow can commit it.

The script intentionally contains NO Confluence logic — that is step 2.
"""

import os
import re
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path

import requests
from requests.auth import HTTPBasicAuth

from dotenv import load_dotenv

CURSOR_AGENT_URL = "https://api.cursor.com/v0/agents"
DEFAULT_MODEL = "claude-4.5-sonnet-thinking"
# Branch the Cursor Background Agent commits PROJECT_DOCUMENTATION.md to.
# Override with the DOCS_BRANCH env var if needed.
DEFAULT_DOCS_BRANCH = "chore/ai-generated-documentation-v1"

MAX_DIFF_SIZE = 15_000
TRACKED_EXTENSIONS = ["*.py", "*.yaml", "*.yml", "*.toml", "*.json", "*.txt"]
LOCAL_DOC_PATH = Path("PROJECT_DOCUMENTATION.md")


# ---------------------------------------------------------------------------
# Git helpers
# ---------------------------------------------------------------------------


def run(cmd: list[str]) -> str:
    return subprocess.run(cmd, capture_output=True, text=True).stdout.strip()


def run_checked(cmd: list[str]) -> None:
    subprocess.run(cmd, check=True)


def get_git_diff() -> tuple[str, str]:
    """Return (stat summary, unified diff) for the latest commit."""
    stat = run(["git", "diff", "HEAD~1", "HEAD", "--stat"])
    diff = run(["git", "diff", "HEAD~1", "HEAD", "--"] + TRACKED_EXTENSIONS)
    if len(diff) > MAX_DIFF_SIZE:
        diff = diff[:MAX_DIFF_SIZE] + "\n\n... [diff truncated – showing first 15 000 chars]"
    return stat, diff


def get_commit_info() -> dict:
    raw = run(["git", "log", "-1", "--pretty=format:%s|%an|%ci"])
    parts = raw.split("|")
    sha = os.environ.get("GITHUB_SHA", "unknown")
    return {
        "sha": sha,
        "short_sha": sha[:8],
        "message": parts[0] if parts else "unknown",
        "author": parts[1] if len(parts) > 1 else "unknown",
        "date": parts[2] if len(parts) > 2 else datetime.now(timezone.utc).isoformat(),
        "repository": os.environ.get("GITHUB_REPOSITORY", "unknown"),
        "run_url": (
            f"https://github.com/{os.environ.get('GITHUB_REPOSITORY', '')}"
            f"/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}"
        ),
    }


def read_project_file(rel_path: str) -> str:
    """Read a repo file relative to CWD; return empty string if not found."""
    try:
        return Path(rel_path).read_text(encoding="utf-8")
    except FileNotFoundError:
        return ""


# ---------------------------------------------------------------------------
# Cursor Background Agent API
# ---------------------------------------------------------------------------


def call_cursor_agent(prompt: str, docs_branch: str) -> None:
    """Trigger the Cursor Background Agent and wait for it to finish.

    The agent is instructed to write PROJECT_DOCUMENTATION.md directly to
    `docs_branch` in the remote repository — it does not return text content.
    """
    api_key = os.environ["CURSOR_AGENT_KEY"]
    model = os.environ.get("CURSOR_MODEL", DEFAULT_MODEL)
    payload = {
        "prompt": {"text": prompt},
        "model": model,
        "source": {
            "repository": os.environ.get("GITHUB_REPOSITORY", "unknown"),
            "ref": "main",
        },
        "target": {
            "autoCreatePr": True,
            "branchName": docs_branch,
            "commitMessage": "AI Generated Docs update.",
        },
    }
    resp = requests.post(
        CURSOR_AGENT_URL,
        headers={"Content-Type": "application/json"},
        auth=HTTPBasicAuth(api_key, ""),
        json=payload,
        timeout=180,
    )
    print(resp.request.body.decode("utf-8"))
    resp.raise_for_status()
    data = resp.json()

    if "status" in data:
        await_agent_completion(data["id"])
        return

    raise ValueError(
        f"Unexpected Cursor Agent API response format. Keys: {list(data.keys())}\nFull response: {data}"
    )


def await_agent_completion(agent_id: str) -> None:
    """Poll the agent status endpoint until the agent finishes."""
    status_url = f"https://api.cursor.com/v0/agents/{agent_id}"
    api_key = os.environ["CURSOR_AGENT_KEY"]

    while True:
        print(f"Waiting for agent {agent_id} to finish…")
        resp = requests.get(status_url, auth=HTTPBasicAuth(api_key, ""))
        resp.raise_for_status()
        data = resp.json()

        status = data.get("status")
        print(f"Current Status: {status}")

        if status == "FINISHED":
            return

        if status == "FAILED":
            raise Exception(f"Agent failed: {data.get('error')}")

        time.sleep(5)


# ---------------------------------------------------------------------------
# Prompt builder
# ---------------------------------------------------------------------------

_MARKDOWN_RULES = """
Analyze the repository and create technical documentation. Return the content
in Markdown format, following Confluence best practices: use clear headers
(#, ##, ###), bullet points, and tables. For diagrams, use Mermaid syntax.

Additional formatting rules:
- Include the emoji exactly as shown in each section heading.
- Wrap code samples in fenced blocks with the appropriate language tag.
- Wrap all diagrams in ```mermaid fenced blocks.
- Use Markdown tables (| col | col |) with a separator row.
- Return ONLY the Markdown content - no preamble, no closing remarks.
"""

_SECTIONS_NEW = """\
Generate ALL sections below in this exact order. Do not skip any.

## Index

## 💻 About
What this service is, what problem it solves, and what it does. Derive from the code and project files.

## 📝 Requirements
Technical and functional requirements: databases, external services, OS-level dependencies, env vars.
Only populate if the project files reveal concrete requirements; otherwise add only the heading.

## 🪨 Assumptions
Non-obvious constraints or invariants the service relies on — e.g. "campaigns table is always
present", "IP is always IPv4 or IPv6". Derive from the code and architecture. Write simple
sentences in bullet point style.

## ⚙️ Flow and Architecture

### Flow Diagram
A very simple Mermaid sequence or flowchart diagram tracing the end-to-end request/response lifecycle.

### Component Diagram
A very simple Mermaid diagram showing the main system components and their interactions.

### Deployment Diagram
A very simple Mermaid diagram showing how the service is deployed (containers, ports, networks).

## 🚀 Operational Overview

### Failure Model Matrix
A table of the main possible failure scenarios with exactly these columns:
| Chance of Occurrence | Failure Type | Affected Component(s) | Impact on Feature | Notes |
Chance values: Routinely / Occasionally / Rare / Very Rare.
Impact values: No Impact / Degraded / Failure.

### System Component Limits
_To be completed by the team._

### SLA Direct
_To be completed by the team._

## ⌨️ Usage
Practical examples of how to call the service endpoints.

## 📈 Scalability Plan
_To be completed by the team._

## 🚀 Performance Tests
_To be completed by the team._
"""

_SECTIONS_UPDATE = """\
Rules for updating the existing page:
- Identify which sections are affected by these diffs and update ONLY those sections.
- Do NOT modify sections unrelated to the changes.
- Do NOT touch sections marked "_To be completed by the team._".
- Preserve the exact structure and order of all sections.
- Return the FULL updated page in Markdown (all sections, not just the changed ones).

Sections to consider updating based on what changed:
- 💻 About          → if the service purpose or top-level behaviour changed
- 📝 Requirements   → if new packages, databases, or external services were introduced
- 🪨 Assumptions    → if an assumption was invalidated or a new constraint added
- ⚙️ Flow and Architecture → if data flow, components, or deployment topology changed
- 🚀 Operational Overview / Failure Model Matrix → if new failure modes were introduced
- ⌨️ Usage          → if endpoints, request format, or response schema changed
"""


def build_cursor_prompt(stat: str, diff: str, info: dict, existing_md: str) -> str:
    """Construct the prompt sent to the Cursor Background Agent.

    The agent is instructed to write (not return) the file directly to the
    repository. When existing_md is present it updates in place; otherwise it
    creates a fresh document.
    """
    mode_verb = "update the existing" if existing_md else "create a new"
    sections = _SECTIONS_UPDATE if existing_md else _SECTIONS_NEW

    existing_section = (
        f"=== EXISTING PROJECT_DOCUMENTATION.md ===\n{existing_md}\n\n"
        if existing_md
        else ""
    )

    return f"""You are a technical writer maintaining service documentation for a project from the AppTech team of Aptoide.

=== TASK ===
Based on the Git diffs below, {mode_verb} `PROJECT_DOCUMENTATION.md` in the root of the repository
to reflect the latest changes in the project. Write the file directly — do not return it as a
response message.

{existing_section}=== LATEST COMMIT ===
SHA: {info['short_sha']}
Message: {info['message']}
Author: {info['author']}
Date: {info['date']}

Files changed:
{stat}

Diff:
{diff}

=== INSTRUCTIONS ===
{sections}
=== FORMATTING ===
{_MARKDOWN_RULES}
=== ADDITIONAL NOTES ===
Write ONLY the complete Markdown content of `PROJECT_DOCUMENTATION.md` to the file — no
explanations, no preamble, no closing remarks."""


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------


def main() -> None:
    load_dotenv()
    docs_branch = os.environ.get("DOCS_BRANCH", DEFAULT_DOCS_BRANCH)

    # Step 1a: Gather context
    print("Collecting git diff…")
    stat, diff = get_git_diff()
    info = get_commit_info()

    existing_md = read_project_file(str(LOCAL_DOC_PATH))
    if existing_md:
        print(f"Found {LOCAL_DOC_PATH} ({len(existing_md)} chars) – will update.")
    else:
        print(f"{LOCAL_DOC_PATH} not found – will create from scratch.")

    # Step 1: Trigger Cursor Background Agent
    prompt = build_cursor_prompt(stat, diff, info, existing_md)
    print(f"Calling Cursor Agent (target branch: {docs_branch})…")
    call_cursor_agent(prompt, docs_branch)
    
    print("Step 1 complete. PROJECT_DOCUMENTATION.md is ready for CI to commit.")


if __name__ == "__main__":
    main()
