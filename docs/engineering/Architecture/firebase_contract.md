# Firebase boundary

Observed SDK boundary, not a new wire protocol. See [ARCHITECTURE](../ARCHITECTURE.md).

FirebaseAuthRepository uses Credential Manager/Google ID and Firebase Auth; session callbacks remove their auth listener on flow close. Synced repositories use Firebase Firestore SDK calls and await.

## Collections

Source: app/src/main/java/com/kinderman/sdo/data/repository/FirestoreCollections.kt.

| Constant | Collection |
|---|---|
| USERS | users |
| CHARACTERS | characters |
| CAMPAIGNS | campaigns |
| CAMPAIGN_MEMBERS | campaignMembers |
| CAMPAIGN_INVITES | campaignInvites |
| CAMPAIGN_AUDIT | campaignAudit |
| CAMPAIGN_LIBRARY | campaignLibrary |
| CAMPAIGN_DELIVERIES | campaignDeliveries |
| CAMPAIGN_JOURNAL | campaignJournal |

Character sync exchanges CharacterRecord documents; local dirty/deleted state and CharacterSyncConflict are implemented in SyncedCharacterRepository and domain models. Membership and ownership constrain visible character queries. This is not evidence of real-time simultaneous editing.

## Authoritative field and security contracts

- DTO/entity fields: CharacterRecord.kt, CampaignRecords.kt, OperationRecords.kt and OwnerRecord.kt under app/src/main/java/com/kinderman/sdo/data/local/.
- Normalization and RPG contracts: existing docs/contracts/ documents.
- Repository operations: app/src/main/java/com/kinderman/sdo/data/repository/.
- Firestore security: firebase/firestore.rules.
- Deployment procedure: [Firebase deployment](../../firebase-deploy.md).
- Planned connectivity changes: [Synchronization requirements](../../product/requirements/synchronization.md).

No Bluetooth UUIDs, camera protocol or custom HTTP endpoint contract exists in the inspected application.

Sheet sync adds an optional fieldUpdatedAt map of conflict-group IDs to client-derived revision timestamps; absent metadata uses legacy document-time fallback. Local Room schema 30 persists it. Editor baselines are in-memory only and never serialized. [Synchronization policy](../../product/requirements/synchronization.md#sync-plan-003---latest-field-realtime-sheet-convergence) owns current guarantees and deferred work.
