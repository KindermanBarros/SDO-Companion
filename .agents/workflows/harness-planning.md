---
description: Plan a complex SDO feature into approved vertical slices and a sprint contract.
---
# Workflow: Harness Planning
Follow [Project contract](../gates/project-contract.md). Use only for multi-slice features requiring ordered delivery; bounded work uses [Feature Delivery](feature-delivery.md).

1. **Record**: Read affected `docs/product/requirements/` and create one single file `docs/changes/YYYY-MM-DD-feature-name.md` linked from `docs/product/product.md` and `.docs/TASK_PLANNING.md` (place any binary visual exports under `docs/changes/assets/<slug>/`).
2. **Specify (`feature-specification`)**: Add scope, acceptance criteria, screen/state inventory, and the Rule Applicability table to that file. Obtain user approval before UX or slicing.
3. **Design (`ux-design`)**: For UI scope, add UI contract states and IME coverage to that same file, generate/export verified `.pen` references under `docs/changes/assets/<slug>/`, and obtain design approval (record `N/A` for non-UI work).
4. **Slice (`slice-planning`)**: Produce `feature_list.json`, `progress.md`, and `sprint-contract.md` (plus `platform-capability-matrix.md` only when `platform_validation.required` is true). Ensure 1:1 `US-*` mapping, vertical slices with reachable entry points, and explicit test selectors from [CI checks](../gates/ci-checks.md). Obtain user approval before [Harness Generator](harness-generator.md).
5. **Handoff**: Remove `.scratch`, update `.docs` history, and create a scoped signed-off commit in `InDevReview`.
