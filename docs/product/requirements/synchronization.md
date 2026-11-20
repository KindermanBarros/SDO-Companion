# Synchronization & Connectivity

**Capability ID**: `synchronization` — see [Backlog](../product.md#product-backlog), [Firebase boundary](../../engineering/Architecture/firebase_contract.md), and [Memory](../../MEMORY.md).

## Planned changes

### SYNC-PLAN-006 - Spark Firestore Protocol v2

Active architecture (`protocolVersion = 2`, `schemaVersion = 2`) running 100% on the Firebase Spark tier (no Cloud Functions, Cloud Storage, or RTDB sheet mirrors):

| Area | Spark v2 Contract |
|---|---|
| **Authority & Wire Layout** | Firestore `characters/{id}` root (18 metadata keys) + `fields/{fieldKey}` + `items/{itemId}` (`inv_`, `istate_`, `abil_`, `pow_`, `mys_`, `cond_`, `lcond_`, `note_`, `attr_`) + `items/{id}/fields/{key}` + immutable `operations/{opId}` receipts (`<=10` Rules accesses/rule, `<=20`/tx). |
| **Offline-First Outbox** | Account-scoped Room tables (`account_sheets`, `sheet_intents`, `sheet_conflicts`) persist edits immediately (`DRAFT`, `PENDING`, `SENDING`, `CONFIRMED`, `CONFLICT`, `REJECTED`, `ACCESS_DENIED`, `PAUSED_DEPENDENCY`) and drain via `SparkFirestoreSyncEngine` + WorkManager. |
| **Convergence & Conflicts** | Independent fields/items merge automatically; identical-value stale edits auto-converge; bounded resource deltas validate against stored ceilings (`corruption <= 100`); same-field divergent edits surface 3-way (`base`/`local`/`remote`) conflict resolution without clock-skew LWW. |
| **Legacy Compatibility** | `LegacyFirestoreNormalizer` + `CharacterMigrationManager` normalize `protocolVersion < 2` monolithic docs and mixed numeric/Timestamp dates while preserving unknown keys in `unrecognizedFieldsPayload`. |
| **Presence, Media & Grid** | Firestore `presence/{uid}_{connId}` (`<=120s` TTL); local full-res `localPhotoUri` + optional `<=64 KiB` `media/avatar` thumbnail; isolated RTDB `grid/{campaignId}/{gridId}` with RTDB-only `acl/{uid}`. |

Deployment and migration procedures: [Firebase deploy](../../firebase-deploy.md).
