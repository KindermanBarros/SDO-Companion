---
description: Review the changed risk without rerunning valid evidence.
---
# Feature Review
Follow [Project contract](../gates/project-contract.md).
Independent review is used when requested or justified by security, data-loss or complex-change risk; bounded low-risk work can use one owner.

- Read the active task/contract, relevant backlog row, diff and actual evidence. Do not load every historical report.
- Review correctness, boundaries, accessibility and claim-to-test traceability only where changed.
- Reuse successful matching checks; do not rerun a suite because the task reached review.
- A concise findings section in the existing summary suffices; separate test/code reports only when useful for substantial work.
- Inspect actual reference/capture artifacts for claimed visual proof. Missing infrastructure/runtime remains unverified.
- Record concrete severity/location/follow-up; put deferred findings in the living backlog.
- Developer approval owns Done/merge/release. Agent handoff is InDevReview.
- Update affected docs/history and remove .scratch before every scoped signed-off commit.
