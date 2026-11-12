# ARCHITECTURE — SDO Companion

> **Sealed.** Edit only when the user explicitly asks. Preserve the architecture confirmed during setup; propose migrations separately.

Module responsibilities: [PROJECT_CONTEXT](PROJECT_CONTEXT.md).

## Decision record

Append changes to [Architecture decisions](ArchitectureDecisions/architecture_decisions.md) with objective, date, time in America/La_Paz, modification and reason. Preserve existing records.

## Pattern

Compose with ViewModels and repository interfaces in a single Android application module. AppViewModel exposes several StateFlows, not a single unified UiState; AuthViewModel also exposes Compose state. SdoApp collects flows with collectAsStateWithLifecycle and invokes ViewModel callbacks.

- MainActivity hosts SdoApp. Standard and Cybergrunge themes share screens and responsive components.
- SdoApp owns AppSurface, selected character and a rememberSaveable back stack; it does not use a NavHost.
- Screen callbacks select surfaces. Message StateFlow drives SnackbarHost and is explicitly dismissed.
- This baseline describes existing behavior; no architecture migration is introduced.

## Layers and dependency rules

- :app is the only module; there are no project-module dependencies.
- domain contains models, rules, use cases and repository interfaces.
- data implements repositories using Room, Firebase Auth and Firestore.
- presentation contains ViewModels and screen composables; ui contains shared/theme components.
- SdoApplication wires implementations; presentation factories accept repository contracts.
- Domain is not entirely platform-independent: AuthRepository.loginWithGoogle accepts android.app.Activity.
- Preserve existing replaceable contracts. Do not interpret this observation as approval of every current dependency.

## Package layout

```text
app/src/main/java/com/kinderman/sdo/
  MainActivity.kt
  SdoApplication.kt
  data/{auth,local,repository}/
  domain/{catalog,creation,model,policy,progression,repository,usecase}/
  presentation/{character,dashboard,historian,login,session,settings,sync}/
  ui/{core,shared,standard,cybergrunge}/
```

## Cross-cutting

| Concern | Existing implementation |
|---|---|
| DI | Manual SdoApplication wiring and ViewModelFactories; no Hilt/Koin |
| Navigation | AppSurface and saveable custom stack in SdoApp |
| Async | Coroutines, Flow, ViewModel scopes; application IO SupervisorJob |
| Persistence | Room sdo.db, explicit migrations through 27 to 28; SharedPreferences for preferences |
| Permissions | INTERNET only; no dangerous runtime permission flow |
| Connectivity | Firebase Auth and explicit repository Firestore synchronization |

## Lifecycle and data contract

Startup initializes Firebase, Room, repository wiring, stored-character migration and bundled-catalog refresh. The application IO scope lives for the process; no explicit cancellation hook exists in SdoApplication.

SdoApp forwards foreground/background lifecycle events to AppViewModel, which manages autosave/debounce/sync jobs in viewModelScope. Firebase auth callbackFlow removes its listener with awaitClose.

[Firebase boundary](Architecture/firebase_contract.md) lists collection names and authoritative contract locations. Existing external contracts and backlog remain in docs/; they are preserved.
