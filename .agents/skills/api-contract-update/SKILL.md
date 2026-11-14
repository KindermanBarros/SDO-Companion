---
name: api-contract-update
description: Deliver a data, catalog, or Firebase contract change through domain, UI, and tests.
---
# Skill — API & Data Contract Update

Follow [api-contract-rules.md](../../rules/api-contract-rules.md) and [firebase_contract.md](../../../.docs/Architecture/firebase_contract.md).

## Workflow
1. **Classify impact**: Identify whether the Firestore document, Room schema, or bundled JSON catalog change is backward-compatible, risky, or breaking, and list affected layers (`data/`, `domain/`, `presentation/`, `ui/`).
2. **Implement ([android-implementation](../android-implementation/SKILL.md))**:
   - Update canonical catalog JSON + generator scripts (`tools/generate_*.py --check`) or Firestore/Room mappings first.
   - Decode optional fields and unknown enums defensively; keep DTO/entity types inside `data/`.
   - Update domain models/interfaces and UI screens only when the contract change surfaces there.
3. **Verify ([android-testing](../android-testing/SKILL.md), [code-quality-fix](../code-quality-fix/SKILL.md))**:
   - Add/update JVM unit/integration or Room migration tests covering valid, missing, and unknown/fallback payloads.
   - Run triggered checks from [CI checks](../../gates/ci-checks.md).
