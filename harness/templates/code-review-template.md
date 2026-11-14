# Review Template

## Review Summary
- **Feature / Bug**: `<brief description>`
- **Reviewer / Date**: `Agent` / `<YYYY-MM-DD HH:MM America/La_Paz>`
- **Commit / Merge base**: `<commit>` / `<base>`
- **Reviewed files**: `<production files>` | `<test files>`

## Rule Applicability Reconciliation

| Rule ID | Approved decision / rationale | Trigger observed | Evidence checked | Result |
|---|---|---|---|---|
| ARCH..SEC | `<from spec_v<N>.md#rule-applicability>` | `<yes/no>` | `<check or file:line>` | `PASS` / `REVISION REQUIRED` / `N/A` |

## Requirement-to-Production Traceability & State Reachability

| Source ID | Required behavior | Production entry point | Completion / cleanup path | Test evidence | Result |
|---|---|---|---|---|---|
| FR-001 / AC-001 | `<behavior>` | `<Screen/ViewModel/UseCase>` | `<production completion site; no test-only substitute>` | `<Test#method>` | `PASS` / `REVISION REQUIRED` |

## Build, Quality & Test Results

| Check | Exit code | Commit / Timestamp | Provenance | Result | Notes / Scope |
|---|---:|---|---|---|---|
| `:app:assembleDebug` | | | Executed / Recorded / Not run | ✅ / ❌ | |
| `:app:testDebugUnitTest` | | | Executed / Recorded / Not run | ✅ / ❌ | |
| `:app:koverXmlReportDebug` | | | Executed / Recorded / Not run | ✅ / ❌ | Overall `>= 80%`, new classes `>= 90%` |
| `:app:connectedDebugAndroidTest` | | | Executed / Recorded / Skipped | ✅ / ❌ / ⏭ | When runtime/UI boundary is in scope |
| `:app:ktlintCheck` | | | Executed / Recorded / Not run | ✅ / ❌ | |
| `:app:detekt` | | | Executed / Recorded / Not run | ✅ / ❌ | |
| `:app:lintDebug` | | | Executed / Recorded / Not run | ✅ / ❌ | |
| Suppression & stub audit | | | Executed / Recorded | ✅ / ❌ | No new suppressions, `TODO()`, or placeholders |

## Findings Checklist
- [ ] **Architecture & Layering**: Presentation -> Domain <- Data respected; no DTOs or framework types leaked into Domain/UI.
- [ ] **Implementation & Testing**: Complete state transitions; no dead states, no-op callbacks, or unrelated changes.
- [ ] **UI & Accessibility**: Verified against approved `UI_contract.md` / device screenshot when UI is in scope.
- [ ] **Security**: No hardcoded secrets, PII logs, or unvalidated untrusted input across trust boundaries.

## Release Risk & Verdict
- **Release Risk**: `low` / `medium` / `high` (`<reason, compatibility, rollback notes>`)
- **Verdict**: `✅ Ready to merge` / `⚠️ Merge with noted risks` / `❌ Do not merge — <blocking issue>`
