# Clean State Checklist

Copy Core Checks and only the conditional rows triggered by the approved Rule Applicability matrix or diff; record `N/A — <reason>` for non-applicable rows.

## Core Checks
- [ ] Build/compile evidence succeeds for every affected module (`./gradlew :app:assembleDebug`).
- [ ] `./gradlew :app:ktlintCheck`, `./gradlew :app:detekt`, and `./gradlew :app:lintDebug` exit 0 (or baseline debt is explicitly separated).
- [ ] Required tests run with non-zero test counts and applicable Kover coverage thresholds pass (`./gradlew :app:koverXmlReportDebug`).
- [ ] No new suppression, baseline, exclusion, placeholder, dummy, no-op, or secret is introduced.
- [ ] Changed files stay within approved scope and architectural boundaries.
- [ ] `.scratch/` is completely removed and no stale/orphan artifact remains.

## Conditional Results

| Trigger | Scope check | Result (`PASS` / `FAIL` / `N/A — <reason>`) | Evidence |
|---|---|---|---|
| `API` | DTOs, catalog/Firebase adapters, and error/fallback cases verified | | |
| `ROOM` | Schema, migrations, transactions, and restart persistence verified | | |
| `NAV` | Production-entry navigation, back-stack, and return journey verified | | |
| `UI` | Design states, accessibility, semantics/testTags, localization, and visual/device evidence verified | | |
| `PLATFORM` | Capability matrix complete (`fail_loudly`) and real runtime boundary tests pass | | |
| `OBS` | Structured logs follow `.agents/rules/observability.md` with zero PII/secrets | | |
| `RESET` | Authorized reset/delete is scoped, idempotent, and verified without unrelated data loss | | |
| `ADR` | Architectural decision or change record documents context, decision, and trade-offs | | |
