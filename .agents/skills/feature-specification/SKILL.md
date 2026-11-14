---
name: feature-specification
description: Clarify product requirements and write an approved spec before UX design.
---
# Skill — Feature Specification

Follow [product-documentation.md](../../rules/product-documentation.md) and read affected files in `docs/product/requirements/`. Use for substantial new features or [Harness Planning](../../workflows/harness-planning.md); bounded fixes use a short inline plan summarized in `docs/MEMORY.md`.

## Execution
1. **Classify scope**: Determine whether the request is a new screen, an enhancement to an existing screen, or logic-only. Preserve any user-supplied mockups under the workspace's `design/` folder.
2. **Rule Applicability**: Evaluate the 10 rows in [rule-applicability-template.md](../../../harness/templates/rule-applicability-template.md) (`Required`, `Not applicable — <reason>`, or `Exception — approved by <user/date>`).
3. **Clarify**: Surface explicit assumptions and ask concise questions only for unresolved product decisions (user goal, observable behaviors, business rules, scope boundaries, and screen/state inventory).
4. **Write `spec.md`**: Follow [feature-spec-template.md](../../../harness/templates/feature-spec-template.md) (`$FEATURE_DIR/spec.md` or `docs/current/spec_v{N}.md`). Decompose each functional requirement (`FR-*`) into one acceptance criterion (`AC-*`) per distinct outcome (happy path, fallback, error, compatibility).
5. **Approval**: Present `spec.md` and obtain explicit user approval before proceeding to [ux-design](../ux-design/SKILL.md) (if UI changes) or [slice-planning](../slice-planning/SKILL.md).
