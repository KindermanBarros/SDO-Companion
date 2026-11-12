#!/usr/bin/env python3
"""Portable onboarding contracts; Android/toolchain execution is not simulated as product evidence."""
from __future__ import annotations

import contextlib
import io
import json
import os
from pathlib import Path
import shutil
import shlex
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
import project_onboarding as onboarding
import project_profile as profiles
import kotlin_ast_checker as ast


class OnboardingContracts(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="harness-onboarding-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name).resolve() / "consumer"
        self.root.mkdir()
        self.harness = Path(self.temporary.name) / "shared-harness"
        (self.harness / ".agents").mkdir(parents=True)
        (self.harness / "harness/scripts").mkdir(parents=True)
        (self.harness / "AGENTS.md").write_text("# Generic harness policy\n", encoding="utf-8")
        (self.harness / "setup_prompt.md").write_text("# Customize my product\n", encoding="utf-8")
        self.output = io.StringIO()
        self.redirect = contextlib.redirect_stdout(self.output)
        self.redirect.__enter__()
        self.addCleanup(self.redirect.__exit__, None, None, None)

    def put(self, name, content):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path

    def module(self, name="mobile", language="kotlin"):
        self.put(name + "/build.gradle.kts", 'plugins { id("com.android.application") }\n')
        self.put(name + "/src/main/" + language + "/example/domain/Logic.kt", "package example.domain\nclass Logic\n")
        self.put(name + "/src/test/" + language + "/example/LogicTest.kt", "package example\nclass LogicTest { @Test fun works() { assertEquals(1, 1) } }\n")
        self.put("settings.gradle.kts", 'include(":' + name.replace("/", ":") + '")\n')

    def profile(self):
        return json.loads((self.root / profiles.PROFILE).read_text(encoding="utf-8"))

    def save_profile(self, data):
        onboarding.write_json(self.root / profiles.PROFILE, data)

    def discover(self):
        return onboarding.discover(self.root)

    def ready(self):
        """A harness fixture with real subprocess gates and a report-writing fake test tool."""
        self.module()
        self.discover()
        profile = self.profile()
        writer = self.put("test-tool.py", "from pathlib import Path\n"
                          "p = Path('mobile/build/test-results/testDebugUnitTest/TEST-Fixture.xml')\n"
                          "p.parent.mkdir(parents=True, exist_ok=True)\n"
                          "p.write_text('<testsuite tests=\"1\" failures=\"0\" errors=\"0\"><testcase name=\"fixture\"/></testsuite>')\n")
        profile["commands"] = {"build": [sys.executable, "-c", "print('fixture build')"],
                               "unit_tests": [sys.executable, str(writer)],
                               "source_rules": [sys.executable, "-c", "raise SystemExit(99)"]}
        profile["unknowns"] = []
        self.save_profile(profile)
        self.put("docs/product/product.md", "# Fixture product\n")
        self.put("docs/product/design_system.md", "# Approved fixture design\n")
        plan = self.put("docs/current/implementation_plan_v1.md", "# Fixture approved plan\n")
        onboarding.write_json(self.root / "docs/onboarding/approval.json", {
            "approved_by": "contract-test-user", "approved_at": "2026-11-12T00:00:00Z",
            "plan": plan.relative_to(self.root).as_posix(), "plan_sha256": onboarding.digest(plan),
            "profile_sha256": onboarding.digest(self.root / profiles.PROFILE)})
        for name in ("check-feature-lifecycle.sh", "check-full-source-rules.sh"):
            (self.harness / "harness/scripts" / name).write_text("#!/usr/bin/env bash\nexit 0\n", encoding="utf-8")
        state = self.root / onboarding.STATE
        state.mkdir(exist_ok=True)
        temp = self.put(onboarding.STATE + "/setup-session.txt", "fixture temporary\n")
        onboarding.record(self.root, temp, "temporary")
        onboarding.discover(self.root, refresh=True)

    def verify(self):
        # Git for Windows bash is not always on Windows PATH. No silent skips.
        bash = shutil.which("bash")
        if not bash and os.name == "nt":
            candidate = Path("C:/Program Files/Git/bin/bash.exe")
            if candidate.is_file():
                os.environ["PATH"] = str(candidate.parent) + os.pathsep + os.environ["PATH"]
                bash = str(candidate)
        self.assertTrue(bash, "bash is required for onboarding verification contracts")
        onboarding.verify(self.root, self.harness)

    def shell(self, script, *arguments, cwd=None):
        bash = shutil.which("bash")
        if not bash and os.name == "nt":
            bash = "C:/Program Files/Git/bin/bash.exe"
        self.assertTrue(bash and Path(bash).is_file(), "bash is required; shell contracts cannot be skipped")
        tools = Path(self.temporary.name) / "tools"
        tools.mkdir(exist_ok=True)
        shim = tools / "python3"
        shim.write_text("#!/usr/bin/env bash\nexec " + shlex.quote(Path(sys.executable).as_posix()) + ' -B "$@"\n', encoding="utf-8")
        shim.chmod(0o755)
        environment = os.environ.copy()
        environment["PATH"] = str(tools) + os.pathsep + environment["PATH"]
        environment["PYTHONIOENCODING"] = "utf-8"
        return subprocess.run([bash, str(script), *arguments], cwd=str(cwd or SCRIPTS.parents[1]),
                              env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              encoding="utf-8", errors="replace")

    def test_instrumentation_contract_accepts_foreign_app_identifiers(self):
        self.module()
        self.discover()
        profile = self.profile()
        profile["instrumentation"] = {"application_id": "com.kinderman.sdo", "test_application_id": "com.kinderman.sdo.test", "runner": "androidx.test.runner.AndroidJUnitRunner"}
        self.save_profile(profile)
        self.assertEqual(profiles.load_profile(self.root)["instrumentation"], profile["instrumentation"])

    def test_instrumentation_contract_rejects_shell_and_component_fragments(self):
        self.module()
        self.discover()
        profile = self.profile()
        for invalid in ("com.kinderman.sdo;echo unsafe", "com.kinderman.sdo/Runner", "com.kinderman.*", "", None):
            with self.subTest(identifier=invalid):
                profile["instrumentation"] = {"application_id": invalid, "test_application_id": "com.kinderman.sdo.test", "runner": "androidx.test.runner.AndroidJUnitRunner"}
                self.save_profile(profile)
                with self.assertRaisesRegex(ValueError, "qualified Java names"):
                    profiles.load_profile(self.root)

    def test_instrumentation_contract_rejects_incomplete_fields(self):
        self.module()
        self.discover()
        profile = self.profile()
        profile["instrumentation"] = {"application_id": "com.kinderman.sdo"}
        self.save_profile(profile)
        with self.assertRaisesRegex(ValueError, "requires application_id"):
            profiles.load_profile(self.root)

    def test_empty_directory_selects_creation_without_inventing_application(self):
        observed = self.discover()
        self.assertEqual("create", observed["mode"])
        self.assertEqual([], observed["modules"])
        self.assertFalse((self.root / "app").exists())
        with self.assertRaisesRegex(ValueError, "at least one module"):
            onboarding.check(self.root)

    def test_existing_app_records_file_line_and_dependency_evidence(self):
        self.module()
        self.put("mobile/build.gradle.kts", 'dependencies { implementation(project(":core")) }\n')
        self.put("mobile/src/main/kotlin/example/data/Storage.kt", "package example.data\n@Database class Storage : RoomDatabase()\n")
        observed = self.discover()
        self.assertEqual("adopt", observed["mode"])
        self.assertEqual(":core", observed["modules"][0]["dependencies"][0]["target"])
        self.assertEqual("mobile/build.gradle.kts:1", observed["modules"][0]["dependencies"][0]["evidence"])
        self.assertTrue(observed["signals"]["persistence"])
        onboarding.check(self.root)

    def test_multiple_nested_modules_and_both_source_languages(self):
        self.module("mobile", "java")
        self.module("features/search", "kotlin")
        self.discover()
        self.assertEqual({":mobile", ":features:search"}, {m["name"] for m in self.profile()["modules"]})
        self.assertEqual(2, len(profiles.source_roots(self.root, "main")))

    def test_discovery_does_not_overwrite_edited_profile(self):
        self.module()
        self.discover()
        profile = self.profile()
        profile["unknowns"] = ["My choice pending"]
        self.save_profile(profile)
        onboarding.discover(self.root, refresh=True)
        self.assertEqual(["My choice pending"], self.profile()["unknowns"])

    def test_added_modified_and_deleted_sources_invalidate_map(self):
        self.module()
        self.discover()
        path = self.put("mobile/src/main/kotlin/example/domain/New.kt", "class New\n")
        with self.assertRaisesRegex(ValueError, "stale architecture"):
            onboarding.check(self.root)
        onboarding.discover(self.root, refresh=True)
        path.write_text("class Changed\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "stale architecture"):
            onboarding.check(self.root)
        onboarding.discover(self.root, refresh=True)
        path.unlink()
        with self.assertRaisesRegex(ValueError, "stale architecture"):
            onboarding.check(self.root)

    def test_user_edited_architecture_map_is_preserved(self):
        self.module()
        self.discover()
        edited = self.put(onboarding.MAP_MD, "# My annotations\n")
        with self.assertRaisesRegex(ValueError, "user-owned/modified"):
            onboarding.discover(self.root, refresh=True)
        self.assertEqual("# My annotations\n", edited.read_text(encoding="utf-8"))

    def test_external_and_traversal_profile_paths_are_rejected(self):
        for name in ("../escape", "C:/escape", "/escape", "..\\escape"):
            with self.subTest(path=name), self.assertRaises(ValueError):
                profiles.inside(self.root, name)

    def test_source_symlink_escape_is_rejected(self):
        outside = Path(self.temporary.name) / "outside"
        outside.mkdir()
        onboarding.create_directory_link(self.root / "escape", outside)
        with self.assertRaisesRegex(ValueError, "outside project"):
            profiles.inside(self.root, "escape/source")

    def test_invalid_profile_cannot_fall_back_to_legacy_app(self):
        self.put(profiles.PROFILE, '{"version": 999}')
        with self.assertRaisesRegex(ValueError, "unsupported"):
            profiles.source_roots(self.root, "main")

    def test_missing_configured_source_root_fails(self):
        self.module()
        self.discover()
        profile = self.profile()
        profile["modules"][0]["main"] = ["mobile/src/nonexistent"]
        self.save_profile(profile)
        with self.assertRaisesRegex(ValueError, "missing configured"):
            profiles.load_profile(self.root)

    def test_shell_string_commands_are_rejected(self):
        self.module()
        self.discover()
        profile = self.profile()
        profile["commands"] = {"build": "echo passed"}
        self.save_profile(profile)
        with self.assertRaisesRegex(ValueError, "argument array"):
            profiles.load_profile(self.root)

    def test_command_names_cannot_escape_log_directory(self):
        self.module()
        self.discover()
        profile = self.profile()
        profile["commands"] = {"../../escape": ["echo", "bad"]}
        self.save_profile(profile)
        with self.assertRaisesRegex(ValueError, "invalid/reserved command name"):
            profiles.load_profile(self.root)

    def test_prepare_rerun_preserves_user_prompt_and_agent_instructions(self):
        self.put("AGENTS.md", "# My rules\n")
        self.put("setup_prompt.md", "# My app preferences\n")
        onboarding.prepare(self.root, self.harness)
        first = onboarding.manifest(self.root)
        onboarding.prepare(self.root, self.harness)
        self.assertEqual(first["files"], onboarding.manifest(self.root)["files"])
        self.assertEqual("# My app preferences\n", (self.root / "setup_prompt.md").read_text(encoding="utf-8"))
        self.assertEqual("# My rules\n", (self.root / "AGENTS.md").read_text(encoding="utf-8"))
        self.assertFalse(any(item["path"] in ("setup_prompt.md", "AGENTS.md") for item in first["files"]))

    def test_interrupted_prepare_can_resume_without_replacing_files(self):
        conflict = self.root / "harness"
        conflict.mkdir()
        with self.assertRaisesRegex(ValueError, "conflict"):
            onboarding.prepare(self.root, self.harness)
        self.assertTrue(onboarding.is_directory_link(self.root / ".agents"))
        conflict.rmdir()
        onboarding.prepare(self.root, self.harness)
        self.assertTrue(onboarding.is_directory_link(self.root / "harness"))
        self.assertEqual("pending", onboarding.manifest(self.root)["status"])

    def test_cleanup_is_blocked_before_verification(self):
        self.discover()
        with self.assertRaisesRegex(ValueError, "successful onboarding"):
            onboarding.cleanup(self.root, apply=True)

    def test_successful_verify_runs_real_processes_and_cleans_only_temporary(self):
        self.ready()
        self.verify()
        self.assertFalse((self.root / onboarding.STATE / "setup-session.txt").exists())
        self.assertTrue((self.root / profiles.PROFILE).exists())
        self.assertTrue((self.root / "docs/onboarding/approval.json").exists())
        receipt = json.loads((self.root / onboarding.STATE / "verification.json").read_text(encoding="utf-8"))
        self.assertEqual("passed", receipt["status"])
        self.assertEqual(1, receipt["executed_tests"])
        self.assertEqual(["lifecycle", "source_rules", "build", "unit_tests"], [c["gate"] for c in receipt["commands"]])

    def test_failed_verification_keeps_setup_files(self):
        self.ready()
        (self.harness / "harness/scripts/check-full-source-rules.sh").write_text("exit 7\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "source_rules failed"):
            self.verify()
        self.assertTrue((self.root / onboarding.STATE / "setup-session.txt").exists())

    def test_approval_fingerprints_must_match_exact_plan_and_profile(self):
        self.ready()
        self.put("docs/current/implementation_plan_v1.md", "# Changed after approval\n")
        with self.assertRaisesRegex(ValueError, "approved onboarding implementation plan"):
            self.verify()

    def test_empty_or_stale_test_output_cannot_pass(self):
        self.ready()
        self.put("test-tool.py", "print('success without tests')\n")
        with self.assertRaisesRegex(ValueError, "no executed tests"):
            self.verify()
        self.put("mobile/build/test-results/testDebugUnitTest/TEST-Old.xml", '<testsuite tests="1"><testcase name="old"/></testsuite>')
        with self.assertRaisesRegex(ValueError, "stale test report"):
            self.verify()

    def test_skipped_test_cases_cannot_supply_nonzero_evidence(self):
        self.ready()
        self.put("test-tool.py", "from pathlib import Path\n"
                 "p=Path('mobile/build/test-results/testDebugUnitTest/TEST-Skip.xml')\n"
                 "p.parent.mkdir(parents=True, exist_ok=True)\n"
                 "p.write_text('<testsuite tests=\"1\"><testcase name=\"skip\"><skipped/></testcase></testsuite>')\n")
        with self.assertRaisesRegex(ValueError, "skipped tests"):
            self.verify()

    def test_uninstall_preview_and_apply_preserve_user_edits_and_shared_harness(self):
        onboarding.prepare(self.root, self.harness)
        self.put("setup_prompt.md", "# User edited\n")
        preview = onboarding.cleanup(self.root, uninstall=True)
        self.assertTrue(onboarding.is_directory_link(self.root / "harness"))
        self.assertIn({"path": "setup_prompt.md", "action": "preserve-modified"}, preview)
        onboarding.cleanup(self.root, uninstall=True, apply=True)
        self.assertFalse(onboarding.is_directory_link(self.root / "harness"))
        self.assertTrue((self.harness / "harness/scripts").is_dir())
        self.assertEqual("# User edited\n", (self.root / "setup_prompt.md").read_text(encoding="utf-8"))
        self.assertTrue((self.root / onboarding.MANIFEST).exists())

    def test_tampered_manifest_cannot_delete_outside_root(self):
        self.discover()
        data = onboarding.manifest(self.root)
        data["files"].append({"path": "../outside", "lifetime": "persistent", "kind": "file", "fingerprint": "x"})
        onboarding.write_json(self.root / onboarding.MANIFEST, data)
        with self.assertRaisesRegex(ValueError, "unsafe manifest"):
            onboarding.cleanup(self.root, uninstall=True, apply=True)

    def test_architecture_checker_finds_violation_in_non_app_module(self):
        self.module("features/search")
        self.put("features/search/src/main/kotlin/example/domain/Bad.kt", "package example.domain\nimport android.content.Context\nclass Bad\n")
        self.discover()
        result = ast.main(["architecture", "--project-root", str(self.root), "--all"])
        self.assertEqual(1, result)
        self.assertIn("domain layer must not import Android", self.output.getvalue())

    def test_legacy_source_roots_are_unchanged_when_profile_absent(self):
        self.assertEqual([self.root / "app/src/main/java"], profiles.source_roots(self.root, "main"))

    def test_custom_navigation_has_no_notes_app_destination_requirements(self):
        self.module()
        paths = {
            "destinations": self.put("mobile/src/main/kotlin/example/Destinations.kt", 'sealed class Destinations(val route: String) { object Home : Destinations("home") }\n'),
            "graph": self.put("mobile/src/main/kotlin/example/AppNavGraph.kt", "fun AppNavGraph() {}\n"),
            "host": self.put("mobile/src/main/kotlin/example/AppNavigationHost.kt", "fun AppNavigationHost() {}\n"),
            "test": self.put("mobile/src/androidTest/kotlin/example/NavigationTest.kt", "fun fixture() { AppNavigationHost() }\n"),
            "contract_test": self.put("mobile/src/androidTest/kotlin/example/NavigationContractTest.kt", "fun fixture() { AppNavigationHost() }\n"),
        }
        self.put(".agents/rules/navigation-rules.md", "# Navigation policy fixture\n")
        self.discover()
        profile = self.profile()
        profile["navigation"] = {key: path.relative_to(self.root).as_posix() for key, path in paths.items()}
        profile["navigation"].update({"required_arguments": {}, "optional_arguments": {}})
        self.save_profile(profile)
        self.assertEqual(0, ast.main(["navigation", "--project-root", str(self.root)]))
        result = self.shell(SCRIPTS / "check-full-source-rules.sh", "--project-root", str(self.root))
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("all checkers passed", result.stdout)

    def test_initializer_cli_installs_editable_prompt_without_existing_gradle_project(self):
        result = self.shell(SCRIPTS / "init-harness.sh", "--project-root", str(self.root))
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("Onboarding pending (create)", result.stdout)
        prompt = self.root / "setup_prompt.md"
        self.assertIn("## My project", prompt.read_text(encoding="utf-8"))
        prompt.write_text("# My customized first prompt\n", encoding="utf-8")
        result = self.shell(SCRIPTS / "init-harness.sh", "--project-root", str(self.root))
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertEqual("# My customized first prompt\n", prompt.read_text(encoding="utf-8"))
        self.assertFalse((self.root / "app").exists())

    def test_dynamic_tags_require_registry_only_when_used(self):
        self.module()
        path = self.put("mobile/src/main/kotlin/example/ui/Tag.kt",
                        'fun StaticTag() { Modifier.testTag("static") }\n')
        self.discover()
        self.assertEqual(0, ast.main(["compose", "--project-root", str(self.root), "--all"]))
        path.write_text('fun DynamicTag(id: String) { Modifier.testTag("item_$id") }\n', encoding="utf-8")
        self.assertEqual(1, ast.main(["compose", "--project-root", str(self.root), "--all"]))
        self.assertIn("Missing dynamic test-tag registry", self.output.getvalue())

    def test_production_journey_checker_accepts_configured_instrumented_root(self):
        self.module()
        test = self.put("mobile/src/androidTest/kotlin/example/JourneyTest.kt", """class JourneyTest {
    @Test fun opensAndReturns() {
        composeRule.setContent { AppNavigationHost() }
        composeRule.onNodeWithTag("open").performClick()
        composeRule.onNodeWithTag("select").performClick()
        composeRule.onNodeWithTag("result").assertIsDisplayed()
    }
}
""")
        self.discover()
        self.assertEqual(0, ast.main(["journey", "--project-root", str(self.root), "--test-file", str(test),
                                     "--test-method", "opensAndReturns", "--production-entry", "AppNavigationHost"]))

    def test_existing_ast_navigation_security_and_http_contracts(self):
        for name in ("ast-checkers-contract-test.sh", "navigation-rules-contract-test.sh",
                     "ai-security-rules-contract-test.sh", "android-test-http-boundary-contract-test.sh"):
            with self.subTest(contract=name):
                result = self.shell(SCRIPTS / "tests" / name)
                self.assertEqual(0, result.returncode, result.stdout)

    def test_full_source_bundle_scans_profile_module_and_aggregates_failures(self):
        self.module("features/search")
        self.put("features/search/src/main/kotlin/example/ui/Bad.kt",
                 '@Composable fun Bad() { Button(onClick = {}) { Text("Visible text") } }\n')
        self.discover()
        result = self.shell(SCRIPTS / "check-full-source-rules.sh", "--project-root", str(self.root))
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("features/search/src/main/kotlin/example/ui/Bad.kt", result.stdout.replace("\\", "/"))
        for gate in ("Architecture rules", "Compose rules", "Localization rules", "Navigation rules", "Test assertion rules", "AI security rules"):
            self.assertIn(">> " + gate, result.stdout)
        self.assertIn("one or more checkers failed", result.stdout)
        self.assertNotIn("Python was not found", result.stdout)

    def test_lifecycle_allows_empty_only_in_explicit_onboarding_mode(self):
        tracker = self.put("docs/product/product.md", "# Product\n<!-- HARNESS_TRACKER_START -->\n"
                           "| ID | Feature | Workspace | Status | Updated | Notes |\n"
                           "|---|---|---|---|---|---|\n<!-- HARNESS_TRACKER_END -->\n")
        result = self.shell(SCRIPTS / "check-feature-lifecycle.sh", str(tracker), cwd=self.root)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("no feature rows", result.stdout)
        result = self.shell(SCRIPTS / "check-feature-lifecycle.sh", str(tracker), "--allow-empty", cwd=self.root)
        self.assertEqual(0, result.returncode, result.stdout)
        tracker.write_text("# Malformed tracker\n", encoding="utf-8")
        result = self.shell(SCRIPTS / "check-feature-lifecycle.sh", str(tracker), "--allow-empty", cwd=self.root)
        self.assertNotEqual(0, result.returncode)

    def test_new_profile_source_security_checks_non_app_module(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location("security_checker", SCRIPTS / "check-ai-security-rules.py")
        security = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(security)
        self.module("mobile")
        self.put("mobile/src/main/AndroidManifest.xml", '<application android:usesCleartextTraffic="true"/>')
        self.discover()
        findings = security.collect_findings(self.root)
        self.assertIn("AISEC-CLEARTEXT", [item["id"] for item in findings])


if __name__ == "__main__":
    unittest.main(verbosity=2)
