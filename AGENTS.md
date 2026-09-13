# AGENTS.md

Instructions for AI coding assistants working in this repository.

## Project

SDO Companion is an offline-first Android app for character sheets, campaigns, canonical catalogs,
and RPG session operations.

### Stack

- Android Gradle Plugin 9.4.0; Gradle 9.7.1.
- Kotlin 2.4.20 and Java 25.
- Jetpack Compose with Material 3.
- Room 2.8.4 for local persistence.
- Firebase Authentication and Cloud Firestore.
- Kotlin Coroutines, StateFlow, and Kotlinx Serialization.
- Minimum Android API 30; compile and target API 37.

Do not introduce older Android Views, LiveData, or callback-based Firebase APIs into new code.

## Repository map

- `app/src/main/java/com/kinderman/sdo/domain/`: models, policies, catalogs, repository contracts.
- `app/src/main/java/com/kinderman/sdo/data/`: Room, Firebase, and offline-first repositories.
- `app/src/main/java/com/kinderman/sdo/presentation/`: Compose screens, navigation, and ViewModels.
- `app/src/main/java/com/kinderman/sdo/ui/`: themes and reusable UI components.
- `app/src/test/`: JVM unit tests.
- `app/src/androidTest/`: Android and Room migration tests.
- `catalogs/`: generated canonical JSON catalogs.
- `tools/`: catalog generators and validators.
- `firebase/`: Firestore rules, indexes, and rule tests.
- `docs/`: audits and operational documentation.

Read [README.md](README.md) for setup and [DESIGN_GUIDE.md](DESIGN_GUIDE.md) before UI work.

## Exact commands

Run from the repository root unless a command says otherwise.

```bash
# Validate every generated catalog
python3 tools/generate_catalog.py --check
python3 tools/generate_gem_catalog.py --check
python3 tools/generate_item_catalog.py --check

# JVM unit tests
./gradlew testDebugUnitTest --stacktrace

# Android lint
./gradlew lintDebug --stacktrace

# Same Android checks used for pull requests
./gradlew lintDebug testDebugUnitTest --build-cache --stacktrace

# Debug APK
./gradlew assembleDebug --stacktrace

# Firestore rules (installs the pinned dependencies first)
cd firebase
npm ci
npm run test:rules
```

For a focused JVM test:

```bash
./gradlew testDebugUnitTest --tests com.kinderman.sdo.domain.policy.CharacterAccessPolicyTest --stacktrace
```

Report commands that could not run and the exact reason. Do not call work complete after a failing
relevant check.

## Architecture rules

- Keep business rules in `domain/`; Compose code must not decide permissions or persistence rules.
- Keep repository interfaces in `domain/repository/` and implementations in `data/repository/`.
- Persist local changes before remote synchronization.
- Preserve unsynced local data when Firebase is unavailable.
- Enforce permissions in domain policy, repository validation, and `firebase/firestore.rules`.
- Never hide authorization errors by returning an empty collection.
- Treat catalog source files and their generated JSON as one change.
- Add a Room migration when a persisted schema changes; never use destructive migration fallback.

## Kotlin and Compose style

Prefer immutable state and explicit event callbacks.

```kotlin
// Good
val editable = CharacterAccessPolicy.canEdit(session, character)
CharacterCard(character = character, onOpen = { onOpen(character.id) })

// Avoid: permission logic embedded only in UI
val editable = character.ownerId == currentUid || isAdminEmail
```

Use structured coroutine errors:

```kotlin
// Good: preserve and surface the failure
repository.sync(session)

// Avoid: turns permission and network failures into missing data
runCatching { repository.sync(session) }.getOrDefault(emptyList())
```

Use `MaterialTheme.colorScheme` and existing components from `ui/`. Do not hard-code theme colors
inside screens. Interactive targets must remain at least 48 dp and include content descriptions.

## Tests required by change

- Domain or policy change: add/update a JVM unit test.
- Room entity or converter change: add/update a migration or conversion test.
- Firestore permission change: add positive and negative rule tests.
- Compose behavior change: add a UI test when the behavior can regress without a compiler error.
- Catalog change: run all three catalog checks and commit regenerated outputs.

Test every authority path separately: owner, campaign creator, other campaign member, outsider, and
global administrator.

## Boundaries

### Always

- Inspect nearby code and existing tests before editing.
- Keep changes scoped; preserve unrelated user work.
- Use existing naming, formatting, and architecture.
- Validate authority in code and Firebase rules, not only by hiding buttons.
- Use one focused commit per checklist item.

### Ask first

- Changing canonical RPG rules or generated catalog meaning.
- Adding or upgrading dependencies, Gradle, Kotlin, SDK, or Firebase versions.
- Changing database schemas without an existing approved migration plan.
- Changing authentication providers, administrator identity, or campaign ownership semantics.
- Deleting or renaming public models, persisted fields, catalog IDs, or navigation destinations.

### Never

- Commit `google-services.json`, keystores, passwords, tokens, or local configuration.
- Edit production secrets or deploy Firebase rules from an unreviewed branch.
- Weaken Firestore rules to solve a client-side permission error.
- Rewrite published Git history or discard unrelated changes.
- Mark failed synchronization as successful or delete dirty local records after a failed sync.
- Hand-edit generated catalog JSON without updating its source and generator.

## Pull requests

Use Conventional Commit prefixes such as `fix:`, `feat:`, `test:`, and `docs:`.
In the PR, summarize user-visible behavior, authority impact, files changed, and checks executed.
Link rules or design documentation instead of duplicating it.
