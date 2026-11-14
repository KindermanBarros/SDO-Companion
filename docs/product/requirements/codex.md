# RPG codex

**Capability ID**: `codex`

Status: adoption draft, pending approval.

## User goal

Players consult the codex for the SDO RPG system.

## Requirements

| ID | Product intent | Evidence qualification |
|---|---|---|
| CODEX-001 | The beta provides a codex for the user's RPG system. | Requested in setup_prompt.md; catalog JSON, BuiltInCatalog, LocalCatalogRepository and historian UI exist; execution and completeness not verified |

Existing catalog schemas and normalization contracts remain authoritative for their current scope. Source presence does not establish catalog completeness or a new editing workflow.

## Compact UI review

Compact UI review: origin/category choices use one modal trigger per existing enum; search, archive and create behavior remain in scope. See [approved UI contract](../../current/UI_contract.md). Component regression tests pass; owner visual acceptance pending.

## Planned changes

Onboarding inventories catalog inputs and consumers. No catalog regeneration, content updates, schema changes or codex UI changes are proposed.

## References

- `setup_prompt.md`
- `catalogs/README.md`
- `docs/contracts/Contrato de Normalização de Dados do Companion.md`
- `app/src/main/java/com/kinderman/sdo/data/repository/LocalCatalogRepository.kt`
- `app/src/main/java/com/kinderman/sdo/presentation/historian/HistorianDashboardScreen.kt`

Owner review repair: preserve existing contracts while fixing theme outlines, clipped text and compact controls. Panel quick Session/Master removed; navigation retained. See [revision evidence](../../current/summary_v4.md). Installed-app acceptance pending.
