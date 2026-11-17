# Migration Integrity Rules

Apply whenever creating, modifying, or executing Firestore data migrations (`firebase/migration/`), wire codec conversions (`SparkWireCodec`), or Room schema migrations (`AppDatabase`).

## Mandatory Conversion & Parity Gates
1. **Verify through the production client codec, not only migration scripts**:
   - JS/Python parity checks alone are insufficient. Decode and hydrate **all** migrated records through the actual Kotlin client codec and domain mappers (e.g., `SparkWireCodec.fromSparkDocuments` -> `normalizedForSparkV2()` -> `toDomain()`, or Room `MigrationTestHelper`) before accepting a migration.
   - Explicitly test enum/label compatibility (`@PropertyName` Portuguese labels vs enum `.name`), numeric types (`Long` vs `Double`), nullable/defaulted fields, and embedded payloads (`canonicalAbilitiesPayload`, `canonicalItemsPayload`, `media/avatar`).
2. **Verify 100% field-for-field backup parity and round-trip**:
   - Compare every migrated record against the pre-migration backup across all sub-collections and nested payloads (`inventory`, `abilities`, `powers`, `attributes`, `personalNotes`, `photoUri`, `campaigns`, `members`, `invites`), and verify client round-trip (`toSparkDocuments` -> `fromSparkDocuments`).
   - Document any intentional normalizations (such as `Passive` ability `duration: null -> Instant`) explicitly; zero unexplained mismatches are allowed.
3. **Never swallow conversion errors silently**:
   - Sync loops that use `runCatching` (such as `SparkFirestoreSyncEngine.sync`) must not mask codec/hydration failures during migration verification; tests must assert zero decode failures across **every** remote/migrated sheet.
4. **Private snapshot hygiene**:
   - Store temporary production exports strictly in `.scratch/` (or external backup paths outside the repo) and remove them with `tools/pre-commit-cleanup.ps1` before committing.
