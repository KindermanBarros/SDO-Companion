# ARCHITECTURE — SDO Companion

> **Sealed.** Edit only when the user explicitly asks. Current baseline is documented below; target upgrades follow the approved incremental roadmap in [architecture_upgrade_playbook.md](../current/architecture_upgrade_playbook.md).

Module responsibilities: [PROJECT_CONTEXT](PROJECT_CONTEXT.md). Skill reference: [android-architecture](../../.agents/skills/android-architecture/SKILL.md).

## Decision record

Record new architecture decisions in [Memory](../MEMORY.md) with objective, timestamp, change and reason. The [baseline decisions](ArchitectureDecisions/architecture_decisions.md) are historical provenance; preserve them without starting a second log.

## Current baseline

Compose UI with Hilt-injected ViewModels, Room + Firebase Spark Firestore Protocol v2 in `:app`, and a pure Kotlin/JVM `:core:domain` module.

- `MainActivity` (`@AndroidEntryPoint`) hosts `SdoApp`. Standard and Cybergrunge visual modes share screens and responsive components (`ui/core`, `ui/shared`, `ui/standard`, `ui/cybergrunge`).
- `SdoApp` owns `AppSurface` (`DASHBOARD`, `SHEET`, `SESSION`, `HISTORIAN`, `JOURNAL`, `LIBRARY`, `SETTINGS`), selected character/campaign IDs, and a `rememberSaveable` back stack (no `NavHost`).
- `AuthViewModel`, `JournalViewModel`, and `LibraryViewModel` expose `StateFlow` UI states; `AppViewModel` still exposes multiple independent `StateFlow`s. `SdoApp` collects flows with `collectAsStateWithLifecycle`.
- Startup (`SdoApplication`, `@HiltAndroidApp`) initializes Firebase and uses Hilt (`DatabaseModule`, `RepositoryModule`) to wire Room `sdo.db` (v35, `exportSchema = true`, `MIGRATION_1_2`..`MIGRATION_34_35`), `SparkFirestoreSyncEngine`, repositories, stored-character migration, and bundled-catalog refresh in a process-lifetime IO `SupervisorJob`.

## Module & package layout

```text
core/domain/src/main/kotlin/com/kinderman/sdo/domain/   # Pure Kotlin/JVM module (zero Android/Firebase deps)
  {catalog,creation,model,policy,progression,repository,usecase}/
app/src/main/java/com/kinderman/sdo/                    # Android application module (depends on :core:domain)
  MainActivity.kt
  SdoApplication.kt
  data/{auth,di,local,repository,sync}/
  presentation/{character,dashboard,historian,journal,library,login,session,settings,sync}/
  ui/{core,shared,standard,cybergrunge}/
```

## Cross-cutting baseline

| Concern | Current implementation |
|---|---|
| DI | Hilt 2.60.1 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `DatabaseModule`, `RepositoryModule`) |
| Navigation | Custom `AppSurface` + `rememberSaveable` stack in `SdoApp` |
| Async | Coroutines, `Flow`/`StateFlow`, `viewModelScope`, `applicationScope` IO `SupervisorJob`, WorkManager |
| Persistence | Room `sdo.db` v35 (`exportSchema = true`, schema in `app/schemas/`); SharedPreferences for theme/visual preferences |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE` (no dangerous runtime permissions) |
| Sync & Auth | Firebase Auth (Google Credential Manager via `GoogleIdTokenFetcher`) + Spark Firestore Protocol v2 (`data/sync/`); [Firebase boundary](Architecture/firebase_contract.md) |

## Remaining boundary debt (do not expand)

- `AppViewModel` (854 lines) still coordinates multiple features and exposes ~20 independent flows.
- `SparkFirestoreSyncEngine.kt` (~2,803 lines after extracting contracts, local store, remote gateway, and helpers) can be further split by concern (discovery/hydration/drain/presence).
- `app/build.gradle.kts` still carries dual JSON stacks (`gson` + `kotlinx.serialization.json`).

## Target architecture (remaining incremental steps)

Follow [android-architecture](../../.agents/skills/android-architecture/SKILL.md) incrementally via [architecture_upgrade_playbook.md](../current/architecture_upgrade_playbook.md):

1. **Completed:** Pure Kotlin/JVM `:core:domain` module (46 main files + 23 pure JVM test suites; compiler-enforced zero Android/Firebase deps), `GeneratedCatalogEntries.kt` migrated from 22,224 lines to an 18-line JSON resource loader (`built_in_catalog.json`), Room `exportSchema = true`, `navigation-compose` removed, `SparkFirestoreSyncEngine` split into 5 files, `AuthViewModel` & `LibraryViewModel` on `StateFlow`, and Hilt DI (`2.60.1`) active.
2. **Next (`Step 3.3`):** Extract remaining feature ViewModels (`HistorianViewModel`, `SessionViewModel`, `DashboardViewModel`) from `AppViewModel`.
3. **Final (`Phase 4` remaining):** Extract `:core:data` and `:core:ui`, then `:feature:*` modules as needed.
