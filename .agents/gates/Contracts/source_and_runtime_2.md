# Contract inventory 2

Parent: [Source and runtime contracts](source_and_runtime.md). Missing mandatory tooling means Blocked.

### 8. Navigation Rules
```bash
bash harness/scripts/check-navigation-rules.sh
bash harness/scripts/tests/navigation-rules-contract-test.sh
```
**Must pass.** Catches raw route literals and prefixes, unencoded dynamic arguments,
invalid required/optional argument defaults, and missing production navigation tests.

### 8a. Kotlin AST Checker Contract
```bash
bash harness/scripts/tests/ast-checkers-contract-test.sh
```
**Must pass.** Verifies the shared AST-backed Compose, localization, architecture,
navigation, and rendering-assertion entry points ignore rule-shaped comments/literals
and reject actual violations.

### 8b. Full Source Rules Bundle Contract
```bash
bash harness/scripts/tests/full-source-rules-contract-test.sh
```
**Must pass.** Proves the bundle forces full-source scans, runs every checker even
when an earlier checker fails, and fails on an untouched source violation.

### 9. Rule Applicability Harness Contract
```bash
bash harness/scripts/tests/rule-applicability-contract-test.sh
```
**Must pass.** Ensures requirement artifacts carry all ten rule decisions and that the
stage gate rejects incomplete matrices.

### 9a. Separate UX Design Stage Contract
```bash
python3 harness/scripts/tests/ux-design-stage-contract-test.py
```
**Must pass.** Requirements lead to a complete UI_contract inventory before Pen generation.
Inventory gates allow only design-reference fields to be Pending; full UX/planning gates require
completed references and matching runtime target metadata. No design.md is required.

### 10. Acceptance-Test Traceability Contract
bash harness/scripts/check-acceptance-test-traceability.sh "$FEATURE_DIR" --evaluate
bash harness/scripts/tests/acceptance-test-traceability-contract-test.sh

**Must pass** before a harness slice is marked tested or evaluated. It proves that
each acceptance Test ID maps to one declared Kotlin test method, a suite-scoped Gradle
or instrumented selector, its declared shared JSON scenario(s), and successful
evidence. When the row explicitly claims rich-text or inline-formatting appearance,
the same gate invokes the rendered-output contract for the named JVM Roborazzi method.

### 10.0 Rendered Output Evidence Contract
```bash
bash harness/scripts/check-rendered-output-contract.sh \
  --project-root "$PWD" \
  --test-file app/src/test/java/<package>/roborazzi/<Feature>RoborazziTest.kt \
  --test-method <method> \
  --claim "Following text visibly inherits Bold"
bash harness/scripts/tests/rendered-output-contract-test.sh
```
**Must pass** for any acceptance, reproduction, or visual row that claims formatted
text is visibly rendered. The named `*RoborazziTest` method must call
`captureRoboImage()` against the approved Pen export. ViewModel marks, toolbar state,
text content, and device screenshots are supplemental only. The acceptance,
visual-evidence, and bug-reproduction gates invoke this contract automatically where
the claim is explicit.

### 10.1 Production Journey Planning Contract
```bash
bash harness/scripts/check-journey-planning-contract.sh "$FEATURE_DIR"
bash harness/scripts/tests/journey-planning-contract-test.sh
```
**Must pass during harness planning.** It requires every slice to classify whether
it crosses a production navigation/lifecycle boundary, mirrors that decision in
`feature_list.json`, and assigns navigation/lifecycle acceptance rows to a named
production-entry instrumented journey with real actions, a return boundary, and a
visible post-return assertion.

### 10a. Evaluation/Fix Lifecycle Contract
bash harness/scripts/check-evaluation-fix-contract.sh "$FEATURE_DIR" --evaluation
bash harness/scripts/tests/review-lifecycle-contract-test.sh

**Must pass** whenever evaluator/fix workflows, review templates, or their artifact
validators change. It rejects score/routing mismatches, contradictory successful
evidence, and fix passes that advance beyond a blocked stage or leave review findings
without in-report resolution status.

### 10b. Gate Failure Stop Contract
```bash
bash harness/scripts/tests/gate-failure-stop-contract-test.sh
```
**Must pass.** Ensures generator and fix workflows stop immediately when a required gate
fails or its prerequisite is unavailable.

### 10c. Project Documentation Root Contract
```bash
bash harness/scripts/tests/project-doc-location-contract-test.sh
```
**Must pass** when stage artifact location handling changes. Consumer project artifacts are
accepted under the consumer repository's root `docs/` and rejected under the nested shared
`android-agent-harness/docs/` tree.
