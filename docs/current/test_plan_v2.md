# Harness setup verification plan v2

Covered by the prospective setup authorization; actual results belong in summary_v2.md and onboarding-summary.md.

Canonical applicability: [spec_v2.md#rule-applicability](spec_v2.md#rule-applicability).

## Required-rule mapping

| Rule | Verification |
|---|---|
| ARCH | H-01 profile/source reconciliation; H-03 fixture isolation; source-gate findings retained |
| IMPL | H-01 runner contract, H-02 UTF-8 regression; no production app behavior changes |
| TEST | H-01–H-07 exact exits, non-zero fresh XML and real runtime evidence |
| SEC | H-01 rejects malformed runner identifiers; official runtime signature; canonical AI/security negative contracts; production Firebase untouched |

## Cases and procedures

- **H-01:** Given a foreign consuming app, run `bash harness/scripts/tests/android-instrumented-runner-contract-test.sh`. Expect its actual configured instrumentation component and rejection of failing, zero-test, missing-package and invalid-identifier fixtures. Run profile negative unit fixtures as well.
- **H-02:** Given UTF-8 documents and a non-UTF-8 process default, run `python -X utf8 harness/scripts/tests/product-docs-contract-test.py`. Expect the dedicated subprocess regression and all remaining cases to pass. Run the actual product-doc checker with `--change-dir docs/current --summary docs/current/summary_v2.md`.
- **H-03:** Given isolated fixture roots and a consuming-app profile, run the AST contract script and portable onboarding contract suite. Expect intended source-rule rejections, preservation/cleanup contracts, exact approval/fingerprint checks and non-zero execution, without inheriting unrelated layer roots.
- **H-04:** Build :app:assembleDebug, then execute :app:cleanTestDebugUnitTest :app:testDebugUnitTest --rerun-tasks. Inspect fresh `app/build/test-results/testDebugUnitTest/TEST-*.xml`; require non-zero testcases and no failures, errors or skips.
- **H-05:** Run ktlintCheck, detekt, :app:lintDebug, :app:koverXmlReportDebug and the canonical full-source bundle. Inspect task/source/report evidence to rule out empty plugin success. Require >=80% overall coverage through `check-coverage.sh`; preserve all failures.
- **H-06:** On a booted existing Android API >=30 emulator, run the named real SQLite migration method from test_plan_v1.md through the configured direct runner. Require non-zero named-method XML and app/test packages remaining installed. The fixture verifies local Room migrations and does not deliberately perform remote synchronization.
- **H-07:** Refresh architecture, validate profile/lifecycle/product docs, verify matching prospective-authorization artifact bindings, and run onboarding check/verify. Unresolved profile unknowns must prevent completion and cleanup.

## Runtime and coverage boundary

No product UI/navigation claim or visual comparison is introduced. Existing app JVM tests establish only their own assertions. Android platform claims require H-06; component or fake tests cannot replace it. No new ViewModel/use-case code is planned. Preserve the default 80% overall threshold; no baseline or broad package exclusions.
