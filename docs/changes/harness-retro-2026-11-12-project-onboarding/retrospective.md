Historical imported harness-export record. Its no-Gradle/no-Git environment and script test results describe that export, not the SDO Android repository. Retained for provenance; no active gate depends on this record.

# Project onboarding, architecture discovery and setup lifecycle

## Incident and classification

Classification: `WORKFLOW_GAP`.

The initializer copied a notes-app identity, assumed a consuming project existed,
installed browser dependencies unconditionally, and had no installation ownership
manifest or post-verification cleanup. The generator required a compiling baseline
before implementation. Source checks also assumed `app/src` and notes destinations.
These constraints prevented an AI agent from owning a new project's foundation.

## Authorization and scope

The user requested all five proposed improvements and explicitly approved the
protected file list and implementation outline with "Yes" on 2026-11-12. The user
then directly requested root `setup_prompt.md` and instructed continuation under
the existing approval. The protected instruction files changed are exactly:

- `AGENTS.md`
- `.agents/workflows/project-onboarding.md` (new)
- `.agents/skills/project-onboarding/SKILL.md` (new)
- `.agents/skills/harness-router/SKILL.md`
- `.agents/skills/context-management/SKILL.md`
- `.agents/rules/android-architecture.md`
- `.agents/workflows/harness-generator.md`

Supporting scope includes installer/discovery/profile/cleanup code, source-checker
integration, examples, contract tests, README setup guidance and this audit record.
No application source, product capability, lifecycle tracker or memory record changed.
The Skill invocation interface was unavailable; `harness-router` and
`harness-retrospective` were read from their repository `SKILL.md` files.

## Invariants and implementation evidence

1. Observed architecture is source evidence, never implicit policy approval.
   `harness/scripts/project_onboarding.py` records candidate modules, literal project
   dependencies, packages, service/navigation/DI/persistence signals, tests and hashes.
   Excerpt: `"Layer names and dependency/service signals are candidates, not proof of architectural ownership."`
2. AI can adopt an app or author an approved new foundation before the generator's
   existing baseline gate. `.agents/workflows/project-onboarding.md` owns the stages.
   Excerpt: `"It does not waive any gate after creation."`
3. Paths and commands come from validated `harness-project.json`, with legacy
   defaults retained when no profile exists. `harness/scripts/project_profile.py`
   rejects missing/escaping roots, invalid command shapes/names and malformed contracts.
   Architecture, Compose, localization, navigation, assertions, journey/IME source,
   HTTP-fixture and AI-security checks consume configured roots/contracts.
4. Cleanup requires successful verification and removes only unchanged owned files.
   Excerpt from `project_onboarding.py`: `"Validate the complete manifest before the first destructive action."`
   Uninstall previews persistent removal; apply never recursively removes directories.
   Real native directory junctions support Windows without symbolic-link privileges.
   The manifest/receipts remain local under `.harness-state/` with their own ignore file.
5. Process success alone cannot prove tests. Verification checks actual command exits,
   fresh non-zero Gradle XML test cases, no failures/skips, approval hashes and source
   fingerprints. The canonical source-rule gate cannot be replaced by profile commands.
6. User prompt/preferences survive reruns. Root `setup_prompt.md` is copied only when
   absent. Excerpt: `"This file is an instruction prompt, not a script"`.

The generator refreshes the map after implementation/test source changes and reviews
observed boundaries against the approved slice. Discovery never rewrites the profile.
The lifecycle validator permits an empty initial tracker only with explicit
`--allow-empty`; all default lifecycle checks remain strict.

## Regression findings resolved

- Windows WinError 1314 during real symbolic-link fixtures: added actual junction
  installation/removal and reran the same filesystem-boundary contracts.
- Compose required a missing dynamic-tag registry for apps with no dynamic tags:
  registry enforcement now activates only for actual dynamic-tag calls. Missing
  registries still fail when dynamic tags are used; no exclusions were introduced.
- Full-source root discovery ran before parsing explicit root overrides: explicit
  consumer roots now work from a standalone harness checkout and Groovy settings
  are recognized alongside Kotlin settings.

## Verification

Commands and complete output are retained in `verification.log` and
`syntax-checks.log` beside this record. Run from the harness root:

- Contract result: exit `0`, **32 tests passed**, no skips. Exact log excerpt:
  `Ran 32 tests in 23.009s`, followed by `OK`.
- Syntax result: exit `0`. Exact excerpts: `PASS: Python syntax (5 files)`,
  `PASS: Bash syntax (10 files)`, `PASS: JSON examples (2 files)` and
  `PASS: PowerShell full-source bundle syntax`.
- Index/whitespace result: `PASS: AGENTS index length (119 lines)` and
  `PASS: changed code/template/prompt whitespace`.

```text
python3 -B harness/scripts/tests/project-onboarding-contract-test.py
```

The actual local interpreter was `C:/Users/kindb/Python/python.exe` (Python 3.8.5),
with `C:/Program Files/Git/bin/bash.exe` for shell contracts. The test harness
provided a temporary Python launcher for legacy shell contracts; the production
source-check adapters honor `HARNESS_PYTHON`, and onboarding passes its interpreter
to the canonical gate. No dependencies were downloaded.

The suite exercises empty/existing/multiple-module layouts; source-map freshness;
approval and command boundaries; interrupted setup/reruns; real Windows links;
temporary cleanup/uninstall preservation; custom navigation/journey roots; passing
and failing full-source bundles; and the existing AST, navigation, AI-security and
HTTP-boundary shell contracts. The fixture subprocesses used to test base verifier
control flow are test doubles, not evidence of an Android application build.

Shell syntax, Python parsing, example JSON and native PowerShell syntax are checked
separately. No app build, Kover, lint or device test is claimed: this exported harness
contains no Gradle application or Android source. `git diff --check`/PR creation
cannot run because this directory has no `.git` metadata or configured origin.

## Remaining boundaries

Static discovery does not evaluate Gradle, convention plugins, aliases, projectDir
remaps or arbitrary custom source sets. AI reconciles these before profile approval.
Specialized visual/device runners still require their approved module configuration;
an unsupported runtime remains blocked. Browser/SDK dependencies are installed only
through an approved consuming-project plan. No external jobs or shared caches are
installed or removed implicitly. Existing user-edited maps are preserved and a
refresh errors instead of overwriting them.

The existing description-budget test has unrelated stale inventory assumptions
(39 descriptions versus 46 before onboarding) and references to consumer-only
documentation. That baseline mismatch was observed, not silently rewritten or
reported as passing; it is outside this approved protected-file scope.
