# Character sheets

**Capability ID**: `character-sheets`

Status: adoption draft, pending approval.

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

Backlog requested by owner on 2026-11-12; not yet implemented. Allow adding a profile photo and displaying it in the sheet, character list, Session Mode, and Historian/GM Mode. Reuse the photo in the coin-shaped battle-grid tokens planned in CAMPAIGN-PLAN-004.

Given the display surfaces and token reuse, this item is tracked as a character portrait. Confirm during refinement whether a separate user-account avatar is also requested. Define image source, replacement/removal, empty-state presentation, cropping, storage/sync, and permissions before implementation; none of these choices are fixed here.

## References

- `setup_prompt.md`
- `docs/contracts/Contrato do SDO Companion.md`
- `docs/contracts/Glossário de Enums do Companion.md`
- `app/src/main/java/com/kinderman/sdo/presentation/character/CharacterCreationWizard.kt`
- `app/src/main/java/com/kinderman/sdo/domain/usecase/CharacterUseCases.kt`
- `app/src/androidTest/java/com/kinderman/sdo/data/local/AppDatabaseMigrationInstrumentedTest.kt`

Owner review repair: preserve existing contracts while fixing theme outlines, clipped text and compact controls. Panel quick Session/Master removed; navigation retained. See [revision evidence](../../current/summary_v4.md). Owner explicitly accepted this UI delivery on 2026-11-13; automated evidence limitations remain in the linked summary.
