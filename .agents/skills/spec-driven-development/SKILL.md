---
name: spec-driven-development
description: Write a concise specification before coding when requirements are ambiguous.
---
# Spec-Driven Development

Use when starting a substantial feature or when product requirements are ambiguous. Skip for bounded fixes or self-contained enhancements covered by `.docs/TASK_PLANNING.md`.

## Phases
1. **Specify**: Surface explicit assumptions, resolve open product questions with the user, and write a spec covering Objective, Scope/Boundaries (Always / Ask first / Never), Functional & Acceptance Criteria, and Verification commands ([CI checks](../../gates/ci-checks.md)).
2. **Plan**: Identify affected files/layers in [.docs/ARCHITECTURE.md](../../../.docs/ARCHITECTURE.md), implementation order, risks, and selected checks.
3. **Tasks**: Decompose into small, independently verifiable steps (each touching a bounded set of files).
4. **Implement**: Execute one step at a time, loading only the active source files and triggered rules. Update the spec in place if scope changes.
