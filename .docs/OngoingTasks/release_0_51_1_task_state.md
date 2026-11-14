# Publish v0.51.1 - InProgress

- Date time: 2026-11-13 21:28 America/La_Paz
- Task objective: Publish the owner-requested release of the approved main-branch UI fixes using the existing signed Android workflow.
- Open questions: None; patch release follows existing semantic version policy.
- Shared dependencies and references: main a38fe7c; previous release v0.51.0; .github/workflows/sdo-companion-android.yml.
- Scope/files: release workflow execution and this task, task index, product backlog and history audit only; no application changes.
- Steps:
  1. Inspect latest release and CI: successful run 43539896245 skipped publication because no conventional release commit was found.
  2. Commit the release request with a fix prefix, push main and dispatch existing release workflow.
  3. Verify versioned APK publication and successful Firebase/signature checks; record durable results.
- Checks: scoped diff/links and pre-commit hook; CI canonical catalogs, signed release build and certificate verification. Reuse prior UI PR CI evidence; no new runtime acceptance claimed.
- State: InProgress; release publication pending. No dependency/module changes.
