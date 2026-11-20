# Character Sheets

**Capability ID**: `character-sheets` — see [Backlog](../product.md#product-backlog) and [Memory](../../MEMORY.md).

## Delivered Scope

- **Creation & Management**: guided `CharacterCreationWizard` (Heritage Points budget, race/path presets, attributes/skills) and 16-section `CharacterSheetScreen` (`Identity`, `Attributes`, `Resources`, `Protections`, `Body/Organs`, `Inventory`, `Powers`, `Mystic`, `Conditions`, `Transport`, `Familiars`, `Notes`, `Narrative`).
- **Unified Inventory Modal (`InventoryItemModal`)**: full-screen catalog, custom builder, enhancements, and Ash tabs shared between Creation and Sheet.
- **Profile Avatars (`SHEET-PLAN-001`)**: preset portraits plus local photo picker with 1:1 `AvatarCropDialog` for non-square images and optional `<= 64 KiB` / `<= 256px` `media/avatar` cloud thumbnail.
- **Canonical Persistence**: Room `v36` (`CharacterRecord`) + `LegacyFirestoreNormalizer` + `CharacterMigrationManager` preserving unrecognized fields (`unrecognizedFieldsJson`).

## Planned changes

No open character-sheet-only backlog items remain (`SHEET-PLAN-001` delivered).
