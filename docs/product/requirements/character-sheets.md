# Character sheets

**Capability ID**: `character-sheets`

Status: delivered changes accepted by the owner; unimplemented planned changes remain open. See [Memory](../../MEMORY.md) and the [backlog](../product.md#product-backlog). Existing evidence limits remain unchanged.

## User goal

Players create and manage RPG character sheets for use in SDO campaigns.

## Requirements

| ID | Product intent | Evidence qualification |
|---|---|---|
| SHEET-001 | Players can create character sheets. | Requested in setup_prompt.md; CharacterCreationWizard and CharacterCreationTest exist; execution not verified |
| SHEET-002 | Players can manage their character sheets. | Requested in setup_prompt.md; CharacterSheetScreen and CharacterUseCases exist; execution not verified |
| SHEET-003 | Character data follows the existing canonical RPG contracts. | Existing docs/contracts and typed domain models; contract compliance is not certified by onboarding |

Source observations: Room persistence, migrations and Firebase synchronization exist. Offline durability, authorization, conflict resolution and migration correctness remain runtime/integration verification boundaries, not newly approved requirements.

## Compact UI review

Compact UI review: body/armor uses section index 11; race choices show catalog-derived facts and preserve pending selection; editing drafts use retained state. See [approved UI contract](../../current/UI_contract.md). Production recreation/IME acceptance remains pending.

## Planned changes

### SHEET-PLAN-001 — Character profile photos

Status: InDevReview; character profile photos implemented. Final debug build passed; tests skipped by owner, runtime acceptance pending.

Allow adding a profile photo and displaying it in the sheet, character list, Session Mode, and Historian/GM Mode, with circular coin-token mode for future battle-grid tokens (`CAMPAIGN-PLAN-004`). Delivered scope: `photoUri` on `Character`/`CharacterRecord` (Room v31 `MIGRATION_30_31`, `"identity"` sync conflict tracking), `SdoCharacterAvatar` + `SdoCharacterPhotoPicker` (`PickVisualMedia` center-cropped and scaled to `<=256x256` JPEG Base64 data URI with replace/remove when online and editable), and integration across `SheetHero`, `IdentitySection`, `CharacterCreationWizard`, `DashboardScreen`, `SessionModeScreen`, and `HistorianDashboardScreen`. See [Memory](../../MEMORY.md).

## References

- `setup_prompt.md`
- `docs/contracts/Contrato do SDO Companion.md`
- `docs/contracts/Glossário de Enums do Companion.md`
- `app/src/main/java/com/kinderman/sdo/presentation/character/CharacterCreationWizard.kt`
- `app/src/main/java/com/kinderman/sdo/domain/usecase/CharacterUseCases.kt`
- `app/src/androidTest/java/com/kinderman/sdo/data/local/AppDatabaseMigrationInstrumentedTest.kt`

Owner review repair: preserve existing contracts while fixing theme outlines, clipped text and compact controls. Panel quick Session/Master removed; navigation retained. See [revision evidence](../../current/summary_v4.md). Owner explicitly accepted this UI delivery on 2026-11-13; automated evidence limitations remain in the linked summary.
