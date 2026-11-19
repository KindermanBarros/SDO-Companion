# Architecture & Harness Upgrade Playbook (Step-by-Step)

This playbook is written for **incremental, low-risk execution**. Execute **one step at a time**, in order. Never combine multiple steps into one commit.

## Global Rules (Read Before Every Step)
1. **Never edit sealed docs** (`AGENTS.md`, `docs/engineering/ARCHITECTURE.md`, `CODE_STANDARDS.md`, `GIT_STANDARDS.md`, `PERSONAS.md`) unless the user explicitly asks.
2. **Never break Room migrations or Firestore Rules.** If you touch Room entities, add a migration or keep existing schema intact. Never change `firebase/firestore.rules` unless a step explicitly says so.
3. **Use targeted Gradle commands** to save time:
   - Unit tests: `./gradlew :app:testProdDebugUnitTest`
   - Build check: `./gradlew :app:assembleProdDebug`
   - Harness check: `powershell -ExecutionPolicy Bypass -File .agents/gates/validate-harness.ps1`
4. **Before every commit:** run `powershell -ExecutionPolicy Bypass -File tools/pre-commit-cleanup.ps1`, run the step's verification commands, and update `docs/MEMORY.md` with a short summary.
5. **If a test fails:** fix the failure before moving to the next step. Never skip or delete failing tests.

---

## Phase 0 — Harness Cleanup (No App Code Changes)

### Step 0.1 — Fix corrupted Markdown table in `docs/MEMORY.md`
- **Goal:** Repair lines 168–178 of `docs/MEMORY.md` where duplicated bullets broke the `|---|---|---|` table separator.
- **Files:** `docs/MEMORY.md`
- **Action:**
  1. Read lines 165–182 of `docs/MEMORY.md`.
  2. Replace the corrupted separator block with a clean `|---|---|---|` line (do not delete historical table rows).
  3. Keep the rest of `docs/MEMORY.md` intact.
- **Verify:** `powershell -ExecutionPolicy Bypass -File .agents/gates/validate-harness.ps1` (must print `PASS`).

### Step 0.2 — Remove retired `harness/scripts/` dead weight
- **Goal:** Delete the 113 retired scripts under `harness/scripts/` that have zero active references.
- **Action:**
  1. Confirm zero references outside `harness/scripts/`: search `.agents/`, `.github/`, `tools/`, `harness-project.json` for `harness/scripts`.
  2. Delete the `harness/scripts/` directory.
- **Verify:** `powershell -ExecutionPolicy Bypass -File .agents/gates/validate-harness.ps1` (must print `PASS`).

### Step 0.3 — Clean stale artifacts in `docs/current/`
- **Goal:** Remove obsolete screenshots (`*.png`) and superseded `v1..v4` plans from `docs/current/`.
- **Action:**
  1. Keep `docs/current/architecture_upgrade_playbook.md` and any file still linked from `docs/MEMORY.md` or `docs/product/`.
  2. Delete unreferenced `.png` files and superseded `spec_v1..v4`, `implementation_plan_v1..v4`, `test_plan_v1` files in `docs/current/`.
- **Verify:** `powershell -ExecutionPolicy Bypass -File .agents/gates/validate-harness.ps1` (must print `PASS` with zero broken links).

### Step 0.4 — Align `harness-project.json` and prune unused skills
- **Goal:** Remove unused generic skills and fix `harness-project.json` commands.
- **Action:**
  1. In `harness-project.json`, update `commands.build` to `:app:assembleProdDebug` and `commands.unit_tests` to `:app:testProdDebugUnitTest`, and remove obsolete onboarding `unknowns`.
  2. Ask the user for confirmation before deleting unused skills in `.agents/skills/` (candidates: `analytics-event-validator`, `api-mocking`, `competitor-analysis`, `harnesslens`, `shared-json-scenarios`).
  3. Update `docs/engineering/SKILLS.md` so the installed local skills section lists `android-architecture`.
- **Verify:** `powershell -ExecutionPolicy Bypass -File .agents/gates/validate-harness.ps1`.

---

## Phase 1 — Project Quick Wins & Clean Domain Boundaries

### Step 1.1 — Remove unused `navigation-compose` dependency
- **Goal:** Remove unused library from `app/build.gradle.kts` and `gradle/libs.versions.toml`.
- **Action:**
  1. Search `app/src` for `androidx.navigation`. Confirm 0 hits.
  2. Remove `implementation(libs.androidx.navigation.compose)` from `app/build.gradle.kts`.
  3. Remove the unused `androidx-navigation-compose` entry from `gradle/libs.versions.toml` if unused elsewhere.
- **Verify:** `./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest`.

### Step 1.2 — Enable Room schema export (`exportSchema = true`)
- **Goal:** Export Room v35 JSON schema so future migrations are verifiable.
- **Files:** `app/build.gradle.kts`, `app/src/main/java/com/kinderman/sdo/data/local/AppDatabase.kt`
- **Action:**
  1. In `app/build.gradle.kts`, configure KSP arg: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
  2. In `AppDatabase.kt`, change `exportSchema = false` to `exportSchema = true`.
  3. Build once so `app/schemas/com.kinderman.sdo.data.local.AppDatabase/35.json` is generated and commit it.
- **Verify:** `./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest`.

### Step 1.3 — Remove Firestore annotations from `domain/model/`
- **Goal:** Make `Character.kt` and `ItemCreation.kt` 100% pure Kotlin (zero `com.google.firebase.*` imports).
- **Files:**
  - `app/src/main/java/com/kinderman/sdo/domain/model/Character.kt`
  - `app/src/main/java/com/kinderman/sdo/domain/model/ItemCreation.kt`
  - `app/src/main/java/com/kinderman/sdo/data/sync/SparkWireCodec.kt` (and any Firestore mapper)
- **Action:**
  1. Inspect every `@Exclude`, `@IgnoreExtraProperties`, and `@PropertyName` in `Character.kt` and `ItemCreation.kt`.
  2. Search `app/src/main` for `toObject(Character::class.java)` or direct Firestore POJO serialization of `Character` / `ItemCreation`.
  3. Ensure all serialization/deserialization goes through explicit mappers/DTOs in `data/sync/` (never direct Firestore reflection on domain classes).
  4. Remove all `com.google.firebase.firestore.*` imports and annotations from `domain/model/`.
- **Verify:** `./gradlew :app:testProdDebugUnitTest` (especially `SparkWireCodecTest` and `SparkFirestoreSyncEngineTest`).

### Step 1.4 — Remove `android.app.Activity` from `AuthRepository`
- **Goal:** Make `domain/repository/AuthRepository.kt` 100% pure Kotlin (zero `android.*` imports).
- **Files:**
  - `app/src/main/java/com/kinderman/sdo/domain/repository/AuthRepository.kt`
  - `app/src/main/java/com/kinderman/sdo/data/auth/FirebaseAuthRepository.kt`
  - `app/src/main/java/com/kinderman/sdo/presentation/AuthViewModel.kt`
  - `app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt`
- **Action:**
  1. Extract the Credential Manager Google ID token retrieval from `FirebaseAuthRepository` into a UI-layer helper `GoogleIdTokenFetcher(activity: Activity)` in `presentation/login/`.
  2. Change `AuthRepository.loginWithGoogle(activity: Activity)` to `suspend fun loginWithGoogleIdToken(idToken: String): UserSession?`.
  3. Update `AuthViewModel` and `SdoApp` to fetch the token in the UI boundary and pass `idToken` to `AuthViewModel`.
  4. Confirm 0 `android.*`, `androidx.*`, and `com.google.firebase.*` imports remain anywhere in `domain/`.
- **Verify:** `./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest`.

---

## Phase 2 — Decompose Monoliths (Keep Behavior Identical)

### Step 2.1 — Split `SparkFirestoreSyncEngine.kt` (4,030 lines) into focused classes
- **Goal:** Extract cohesive internal classes in `data/sync/` without changing public signatures or sync semantics.
- **Rules:** Extract **one class per commit**. Do not rewrite logic while moving it.
- **Order of extraction:**
  1. `SheetMediaSyncStore.kt` (local photo storage, thumbnail <= 64KiB encoding, media doc read/write)
  2. `SheetPresenceTracker.kt` (Firestore presence heartbeats and listeners)
  3. `SheetDiscoveryService.kt` (accessible sheet IDs, admin listing, head checks)
  4. `SheetHydrationService.kt` (remote snapshot fetch, receipt timestamp normalization, cache hydration)
  5. `SheetIntentDrainExecutor.kt` (transaction execution, create/delete recovery, conflict generation)
- **Verify after EACH extraction:** `./gradlew :app:testProdDebugUnitTest` (`SparkFirestoreSyncEngineTest` + `SparkWireCodecTest` must pass 100%).

### Step 2.2 — Move `GeneratedCatalogEntries.kt` (22,224 lines) to JSON assets
- **Goal:** Stop compiling 22k lines of static Kotlin catalog data into DEX bytecode.
- **Action:**
  1. Follow the existing pattern in `app/src/main/assets/player_handbook_rules.json` and `GameRulesGuide.kt`.
  2. Update `tools/generate_catalog.py` (and `--check`) to emit a compact JSON asset under `app/src/main/assets/built_in_catalog.json` instead of 22k lines of Kotlin.
  3. Load and cache the JSON lazily via `kotlinx.serialization.json` inside `LocalCatalogRepository` / `BuiltInCatalog`.
  4. Keep all entry IDs and fields identical so `BuiltInCatalogTest` passes unchanged.
- **Verify:** Run catalog `--check` scripts + `./gradlew :app:testProdDebugUnitTest :app:assembleProdDebug`.

---

## Phase 3 — Adopt Hilt DI & Feature ViewModels (Per `android-architecture` Skill)

### Step 3.1 — Add Hilt Gradle setup and `@HiltAndroidApp`
- **Goal:** Wire Hilt with KSP (already enabled in `app/build.gradle.kts`).
- **Action:**
  1. Add `hilt-android` and `hilt-compiler` (via `ksp`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
  2. Annotate `SdoApplication` with `@HiltAndroidApp` and `MainActivity` with `@AndroidEntryPoint`.
  3. Create `data/di/DatabaseModule.kt` (`@Module @InstallIn(SingletonComponent::class)` providing `AppDatabase` and DAOs) and `data/di/RepositoryModule.kt` (`@Binds` for every repository interface).
- **Verify:** `./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest`.

### Step 3.2 — Migrate ViewModels to `@HiltViewModel`
- **Goal:** Remove `AppViewModelFactory`, `AuthViewModelFactory`, `JournalViewModelFactory`, and `applicationContext as SdoApplication` in `SdoApp.kt`.
- **Action:**
  1. Annotate `AuthViewModel`, `JournalViewModel`, and `AppViewModel` with `@HiltViewModel` and `@Inject constructor(...)`.
  2. Replace `viewModel(factory = ...)` in `SdoApp.kt` with `hiltViewModel()`.
  3. Convert `AuthViewModel.state` from Compose `mutableStateOf` to `StateFlow<AuthUiState>`.
- **Verify:** `./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest`.

### Step 3.3 — Split `AppViewModel` (854 lines) by feature
- **Goal:** One focused `@HiltViewModel` per feature screen with a single `StateFlow<*UiState>`.
- **Order:** Extract `LibraryViewModel` -> `HistorianViewModel` -> `SessionViewModel` -> `DashboardViewModel`, leaving `AppSessionCoordinator` only for global session + sync status pill.
- **Verify after each ViewModel:** `./gradlew :app:testProdDebugUnitTest :app:assembleProdDebug`.

---

## Phase 4 — Incremental Multi-Module Extraction

Only start Phase 4 after Phases 1–3 are complete and green.
1. Create `:core:model` (pure `kotlin("jvm")` module) and move `domain/model/` into it.
2. Create `:core:domain` (pure `kotlin("jvm")` module depending on `:core:model`) and move `domain/{policy,progression,creation,repository,usecase}/` into it.
3. Create `:core:data` (Android library depending on `:core:domain`) and move `data/` into it.
4. Create `:core:ui` (Android library with Compose) and move `ui/` into it.
5. Update `settings.gradle.kts`, `harness-project.json`, and `docs/engineering/PROJECT_CONTEXT.md` after each module extraction.
- **Verify after each module:** `./gradlew :app:assembleProdDebug testProdDebugUnitTest`.
