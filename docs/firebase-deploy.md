# Firebase rules deploy and rollback

Firestore rules and indexes are validated in the emulator before any deployment.

## Automatic publishing

- Pull requests only validate the rules.
- Changes to `firebase/**`, `firebase.json`, or the workflow merged to `main` are validated and deployed to the `sdo-companion` project.
- Each GitHub Release republishes the configuration from that release tag, keeping rules aligned with the app version.
- Each run retains a 90-day artifact containing the validated `firebase.json`, rules, and indexes.

The `firebase-production` environment uses the first available secret in order:

1. `FIREBASE_SERVICE_ACCOUNT_SDO_COMPANION`
2. `FIREBASE_SERVICE_ACCOUNT`
3. `FIREBASE_SERVICE_ACCOUNT_KEY`
4. `GCP_SA_KEY`
5. `GOOGLE_APPLICATION_CREDENTIALS_JSON`

The value must be the full JSON of a service account authorized to publish rules and indexes to the project.
For legacy compatibility, the workflow also accepts `FIREBASE_TOKEN`.

## Rollback

1. Open **Actions > Firebase rules deploy > Run workflow**.
2. Enter the stable tag or SHA in `ref` (for example, `v0.24.0`).
3. Run the workflow.
4. Confirm the target project, ref, and published commit in the run summary.

Rollback restores the rules and index declarations from that ref. Existing built indexes are not automatically deleted to avoid destructive removals during recovery.
