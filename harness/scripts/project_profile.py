"""Shared, dependency-free project configuration for harness checks.

Profiles describe locations and commands, never exemptions from source rules.
Absent profiles retain the historical app/ layout for existing consumers.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

PROFILE = "harness-project.json"


def inside(root: Path, name: str) -> Path:
    if not isinstance(name, str) or not name or "\\" in name:
        raise ValueError("profile paths must be non-empty relative POSIX paths")
    relative = Path(name)
    if relative.is_absolute() or ".." in relative.parts or re.match(r"^[A-Za-z]:", name):
        raise ValueError("profile path escapes project: " + name)
    path = (root / relative).resolve()
    try:
        path.relative_to(root.resolve())
    except ValueError:
        raise ValueError("profile path resolves outside project: " + name)
    return path


def load_profile(root: Path, required: bool = False) -> dict | None:
    path = inside(root, PROFILE)
    if not path.exists():
        if required:
            raise ValueError("missing harness-project.json; run project onboarding")
        return None
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("version") != 1:
        raise ValueError("unsupported project profile version")
    modules = data.get("modules")
    if not isinstance(modules, list) or not modules:
        raise ValueError("profile must declare at least one module")
    names = set()
    for module in modules:
        if not isinstance(module, dict) or not isinstance(module.get("name"), str):
            raise ValueError("each profile module requires a name")
        if module["name"] in names:
            raise ValueError("duplicate profile module: " + module["name"])
        names.add(module["name"])
        module_path = inside(root, module.get("path"))
        if not module_path.is_dir():
            raise ValueError("missing module directory: " + module["path"])
        if not any((module_path / name).is_file() for name in ("build.gradle", "build.gradle.kts")):
            raise ValueError("module has no Gradle build file: " + module["path"])
        for key in ("main", "test", "android_test"):
            roots = module.get(key)
            if not isinstance(roots, list) or (key == "main" and not roots):
                raise ValueError("module requires source-root arrays; main must be non-empty")
            for item in roots:
                path = inside(root, item)
                if not path.is_dir():
                    raise ValueError("missing configured source directory: " + item)
                try:
                    path.relative_to(module_path)
                except ValueError:
                    raise ValueError("source directory is outside its module: " + item)
        layers = module.get("layers", {})
        if not isinstance(layers, dict) or set(layers) - {"ui", "domain", "data"}:
            raise ValueError("layers must map ui/domain/data to source directories")
        for layer_roots in layers.values():
            if not isinstance(layer_roots, list):
                raise ValueError("layer roots must be arrays")
            for item in layer_roots:
                layer = inside(root, item)
                if not layer.is_dir() or not any(is_under(layer, inside(root, p)) for p in module["main"]):
                    raise ValueError("layer directory must exist inside a main source root: " + item)
    commands = data.get("commands")
    if not isinstance(commands, dict):
        raise ValueError("commands must be an object of argument arrays")
    for name, argv in commands.items():
        if not re.fullmatch(r"[a-z][a-z0-9_]*", name) or name == "lifecycle":
            raise ValueError("invalid/reserved command name: " + name)
        if not isinstance(argv, list) or not argv or any(not isinstance(arg, str) or not arg.strip() for arg in argv):
            raise ValueError("command must be a non-empty argument array: " + name)
    if not isinstance(data.get("unknowns"), list) or any(not isinstance(item, str) for item in data["unknowns"]):
        raise ValueError("profile requires an unknowns string array")
    nav = data.get("navigation", {})
    if not isinstance(nav, dict):
        raise ValueError("navigation must be an object")
    for key in ("destinations", "graph", "host", "test", "contract_test"):
        if key in nav and not inside(root, nav[key]).is_file():
            raise ValueError("missing navigation file: " + nav[key])
    for key in ("required_arguments", "optional_arguments"):
        if key in nav and (not isinstance(nav[key], dict) or any(
            not isinstance(values, list) or any(not isinstance(v, str) for v in values)
            for values in nav[key].values()
        )):
            raise ValueError("navigation arguments must map destination names to string arrays")
    instrumentation = data.get("instrumentation")
    if instrumentation is not None:
        if not isinstance(instrumentation, dict) or set(instrumentation) != {"application_id", "test_application_id", "runner"}:
            raise ValueError("instrumentation requires application_id, test_application_id and runner")
        for value in instrumentation.values():
            if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+", value, re.ASCII):
                raise ValueError("instrumentation identifiers must be qualified Java names")
    return data


def is_under(path: Path, parent: Path) -> bool:
    try:
        path.resolve().relative_to(parent.resolve())
        return True
    except ValueError:
        return False


def source_roots(root: Path, kind: str) -> list[Path]:
    profile = load_profile(root)
    if profile is None:
        suffix = {"main": "main/java", "test": "test", "android_test": "androidTest"}[kind]
        return [root / "app/src" / suffix]
    return sorted({inside(root, item) for module in profile["modules"] for item in module[kind]})


def layer_roots(root: Path, layer: str) -> list[Path]:
    profile = load_profile(root)
    if profile is None:
        return []
    return [inside(root, item) for module in profile["modules"] for item in module.get("layers", {}).get(layer, [])]


def main_directories(root: Path) -> list[Path]:
    """Resource/manifest roots, alongside configured Kotlin/Java roots."""
    profile = load_profile(root)
    if profile is None:
        return [root / "app/src/main"]
    result = set()
    for module in profile["modules"]:
        for item in module["main"]:
            path = inside(root, item)
            result.add(path.parent if path.name in ("java", "kotlin") else path)
    return sorted(result)


if __name__ == "__main__":
    import argparse
    import sys

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", type=Path, default=Path.cwd())
    parser.add_argument("--roots", choices=("main", "test", "android_test", "main_directories"))
    parser.add_argument("--instrumentation", action="store_true")
    args = parser.parse_args()
    try:
        root = args.project_root.resolve()
        profile = load_profile(root, required=True)
        if args.instrumentation:
            config = profile.get("instrumentation")
            if config is None:
                raise ValueError("profile requires an explicit instrumentation contract")
            for key in ("application_id", "test_application_id", "runner"):
                print(config[key])
        elif args.roots:
            roots = main_directories(root) if args.roots == "main_directories" else source_roots(root, args.roots)
            for directory in roots:
                print(directory.as_posix())
        else:
            print("PASS: project profile paths and command shapes")
    except (OSError, ValueError, TypeError, KeyError) as error:
        print("FAIL: " + str(error), file=sys.stderr)
        sys.exit(1)
