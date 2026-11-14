# Test Review — <Feature ID or vN>

## Scope & Command Evidence
- **Feature / Commit**: `<slice>` / `<commit>`
- **Reviewed files**: `<production files>` | `<test files>`

| Command | Exit code | Timestamp / Commit | Provenance | Result / Detail |
|---|---:|---|---|---|
| `./gradlew :app:testDebugUnitTest` | | | Executed / Recorded / Not run | |
| `./gradlew :app:koverXmlReportDebug` | | | Executed / Recorded / Not run | Overall `>= 80%`, new classes `>= 90%` |
| `./gradlew :app:connectedDebugAndroidTest` | | | Executed / Recorded / Skipped | When runtime/UI boundary is in scope |

## Rule Applicability & Requirement-to-Test Traceability

| Rule / Source ID | Approved decision / Required behavior | Test file + method / check | Production trigger & observable assertion | Result |
|---|---|---|---|---|
| ARCH..SEC / FR-001 / AC-001 | `<decision or behavior>` | `<Test#method or check>` | `<trigger -> assertion>` | `PASS` / `REVISION REQUIRED` / `N/A` |

## Test Quality Checklist
- [ ] Descriptive behavior names; each mapped test exercises a real production trigger and asserts an observable outcome.
- [ ] No tautological assertions, unused captures, assertion-free tests, or uncontrolled timing/threading.
- [ ] Layer isolation respected (JVM unit/integration vs instrumented runtime boundaries).
- [ ] Bug fixes only: reproduction test proved RED before fix and GREEN after fix.

## Verdict
**APPROVED / REVISION REQUIRED** — `<summary and any unverified traceability rows>`
