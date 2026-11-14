---
description: Deliver one authorized feature with proportional planning and evidence.
---
# Feature Delivery
Follow [Project contract](../gates/project-contract.md) and root lean-execution policy.

1. Read requested backlog/capability entries and resume/create the task state.
2. Record scope, affected files, risks and selected checks once. Bounded changes need no duplicate stage reports.
3. For new substantial behavior, approve requirements/plan; for new visual direction approve design. Reuse approvals already covering the exact work.
4. Implement only affected layers with existing architecture/components. Keep user data and production/test boundaries safe.
5. Execute selected checks once. Reuse matching build/test/runtime evidence across stages.
6. Review the actual diff and changed risk. Independent review is justified for security/data-loss/complex work or when requested, not automatic fan-out.
7. Reconcile affected requirements and the living backlog; add deferred findings without expanding scope.
8. Record outcome, command/result, limitations and next action in one summary. Enter InDevReview and append history.
9. Promote required final evidence, remove .scratch entirely, check staged scope and commit with sign-off. Publish only with authorization.

Owner CI/build-only/manual-runtime preferences control local execution; omitted evidence is not acceptance.
Use [CI checks](../gates/ci-checks.md), not retired script gates. Rerun only checks invalidated by a relevant edit/failure.
Complex ordered work uses [Harness planning](harness-planning.md).
