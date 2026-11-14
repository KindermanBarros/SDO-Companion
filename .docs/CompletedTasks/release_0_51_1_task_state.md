# Publish v0.51.1 - Done

- Date time: 2026-11-13 22:12 America/La_Paz
- Task objective: Publish the owner-requested release of the approved main-branch UI fixes using the existing signed Android workflow.
- Open questions: None; patch release follows existing semantic version policy.
- Shared dependencies and references: main a38fe7c; previous release v0.51.0; .github/workflows/sdo-companion-android.yml.
- Scope/files: release workflow execution and this task, task index, product backlog and history audit only; no application changes.
- Steps:
  1. Inspect latest release and CI: successful run 43539896245 skipped publication because no conventional release commit was found.
  2. Commit the release request with a fix prefix, push main and dispatch existing release workflow.
  3. Verify versioned APK publication and successful Firebase/signature checks; record durable results.
- Checks: scoped diff/links and pre-commit hook; CI canonical catalogs, signed release build and certificate verification. Reuse prior UI PR CI evidence; no new runtime acceptance claimed.
- State: Done; published v0.51.1 from 6d85b83. No dependency/module changes.
- Evidence: [CI run 43540829981](https://github.com/KindermanBarros/SDO-Companion/actions/runs/43540829981) completed successfully; catalog checks, Firebase input restoration, signed release build and certificate comparison passed.
- Delivery: [GitHub release](https://github.com/KindermanBarros/SDO-Companion/releases/tag/v0.51.1), sdo-companion-v0.51.1.apk, 8,959,592 bytes; SHA-256 2f04f181a969cd5fce5c3df59be39bc2843872ea36e9a57e896a4bd4257c29df.
- Limitations: no new device smoke test or full test suite run for this release-only task; prior approved UI and CI evidence reused (PR run 43538181869). Publication does not claim new runtime acceptance.
- Recovery: retain v0.51.0 asset; if defects emerge, publish a corrected higher versionCode through the same workflow rather than asking users to downgrade persisted data.

Owner explicitly approved this delivery on 2026-11-14. Existing evidence and limitations remain unchanged.
