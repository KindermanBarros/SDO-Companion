# PROJECT_CONTEXT — SDO Companion

Current source snapshot; [ARCHITECTURE](ARCHITECTURE.md) governs its baseline. Product context: [README](../README.md).

## Modules

| Module | Type / runs on | Responsibility | Depends on |
|---|---|---|---|
| :app | Android application, API 30+ | Character sheets, campaigns, codex, local storage and Firebase integration | No project module |

Confirmed target hardware: smartphone (owner confirmation). Specific models and physical-device acceptance: Pending. Responsive layouts do not establish physical-device acceptance.

## Communication

- MainActivity hosts SdoApp through Compose.
- SdoApp collects ViewModel flows with lifecycle-aware collection and sends callbacks.
- ViewModels coordinate domain contracts; data repositories implement them using Room DAOs and Firebase SDKs.
- Credential Manager supplies Google credentials to Firebase authentication.
- Repositories exchange Firestore documents during synchronization; [contract](Architecture/firebase_contract.md).

## Long-running flows

1. SdoApplication creates repositories and starts migration/catalog refresh in a process-lifetime IO scope.
2. Auth session observation uses callbackFlow with listener removal on close.
3. ViewModel scopes own observations, autosave and sync; foreground/background events trigger existing sync policy.
4. SdoApp retains surface selection and screen state with rememberSaveable and a saveable state holder.

## Where new code goes

Paths below are relative to app/src/main/java/com/kinderman/sdo/.

| Change | Existing location |
|---|---|
| Screens | presentation/ feature folders |
| State and coordination | presentation/AppViewModel.kt, AuthViewModel.kt |
| Surface navigation | presentation/SdoApp.kt |
| Business rules and repository contracts | domain/ |
| Database, auth and persistence integrations | data/ |
| Shared controls and themes | ui/ |
| Tests | Mirrored packages under app/src/test/java and app/src/androidTest/java |

## Current limitations

| Observation | Implication |
|---|---|
| AuthRepository imports Activity | Domain is not wholly platform-independent |
| Manual DI; custom navigation | Do not document Hilt or NavHost as implemented |
| Several independent state flows | Do not claim a single UiState contract |
| Existing onboarding reports failing quality/coverage gates | Full onboarding acceptance remains unverified |
| Legacy script gates removed by owner | Use current CI catalog and distinguish test execution from evidence review |

The baseline survey found 41 JVM-test Kotlin files and 5 instrumented files; no local.properties or local libs artifacts.
Existing product/onboarding records remain historical evidence. Operational instructions: [Project contract](../.agents/gates/project-contract.md).
