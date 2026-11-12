#!/usr/bin/env python3
"""Discover, install, verify and clean up an Android agent harness installation.

No LLM/API is embedded here: the onboarding workflow owns AI reasoning and user
approval. This tool supplies observed evidence and deterministic lifecycle checks.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from project_profile import PROFILE, inside, load_profile

STATE = ".harness-state"
MANIFEST = STATE + "/installation.json"
MAP = "docs/architecture/architecture-map.json"
MAP_MD = "docs/architecture/architecture-map.md"
IGNORED = {".git", ".gradle", ".harness", ".harness-state", ".agents", ".codex", "harness", "node_modules", "build", ".idea", ".qa-artifacts", ".scratch"}
SIGNALS = {
    "navigation": r"\b(NavHost|composable|NavController|navigate)\b",
    "persistence": r"\b(RoomDatabase|Room|Database|Dao|DataStore)\b",
    "networking": r"\b(Retrofit|OkHttpClient|HttpClient|GET|POST)\b",
    "dependency_injection": r"\b(HiltAndroidApp|HiltViewModel|Inject|Module|Koin|single)\b",
    "presentation": r"\b(ViewModel|StateFlow|UiState)\b",
}


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def is_directory_link(path: Path) -> bool:
    if path.is_symlink():
        return True
    return os.name == "nt" and path.exists() and bool(path.lstat().st_file_attributes & 0x400)


def create_directory_link(path: Path, target: Path) -> None:
    if os.name != "nt":
        path.symlink_to(os.path.relpath(target, path.parent), target_is_directory=True)
        return
    # Junctions require no symbolic-link privilege. Encode the PowerShell program
    # and quote literals so project names cannot become shell syntax.
    quote = lambda value: "'" + str(value).replace("'", "''") + "'"
    script = "$ErrorActionPreference='Stop'; New-Item -ItemType Junction -Path " + quote(path) + " -Target " + quote(target) + " | Out-Null"
    encoded = base64.b64encode(script.encode("utf-16le")).decode("ascii")
    subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded], check=True,
                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def link_fingerprint(path: Path) -> str:
    return os.readlink(path) if path.is_symlink() else str(path.resolve())


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    # A sibling temporary file makes interrupted writes leave the previous state intact.
    handle, name = tempfile.mkstemp(prefix=".harness-write-", dir=str(path.parent))
    try:
        with os.fdopen(handle, "w", encoding="utf-8", newline="\n") as stream:
            json.dump(data, stream, indent=2, sort_keys=True)
            stream.write("\n")
        os.replace(name, path)
    finally:
        if os.path.exists(name):
            os.unlink(name)


def inventory(root: Path) -> dict[str, str]:
    result = {}
    for current, directories, files in os.walk(root, followlinks=False):
        directories[:] = sorted(d for d in directories if d not in IGNORED and not is_directory_link(Path(current) / d))
        for name in sorted(files):
            path = Path(current) / name
            relative = path.relative_to(root).as_posix()
            if path.is_symlink() or relative in (MAP, MAP_MD):
                continue
            if (name in (PROFILE, "settings.gradle", "settings.gradle.kts", "build.gradle", "build.gradle.kts", "gradle.properties", "libs.versions.toml", "AndroidManifest.xml")
                    or path.suffix in (".kt", ".java")):
                result[relative] = digest(path)
    return result


def observe(root: Path) -> dict:
    hashes = inventory(root)
    builds = [p for p in hashes if Path(p).name in ("build.gradle", "build.gradle.kts")
              and (Path(p).parent != Path(".") or (root / "src").is_dir())]
    modules = []
    for build in builds:
        parent = Path(build).parent
        path = parent.as_posix()
        text = (root / build).read_text(encoding="utf-8")
        roots = {}
        for key, folder in (("main", "main"), ("test", "test"), ("android_test", "androidTest")):
            roots[key] = [p.relative_to(root).as_posix() for p in (root / parent / "src" / folder / "java", root / parent / "src" / folder / "kotlin") if p.is_dir()]
        if not roots["main"] and (root / parent / "src/main").is_dir():
            roots["main"] = [(parent / "src/main").as_posix()]
        layers = {}
        for source in roots["main"]:
            for directory in (root / source).rglob("*"):
                if directory.is_dir() and not directory.is_symlink() and directory.name in ("ui", "domain", "data"):
                    layers.setdefault(directory.name, []).append(directory.relative_to(root).as_posix())
        edges = []
        for number, line in enumerate(text.splitlines(), 1):
            for match in re.finditer(r"project\s*\(\s*(?:path\s*[:=]\s*)?[\"'](:[^\"']+)[\"']", line):
                edges.append({"target": match.group(1), "evidence": build + ":" + str(number)})
        modules.append({"name": ":" + ":".join(parent.parts), "path": path, **roots,
                        "layers": layers, "dependencies": edges, "evidence": build + ":1"})
    signals = {key: [] for key in SIGNALS}
    packages = []
    tests = []
    for name in hashes:
        if Path(name).suffix not in (".kt", ".java"):
            continue
        for number, line in enumerate((root / name).read_text(encoding="utf-8").splitlines(), 1):
            location = name + ":" + str(number)
            match = re.match(r"\s*package\s+([\w.]+)", line)
            if match:
                packages.append({"name": match.group(1), "evidence": location})
            for key, pattern in SIGNALS.items():
                if re.search(pattern, line):
                    signals[key].append({"evidence": location, "confidence": "lexical candidate; AI must inspect declaration"})
            if re.search(r"@Test\b|@RunWith\b", line):
                tests.append({"evidence": location})
    unknowns = ["Static discovery does not evaluate Gradle, convention plugins, aliases, projectDir remaps, custom source sets or runtime wiring.",
                "Layer names and dependency/service signals are candidates, not proof of architectural ownership.",
                "AI must reconcile every declared Gradle module and source set with the profile before approval."]
    if not modules:
        unknowns.append("No module build files found. Choose create mode or configure nonstandard/root-module layout explicitly.")
    return {"version": 1, "mode": "adopt" if modules else "create", "modules": modules,
            "signals": signals, "packages": packages, "test_entry_points": tests,
            "unknowns": unknowns, "fingerprints": hashes}


def manifest(root: Path) -> dict:
    inside(root, STATE)
    state = root / STATE
    state.mkdir(parents=True, exist_ok=True)
    local_ignore = state / ".gitignore"
    if not local_ignore.exists():
        local_ignore.write_bytes(b"*\n")
    path = root / MANIFEST
    if not path.exists():
        return {"version": 1, "root": str(root), "status": "pending", "files": []}
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("version") != 1 or data.get("root") != str(root):
        raise ValueError("installation manifest belongs to a different project")
    return data


def record(root: Path, path: Path, lifetime: str) -> None:
    data = manifest(root)
    name = path.relative_to(root).as_posix()
    linked = is_directory_link(path)
    entry = {"path": name, "lifetime": lifetime, "kind": "link" if linked else "file",
             "fingerprint": link_fingerprint(path) if linked else digest(path)}
    data["files"] = [item for item in data["files"] if item["path"] != name] + [entry]
    write_json(root / MANIFEST, data)


def owned_write(root: Path, name: str, content: str, replace: bool = False) -> None:
    path = inside(root, name)
    if path.exists():
        entries = [item for item in manifest(root)["files"] if item["path"] == name]
        if not replace:
            return
        if not entries or path.is_symlink() or digest(path) != entries[0]["fingerprint"]:
            raise ValueError("refusing to overwrite user-owned/modified file: " + name)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    record(root, path, "persistent")


def discover(root: Path, refresh: bool = False) -> dict:
    data = observe(root)
    profile_path = root / PROFILE
    if not profile_path.exists():
        profile = {"version": 1, "modules": [{key: value for key, value in m.items() if key not in ("dependencies", "evidence")} for m in data["modules"]],
                   "commands": {}, "navigation": {}, "unknowns": data["unknowns"]}
        owned_write(root, PROFILE, json.dumps(profile, indent=2) + "\n")
    data["fingerprints"] = inventory(root)
    if (root / MAP).exists() and not refresh:
        previous = json.loads((root / MAP).read_text(encoding="utf-8"))
        if previous.get("fingerprints") != data["fingerprints"]:
            raise ValueError("stale architecture map; use discover --refresh explicitly")
    owned_write(root, MAP, json.dumps(data, indent=2, sort_keys=True) + "\n", refresh)
    lines = ["# Observed architecture", "", "This is source evidence, not approved architecture policy.", "",
             "## Modules", "", "| Candidate module | Build evidence | Dependencies |", "|---|---|---|"]
    for module in data["modules"]:
        lines.append("| " + module["name"] + " | `" + module["evidence"] + "` | " + ", ".join(edge["target"] + " (`" + edge["evidence"] + "`)" for edge in module["dependencies"]) + " |")
    for key, entries in data["signals"].items():
        lines.extend(["", "## " + key.replace("_", " ").title(), ""])
        lines.extend("- `" + item["evidence"] + "` — lexical candidate; inspect before asserting ownership." for item in entries)
        if not entries:
            lines.append("Unknown: no lexical candidate found.")
    lines.extend(["", "## Unknowns", ""] + ["- " + item for item in data["unknowns"]])
    owned_write(root, MAP_MD, "\n".join(lines) + "\n", refresh)
    return data


def check(root: Path) -> None:
    load_profile(root, required=True)
    path = root / MAP
    if not path.is_file():
        raise ValueError("missing architecture map; run discover")
    old = json.loads(path.read_text(encoding="utf-8"))
    if old.get("fingerprints") != inventory(root):
        raise ValueError("stale architecture map; run discover --refresh and review changes")


def prepare(root: Path, harness: Path) -> None:
    if root == harness:
        raise ValueError("choose a consuming project root; do not install the harness into itself")
    for name in (".agents", "harness"):
        path = root / name
        target = harness / name
        if not target.is_dir():
            raise ValueError("missing harness installation directory: " + str(target))
        if is_directory_link(path):
            if path.resolve() != target.resolve():
                raise ValueError("conflicting existing symlink: " + str(path))
        elif path.exists():
            raise ValueError("preserving existing path; resolve installation conflict: " + str(path))
        else:
            create_directory_link(path, target)
            record(root, path, "persistent")
    # Install generic policy, never a fabricated project identity.
    owned_write(root, "AGENTS.md", (harness / "AGENTS.md").read_text(encoding="utf-8"))
    if not (root / "AGENTS.md").read_text(encoding="utf-8").strip():
        raise ValueError("existing AGENTS.md is empty; restore project instructions before continuing")
    owned_write(root, "setup_prompt.md", (harness / "setup_prompt.md").read_text(encoding="utf-8"))
    temporary = root / STATE / "setup-session.txt"
    temporary.parent.mkdir(parents=True, exist_ok=True)
    if not temporary.exists():
        temporary.write_text("Temporary setup session; removed only after successful verification.\n", encoding="utf-8")
        record(root, temporary, "temporary")
    data = discover(root, refresh=(root / MAP).exists())
    print("Onboarding pending (" + data["mode"] + "). Open setup_prompt.md in your AI coding agent.")
    print("No application code, product approvals, builds, or design evidence were fabricated.")


def cleanup(root: Path, uninstall: bool = False, apply: bool = False) -> list[dict]:
    data = manifest(root)
    if not uninstall and data["status"] != "verified":
        raise ValueError("temporary cleanup requires successful onboarding verification")
    results = []
    actions = []
    for item in data["files"]:
        if not uninstall and item["lifetime"] != "temporary":
            continue
        # Resolve the parent, never the final symlink: unlink only the installed link.
        relative = Path(item["path"])
        if relative.is_absolute() or ".." in relative.parts or re.match(r"^[A-Za-z]:", item["path"]):
            raise ValueError("unsafe manifest path")
        parent = inside(root, relative.parent.as_posix())
        path = parent / relative.name
        exists = is_directory_link(path) or path.exists()
        same = exists and ((item["kind"] == "link" and is_directory_link(path) and link_fingerprint(path) == item["fingerprint"])
                           or (item["kind"] == "file" and path.is_file() and not is_directory_link(path) and digest(path) == item["fingerprint"]))
        action = "remove" if same else "preserve-modified" if exists else "already-absent"
        if same:
            actions.append((path, item["kind"]))
        results.append({"path": item["path"], "action": action})
    # Validate the complete manifest before the first destructive action.
    if apply:
        for path, kind in actions:
            if kind == "link" and not path.is_symlink():
                path.rmdir()  # Remove a Windows junction itself, never its target.
            else:
                path.unlink()  # Never recursively delete directories or shared dependencies.
    write_json(root / STATE / ("uninstall-receipt.json" if uninstall else "cleanup-receipt.json"), {"applied": apply, "files": results})
    return results


def verify(root: Path, harness: Path) -> None:
    check(root)
    profile = load_profile(root, required=True)
    approval_path = root / "docs/onboarding/approval.json"
    approval = json.loads(approval_path.read_text(encoding="utf-8"))
    if (not approval.get("approved_by") or not approval.get("approved_at")
            or "<" in str(approval.get("approved_by")) or "<" in str(approval.get("approved_at"))
            or approval.get("profile_sha256") != digest(root / PROFILE)):
        raise ValueError("missing/stale user approval for project profile")
    plan = inside(root, approval.get("plan", "docs/current/implementation_plan_v1.md"))
    if not plan.is_file() or approval.get("plan_sha256") != digest(plan):
        raise ValueError("missing/stale approved onboarding implementation plan")
    for document in ("docs/product/product.md", "docs/product/design_system.md"):
        if not (root / document).is_file() or not (root / document).read_text(encoding="utf-8").strip():
            raise ValueError("missing product documentation: " + document)
    required = ("build", "unit_tests", "source_rules")
    if any(key not in profile["commands"] for key in required):
        raise ValueError("profile requires build, unit_tests and source_rules command arrays")
    if profile.get("unknowns"):
        raise ValueError("resolve profile unknowns before verification")
    data = manifest(root)
    data["status"] = "verifying"
    write_json(root / MANIFEST, data)
    receipts = []
    commands = [("lifecycle", ["bash", str(harness / "harness/scripts/check-feature-lifecycle.sh"), "--allow-empty"])]
    # Source policy cannot be replaced by a profile command that simply exits 0.
    commands.append(("source_rules", ["bash", str(harness / "harness/scripts/check-full-source-rules.sh"), "--project-root", str(root)]))
    commands.extend((key, argv) for key, argv in profile["commands"].items() if key != "source_rules")
    test_started_ns = 0
    previous_reports = {}
    for key, argv in commands:
        if key == "unit_tests":
            test_started_ns = time.time_ns()
            previous_reports = {str(report): (report.stat().st_mtime_ns, digest(report))
                                for module in profile["modules"]
                                for report in inside(root, module["path"]).glob("build/test-results/**/TEST-*.xml")}
        print("Running " + key + ": " + repr(argv), flush=True)
        environment = os.environ.copy()
        environment["HARNESS_PYTHON"] = Path(sys.executable).as_posix()
        environment["PYTHONIOENCODING"] = "utf-8"
        result = subprocess.run(argv, cwd=str(root), env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
        log = root / STATE / (key + ".log")
        log.write_text(result.stdout, encoding="utf-8")
        receipts.append({"gate": key, "argv": argv, "exit_code": result.returncode, "log": log.relative_to(root).as_posix(), "log_sha256": digest(log)})
        write_json(root / STATE / "verification.json", {"status": "running" if result.returncode == 0 else "failed", "commands": receipts})
        if result.returncode:
            raise ValueError(key + " failed; see " + str(log))
    # Require source-fed, non-zero test evidence rather than trusting process exit alone.
    counts = 0
    report_receipts = []
    for module in profile["modules"]:
        for report in inside(root, module["path"]).glob("build/test-results/**/TEST-*.xml"):
            signature = (report.stat().st_mtime_ns, digest(report))
            if previous_reports.get(str(report)) == signature or report.stat().st_mtime_ns < test_started_ns - 2_000_000_000:
                raise ValueError("stale test report; rerun unit tests: " + str(report))
            tree = ET.parse(str(report)).getroot()
            if int(tree.attrib.get("failures", "0")) or int(tree.attrib.get("errors", "0")):
                raise ValueError("test report contains failures: " + str(report))
            cases = tree.findall(".//testcase")
            if any(case.find("failure") is not None or case.find("error") is not None for case in cases):
                raise ValueError("test case failed: " + str(report))
            if any(case.find("skipped") is not None for case in cases) or int(tree.attrib.get("skipped", "0")):
                raise ValueError("skipped tests cannot pass onboarding: " + str(report))
            counts += len(cases)
            report_receipts.append({"path": report.relative_to(root).as_posix(), "sha256": signature[1], "executed_tests": len(cases)})
    if counts <= 0:
        raise ValueError("unit test gate produced no executed tests in Gradle XML reports")
    check(root)
    data = manifest(root)
    data["status"] = "verified"
    write_json(root / MANIFEST, data)
    write_json(root / STATE / "verification.json", {"status": "passed", "commands": receipts, "executed_tests": counts,
                                                   "test_reports": report_receipts, "fingerprints": inventory(root)})
    cleanup(root, apply=True)
    print("Onboarding verified; unchanged temporary setup files removed.")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", type=Path, default=Path.cwd())
    parser.add_argument("--harness-root", type=Path, default=Path(__file__).resolve().parents[2])
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("prepare")
    discovery = commands.add_parser("discover")
    discovery.add_argument("--refresh", action="store_true")
    commands.add_parser("check")
    commands.add_parser("verify")
    removal = commands.add_parser("cleanup")
    removal.add_argument("--uninstall", action="store_true")
    removal.add_argument("--apply", action="store_true")
    args = parser.parse_args(argv)
    root, harness = args.project_root.resolve(), args.harness_root.resolve()
    try:
        if not root.is_dir():
            raise ValueError("project root must be an existing directory")
        if args.command == "prepare":
            prepare(root, harness)
        elif args.command == "discover":
            data = discover(root, args.refresh)
            print("Observed " + str(len(data["modules"])) + " candidate modules; architecture map recorded.")
        elif args.command == "check":
            check(root)
            print("PASS: project profile and architecture fingerprints")
        elif args.command == "verify":
            verify(root, harness)
        else:
            print(json.dumps(cleanup(root, args.uninstall, args.apply), indent=2))
    except (OSError, ValueError, KeyError, TypeError, ET.ParseError, subprocess.CalledProcessError) as error:
        if args.command == "verify" and (root / MANIFEST).is_file():
            data = manifest(root)
            data["status"] = "failed"
            write_json(root / MANIFEST, data)
        print("FAIL: " + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
