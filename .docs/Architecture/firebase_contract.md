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

Character sync exchanges CharacterRecord documents; local dirty/deleted state and CharacterSyncConflict are implemented in SyncedCharacterRepository and domain models. Membership and ownership constrain visible character queries. This is not evidence of real-time simultaneous editing.

## Authoritative field and security contracts

- DTO/entity fields: CharacterRecord.kt, CampaignRecords.kt, OperationRecords.kt and OwnerRecord.kt under app/src/main/java/com/kinderman/sdo/data/local/.
- Normalization and RPG contracts: existing docs/contracts/ documents.
- Repository operations: app/src/main/java/com/kinderman/sdo/data/repository/.
- Firestore security: firebase/firestore.rules.
- Deployment procedure: [Firebase deployment](../../docs/firebase-deploy.md).
- Planned connectivity changes: [Synchronization requirements](../../docs/product/requirements/synchronization.md).

No Bluetooth UUIDs, camera protocol or custom HTTP endpoint contract exists in the inspected application.
