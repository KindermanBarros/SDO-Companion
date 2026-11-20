# Firebase Rules Deploy & Migration

Firestore and RTDB rules are validated in the Firebase Emulator Suite (`npm --prefix firebase run test:rules`) before deployment.

## Automated & Manual Publishing

- **Pull Requests**: `.github/workflows/firebase-rules.yml` validates rules + migration contracts and deploys to `sdo-companion-dev`.
- **Main Branch**: pushes to `main` touching `firebase/**`, `firebase.json`, or `.github/workflows/firebase-rules.yml` automatically validate and deploy Firestore rules/indexes to production (`sdo-companion`).
- **Manual / Rollback**: run **Actions > Firebase rules deploy > Run workflow** with a target tag, branch, or commit SHA in `ref`.

Supported `firebase-production` credentials (checked in order): `FIREBASE_SERVICE_ACCOUNT_SDO_COMPANION`, `FIREBASE_SERVICE_ACCOUNT`, `FIREBASE_SERVICE_ACCOUNT_KEY`, `GCP_SA_KEY`, `GOOGLE_APPLICATION_CREDENTIALS_JSON`, or `FIREBASE_TOKEN`.

## Administrative CLI (`firebase/migration/`)

Local read-only inventory, backup, migration, and rollback scripts (never commit exported user data):

```powershell
node firebase/scripts/inventory-sheets.cjs
node firebase/migration/migrate-cli.mjs backup --project sdo-companion --out C:/SDO-Backups/spark-backup.json
```

All CLI commands default to `--dry-run` and require explicit `--execute --confirm-prod` flags before mutating production.
