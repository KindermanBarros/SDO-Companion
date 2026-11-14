---
name: ux-design
description: Define UI_contract states and validate Pen visual references.
---
# Skill — UX Design

Run for new screens or visual redesigns after requirements clarification. Small UI repairs preserving existing identity use [Create UI and Verify](../../workflows/create-ui-and-verify.md) directly without a full UX cycle.

## Workflow
1. **Define `UI_contract.md`**: Use [UI_contract-template.md](../../../harness/templates/UI_contract-template.md) and [design_system.md](../../../docs/product/design_system.md). Enumerate every required screen state (loading, empty, ready, error, overlay) and `## IME State Coverage` (closed/open/dismissed keyboard behavior: `screen-no-toolbar`, `screen-toolbar-dismissed`, or `sheet-stays-open`). For complex features, also align [visual-target-template.json](../../../harness/templates/visual-target-template.json).
2. **Validate inventory**: Ensure every state has a stable ID, viewport, controls, copy, whole-word wrapping/no-ellipsis compliance, and fixture before generating visuals.
3. **Generate & export `.pen` visuals**: Use [pen-design-editor](../pen-design-editor/SKILL.md) to create/update frames, export review PNGs, and fill each row's Pen source, node ID, and PNG path.
4. **Approval**: Present `UI_contract.md` and exported PNGs for user approval, then clean temporary candidate files from `.scratch/`.
