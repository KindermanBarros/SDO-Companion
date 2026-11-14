---
description: Verify a merged SDO Android build with approved acceptance cases and separate exploratory evidence.
---
# Workflow: Feature Acceptance
Follow [Project contract](../gates/project-contract.md) and [Release checklist](../gates/release-checklist.md). Entry requires a merged change, approved requirements, and an identified QA build (`com.kinderman.sdo`).

## 0. Bootstrap
Keep ephemeral run artifacts under `.scratch/qa/<suite-id>/` and remove `.scratch` before commit. Verify package, version, and signing identity before running device actions or mocks; missing identity is `INCONCLUSIVE`. Never mutate production data.

## 1. Plan — test-design
Write the suite test plan with acceptance cases, oracles, fixture hashes, and an exploration budget; obtain user approval before execution. When real backend flows are blocked, preserve `INCONCLUSIVE` real-server results and revise/reapprove mocked cases via `api-mocking` before running them. Mock success never proves the live Firebase backend.

## 2. Explore — exploratory-qa
Execute approved cases first, then run bounded risk exploration separately. Keep exploratory findings distinct from planned case verdicts.

## 3. Consolidate & cleanup
Critical defects yield `BLOCKED`; missing critical proof or failed cleanup yields `INCONCLUSIVE`. Always restore device/mock state in a finally path, promote only concise final summary evidence to durable docs, remove `.scratch`, update `.docs` task/history, and hand off `InDevReview`.
