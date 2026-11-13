---
description: Update SDO UI from approved visual states and verify production behavior and appearance.
---
# Workflow: Create UI and Verify

Read [Project contract](../gates/project-contract.md). Planner owns references/plan; implementer changes UI; tester verifies; fresh evaluator reviews.

## Entry

Use for any visible screen/component/style/layout/interaction change. For a reported defect, use [Bug fixing](bug-fixing.md).
Keep small tasks proportionate by reusing references that cover the changed states; do not omit approval or verification.
Read docs/product/design_system.md and the screen's current Standard/Cybergrunge theme.

## 1. Reference design — ux-design

Clarify behavior and inventory every affected state before generating visuals.
Create UI_contract.md with stable state IDs, viewport, controls, fixture data and IME behavior.
Classify supplied images as approved references or defect evidence; preserve originals separately.
Generate missing design states with pen-design-editor, export and inspect each PNG, including fonts/icons.
Capture export output; font/asset/glyph errors fail the reference gate even if the export process exits zero.
Do not turn an implementation screenshot into an approved golden.
Obtain design approval before implementation planning proceeds.

For editable inputs, inventory closed/open/dismissed states in the same full-window viewport, including system-ime and reachable controls.
A cropped component or keyboard preview cannot prove production window geometry.

## 2. Implementation plan — implementation-plan

Bind each Plan stage ID to an implementation step, approved image, named visual test/method and verification command.
The test plan also declares production Activity IME open/dismiss tests and real JUnit report locations when applicable.
Keep the approved spec stable during visual iterations unless behavior or scope changes.
Obtain implementation approval before application edits.

## 3. Implementation — android-ui-layer

Reuse existing tokens/components and manual wiring; follow .docs architecture and code standards.
Any design-system exception must be explicitly approved and recorded in UI_contract.md.
Follow the adopted persona's testing rules. Do not create test dependencies silently.

## 4. Verification — ui-verification and android-testing

Capture mandatory device screenshots using Android CLI or adb and record the selected runtime/build identity.
Compare declared states to their approved Pen exports and retain actual/compare artifacts on failure.
For spatial claims, assert tagged visual bounds and concrete relationships/tolerances, not just outer touch targets.
Production navigation/lifecycle/IME claims require shipped-entry instrumented tests; screenshots do not replace those boundaries.
When Roborazzi is required, verify its configured task and dependency first. It is not installed in the observed baseline; missing infrastructure blocks that proof.
A declared rendered-text claim requires the named captureRoboImage test and source-fed rendered-output validation.
Do not claim visual acceptance from screenshot existence alone.

Loop from verification to implementation within scope, at most three failed repair iterations; then surface remaining deviations.

## 5. Quality, review and handoff

Apply [CI checks](../gates/ci-checks.md) and [Feature review](feature-review.md).
Review UI inventory, approved references, implementation plan and verification evidence directly.
Inspect Pen export logs, visual comparisons and real IME results when in scope; no legacy script gate applies.
Reconcile product requirements in place, update .docs, append history, enter InDevReview and commit the validated task automatically.
