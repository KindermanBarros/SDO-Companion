# Synchronization & Connectivity

**Capability ID**: `synchronization` — see [Backlog](../product.md#product-backlog), [Firebase boundary](../../engineering/Architecture/firebase_contract.md), and [Memory](../../MEMORY.md).

## Planned changes

### SYNC-PLAN-006 - Spark Firestore Protocol v2

Active architecture (`protocolVersion = 2`, `schemaVersion = 2`) running 100% on the Firebase Spark tier (no Cloud Functions, Cloud Storage, or RTDB sheet mirrors):

| Area | Spark v2 Contract |
|---|---|
| **Authority & Wire Layout** | Firestore `characters/{id}` root (18 metadata keys) + `fields/{fieldKey}` + `items/{itemId}` (`inv_`, `istate_`, `abil_`, `pow_`, `mys_`, `cond_`, `lcond_`, `note_`, `attr_`) + `items/{id}/fields/{key}` + immutable `operations/{opId}` receipts (`<=10` Rules accesses/rule, `<=20`/tx). |
| **Offline-First Outbox** | Account-scoped Room tables (`account_sheets`, `sheet_intents`, `sheet_conflicts`) persist completed edits (commit-on-blur / flush on sheet text inputs, coalesced `PENDING` `PATCH_FIELDS` batches) and drain via debounced `SparkFirestoreSyncEngine` + `ExistingWorkPolicy.REPLACE` WorkManager. |
| **Convergence & Conflicts** | Independent fields/items merge monotonically; identical-value stale edits and idempotent item `CREATE`s auto-converge; multi-target conflicts surface 3-way (`base`/`local`/`remote`) resolution with transitive topological rebasing of dependent chains. |
| **Legacy Compatibility** | `LegacyFirestoreNormalizer` + `CharacterMigrationManager` normalize `protocolVersion < 2` monolithic docs and mixed numeric/Timestamp dates while preserving unknown keys in `unrecognizedFieldsPayload`. |
| **Presence, Media & Grid** | Firestore `presence/{uid}_{connId}` (`<=120s` TTL); local full-res `localPhotoUri` + optional `<=64 KiB` `media/avatar` thumbnail; isolated RTDB `grid/{campaignId}/{gridId}` with RTDB-only `acl/{uid}`. |

Deployment and migration procedures: [Firebase deploy](../../firebase-deploy.md).
