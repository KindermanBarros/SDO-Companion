# Android Architecture Rules

Follow the confirmed baseline in [.docs/ARCHITECTURE.md](../../.docs/ARCHITECTURE.md) and [.docs/PROJECT_CONTEXT.md](../../.docs/PROJECT_CONTEXT.md). Generic templates do not authorize architectural migrations.

## Module & layer boundaries
- Single `:app` module (`com.kinderman.sdo`). Dependencies flow inward: `presentation` / `ui` → `domain` ← `data`.
- **UI & Presentation (`ui/`, `presentation/`)**: Compose screens, shared theme components, `AppViewModel`, `AuthViewModel`, and custom saveable `AppSurface` navigation in `SdoApp`. Must not import Room DAOs, Firestore SDKs, or data-layer DTOs directly.
- **Domain (`domain/`)**: Domain models, catalog/progression/creation rules, use cases, and repository interfaces. Keep platform-independent (note: existing `AuthRepository.loginWithGoogle` `Activity` parameter is a documented baseline observation, not a pattern to expand).
- **Data (`data/`)**: Room entities/DAOs (`sdo.db`), Firebase Auth, Firestore synchronization, and repository implementations. Map persistence/remote payloads to domain models inside `data/`.

## Dependency injection & state
- Manual wiring in `SdoApplication` and `ViewModelProvider.Factory` is the active DI pattern; do not introduce Hilt or Koin without an explicit owner request and approved ADR.
- `AppViewModel` exposes multiple `StateFlow`s and coordinates autosave/sync jobs in `viewModelScope`; `SdoApp` owns `AppSurface` and a saveable back stack without `NavHost`.

## Code hygiene & verification
- Never use wildcard imports (`import foo.*`), inline fully-qualified class names in bodies, or unowned coroutine scopes (`GlobalScope`).
- Keep `@Composable` functions out of ViewModel classes and `ViewModel` classes out of composable files so Kover exclusions remain exact.
- Verify touched boundaries with triggered checks from [CI checks](../gates/ci-checks.md) and [review-checklist.md](../gates/review-checklist.md).
