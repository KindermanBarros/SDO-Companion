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

## Planned changes

Onboarding adds documentation and baseline evidence only. No new sheet behavior, schema, calculation, ownership, synchronization, or UI change is proposed.

## References

- `setup_prompt.md`
- `docs/contracts/Contrato do SDO Companion.md`
- `docs/contracts/Glossário de Enums do Companion.md`
- `app/src/main/java/com/kinderman/sdo/presentation/character/CharacterCreationWizard.kt`
- `app/src/main/java/com/kinderman/sdo/domain/usecase/CharacterUseCases.kt`
- `app/src/androidTest/java/com/kinderman/sdo/data/local/AppDatabaseMigrationInstrumentedTest.kt`
