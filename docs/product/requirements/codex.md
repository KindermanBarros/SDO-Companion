# RPG Codex & Rules

**Capability ID**: `codex` — see [Backlog](../product.md#product-backlog) and [Memory](../../MEMORY.md).

## Delivered & Active Scope

- **Compendium**: spoiler-free built-in catalogs (`BuiltInCatalog`, `catalogs/*.json`), campaign-scoped library entries (`CampaignLibraryEntry`), and GM session additions.
- **Player's Handbook Rules (`BACKLOG-022`, `InDevReview`)**: `app/src/main/assets/player_handbook_rules.json` (1,853 atomic rules across 185 topics parsed by `GameRulesGuide.kt`) with search, theme chips, and `#KEYWORD` filters in `LibraryScreen.kt`.
- **Deities (BACKLOG-025)**. The Compendium bundles public lore for 16 deities. Library and Historian detail views load GM lore only for a verified administrator or the owner of the relevant campaign.

- **Divine rules (BACKLOG-026)**. Canonical rules define a shared Presence capacity for living Anchors and Avatars, suppression and recovery of current Pillars, milestone growth and permanent losses, and prepared vulnerability requirements for sealing and divine death. Structured GM records expose acts, Pillars, gifts, passives, hooks, and mysteries under the existing spoiler policy.
