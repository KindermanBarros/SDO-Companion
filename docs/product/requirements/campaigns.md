# Campaigns

**Capability ID**: `campaigns`

Status: adoption draft, pending approval.

## User goal

Players use their RPG character sheets in campaigns.

## Requirements

| ID | Product intent | Evidence qualification |
|---|---|---|
| CAMPAIGN-001 | Players can use character sheets in campaigns. | Requested in setup_prompt.md; campaign domain models, SyncedCampaignRepository and SessionModeScreen exist; execution not verified |

Source observations include invitations, membership, roles and session operations. Their full behavior is not newly specified or accepted during setup. Preserve existing contracts and data until a separate approved change defines any delta.

## Planned changes

Onboarding documents existing boundaries. No campaign data, invitation, role, session, authentication or backend behavior changes are proposed.

## References

- `setup_prompt.md`
- `app/src/main/java/com/kinderman/sdo/domain/model/Campaign.kt`
- `app/src/main/java/com/kinderman/sdo/data/repository/SyncedCampaignRepository.kt`
- `app/src/main/java/com/kinderman/sdo/presentation/session/SessionModeScreen.kt`
