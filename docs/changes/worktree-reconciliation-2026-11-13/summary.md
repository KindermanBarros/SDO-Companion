# Pending worktree reconciliation

Timestamp: 2026-11-13 20:47 America/La_Paz. Status: InDevReview.
Owner explicitly accepted all five prior tasks, including UI, on 2026-11-13. Their task records were moved to [.docs/CompletedTasks](../../../.docs/CompletedTasks/accepted_2026_11_13.md), preserving evidence and limitations. The six future product requests remain open.

## Disposition

| Category | Decision |
|---|---|
| Root app, tests, resources and current UI docs | Stale copies or subsets of committed PR142 work; retain the newer PR versions without reintroducing ellipsis or old controls |
| Catalog tools and harness templates | Restore tracked source; catalog CI and installed skills still reference them |
| Legacy harness scripts | Preserve tracked source/history; active retired gates remain disabled; no broad script deletion |
| Firebase alias and setup summary | Commit non-secret project selection and verified setup evidence; credentials stay outside Git |
| Product backlog records and sync requirement | Preserve current committed requirements; add missing sync index link |
| Knowledge note, memory scaffold, imported onboarding audit | Commit durable records; mark imported export claims as historical, not SDO test results |
| Misplaced HISTORY wrapper entry | Append the existing entry to the audit ledger; restore the policy wrapper |
| Two untracked raw export logs and intermediate instrumented screenshot | Remove disposable output; retain durable export summary and existing final UI captures |
| Root Pen-tool installation commit | Preserve by integrating branch history, not discarding the branch commit |

## Verification boundary

No application behavior changes. Owner runtime acceptance is recorded separately from automated evidence. Structural documentation and whitespace checks are selected; Android suites are not required for reconciliation. Scratch must be absent and both owned checkouts clean at handoff. External worktrees and owner attachments remain untouched.
Checks: 45-document structural validation and git diff --check passed. All 26 root app/test/resource blobs already occur in committed PR history. No Android tests rerun.

The four root-only documentation snapshots are preserved exactly in [root-documentation.patch](root-documentation.patch), relative to root commit 3a5280a; they do not supersede the canonical current documents. The other 66 modified tracked files match committed Git blobs exactly.

## Outcome

Root and PR checkouts are reconciled with both histories preserved. All 70 modified tracked files and 150 prior deletions were reconciled; the 134 loose paths were preserved in committed current files/history or classified as the three disposable artifacts. Two former task paths were matched against preserved historical blobs after archival. Tools/templates were restored. Owner explicitly authorized this scoped reconciliation after automatic-review rejection. The final Git status is checked again after synchronization; external worktrees and attachments remain untouched. This reconciliation task enters InDevReview; the five previously accepted tasks are Done.

## PR142 history correction

2026-11-13 21:02 America/La_Paz. Owner authorized resolving GitHub's rebase conflict. Preserve 5879d1d in a local backup ref and consolidate the same approved application tree on main 3815744; publish only with an explicit lease matching the previous remote tip. This removes the conflicting per-commit replay while retaining the original history locally. Documentation and Git checks apply; no new Android/runtime results are claimed.
