---
name: android-implementation
description: Implement an Android feature across affected data, domain, and UI layers.
---
# Skill — Android Implementation

Implement only the layers affected by the approved change following [.docs/ARCHITECTURE.md](../../../.docs/ARCHITECTURE.md), [.docs/CODE_STANDARDS.md](../../../.docs/CODE_STANDARDS.md), and [implementation-rules.md](../../rules/implementation-rules.md). Load conditional rules ([compose-rules.md](../../rules/compose-rules.md), [localization-rules.md](../../rules/localization-rules.md), [navigation-rules.md](../../rules/navigation-rules.md), [api-contract-rules.md](../../rules/api-contract-rules.md), [android-security.md](../../rules/android-security.md)) only when triggered.

## Layer checklist (execute only affected layers)
1. **Data (`data/`, `catalogs/`)**:
   - Update Room entities/DAOs (`sdo.db` with explicit migration when schema changes), Firestore sync mappings, or canonical JSON catalogs (`catalogs/` + `tools/generate_*.py`).
   - Map persistence/remote payloads to domain models inside `data/` with defensive null/unknown-enum fallbacks.
2. **Domain (`domain/`)**:
   - Update domain models, rules, use cases, and repository interfaces without introducing UI or Room/Firestore dependencies.
3. **Presentation & UI (`presentation/`, `ui/`)**:
   - Update `AppViewModel` / `AuthViewModel` `StateFlow`s and manual wiring in `SdoApplication` when needed.
   - Build Compose screens using `Sdo*` design-system components, `Standard`/`Cybergrunge` tokens, `stringResource()`, stable `Modifier.testTag(...)`, whole-word wrapping, and no functional ellipsis ([android-ui-layer](../android-ui-layer/SKILL.md)).
