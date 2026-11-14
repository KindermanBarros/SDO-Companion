# API & Data Contract Rules

Apply when modifying Firestore document schemas ([firebase_contract.md](../../.docs/Architecture/firebase_contract.md)), bundled JSON catalogs (`catalogs/`), or Room entities (`sdo.db`).

## Classification & compatibility
Classify every contract change before coding:
- **Backward compatible**: Optional/nullable fields with safe defaults, new catalog entries preserving schema version.
- **Backward compatible but risky**: New non-null fields with defaults, new enum variants, Room schema migration (`AppDatabase`).
- **Breaking**: Removing/renaming persisted or synced fields, incompatible type changes, or auth/ownership rule changes. Escalate breaking changes and state whether a migration or force update is required.

## Defensive mapping & validation
- New optional remote/serialized fields must be nullable with safe fallbacks (`dto.field ?: default`).
- Enums and catalog kinds decoded from persisted/remote strings must fall back safely without crashing on unknown values.
- Keep persistence/DTO mapping inside `data/` and translate remote/storage failures into domain results before presentation.
- When `catalogs/*.json` or `tools/generate_*.py` change, run the `--check` generators and `tools/validate_item_catalogs.py`.
- Cover changed sync/serialization/migration contracts with deterministic JVM or Room migration tests.
