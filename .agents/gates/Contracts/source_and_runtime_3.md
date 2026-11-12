# Contract inventory 3

Parent: [Source and runtime contracts](source_and_runtime.md). Missing mandatory tooling means Blocked.

### 10d. Production Journey Contract
```bash
bash harness/scripts/tests/journey-test-contract-test.sh
```
**Must pass** for navigation, saved-state, back-stack, destination-recreation, and
post-return persistence regressions. It rejects direct-ViewModel or Content-only tests
when they are declared as production journey evidence and verifies that the stage gate
requires a named production-entry test, real UI gesture, return boundary, and visible
post-return assertion.

### 10e. Role-Profile Alignment Contract
```bash
bash harness/scripts/tests/role-profile-contract-test.sh
```
**Must pass** when role profiles, complex-feature workflows, or named skills change.
It rejects stale `docs/current` paths, obsolete task-selection instructions, invalid
template paths, and unavailable evaluator skill names in `.agents/agents/`.

### 11. Platform Capability Evidence (when a platform boundary is in scope)
```bash
# Generator: validates the selected slice's platform-boundary ownership.
bash harness/scripts/check-platform-evidence.sh "$FEATURE_DIR" --evaluate --slice "$FEATURE_ID"

# Final feature evaluation: validates every declared real boundary.
bash harness/scripts/check-platform-evidence.sh "$FEATURE_DIR" --evaluate
```
**Must pass.** A non-owning slice validates the declared contract without waiting for a later slice's boundary test. The boundary-owning slice and final feature evaluation reject missing capability matrices, pending/unavailable/skipped runtime evidence, and fake-only platform-boundary tests.

### 12. Visual Evidence Contract (when visual verification is required)
```bash
bash harness/scripts/check-visual-evidence-contract.sh "$FEATURE_DIR"
```
**Must pass.** Every visual verification method in the final owner's `feature_list.json` must have a matching `TC-*-VIS-*` row in `sprint-contract.md`, an acceptance-test ID, successful `verifyRoborazziDebug` evidence, and one matching reference-anchor row in `visual_evidence/reference-anchor-verification.md`. Each row names the approved Pen export, visual bounds `testTag`, JVM test, and concrete bounds relationship. Rows that claim rich-text or inline-formatting appearance additionally require the named `RoborazziTest` method to pass the source-fed rendered-output contract.

### 13. Keyboard State Contract (when a planned screen or bottom sheet has text input)
```bash
bash harness/scripts/check-keyboard-mockup-contract.sh "$FEATURE_DIR"
```
**Must pass during harness planning.** This compatibility command validates UI_contract.md through
the shared UI checker. Before Pen, pass `--inventory`: closed/open/dismissed state mappings and
explicit `screen-no-toolbar`, `screen-toolbar-dismissed`, or `sheet-stays-open` behavior are required.
After Pen, run without that flag to require distinct, full-window reference exports. The contract
also checks the focused input/required tags and the IME visible region. No design.md is read.

### 13a. IME State and Production Boundary Contract

```bash
bash harness/scripts/tests/ime-state-boundary-contract-test.sh
```

**Must pass for harness changes.** The UI/bug-fixing gates require an editable-input inventory,
same-window closed/open design frames with `system-ime` visible, and production Activity
open/dismiss tests even when `NAV` is Not applicable. Content replacement, generic test Activities,
missing/skipped/failed JUnit cases, and a command selecting the wrong class are rejected. Component
Roborazzi comparisons remain appearance evidence; they do not establish real IME window geometry.

## Conditional Checks

### Instrumented UI tests (when UI changed)
```bash
bash harness/scripts/run-android-instrumented-tests.sh --all
```
Run when the change modifies Composable screens or navigation. Set `ANDROID_SERIAL` to the
intended emulator/device; the runner installs APKs in place, runs `AndroidJUnitRunner` directly,
and verifies the app remains installed after the test.
