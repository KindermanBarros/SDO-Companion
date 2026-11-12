---
description: Verify a merged SDO Android build with approved acceptance cases and separate exploratory evidence.
---
# Workflow: Feature Acceptance

Read [Project contract](../gates/project-contract.md); planner owns the acceptance plan and tester owns execution.
Entry requires a named merged change, integration branch, approved requirements and the integrated QA build. A checkout SHA alone does not identify an installed APK.

## 0. Bootstrap

Use exploratory-qa's suite layout under .qa-artifacts with separate suites per build/device/environment.
Inspect prerequisites without activating device journeys, mocks, routing changes, captures or QA-data mutations.
Verify installed package com.kinderman.sdo, version/build/signing identity and CI artifact provenance; absent identity is INCONCLUSIVE.
Use disposable QA accounts and synthetic data in the approved environment.
No production-data operation, tool installation or application modification is authorized by acceptance.

## 1. Plan — test-design

Write the versioned suite test plan with selected acceptance cases, complete oracles, environment/fixture hashes and a separate exploration budget.
Present the plan and obtain actual approval before execution; record response, date, approved version/hash and bound inputs in suite-report.md.
Changed build/platform/environment/fixture/scope returns to plan approval; preserve earlier attempts.
Use exploratory-qa approved-plan mode for execution; retain each case's result and evidence.
When real backend behavior is blocked, preserve INCONCLUSIVE real-server assertions.
Investigate mocks only within approved scope using api-mocking; revise and reapprove acceptance cases before executing the new mocked plan.
Mock success never proves the real Firebase backend.

## 2. Explore — exploratory-qa

Run acceptance-support mode with distinct risk targets and an explicit time/action budget.
Keep exploratory findings separate from planned-case outcomes; preserve each original verdict and stop reason.
Use the skill's adaptive investigation and dogfood-report reconciliation procedures.

## 3. Consolidate

Only cases with every required assertion PASS cover their acceptance criterion.
Critical defects produce BLOCKED; missing critical proof/environment/build identity produces INCONCLUSIVE when not already blocked.
Unresolved noncritical risks require an owner disposition; never lower criticality during execution.
READY FOR HUMAN SIGN-OFF needs critical planned proof, no open critical defects, bounded exploration or an explicit human disposition, residual-risk decisions and verified cleanup.
Readiness does not authorize release/promotion or modify the product tracker.
Post-merge defects route through an authorized fix/revert and a new integrated-build run; never patch source inside acceptance.

## 4. Cleanup — always

Apply exploratory-qa and api-mocking cleanup even after failure/interruption.
Verify restoration against original resources/settings and retain cleanup receipts.
Failed restoration makes readiness INCONCLUSIVE unless already BLOCKED.
Complete .docs task/history records; [Release checklist](../gates/release-checklist.md) controls the human release handoff.
Commit only maintained acceptance documentation when applicable and validated; do not stage ignored ephemeral captures or secrets automatically.
