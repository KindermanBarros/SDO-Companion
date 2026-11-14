# Plug-and-play Catalogs — SDO Companion

These JSON files are the structured source of the 500 entries, mirroring `03 - Regras/Catalogos estruturados` in `solidao-dos-oprimidos`. No Markdown parsing, first-selection field entry, or network access is required during app use.

| File | Entries |
| --- | ---: |
| powers.json | 100 powers |
| magic.json | 50 spells |
| ashes.json | 150 ashes: 50 names x 3 purities |
| runes.json | 50 runes |
| knowledge.json | 50 learned + 50 arcane + 50 techniques (specialized v2 contract) |
| gems.json | Minor, major, and enhancement gems with wield-typed effects |
| modifications.json | Compatible modifications per equipment type with normalized effects |

## Contract

Powers use `catalog.schema.json`. Spells, runes, and ashes use `abilities.schema.json`; knowledges use `knowledge.schema.json`. The specialized contracts follow the flat Roll20 grimoire format: a named document holds a homogeneous list, and each entry keeps only its domain fields. The generator converts all three contracts to `CatalogEntry`; on the sheet, Knowledges become `SpecialKnowledge`. Catalog IDs are stable and instance IDs are created when added to a sheet.

| Fields | Target |
| --- | --- |
| id, version | catalogEntryId, catalogVersion |
| name, group, source, ruleReference | name, category, source, and reference |
| costType, costValue, execution, range, targetArea, duration, resistance | canonical fields read directly by UI |
| knowledge, level | required link from Spell or Rune to owned Knowledge |
| source, purity | Ash Source and Purity |
| mechanicalEffect | effect |
| prerequisites, activationCondition, limit, enhancements, deactivationCondition | Power details |
| attribute, initialLevel | Knowledge attribute and value |
| maxLevel | validated catalog limit (5) |
| summary, keywords, repeatable | description, search, and repeatability |

On the sheet, cost consists of a canonical type and integer value (`Nenhum` has no value, `Dose` is exclusive to Ashes, Runes use Arcane). Execution and Duration have independent time values. Spells, Runes, and Ashes load directly from typed fields without text inference.

## Provenance & notes

Source texts were cross-checked against `solidao-dos-oprimidos`. Effects and enhancements from published examples are preserved. Each Ash Purity is an independent entry because it changes both effect and load. All 150 Knowledges start at level 1, max out at 5, and have an editable primary Attribute on the sheet. Arcane Knowledges include schools as well as general magic knowledges (disciplines, crafts, supernatural phenomena). Techniques do not grant extra attacks or implicit damage.

Format proposals do not become published canon until merged in the rules repo. Existing character sheets preserve their snapshots and personal edits.

## Updating

1. Edit the full JSON files, preserving IDs, and bump `catalogVersion` when changing the published contract.
2. Run `python3 tools/generate_catalog.py` from the Companion root.
3. Run `python3 tools/generate_catalog.py --check`.
4. Commit the JSONs and generated Kotlin together; Actions rejects mismatches.

The generator validates fields, types, counts, unique IDs, and completeness before generating typed Kotlin. It does not infer costs, conditions, or mechanics. Room migration 17->18 adds structured fields only to the local catalog; existing sheets across all accounts remain untouched.
