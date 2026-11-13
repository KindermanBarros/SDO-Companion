# Release checklist

Read [Project contract](project-contract.md). Documentation readiness or an automatic commit does not authorize a release.

## Entry and acceptance

- [ ] Owner explicitly requested release/promotion for a named version/build and environment.
- [ ] Required application [CI checks](ci-checks.md) passed with fresh evidence.
- [ ] Independent code/test review and human acceptance are recorded.
- [ ] [Feature acceptance](../workflows/feature-acceptance.md) covers the integrated build; critical gaps prohibit promotion.
- [ ] Smartphone target is confirmed; actual tested models/API levels and remaining coverage gaps are stated.
- [ ] Existing onboarding quality/coverage findings are resolved or explicitly handled through the owning policy; no fabricated pass.

## Data, authentication and compatibility

- [ ] Room schema changes have appropriate real migration evidence.
- [ ] Firestore field/backward compatibility and security rules are reviewed when affected.
- [ ] Firebase/Google sign-in build configuration and certificate/signing identity match the intended environment.
- [ ] Secrets remain external; no keystore/password is committed.
- [ ] Sync/conflict/ownership behavior and failures are validated for affected flows.
- [ ] Any backend deployment or force-update dependency is explicitly planned; no presumed backend change.

## Rollout and operations

- [ ] Required flags, rollout percentage, monitoring window and rollback owner are decided, or N/A has a scope-specific reason.
- [ ] Actual monitoring capabilities are verified; do not claim Crashlytics/analytics is configured merely because Firebase is used.
- [ ] Applicable store, permission and privacy checks are complete.
- [ ] QA mocks/routing/data/capture resources have verified cleanup receipts.
- [ ] Release approval, target artifact and remaining risk disposition are recorded.

A missing critical environment, build identity, runtime assertion or cleanup receipt remains INCONCLUSIVE/BLOCKED.
No release, deployment, production-data operation or push occurs automatically.
