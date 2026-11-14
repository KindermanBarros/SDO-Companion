---
description: Apply authorized UI changes and verify only affected behavior/appearance.
---
# Create UI and Verify
Follow [Project contract](../gates/project-contract.md); use android-ui-layer and current-stage skills only.

1. Resume the task/backlog entry and existing UI contract/reference. Defect screenshots are not goldens.
2. A small repair preserves the approved identity and needs a short delta/check plan, not a fresh design cycle.
3. New product behavior or visual direction requires requirements/design approval; reuse recorded authorization for unchanged scope.
4. Implement using existing semantic tokens/components, full functional text, word-aware wrapping and accessible targets.
5. Select once: build, relevant behavioral regression, and affected CLI/adb captures/runtime boundary as justified by risk.
6. Honor explicit owner build-only/manual-runtime verification. Record missing evidence; never replace real IME/lifecycle proof with component tests.
7. Compare only affected states; reuse prior evidence with matching fingerprints. Do not create another test/report merely because a stage changes.
8. Review changed code/appearance; independent review only for meaningful risk or an owner request.
9. Reconcile backlog/docs, retain minimal final references/captures, remove .scratch, and hand off InDevReview with a scoped signed-off commit.

No Roborazzi infrastructure exists in the current baseline. Do not invent a pixel PASS or add dependencies without approval.
Use [CI checks](../gates/ci-checks.md); rerun only invalidated checks. Preserve unverified limits rather than starting circular validation.
