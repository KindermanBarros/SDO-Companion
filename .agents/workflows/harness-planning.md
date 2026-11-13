---
description: Plan a complex SDO feature into approved vertical slices and a sprint contract.
---
# Workflow: Harness Planning

Read [Project contract](../gates/project-contract.md); use planner.
Use for multiple independently deliverable behaviors with ordering/dependency decisions.

## Workspace and authorities

Read affected living capability requirements and inspect current source before proposing scope.
Create one dated .scratch/YYYY-MM-DD-feature-name workspace and link it from docs/product/product.md.
Keep its lifecycle in the existing Harness Feature Tracker; task state in .docs links that contract and records progress only.
Do not infer lifecycle from folder location or introduce a competing feature tracker.
Review tracker status, approval provenance and linked workspace before selection and after edits.

## 1. Specify — feature-specification

Write spec.md with scope, boundaries, data/navigation impact, acceptance criteria and the required screen/state inventory.
Include all ten Rule Applicability decisions with triggers, rationale and planned evidence.
Resolve material product questions; preserve supplied visual inputs without designing inside the specification stage.
Review the specification for complete scope and supported acceptance/rule decisions.
Present the specification for explicit approval before UX.

## 2. Design — ux-design

For UI scope, create UI_contract.md inventory before Pen generation and apply [Create UI](create-ui-and-verify.md) reference and IME rules.
Review the state inventory, then populate verified references and visual_evidence/visual-target.json.
Obtain design approval before slicing.
Logic-only work records a supported N/A. Changes to product scope return to specification approval.

## 3. Slice — slice-planning

Produce feature_list.json, progress.md and sprint-contract.md in the same workspace.
Each slice delivers an observable vertical behavior with explicit dependencies, acceptance Test IDs and verification commands.
Mirror production_journey ownership between feature_list.json and the sprint contract; real navigation/lifecycle journeys need shipped-entry tests.
Declare platform_validation required/false with rationale; required platforms also need platform-capability-matrix.md and named real-boundary owners.
Declare visual state ownership, approved reference mappings and acceptance evidence.
Review slice dependencies, platform/journey ownership and UI/keyboard evidence requirements against the approved spec.
Obtain approval covering the slices, order, sprint/platform/journey contracts before [Harness generator](harness-generator.md).
No duplicate ad-hoc implementation plan is needed for the same complex execution contract.

## Handoff

Preserve approved hashes/versions and record scope exclusions.
Use the current [CI catalog](../gates/ci-checks.md) for executable gates. Artifact review is not an automated script pass.
Update .docs/history and commit completed validated planning artifacts automatically; do not mark the feature implemented or accepted.
