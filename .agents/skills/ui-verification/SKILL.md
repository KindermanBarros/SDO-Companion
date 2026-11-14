---
name: ui-verification
description: Verify Android UI against approved design references and runtime evidence.
---
# Skill — UI Verification

Follow [Create UI and Verify](../../workflows/create-ui-and-verify.md), [compose-rules.md](../../rules/compose-rules.md), and [testing-runtime-evidence.md](../../rules/testing-runtime-evidence.md). When a structured JSON report is required by a complex workflow, use [ui-verification-template.json](../../../harness/templates/ui-verification-template.json).

## Execution
1. **Build & static checks**: Run selected checks from [CI checks](../../gates/ci-checks.md) (`:app:assembleDebug`, `:app:lintDebug`, or owner-specified CI lane).
2. **Scope & visual comparison**:
   - Compare changed screens/states against approved `.pen` exports and `UI_contract.md` (or existing reference captures).
   - Verify Standard and Cybergrunge theme fidelity, angular card/selector outlines, readable headers, whole-word wrapping, and absence of ellipsis on functional text.
   - Verify keyboard/IME behavior on editable inputs (focused field visible, obstructing bottom bar dismissed).
   - Note: `SDO-Companion` has no Roborazzi task configured; capture Android CLI/adb screenshots when runtime is available, and never fabricate a pixel-comparison PASS when runtime/Roborazzi is absent.
3. **Classify findings**:
   - **Critical**: Missing/broken screen, clipped/ellipsized functional text, overlapping controls, or broken navigation (`FAIL`).
   - **Major**: Theme/token mismatch or layout shift (`FAIL` unless owner-accepted).
   - **Minor**: Within-tolerance spacing/rendering variance (`PASS` with note).
