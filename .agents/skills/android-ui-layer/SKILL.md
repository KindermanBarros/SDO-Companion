---
name: android-ui-layer
description: Implement Compose UI screens and components with unidirectional state flow.
---
# Skill — Android UI Layer

Follow [compose-rules.md](../../rules/compose-rules.md), [localization-rules.md](../../rules/localization-rules.md), [navigation-rules.md](../../rules/navigation-rules.md), and [design_system.md](../../../docs/product/design_system.md).

## Execution
1. **ViewModel & state (`presentation/`)**: Expose state via `StateFlow` (`AppViewModel`, `AuthViewModel`), coordinate domain contracts, and keep Room/Firestore types out of `presentation/`.
2. **Compose screens & shared UI (`presentation/`, `ui/`)**:
   - Pass state and event callbacks to screen composables; keep business logic out of Composables.
   - Consume `Sdo*` design-system components and preserve both `Standard` and `Cybergrunge` themes (never invent `LocalAppColors` or hardcode raw colors).
   - Use `stringResource()` for visible copy (`app/src/main/res/values/strings.xml`) and add stable `Modifier.testTag(...)` to interactive controls.
   - Enforce whole-word wrapping and never use ellipsis on functional UI text (titles, names, labels, selected values, counters, statuses).
   - Keep focused text inputs visible above the IME (`imePadding()`), dismissing obstructing bottom bars while the keyboard is open.
3. **Navigation**: Wire surface transitions through `AppSurface` and saveable state in `SdoApp.kt`.
