---
name: shipping-and-launch
description: Prepare Android and Firebase releases with verification and rollback readiness.
---
# Shipping and Launch

Follow [Project contract](../../gates/project-contract.md) and [Release checklist](../../gates/release-checklist.md). Never publish, tag, or deploy without explicit owner authorization.

## Pre-launch verification
1. **Build & quality**: Run required checks from [CI checks](../../gates/ci-checks.md) (`assembleRelease`/`assembleDebug`, `lintDebug`, `testDebugUnitTest`, catalog `--check` scripts).
2. **Authentication & signing**: Delivered APKs must include configured Firebase inputs (`google-services.json`) and be signed with the registered release certificate verified by `.github/actions/verify-android-signature`. Missing protected inputs must fail the build.
3. **Data & compatibility**: Verify Room migrations (`sdo.db`) and Firestore rules/indexes (`.github/workflows/firebase-rules.yml`) when persistence or sync changes.

## Rollout & recovery
- Use semantic versioning (`vMAJOR.MINOR.PATCH`) and monotonic `versionCode` in `.github/workflows/sdo-companion-android.yml`.
- Retain the previous GitHub Release APK asset; if a post-release defect occurs, ship a forward patch with a higher `versionCode` (never ask users to downgrade Room schemas) and roll back Firestore rules via `firebase-rules.yml` `workflow_dispatch` if needed.
- Record release run URL, artifact name, byte size, SHA-256, and any unexecuted runtime boundaries in the task state before entering `InDevReview`.
