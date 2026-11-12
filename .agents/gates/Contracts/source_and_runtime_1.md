# Contract inventory 1

Parent: [Source and runtime contracts](source_and_runtime.md). Missing mandatory tooling means Blocked.

# CI Checks

## Purpose
Defines the minimum set of checks that must pass before a change is considered ready to merge.

---

## Required Checks

### 1. Build
```bash
./gradlew assembleDebug
```
**Must pass.** A failing build is a hard blocker.

### 2. Unit and Integration Tests
```bash
./gradlew testDebugUnitTest
```
**Must pass.** All declared JVM tests must be green. No Roborazzi suite is configured in the observed SDO baseline.

### 3. Coverage
```bash
./gradlew :app:koverXmlReportDebug
./gradlew :app:koverHtmlReportDebug
./gradlew :app:koverXmlReportDebug
bash harness/scripts/check-coverage.sh app/build/reports/kover/reportDebug.xml
bash harness/scripts/tests/coverage-contract-test.sh
```
**Must pass threshold:**
- Overall project: ≥ 80% line coverage
- New ViewModel classes: ≥ 90%
- New domain use case classes: ≥ 90%

The coverage checker computes weighted line coverage from the Kover XML report and
supports explicit per-file thresholds with `--min-file <path>=<percent>`.

### 4. Ktlint (formatting)
```bash
./gradlew ktlintCheck
```
**Must pass.** Auto-fix with `./gradlew ktlintFormat` before committing.

### 5. Detekt (static analysis)
```bash
./gradlew detekt
```
**Must pass** for errors. Warnings are informational.

### 6. Android Lint
```bash
./gradlew lintDebug
```
**Must pass** for errors. Review warnings in changed files.

### 7. Compose Rules
```bash
bash harness/scripts/check-compose-rules.sh --all
```
Windows:
```powershell
harness\scripts\check-compose-rules.cmd --all
```
**Must pass.** Catches Compose-specific violations not covered by Ktlint/Detekt:
- Hardcoded strings (must use `stringResource()`)
- Hardcoded colors (must use `the screen's existing theme tokens`)
- Interactive elements without `Modifier.testTag(...)`
- `hiltViewModel()` / `viewModel()` used inside `*Content` composables
- Repository / UseCase calls inside Composables
- Unstable `testTag` values (string interpolation)
- `Column` + `forEach` instead of `LazyColumn`

### 7a. Full Source Rules Bundle
```bash
bash harness/scripts/check-full-source-rules.sh
```
Windows:
```powershell
harness\scripts\check-full-source-rules.cmd
```
**Must pass.** This is the required repository-wide source-rule gate used by
generator, evaluation, fix, and CI flows. It passes `--all` to the architecture,
Compose, and localization AST checkers, scans both test roots for assertion quality,
runs navigation checks, executes every checker after earlier failures, and returns
non-zero if any checker reports a violation. Individual checker commands are useful
for diagnosis but cannot replace this bundle as evidence.

The bundle also runs the AI/WebView security evaluator and its negative-case
contract test. These checks are mandatory in every flow that invokes the
full-source bundle; no separate workflow invocation is required. The evaluator
rejects unsafe cleartext, WebView, Mermaid, AI-input logging, and untrusted-output
sink patterns, while the contract test proves unsafe fixtures fail and reports do
not leak fixture content.

### 7b. Android Security Code Rules

Changes that touch an Android security boundary must load
`.agents/rules/android-security.md` and carry its boundary, failure-mode, and test
evidence into the review. The full-source bundle is the canonical CI enforcement
entry point; Android Lint, Detekt, Ktlint, and the applicable instrumented boundary
tests remain required. A missing runtime or unavailable platform boundary is
blocked evidence, not a passing check.
