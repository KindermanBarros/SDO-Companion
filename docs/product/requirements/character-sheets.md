# Character Sheets

**Capability ID**: `character-sheets` — see [Backlog](../product.md#product-backlog) and [Memory](../../MEMORY.md).

## Delivered Scope

- **Creation & Management**: guided `CharacterCreationWizard` (Heritage Points budget, race/path presets, attributes/skills) and 16-section `CharacterSheetScreen` (`Identity`, `Attributes`, `Resources`, `Protections`, `Body/Organs`, `Inventory`, `Powers`, `Mystic`, `Conditions`, `Transport`, `Familiars`, `Notes`, `Narrative`).
- **Unified Inventory Modal (`InventoryItemModal`)**: separate catalog and custom-item entry points shared by Creation and Sheet, with catalog multi-select and saved custom drafts.
- **Profile Avatars (`SHEET-PLAN-001`)**: preset portraits plus local photo picker with 1:1 `AvatarCropDialog` for non-square images and optional `<= 64 KiB` / `<= 256px` `media/avatar` cloud thumbnail.
- **Canonical Persistence**: Room `v37` (`CharacterRecord`) + `LegacyFirestoreNormalizer` + `CharacterMigrationManager` preserving unrecognized fields (`unrecognizedFieldsJson`).

- **Divine profile, guided ascension wizard, PD powers & mark conditions (`BACKLOG-025`, `BACKLOG-026`)**: `PROFILE` embeds a pink `Ascensão Divina` card (GM-editable; hidden from players until awakened above `Mortal`), GMs can unlock a 4-step `DivineAscensionWizard` for `LESSER_GOD` (`8` Pillars) and `GREATER_GOD` (`+4` Pillars) completed directly by the player, `08 PODERES` manages active Domain Acts and passive divine capabilities using `PD`, `13 ESTADO` (`Condições`) embeds `Marcas divinas` alongside conditions, and patron/mark pickers list both catalog deities and Lesser/Greater God sheets from the same campaign.

## Planned changes

BACKLOG-026 adds current divine strength, Presence capacity and links, vulnerability suppression and recovery, and permanent growth/loss controls. Confirmed deity and inventory review fixes are included in the same delivery.
