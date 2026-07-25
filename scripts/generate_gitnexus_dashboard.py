#!/usr/bin/env python3
"""
Generate a self-contained engineering HTML dashboard for the ECCN project.

Includes tech stack, architecture, Mermaid UML (class + component), API surface,
cyclomatic complexity, git state, known risks, and GitNexus graph metrics.

Usage:
  python scripts/generate_gitnexus_dashboard.py
  python scripts/generate_gitnexus_dashboard.py --output reports/gitnexus-dashboard.html
  python scripts/generate_gitnexus_dashboard.py --refresh
  python scripts/generate_gitnexus_dashboard.py --repo eccn-management-service
"""

from __future__ import annotations

import argparse
import html
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse, urlunparse


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_REPO = "eccn-management-service"
DEFAULT_OUTPUT = REPO_ROOT / "reports" / "gitnexus-dashboard.html"
RUN_CJS = REPO_ROOT / ".gitnexus" / "run.cjs"
META_JSON = REPO_ROOT / ".gitnexus" / "meta.json"
POM_XML = REPO_ROOT / "pom.xml"
AGENTS_MD = REPO_ROOT / "AGENTS.md"
MAIN_JAVA = REPO_ROOT / "src" / "main" / "java"
COMPLEXITY_SCRIPT = REPO_ROOT / "scripts" / "analyze_complexity.py"

POM_NS = {"m": "http://maven.apache.org/POM/4.0.0"}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def die(message: str, code: int = 1) -> None:
    print(f"error: {message}", file=sys.stderr)
    sys.exit(code)


def esc(value: Any) -> str:
    return html.escape("" if value is None else str(value), quote=True)


def run_cmd(args: List[str], *, timeout: int = 120, cwd: Optional[Path] = None) -> subprocess.CompletedProcess:
    try:
        return subprocess.run(
            args,
            cwd=str(cwd or REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
    except subprocess.TimeoutExpired:
        die(f"command timed out after {timeout}s: {' '.join(args)}")
    except FileNotFoundError:
        die(f"command not found: {args[0]}")


def run_gitnexus(args: List[str], *, timeout: int = 120) -> subprocess.CompletedProcess:
    if not RUN_CJS.is_file():
        die(
            f"GitNexus runner not found at {RUN_CJS}. "
            "Run `npx gitnexus analyze` from the project root first."
        )
    return run_cmd(["node", str(RUN_CJS), *args], timeout=timeout)


def sanitize_remote_url(url: Optional[str]) -> str:
    if not url:
        return ""
    try:
        parsed = urlparse(url)
        if parsed.username or parsed.password:
            host = parsed.hostname or ""
            if parsed.port:
                host = f"{host}:{parsed.port}"
            return urlunparse(parsed._replace(netloc=host))
        return url
    except Exception:
        return re.sub(r"(://)[^/@]+@", r"\1", url)


def parse_markdown_table(markdown: str) -> List[Dict[str, str]]:
    if not markdown or not markdown.strip():
        return []
    lines = [ln.strip() for ln in markdown.strip().splitlines() if ln.strip()]
    table_lines = [ln for ln in lines if ln.startswith("|")]
    if len(table_lines) < 2:
        return []

    def split_row(line: str) -> List[str]:
        return [p.strip() for p in line.strip("|").split("|")]

    headers = split_row(table_lines[0])
    rows: List[Dict[str, str]] = []
    for line in table_lines[1:]:
        if re.match(r"^[\s|:-]+$", line):
            continue
        cells = split_row(line)
        if len(cells) < len(headers):
            cells.extend([""] * (len(headers) - len(cells)))
        elif len(cells) > len(headers):
            cells = cells[: len(headers)]
        rows.append(dict(zip(headers, cells)))
    return rows


def cypher(repo: str, statement: str) -> List[Dict[str, str]]:
    result = run_gitnexus(["cypher", "-r", repo, statement])
    if result.returncode != 0:
        stderr = (result.stderr or result.stdout or "").strip()
        die(f"cypher failed ({result.returncode}): {stderr}\nQuery: {statement}")
    raw = (result.stdout or "").strip()
    if not raw:
        return []
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        die(f"cypher returned non-JSON output:\n{raw[:500]}")
    if isinstance(payload, dict) and payload.get("error"):
        die(f"cypher error: {payload['error']}\nQuery: {statement}")
    return parse_markdown_table(payload.get("markdown") or "")


def cypher_scalar(repo: str, statement: str, column: str) -> int:
    rows = cypher(repo, statement)
    if not rows:
        return 0
    try:
        return int(float(rows[0].get(column, "0")))
    except ValueError:
        return 0


def pom_text(root: ET.Element, path: str, default: str = "") -> str:
    node = root.find(path, POM_NS)
    if node is not None and node.text:
        return node.text.strip()
    # try without namespace (some POMs)
    node = root.find(path.replace("m:", ""))
    if node is not None and node.text:
        return node.text.strip()
    return default


def resolve_prop(value: str, props: Dict[str, str]) -> str:
    if not value:
        return value
    out = value
    for _ in range(5):
        m = re.search(r"\$\{([^}]+)\}", out)
        if not m:
            break
        key = m.group(1)
        out = out.replace(m.group(0), props.get(key, m.group(0)))
    return out


# ---------------------------------------------------------------------------
# Project collectors
# ---------------------------------------------------------------------------

def collect_stack() -> Dict[str, Any]:
    if not POM_XML.is_file():
        return {"error": "pom.xml missing", "versions": {}, "dependencies": []}

    # Local trusted pom.xml only — no network entity resolution needed.
    parser = ET.XMLParser()
    # Reject DTDs if the parser supports it (Python 3.8+ ElementTree).
    try:
        parser.entity = {}  # type: ignore[attr-defined]
    except Exception:
        pass
    tree = ET.parse(POM_XML, parser=parser)
    root = tree.getroot()

    props: Dict[str, str] = {}
    props_el = root.find("m:properties", POM_NS)
    if props_el is not None:
        for child in props_el:
            tag = child.tag.split("}")[-1]
            if child.text:
                props[tag] = child.text.strip()

    parent_version = pom_text(root, "m:parent/m:version")
    boot = parent_version
    java_ver = props.get("java.version", "")
    cloud = props.get("spring-cloud.version", "")
    lombok = props.get("lombok.version", "")
    artifact = pom_text(root, "m:artifactId")
    version = pom_text(root, "m:version")
    group = pom_text(root, "m:groupId") or pom_text(root, "m:parent/m:groupId")

    deps: List[Dict[str, str]] = []
    deps_el = root.find("m:dependencies", POM_NS)
    if deps_el is not None:
        for dep in deps_el.findall("m:dependency", POM_NS):
            gid = (dep.findtext("m:groupId", default="", namespaces=POM_NS) or "").strip()
            aid = (dep.findtext("m:artifactId", default="", namespaces=POM_NS) or "").strip()
            ver = resolve_prop(
                (dep.findtext("m:version", default="", namespaces=POM_NS) or "").strip(),
                props,
            )
            scope = (dep.findtext("m:scope", default="", namespaces=POM_NS) or "").strip() or "compile"
            if aid:
                deps.append(
                    {
                        "groupId": gid,
                        "artifactId": aid,
                        "version": ver or ("(BOM/parent)" if not ver else ver),
                        "scope": scope,
                    }
                )

    # Highlight key stack rows
    highlights = [
        {"layer": "Language", "tech": "Java", "version": java_ver or "?"},
        {"layer": "Build", "tech": "Maven / Spring Boot parent", "version": boot or "?"},
        {"layer": "Cloud BOM", "tech": "Spring Cloud", "version": cloud or "?"},
        {"layer": "API", "tech": "Spring Web MVC", "version": boot or "?"},
        {"layer": "Persistence", "tech": "Spring Data MongoDB", "version": boot or "?"},
        {"layer": "Security", "tech": "Spring Security", "version": boot or "?"},
        {"layer": "API Docs", "tech": "Springdoc OpenAPI", "version": next(
            (d["version"] for d in deps if "springdoc" in d["artifactId"]), "?"
        )},
        {"layer": "Resilience", "tech": "Resilience4j", "version": next(
            (d["version"] for d in deps if "resilience4j" in d["artifactId"] and d["version"] != "(BOM/parent)"),
            "(BOM)",
        )},
        {"layer": "Cache", "tech": "Caffeine", "version": next(
            (d["version"] for d in deps if d["artifactId"] == "caffeine"), "?"
        )},
        {"layer": "Codegen", "tech": "Lombok", "version": lombok or "?"},
        {"layer": "Testing", "tech": "Testcontainers MongoDB", "version": next(
            (d["version"] for d in deps if d["artifactId"] == "mongodb" and "testcontainers" in d["groupId"]),
            "?",
        )},
    ]

    return {
        "groupId": group,
        "artifactId": artifact,
        "version": version,
        "highlights": highlights,
        "dependencies": deps,
        "properties": props,
    }


def layer_for_path(rel: str) -> str:
    parts = Path(rel).parts
    for layer in (
        "controller",
        "service",
        "repository",
        "model",
        "config",
        "exception",
        "events",
    ):
        if layer in parts:
            return layer
    if rel.endswith("Application.java"):
        return "application"
    return "other"


def collect_architecture() -> Dict[str, Any]:
    packages: Dict[str, List[str]] = defaultdict(list)
    classes: List[Dict[str, Any]] = []
    deps: List[Tuple[str, str]] = []  # (from, to) class names

    if not MAIN_JAVA.is_dir():
        return {"packages": {}, "classes": [], "deps": [], "counts": {}}

    for path in sorted(MAIN_JAVA.rglob("*.java")):
        rel = str(path.relative_to(REPO_ROOT)).replace("\\", "/")
        layer = layer_for_path(rel)
        text = path.read_text(encoding="utf-8", errors="replace")
        pkg_m = re.search(r"^package\s+([\w.]+)\s*;", text, re.M)
        package = pkg_m.group(1) if pkg_m else ""
        # top-level type names
        type_names = re.findall(
            r"^(?:public\s+)?(?:abstract\s+|final\s+)?(?:class|interface|enum|record)\s+(\w+)",
            text,
            re.M,
        )
        primary = type_names[0] if type_names else path.stem
        packages[layer].append(primary)

        # constructor-injected collaborators
        for m in re.finditer(r"private\s+final\s+(\w+)\s+(\w+)\s*;", text):
            type_name, _field = m.group(1), m.group(2)
            if type_name.endswith(("Service", "Repository", "Publisher")) or type_name in (
                "ApplicationEventPublisher",
            ):
                deps.append((primary, type_name))

        classes.append(
            {
                "name": primary,
                "layer": layer,
                "package": package,
                "file": rel,
                "types": type_names,
            }
        )

    counts = {k: len(v) for k, v in sorted(packages.items())}
    return {
        "packages": {k: sorted(set(v)) for k, v in sorted(packages.items())},
        "classes": classes,
        "deps": deps,
        "counts": counts,
        "file_count": len(classes),
    }


def mermaid_safe(name: str) -> str:
    """Mermaid node id — alphanumeric only."""
    return re.sub(r"[^A-Za-z0-9_]", "_", name)


def build_component_mermaid(arch: Dict[str, Any], routes: List[Dict[str, str]]) -> str:
    controllers = arch["packages"].get("controller", [])
    services = arch["packages"].get("service", [])
    repos = arch["packages"].get("repository", [])
    models = arch["packages"].get("model", [])
    configs = arch["packages"].get("config", [])

    lines = [
        "flowchart TB",
        "  subgraph clients [Clients]",
        "    HTTP[HTTP / OpenAPI clients]",
        "  end",
        "  subgraph api [API Layer]",
    ]
    for c in controllers:
        lines.append(f"    {mermaid_safe(c)}[{c}]")
    lines.append("  end")
    lines.append("  subgraph domain [Domain Services]")
    for s in services:
        lines.append(f"    {mermaid_safe(s)}[{s}]")
    lines.append("  end")
    lines.append("  subgraph persistence [Persistence]")
    for r in repos:
        lines.append(f"    {mermaid_safe(r)}[{r}]")
    lines.append("    Mongo[(MongoDB)]")
    lines.append("  end")
    lines.append("  subgraph crosscut [Cross-cutting]")
    for cfg in configs:
        lines.append(f"    {mermaid_safe(cfg)}[{cfg}]")
    lines.append("  end")
    lines.append("  subgraph entities [Domain Models]")
    for m in models:
        lines.append(f"    {mermaid_safe(m)}[{m}]")
    lines.append("  end")

    lines.append("  HTTP --> api")
    # wire controller -> service from deps
    dep_set = set(arch.get("deps") or [])
    for frm, to in dep_set:
        if frm in controllers and (to.endswith("Service") or to in services):
            lines.append(f"  {mermaid_safe(frm)} --> {mermaid_safe(to)}")
        if frm in services and (to.endswith("Repository") or to in repos):
            lines.append(f"  {mermaid_safe(frm)} --> {mermaid_safe(to)}")
        if frm in services and to.endswith("Service") and to != frm:
            lines.append(f"  {mermaid_safe(frm)} --> {mermaid_safe(to)}")

    for r in repos:
        lines.append(f"  {mermaid_safe(r)} --> Mongo")

    # configs touch api
    for cfg in configs:
        lines.append(f"  {mermaid_safe(cfg)} -.-> api")

    lines.append(f"  %% indexed API routes: {len(routes)}")
    return "\n".join(lines)


def build_class_mermaid(arch: Dict[str, Any]) -> str:
    """UML-ish class diagram for controllers, services, repos, key models."""
    packages = arch["packages"]
    deps = arch.get("deps") or []

    lines = ["classDiagram"]
    # Declare classes by layer with stereotypes via notes in names
    for layer, stereotype in (
        ("controller", "Controller"),
        ("service", "Service"),
        ("repository", "Repository"),
        ("model", "Model"),
    ):
        for name in packages.get(layer, []):
            lines.append(f"  class {mermaid_safe(name)} {{")
            lines.append(f"    <<{stereotype}>>")
            lines.append("  }")

    # Relationships
    seen = set()
    for frm, to in deps:
        key = (frm, to)
        if key in seen:
            continue
        seen.add(key)
        # Only include if both ends are known types we declared, or ApplicationEventPublisher skip
        if to == "ApplicationEventPublisher":
            continue
        left = mermaid_safe(frm)
        right = mermaid_safe(to)
        # declare target if missing (e.g. nested)
        all_declared = {
            mermaid_safe(n)
            for layer in ("controller", "service", "repository", "model")
            for n in packages.get(layer, [])
        }
        if right not in all_declared:
            lines.append(f"  class {right}")
        if left in all_declared or True:
            if to.endswith("Repository"):
                lines.append(f"  {left} --> {right} : uses")
            elif to.endswith("Service"):
                lines.append(f"  {left} --> {right} : uses")
            else:
                lines.append(f"  {left} --> {right} : depends")

    # Model ownership hints: repository named XRepository -> X
    for repo in packages.get("repository", []):
        if repo.endswith("Repository"):
            entity = repo[: -len("Repository")]
            if entity in packages.get("model", []):
                lines.append(
                    f"  {mermaid_safe(repo)} ..> {mermaid_safe(entity)} : persists"
                )

    return "\n".join(lines)


def collect_complexity() -> Dict[str, Any]:
    if not COMPLEXITY_SCRIPT.is_file():
        return {"error": "analyze_complexity.py missing"}
    result = run_cmd(
        [
            sys.executable,
            str(COMPLEXITY_SCRIPT),
            str(MAIN_JAVA),
            "--recursive",
            "--format",
            "json",
        ],
        timeout=60,
    )
    # exit code 2 means methods over threshold — still valid JSON
    raw = (result.stdout or "").strip()
    if not raw:
        return {"error": (result.stderr or "no complexity output").strip()}
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        return {"error": f"invalid JSON from complexity script: {raw[:200]}"}

    methods: List[Dict[str, Any]] = []
    for f in payload.get("files") or []:
        for m in f.get("methods") or []:
            methods.append(
                {
                    "class_name": m.get("class_name") or "",
                    "name": m.get("name") or "",
                    "complexity": int(m.get("complexity") or 0),
                    "risk_level": m.get("risk_level") or "",
                    "file_path": f.get("file_path") or "",
                    "line_number": m.get("line_number") or 0,
                }
            )
    methods.sort(key=lambda x: (-x["complexity"], x["class_name"], x["name"]))
    summary = payload.get("summary") or {}
    return {
        "summary": summary,
        "top_methods": methods[:25],
        "methods": methods,
    }


def collect_git() -> Dict[str, Any]:
    def git(*args: str) -> str:
        r = run_cmd(["git", *args], timeout=30)
        return (r.stdout or "").strip()

    branch = git("branch", "--show-current")
    head = git("rev-parse", "HEAD")
    short = head[:7] if head else ""
    status = git("status", "-sb")
    dirty_lines = [ln for ln in status.splitlines()[1:] if ln.strip()]
    log = git("log", "-8", "--pretty=format:%h|%ad|%s", "--date=short")
    commits = []
    for line in log.splitlines():
        parts = line.split("|", 2)
        if len(parts) == 3:
            commits.append({"hash": parts[0], "date": parts[1], "subject": parts[2]})
    remote = sanitize_remote_url(git("remote", "get-url", "origin") or "")
    return {
        "branch": branch,
        "head": head,
        "short": short,
        "status_summary": status.splitlines()[0] if status else "",
        "dirty_files": dirty_lines,
        "dirty_count": len(dirty_lines),
        "commits": commits,
        "remote": remote,
    }


def collect_risks() -> List[str]:
    if not AGENTS_MD.is_file():
        return []
    text = AGENTS_MD.read_text(encoding="utf-8", errors="replace")
    # Prefer Known Project Drift / Risks section
    m = re.search(
        r"## Known Project Drift / Risks\s*\n(.*?)(?=\n## |\Z)",
        text,
        re.S,
    )
    section = m.group(1) if m else ""
    risks = []
    for line in section.splitlines():
        line = line.strip()
        if line.startswith("- "):
            risks.append(line[2:].strip())
    return risks


# ---------------------------------------------------------------------------
# GitNexus collectors
# ---------------------------------------------------------------------------

def load_meta() -> Dict[str, Any]:
    if not META_JSON.is_file():
        die(
            f"GitNexus index metadata not found at {META_JSON}. "
            "Run `npx gitnexus analyze` from the project root first."
        )
    with META_JSON.open(encoding="utf-8") as fh:
        return json.load(fh)


def collect_status() -> Dict[str, Any]:
    result = run_gitnexus(["status"])
    text = (result.stdout or "") + (result.stderr or "")
    up_to_date = "up-to-date" in text.lower() or "✅" in text
    stale = "stale" in text.lower()
    return {"raw": text.strip(), "fresh": up_to_date and not stale, "stale": stale}


def maybe_refresh(status: Dict[str, Any], force: bool) -> Dict[str, Any]:
    if force or status.get("stale"):
        print("Refreshing GitNexus index (`analyze`)...", file=sys.stderr)
        result = run_gitnexus(["analyze"], timeout=300)
        if result.returncode != 0:
            die(f"analyze failed: {(result.stderr or result.stdout or '').strip()}")
        return collect_status()
    return status


def collect_clusters(repo: str) -> List[Dict[str, Any]]:
    rows = cypher(
        repo,
        "MATCH (c:Community) "
        "RETURN c.heuristicLabel AS label, "
        "sum(c.symbolCount) AS symbols, "
        "round(avg(c.cohesion)*100, 1) AS cohesionPct "
        "ORDER BY symbols DESC",
    )
    out = []
    for row in rows:
        try:
            symbols = int(float(row.get("symbols") or 0))
        except ValueError:
            symbols = 0
        try:
            cohesion = float(row.get("cohesionPct") or 0)
        except ValueError:
            cohesion = 0.0
        out.append({"label": row.get("label") or "", "symbols": symbols, "cohesionPct": cohesion})
    return out


def collect_process_mix(repo: str) -> List[Dict[str, Any]]:
    rows = cypher(repo, "MATCH (p:Process) RETURN p.processType AS type, count(*) AS count")
    out = []
    for row in rows:
        try:
            count = int(float(row.get("count") or 0))
        except ValueError:
            count = 0
        out.append({"type": row.get("type") or "", "count": count})
    return out


def collect_top_processes(repo: str, limit: int = 30) -> List[Dict[str, Any]]:
    rows = cypher(
        repo,
        "MATCH (p:Process) "
        "RETURN p.heuristicLabel AS process, p.processType AS type, "
        "p.stepCount AS steps ORDER BY steps DESC "
        f"LIMIT {limit}",
    )
    out = []
    for row in rows:
        try:
            steps = int(float(row.get("steps") or 0))
        except ValueError:
            steps = 0
        out.append(
            {
                "process": row.get("process") or "",
                "type": row.get("type") or "",
                "steps": steps,
            }
        )
    return out


def collect_symbol_counts(repo: str) -> Dict[str, int]:
    return {
        "Class": cypher_scalar(repo, "MATCH (c:Class) RETURN count(c) AS count", "count"),
        "Method": cypher_scalar(repo, "MATCH (m:Method) RETURN count(m) AS count", "count"),
        "Interface": cypher_scalar(repo, "MATCH (i:Interface) RETURN count(i) AS count", "count"),
        "File": cypher_scalar(repo, "MATCH (f:File) RETURN count(f) AS count", "count"),
    }


def collect_routes(repo: str) -> List[Dict[str, str]]:
    rows = cypher(
        repo,
        "MATCH (h)-[:CodeRelation {type: 'HANDLES_ROUTE'}]->(route:Route) "
        "RETURN route.name AS path, route.method AS httpMethod, h.filePath AS handler "
        "ORDER BY path, httpMethod",
    )
    return [
        {
            "route": row.get("path") or "",
            "method": row.get("httpMethod") or "",
            "handler": row.get("handler") or "",
        }
        for row in rows
    ]


def collect_cycles(repo: str) -> Dict[str, Any]:
    result = run_gitnexus(["check", "--cycles", "--json", "-r", repo])
    raw = (result.stdout or "").strip()
    try:
        payload = json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        return {"status": "unknown", "cycleCount": 0, "cycles": [], "raw": raw}
    return {
        "status": payload.get("status", "unknown"),
        "cycleCount": int(payload.get("cycleCount") or 0),
        "cycles": payload.get("cycles") or [],
        "raw": raw,
    }


def parse_detect_changes(text: str) -> Dict[str, Any]:
    risk = "unknown"
    risk_match = re.search(r"Risk level:\s*(\w+)", text, re.IGNORECASE)
    if risk_match:
        risk = risk_match.group(1).lower()
    files = symbols = processes = 0
    summary = re.search(r"Changes:\s*(\d+)\s*files?,\s*(\d+)\s*symbols?", text, re.I)
    if summary:
        files, symbols = int(summary.group(1)), int(summary.group(2))
    proc_match = re.search(r"Affected processes:\s*(\d+)", text, re.I)
    if proc_match:
        processes = int(proc_match.group(1))
    changed: List[Dict[str, str]] = []
    in_symbols = False
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.lower().startswith("changed symbols"):
            in_symbols = True
            continue
        if in_symbols and stripped:
            m = re.match(r"(?:Symbol\s+)?(.+?)\s*→\s*(.+)$", stripped)
            if m:
                changed.append({"name": m.group(1).strip(), "file": m.group(2).strip()})
    return {
        "risk": risk,
        "changed_files": files,
        "changed_symbols": symbols,
        "affected_processes": processes,
        "symbols": changed,
        "raw": text.strip(),
    }


def collect_detect_changes(repo: str) -> Dict[str, Any]:
    result = run_gitnexus(["detect-changes", "-r", repo, "-s", "all"], timeout=180)
    text = (result.stdout or result.stderr or "").strip()
    if not text:
        return {
            "risk": "unknown",
            "changed_files": 0,
            "changed_symbols": 0,
            "affected_processes": 0,
            "symbols": [],
            "raw": "",
        }
    return parse_detect_changes(text)


def collect_snapshot(repo: str, refresh: bool) -> Dict[str, Any]:
    print("Collecting project stack...", file=sys.stderr)
    stack = collect_stack()
    print("Collecting architecture...", file=sys.stderr)
    arch = collect_architecture()
    print("Collecting complexity...", file=sys.stderr)
    complexity = collect_complexity()
    print("Collecting git state...", file=sys.stderr)
    git_state = collect_git()
    print("Collecting known risks...", file=sys.stderr)
    risks = collect_risks()

    print("Collecting GitNexus status...", file=sys.stderr)
    status = maybe_refresh(collect_status(), force=refresh)
    meta = load_meta()
    stats = meta.get("stats") or {}

    print("Collecting GitNexus clusters/processes/routes...", file=sys.stderr)
    clusters = collect_clusters(repo)
    process_mix = collect_process_mix(repo)
    top_processes = collect_top_processes(repo)
    symbol_counts = collect_symbol_counts(repo)
    routes = collect_routes(repo)
    cycles = collect_cycles(repo)
    changes = collect_detect_changes(repo)

    component_mermaid = build_component_mermaid(arch, routes)
    class_mermaid = build_class_mermaid(arch)

    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    last_commit = meta.get("lastCommit") or git_state.get("head") or ""
    short_commit = last_commit[:7] if last_commit else git_state.get("short", "")

    return {
        "repo": repo,
        "generated_at": generated_at,
        "stack": stack,
        "architecture": arch,
        "complexity": complexity,
        "git": git_state,
        "risks": risks,
        "component_mermaid": component_mermaid,
        "class_mermaid": class_mermaid,
        "status": status,
        "branch": git_state.get("branch") or meta.get("branch") or "",
        "last_commit": last_commit,
        "short_commit": short_commit,
        "indexed_at": meta.get("indexedAt") or "",
        "remote_url": sanitize_remote_url(meta.get("remoteUrl")) or git_state.get("remote") or "",
        "stats": {
            "files": int(stats.get("files") or 0),
            "nodes": int(stats.get("nodes") or 0),
            "edges": int(stats.get("edges") or 0),
            "communities": int(stats.get("communities") or 0),
            "processes": int(stats.get("processes") or 0),
        },
        "clusters": clusters,
        "process_mix": process_mix,
        "top_processes": top_processes,
        "symbol_counts": symbol_counts,
        "routes": routes,
        "cycles": cycles,
        "changes": changes,
    }


# ---------------------------------------------------------------------------
# HTML
# ---------------------------------------------------------------------------

def method_badge(method: str) -> str:
    m = (method or "").upper()
    cls = {
        "GET": "m-get",
        "POST": "m-post",
        "PUT": "m-put",
        "PATCH": "m-patch",
        "DELETE": "m-delete",
    }.get(m, "m-other")
    return f'<span class="badge {cls}">{esc(m)}</span>'


def risk_badge(risk: str) -> str:
    r = (risk or "unknown").lower()
    cls = {
        "low": "risk-low",
        "medium": "risk-medium",
        "high": "risk-high",
        "critical": "risk-critical",
        "moderate": "risk-medium",
        "very high": "risk-critical",
    }.get(r, "risk-unknown")
    return f'<span class="badge {cls}">{esc(r)}</span>'


def freshness_badge(status: Dict[str, Any]) -> str:
    if status.get("fresh"):
        return '<span class="badge fresh">index up-to-date</span>'
    if status.get("stale"):
        return '<span class="badge stale">index stale</span>'
    return '<span class="badge risk-unknown">index unknown</span>'


def cycle_badge(cycles: Dict[str, Any]) -> str:
    count = int(cycles.get("cycleCount") or 0)
    st = cycles.get("status") or "unknown"
    if count == 0 and st == "clean":
        return '<span class="badge fresh">clean — 0 cycles</span>'
    if count > 0:
        return f'<span class="badge risk-high">{esc(st)} — {count} cycles</span>'
    return f'<span class="badge risk-unknown">{esc(st)}</span>'


def css_bar(value: float, max_value: float) -> str:
    if max_value <= 0:
        width = 0
    else:
        width = max(2, int(round(100.0 * float(value) / float(max_value))))
    label = int(value) if float(value) == int(value) else f"{value:.1f}"
    return (
        f'<div class="bar-track"><div class="bar-fill" style="width:{width}%"></div>'
        f'<span class="bar-label">{label}</span></div>'
    )


def render_html(data: Dict[str, Any]) -> str:
    stack = data["stack"]
    arch = data["architecture"]
    complexity = data["complexity"]
    git_state = data["git"]
    risks = data["risks"]
    stats = data["stats"]
    symbol_counts = data["symbol_counts"]
    clusters = data["clusters"]
    process_mix = data["process_mix"]
    top_processes = data["top_processes"]
    routes = data["routes"]
    cycles = data["cycles"]
    changes = data["changes"]
    status = data["status"]

    # Stack rows
    stack_rows = []
    for h in stack.get("highlights") or []:
        stack_rows.append(
            "<tr>"
            f"<td>{esc(h['layer'])}</td>"
            f"<td>{esc(h['tech'])}</td>"
            f"<td><code>{esc(h['version'])}</code></td>"
            "</tr>"
        )

    dep_rows = []
    for d in stack.get("dependencies") or []:
        dep_rows.append(
            "<tr>"
            f"<td><code>{esc(d['groupId'])}</code></td>"
            f"<td><code>{esc(d['artifactId'])}</code></td>"
            f"<td><code>{esc(d['version'])}</code></td>"
            f"<td>{esc(d['scope'])}</td>"
            "</tr>"
        )

    # Architecture package table
    pkg_rows = []
    for layer, names in (arch.get("packages") or {}).items():
        pkg_rows.append(
            "<tr>"
            f"<td><code>{esc(layer)}</code></td>"
            f'<td class="num">{len(names)}</td>'
            f"<td>{esc(', '.join(names))}</td>"
            "</tr>"
        )

    # Complexity
    csum = complexity.get("summary") or {}
    top_c = complexity.get("top_methods") or []
    max_c = max((m["complexity"] for m in top_c), default=1)
    c_rows = []
    for m in top_c:
        c_rows.append(
            "<tr>"
            f"<td>{esc(m['class_name'])}</td>"
            f"<td><code>{esc(m['name'])}</code></td>"
            f'<td class="num">{m["complexity"]}</td>'
            f"<td>{css_bar(m['complexity'], max_c)}</td>"
            f"<td>{risk_badge(m.get('risk_level') or 'low')}</td>"
            f"<td><code>{esc(Path(m['file_path']).name if m.get('file_path') else '')}</code></td>"
            "</tr>"
        )

    # Git
    commit_rows = []
    for c in git_state.get("commits") or []:
        commit_rows.append(
            "<tr>"
            f"<td><code>{esc(c['hash'])}</code></td>"
            f"<td>{esc(c['date'])}</td>"
            f"<td>{esc(c['subject'])}</td>"
            "</tr>"
        )
    dirty_items = "".join(
        f"<li><code>{esc(ln)}</code></li>" for ln in (git_state.get("dirty_files") or [])
    ) or "<li class='muted'>Working tree clean (no listed dirty paths).</li>"

    # Risks
    risk_items = "".join(f"<li>{esc(r)}</li>" for r in risks) or "<li class='muted'>No risks section found.</li>"

    # GitNexus clusters / processes / routes
    max_cluster = max((c["symbols"] for c in clusters), default=1)
    cluster_rows = []
    for c in clusters:
        cluster_rows.append(
            "<tr>"
            f"<td>{esc(c['label'])}</td>"
            f'<td class="num">{c["symbols"]}</td>'
            f"<td>{css_bar(c['symbols'], max_cluster)}</td>"
            f'<td class="num">{c["cohesionPct"]:.1f}%</td>'
            "</tr>"
        )

    mix_items = []
    for m in process_mix:
        mix_items.append(
            f'<div class="mix-item"><strong>{esc(m["type"])}</strong>'
            f'<span class="num">{m["count"]}</span></div>'
        )

    process_rows = []
    for p in top_processes:
        type_cls = "type-cross" if "cross" in (p["type"] or "") else "type-intra"
        process_rows.append(
            "<tr>"
            f"<td>{esc(p['process'])}</td>"
            f'<td><span class="badge {type_cls}">{esc(p["type"])}</span></td>'
            f'<td class="num">{p["steps"]}</td>'
            "</tr>"
        )

    route_rows = []
    for r in sorted(routes, key=lambda x: (x["handler"], x["route"], x["method"])):
        handler_short = Path(r["handler"]).name if r["handler"] else ""
        route_rows.append(
            "<tr>"
            f"<td>{method_badge(r['method'])}</td>"
            f"<td><code>{esc(r['route'])}</code></td>"
            f'<td title="{esc(r["handler"])}">{esc(handler_short)}</td>'
            "</tr>"
        )

    max_symbol = max(symbol_counts.values()) if symbol_counts else 1
    symbol_bars = []
    for kind in ("Class", "Method", "Interface", "File"):
        count = symbol_counts.get(kind, 0)
        symbol_bars.append(
            f'<div class="symbol-row"><span class="symbol-kind">{esc(kind)}</span>'
            f"{css_bar(count, max_symbol)}</div>"
        )

    change_rows = []
    for s in changes.get("symbols") or []:
        change_rows.append(
            "<tr>"
            f"<td>{esc(s.get('name', ''))}</td>"
            f"<td><code>{esc(s.get('file', ''))}</code></td>"
            "</tr>"
        )
    if not change_rows:
        change_rows.append(
            '<tr><td colspan="2" class="muted">No changed symbols reported.</td></tr>'
        )

    artifact = stack.get("artifactId") or data["repo"]
    version = stack.get("version") or ""

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>Project Dashboard — {esc(artifact)}</title>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js"
        integrity="sha384-zkWMJO4sgpPUzyuOgDx8HB/K55glbAwajEpk1Go2NWRuPkPA/wIhoEJTuSkmOYrV"
        crossorigin="anonymous"
        referrerpolicy="no-referrer"></script>
<style>
:root {{
  --bg: #0f1419;
  --surface: #1a2332;
  --surface2: #243044;
  --border: #2d3a4f;
  --text: #e7ecf3;
  --muted: #8b9bb4;
  --accent: #3b82f6;
  --radius: 10px;
  --font: "Segoe UI", system-ui, -apple-system, sans-serif;
  --mono: ui-monospace, "Cascadia Code", "SF Mono", Menlo, Consolas, monospace;
}}
* {{ box-sizing: border-box; }}
html {{ scroll-behavior: smooth; }}
body {{
  margin: 0;
  font-family: var(--font);
  background: var(--bg);
  color: var(--text);
  line-height: 1.5;
}}
header {{
  padding: 1.75rem 1.5rem 1.25rem;
  background: linear-gradient(135deg, #152238 0%, #1a2332 55%, #0f1419 100%);
  border-bottom: 1px solid var(--border);
}}
header h1 {{ margin: 0 0 0.25rem; font-size: 1.55rem; font-weight: 650; }}
header .subtitle {{ color: var(--muted); margin-bottom: 0.65rem; font-size: 0.95rem; }}
header .meta {{
  color: var(--muted);
  font-size: 0.88rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem 1.15rem;
  align-items: center;
}}
header code, code {{ font-family: var(--mono); font-size: 0.84em; color: #c5d4eb; }}
nav.toc {{
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.85rem 1.25rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem 0.75rem;
  border-bottom: 1px solid var(--border);
  background: #121820;
  position: sticky;
  top: 0;
  z-index: 20;
}}
nav.toc a {{
  color: #93c5fd;
  text-decoration: none;
  font-size: 0.8rem;
  padding: 0.2rem 0.45rem;
  border-radius: 6px;
  border: 1px solid transparent;
}}
nav.toc a:hover {{ border-color: var(--border); background: var(--surface); }}
main {{
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.25rem 1.25rem 3rem;
}}
section {{
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.1rem 1.2rem 1.25rem;
  margin-bottom: 1.1rem;
}}
section h2 {{
  margin: 0 0 0.85rem;
  font-size: 1.08rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}}
section h3 {{
  margin: 1rem 0 0.55rem;
  font-size: 0.95rem;
  color: var(--muted);
  font-weight: 600;
}}
.cards {{
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 0.75rem;
}}
.card {{
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0.85rem 0.9rem;
}}
.card .label {{
  color: var(--muted);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}}
.card .value {{
  font-size: 1.45rem;
  font-weight: 700;
  margin-top: 0.15rem;
}}
.table-wrap {{ overflow-x: auto; }}
table {{
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}}
th, td {{
  text-align: left;
  padding: 0.42rem 0.5rem;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}}
th {{
  color: var(--muted);
  font-weight: 600;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  cursor: pointer;
  user-select: none;
}}
th:hover {{ color: var(--text); }}
th.sorted-asc::after {{ content: " ▲"; font-size: 0.7em; }}
th.sorted-desc::after {{ content: " ▼"; font-size: 0.7em; }}
td.num, th.num {{ text-align: right; font-variant-numeric: tabular-nums; }}
.muted {{ color: var(--muted); }}
.badge {{
  display: inline-block;
  padding: 0.12rem 0.5rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 650;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  background: var(--surface2);
  border: 1px solid var(--border);
}}
.fresh {{ background: #14532d33; color: #4ade80; border-color: #166534; }}
.stale {{ background: #7c2d1233; color: #fb923c; border-color: #9a3412; }}
.risk-low {{ background: #14532d33; color: #4ade80; border-color: #166534; }}
.risk-medium {{ background: #78350f33; color: #fbbf24; border-color: #92400e; }}
.risk-high, .risk-critical {{ background: #7f1d1d33; color: #f87171; border-color: #991b1b; }}
.risk-unknown {{ background: var(--surface2); color: var(--muted); }}
.m-get {{ background: #14532d33; color: #22c55e; border-color: #166534; }}
.m-post {{ background: #1e3a5f33; color: #60a5fa; border-color: #1d4ed8; }}
.m-put {{ background: #78350f33; color: #f59e0b; border-color: #92400e; }}
.m-patch {{ background: #581c8733; color: #c084fc; border-color: #6b21a8; }}
.m-delete {{ background: #7f1d1d33; color: #ef4444; border-color: #991b1b; }}
.type-cross {{ background: #1e3a5f33; color: #93c5fd; border-color: #1d4ed8; }}
.type-intra {{ background: #14532d33; color: #86efac; border-color: #166534; }}
.bar-track {{
  position: relative;
  height: 1.15rem;
  background: var(--bg);
  border-radius: 4px;
  overflow: hidden;
  min-width: 70px;
}}
.bar-fill {{
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #38bdf8);
  border-radius: 4px;
}}
.bar-label {{
  position: absolute;
  right: 6px;
  top: 0; bottom: 0;
  display: flex;
  align-items: center;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--text);
  text-shadow: 0 0 3px #000;
}}
.symbol-row {{
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 0.5rem;
}}
.symbol-kind {{ color: var(--muted); font-size: 0.85rem; }}
.mix {{ display: flex; flex-wrap: wrap; gap: 0.75rem; margin-bottom: 0.85rem; }}
.mix-item {{
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0.55rem 0.85rem;
  display: flex;
  gap: 0.75rem;
  align-items: baseline;
}}
.mix-item .num {{ font-size: 1.2rem; font-weight: 700; }}
.health-row {{
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.5rem;
}}
.mermaid {{
  background: #0b1220;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 1rem;
  overflow-x: auto;
}}
.mermaid-fallback {{
  font-family: var(--mono);
  font-size: 0.75rem;
  white-space: pre-wrap;
  color: var(--muted);
  background: var(--bg);
  border-radius: 6px;
  padding: 0.75rem;
  display: none;
}}
ul.risk-list, ul.dirty-list {{
  margin: 0.35rem 0 0;
  padding-left: 1.2rem;
}}
ul.risk-list li {{ margin-bottom: 0.35rem; }}
details summary {{
  cursor: pointer;
  color: var(--muted);
  font-size: 0.85rem;
}}
footer {{
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.25rem 2.5rem;
  color: var(--muted);
  font-size: 0.85rem;
}}
footer a {{ color: #60a5fa; }}
</style>
</head>
<body>
<header>
  <h1>ECCN Management Service — Project Dashboard</h1>
  <div class="subtitle">Engineering snapshot · stack, architecture, UML, API, complexity, git, risks, GitNexus</div>
  <div class="meta">
    <span><strong>{esc(artifact)}</strong>{f" <code>{esc(version)}</code>" if version else ""}</span>
    <span>branch <code>{esc(data['branch'])}</code></span>
    <span>HEAD <code title="{esc(data['last_commit'])}">{esc(data['short_commit'])}</code></span>
    <span>generated <code>{esc(data['generated_at'])}</code></span>
    <span>{freshness_badge(status)}</span>
  </div>
  {"<div class='meta' style='margin-top:0.45rem'><span>remote <code>" + esc(data['remote_url']) + "</code></span></div>" if data.get("remote_url") else ""}
</header>
<nav class="toc">
  <a href="#overview">Overview</a>
  <a href="#stack">Stack</a>
  <a href="#architecture">Architecture</a>
  <a href="#uml">UML</a>
  <a href="#api">API</a>
  <a href="#complexity">Complexity</a>
  <a href="#git">Git</a>
  <a href="#risks">Risks</a>
  <a href="#gitnexus">GitNexus</a>
</nav>
<main>

<section id="overview">
  <h2>Overview</h2>
  <div class="cards">
    <div class="card"><div class="label">Main Java files</div><div class="value">{arch.get('file_count', 0)}</div></div>
    <div class="card"><div class="label">Controllers</div><div class="value">{arch.get('counts', {}).get('controller', 0)}</div></div>
    <div class="card"><div class="label">Services</div><div class="value">{arch.get('counts', {}).get('service', 0)}</div></div>
    <div class="card"><div class="label">Repositories</div><div class="value">{arch.get('counts', {}).get('repository', 0)}</div></div>
    <div class="card"><div class="label">Models</div><div class="value">{arch.get('counts', {}).get('model', 0)}</div></div>
    <div class="card"><div class="label">API routes</div><div class="value">{len(routes)}</div></div>
    <div class="card"><div class="label">GN symbols</div><div class="value">{stats['nodes']}</div></div>
    <div class="card"><div class="label">GN processes</div><div class="value">{stats['processes']}</div></div>
    <div class="card"><div class="label">Max complexity</div><div class="value">{int(csum.get('max_complexity') or 0)}</div></div>
    <div class="card"><div class="label">Dirty paths</div><div class="value">{git_state.get('dirty_count', 0)}</div></div>
  </div>
</section>

<section id="stack">
  <h2>Tech stack &amp; versions</h2>
  <p class="muted" style="margin-top:0">From <code>pom.xml</code> (live parse — not tech-stack.md).</p>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Layer</th><th data-type="text">Technology</th><th data-type="text">Version</th></tr></thead>
    <tbody>
      {"".join(stack_rows) if stack_rows else '<tr><td colspan="3" class="muted">No stack data.</td></tr>'}
    </tbody>
  </table>
  </div>
  <details style="margin-top:0.85rem">
    <summary>All Maven dependencies ({len(stack.get('dependencies') or [])})</summary>
    <div class="table-wrap" style="margin-top:0.5rem">
    <table data-sortable>
      <thead><tr><th data-type="text">groupId</th><th data-type="text">artifactId</th><th data-type="text">version</th><th data-type="text">scope</th></tr></thead>
      <tbody>{"".join(dep_rows)}</tbody>
    </table>
    </div>
  </details>
</section>

<section id="architecture">
  <h2>Architecture / packages</h2>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Layer</th><th class="num" data-type="num">Types</th><th data-type="text">Names</th></tr></thead>
    <tbody>
      {"".join(pkg_rows) if pkg_rows else '<tr><td colspan="3" class="muted">No packages found.</td></tr>'}
    </tbody>
  </table>
  </div>
</section>

<section id="uml">
  <h2>UML diagrams <span class="badge">Mermaid</span></h2>
  <h3>Component diagram</h3>
  <pre class="mermaid">{esc(data['component_mermaid']).replace('&quot;', '"')}</pre>
  <details>
    <summary>Component diagram source</summary>
    <pre class="mermaid-fallback" style="display:block">{esc(data['component_mermaid'])}</pre>
  </details>
  <h3>Class diagram</h3>
  <pre class="mermaid">{esc(data['class_mermaid']).replace('&quot;', '"')}</pre>
  <details>
    <summary>Class diagram source</summary>
    <pre class="mermaid-fallback" style="display:block">{esc(data['class_mermaid'])}</pre>
  </details>
  <p class="muted" style="margin-bottom:0">Diagrams are derived from <code>src/main/java</code> package layout and <code>private final</code> injection edges. Mermaid is loaded from jsDelivr to render in-browser.</p>
</section>

<section id="api">
  <h2>API surface <span class="badge">{len(routes)} routes</span></h2>
  <p class="muted" style="margin-top:0">Indexed by GitNexus <code>HANDLES_ROUTE</code> (may omit some mappings such as health if not in the graph).</p>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Method</th><th data-type="text">Route</th><th data-type="text">Handler</th></tr></thead>
    <tbody>
      {"".join(route_rows) if route_rows else '<tr><td colspan="3" class="muted">No routes indexed.</td></tr>'}
    </tbody>
  </table>
  </div>
</section>

<section id="complexity">
  <h2>Cyclomatic complexity</h2>
  <div class="cards" style="margin-bottom:0.9rem">
    <div class="card"><div class="label">Files</div><div class="value">{int(csum.get('files_analyzed') or 0)}</div></div>
    <div class="card"><div class="label">Methods</div><div class="value">{int(csum.get('total_methods') or 0)}</div></div>
    <div class="card"><div class="label">Average</div><div class="value">{f"{float(csum.get('average_complexity') or 0):.2f}"}</div></div>
    <div class="card"><div class="label">Maximum</div><div class="value">{int(csum.get('max_complexity') or 0)}</div></div>
    <div class="card"><div class="label">Over threshold</div><div class="value">{int(csum.get('methods_over_threshold') or 0)}</div></div>
  </div>
  <h3>Highest-complexity methods</h3>
  <div class="table-wrap">
  <table data-sortable>
    <thead>
      <tr>
        <th data-type="text">Class</th>
        <th data-type="text">Method</th>
        <th class="num" data-type="num">v(G)</th>
        <th data-type="num">Bar</th>
        <th data-type="text">Risk</th>
        <th data-type="text">File</th>
      </tr>
    </thead>
    <tbody>
      {"".join(c_rows) if c_rows else '<tr><td colspan="6" class="muted">No complexity data.</td></tr>'}
    </tbody>
  </table>
  </div>
</section>

<section id="git">
  <h2>Git state</h2>
  <div class="health-row">
    <span>Branch <code>{esc(git_state.get('branch', ''))}</code></span>
    <span>HEAD <code>{esc(git_state.get('short', ''))}</code></span>
    <span class="muted">{esc(git_state.get('status_summary', ''))}</span>
  </div>
  <h3>Dirty / untracked</h3>
  <ul class="dirty-list">{dirty_items}</ul>
  <h3>Recent commits</h3>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Hash</th><th data-type="text">Date</th><th data-type="text">Subject</th></tr></thead>
    <tbody>{"".join(commit_rows)}</tbody>
  </table>
  </div>
</section>

<section id="risks">
  <h2>Known project risks / drift</h2>
  <p class="muted" style="margin-top:0">From <code>AGENTS.md</code> — Known Project Drift / Risks.</p>
  <ul class="risk-list">{risk_items}</ul>
</section>

<section id="gitnexus">
  <h2>GitNexus graph metrics</h2>
  <div class="cards" style="margin-bottom:0.9rem">
    <div class="card"><div class="label">Files</div><div class="value">{stats['files']}</div></div>
    <div class="card"><div class="label">Symbols</div><div class="value">{stats['nodes']}</div></div>
    <div class="card"><div class="label">Edges</div><div class="value">{stats['edges']}</div></div>
    <div class="card"><div class="label">Communities</div><div class="value">{stats['communities']}</div></div>
    <div class="card"><div class="label">Processes</div><div class="value">{stats['processes']}</div></div>
  </div>
  <p class="muted">Indexed at <code>{esc(data['indexed_at'])}</code> · {cycle_badge(cycles)}</p>

  <h3>Symbol breakdown</h3>
  {"".join(symbol_bars)}

  <h3>Functional clusters</h3>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Cluster</th><th class="num" data-type="num">Symbols</th><th data-type="num">Distribution</th><th class="num" data-type="num">Cohesion</th></tr></thead>
    <tbody>{"".join(cluster_rows)}</tbody>
  </table>
  </div>

  <h3>Process intelligence</h3>
  <div class="mix">{"".join(mix_items)}</div>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Process</th><th data-type="text">Type</th><th class="num" data-type="num">Steps</th></tr></thead>
    <tbody>{"".join(process_rows)}</tbody>
  </table>
  </div>

  <h3>Working-tree change impact</h3>
  <div class="health-row">
    <span>Risk: {risk_badge(changes.get('risk', 'unknown'))}</span>
    <span class="muted">{changes.get('changed_files', 0)} files · {changes.get('changed_symbols', 0)} symbols · {changes.get('affected_processes', 0)} affected processes</span>
  </div>
  <div class="table-wrap">
  <table data-sortable>
    <thead><tr><th data-type="text">Symbol</th><th data-type="text">File</th></tr></thead>
    <tbody>{"".join(change_rows)}</tbody>
  </table>
  </div>
</section>

</main>
<footer>
  <p>
    Regenerated with <code>python scripts/generate_gitnexus_dashboard.py</code>.
    Interactive graph exploration: <code>node .gitnexus/run.cjs serve</code> →
    <a href="https://gitnexus.vercel.app" rel="noopener noreferrer">gitnexus.vercel.app</a>.
  </p>
</footer>
<script>
mermaid.initialize({{
  startOnLoad: true,
  theme: "dark",
  securityLevel: "loose",
  flowchart: {{ htmlLabels: true, curve: "basis" }},
  classDiagram: {{ useMaxWidth: true }}
}});
(function () {{
  function cellValue(td, type) {{
    var text = (td.textContent || "").trim();
    if (type === "num") {{
      var m = text.replace(/[^0-9.+-]/g, "");
      var n = parseFloat(m);
      return isNaN(n) ? 0 : n;
    }}
    return text.toLowerCase();
  }}
  document.querySelectorAll("table[data-sortable]").forEach(function (table) {{
    var headers = table.querySelectorAll("thead th");
    headers.forEach(function (th, colIndex) {{
      th.addEventListener("click", function () {{
        var type = th.getAttribute("data-type") || "text";
        var tbody = table.tBodies[0];
        if (!tbody) return;
        var rows = Array.prototype.slice.call(tbody.rows);
        var asc = !th.classList.contains("sorted-asc");
        headers.forEach(function (h) {{ h.classList.remove("sorted-asc", "sorted-desc"); }});
        th.classList.add(asc ? "sorted-asc" : "sorted-desc");
        rows.sort(function (a, b) {{
          var av = cellValue(a.cells[colIndex], type);
          var bv = cellValue(b.cells[colIndex], type);
          if (av < bv) return asc ? -1 : 1;
          if (av > bv) return asc ? 1 : -1;
          return 0;
        }});
        rows.forEach(function (row) {{ tbody.appendChild(row); }});
      }});
    }});
  }});
}})();
</script>
</body>
</html>
"""


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate an engineering HTML project dashboard (includes GitNexus metrics)."
    )
    parser.add_argument("--output", "-o", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--repo", "-r", default=DEFAULT_REPO)
    parser.add_argument(
        "--refresh",
        action="store_true",
        help="Run gitnexus analyze before collecting (also runs if index is stale).",
    )
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    output: Path = args.output
    if not output.is_absolute():
        output = (Path.cwd() / output).resolve()

    print(f"Building project dashboard for repo={args.repo}...", file=sys.stderr)
    data = collect_snapshot(args.repo, refresh=args.refresh)

    # Mermaid in HTML: do not HTML-escape the diagram bodies inside <pre class="mermaid">
    # Re-render with unescaped mermaid blocks
    html_doc = render_html(data)
    # Fix double-escaping of mermaid: render_html currently escapes — patch by re-injecting
    html_doc = inject_raw_mermaid(html_doc, data["component_mermaid"], data["class_mermaid"])

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html_doc, encoding="utf-8")
    print(f"Wrote {output}", file=sys.stderr)

    s = data["stats"]
    csum = data["complexity"].get("summary") or {}
    print(
        f"Snapshot: java_files={data['architecture'].get('file_count', 0)} "
        f"routes={len(data['routes'])} "
        f"gn_symbols={s['nodes']} gn_processes={s['processes']} "
        f"max_complexity={csum.get('max_complexity')} "
        f"risks={len(data['risks'])} dirty={data['git'].get('dirty_count')}",
        file=sys.stderr,
    )
    return 0


def inject_raw_mermaid(html_doc: str, component: str, class_diag: str) -> str:
    """Replace escaped mermaid pre blocks with raw Mermaid source for the renderer."""
    # Find first two <pre class="mermaid">...</pre> and replace contents
    pattern = re.compile(r'(<pre class="mermaid">)(.*?)(</pre>)', re.S)
    sources = [component, class_diag]
    idx = 0

    def repl(match: re.Match) -> str:
        nonlocal idx
        if idx >= len(sources):
            return match.group(0)
        body = sources[idx]
        idx += 1
        return f"{match.group(1)}{body}{match.group(3)}"

    return pattern.sub(repl, html_doc, count=2)


if __name__ == "__main__":
    sys.exit(main())
