# Firebase rules deploy and rollback

Firestore rules and indexes are validated in the emulator before any deployment.

## Publishing

- Pull requests, main changes, and releases validate Rules and migration contracts.
- Production deployment requires **Actions > Firebase rules deploy > Run workflow**, selecting the reviewed ref. Automatic deployment is disabled for the breaking Spark v2 cutover; merging the PR does not fence existing clients unexpectedly.
- Each run retains a 90-day artifact containing the validated `firebase.json`, rules, and indexes.

The `firebase-production` environment uses the first available secret in order:

1. `FIREBASE_SERVICE_ACCOUNT_SDO_COMPANION`
2. `FIREBASE_SERVICE_ACCOUNT`
3. `FIREBASE_SERVICE_ACCOUNT_KEY`
4. `GCP_SA_KEY`
5. `GOOGLE_APPLICATION_CREDENTIALS_JSON`

The value must be the full JSON of a service account authorized to publish rules and indexes to the project.
For legacy compatibility, the workflow also accepts `FIREBASE_TOKEN`.

Release readiness: a successful Android build does not verify backend compatibility. Before publishing a Spark v2 APK, confirm production protocol versions, migration manifests and active Rules match the client; complete the coordinated cutover below. Push/release Rules validation does not deploy production.

## Spark v2 data cutover

Firestore is the authority. Before cutover inventory `characters`, legacy RTDB `sheetAggregates`, routing records, memberships, invitations, thumbnails, and any deployed Functions. Preserve RTDB-only remnants as archived data; never recreate deleted/orphan sheets just because a mirror exists. The read-only inventory does not authorize deleting remote data.

1. Establish a maintenance window. Pause client/Admin writers, including any previously deployed Functions. Apply a temporary write-deny fence before exporting data; keep it until the new client and final Rules are ready. Firestore Rules do not stop Admin credentials.
2. Save the current Rules/configuration ref and local exports outside the checkout. Back up legacy RTDB data and original media before denying or retiring those paths.
3. Run the local administrative CLI (requires Firebase CLI credentials, not a hosted backend):

   ```powershell
   node firebase/scripts/inventory-sheets.cjs
   node firebase/migration/migrate-cli.mjs backup --project sdo-companion --out C:/SDO-Backups/spark-before.json
   node firebase/migration/migrate-cli.mjs migrate --all --project sdo-companion --dry-run
   ```

4. Check sheet IDs, owners, campaign links, media inventory and zero parity failures. Retain checksum-verified backups. Test the same process against the Emulator Suite first.
5. Only after operator approval, run `migrate --all --backup C:/SDO-Backups/spark-cutover.json --project sdo-companion --execute --confirm-prod` with the same CLI entry point. It stages subcollections, verifies parity, then atomically activates the v2 root and manifest. Retry interrupted staging; do not manually mark manifests verified.
6. Verify every sheet and campaign association, release the compatible client, then manually deploy the reviewed final Rules/indexes. Old writers are rejected; queued legacy edits remain recoverable. Confirm two-session editing before ending maintenance. Keep receipts, tombstones, backups, and archived mirrors.

The CLI defaults to dry-run and refuses production mutation without both execution flags. Local backup/audit files contain private sheet data; never commit them. No production migration is performed by CI.

## Data rollback

Pause all writers first. Run `rollback --sheet <id> --backup <cutover-backup.json> --audit-out <local-audit.json> --dry-run` using `firebase/migration/migrate-cli.mjs`, inspect the reconstruction, then add `--execute --confirm-prod --project sdo-companion` only for an approved rollback. It preserves current post-cutover values, tombstones and avatar changes rather than blindly restoring an old snapshot. Export failure aborts before mutation; a prepared audit may remain if the later commit fails.

Rollback requires the corresponding stable client and Rules. A legacy root cannot be written by Spark v2 clients/Rules. Keep writers fenced until both data and protocol match, and retain v2 subcollections/receipts for recovery. The administrative reconstruction is not transactionally fenced against other Admin writers.

## Rules rollback

1. Open **Actions > Firebase rules deploy > Run workflow**.
2. Enter the stable tag or SHA in `ref` (for example, `v0.24.0`).
3. Run the workflow.
4. Confirm the target project, ref, and published commit in the run summary.

Rollback restores the rules and index declarations from that ref. Existing built indexes are not automatically deleted to avoid destructive removals during recovery.
