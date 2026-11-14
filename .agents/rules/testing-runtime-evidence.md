# Runtime Testing Evidence Rules

Load only when the change touches Compose rendering, gestures, custom `AppSurface` navigation/back stack, lifecycle/recreation, Room migrations, IME insets, permissions, or visual verification.

## Runtime selection
- Ephemeral exploration uses `exploratory-qa` (`agent-device`) under `.scratch/qa/<run-id>/` and is deleted before commit.
- Maintained runtime tests live in `app/src/androidTest/`: use `createComposeRule()` for isolated component semantics/callbacks and `createAndroidComposeRule<MainActivity>()` when Activity lifecycle, `AppSurface` back stack, state restoration, or IME geometry is claimed.
- Use stable `testTag`s/semantics and `waitUntil`/`waitForIdle` (never `Thread.sleep`). Use local fixtures/emulators, never production backends.

## Platform-Bound Evidence
- Platform-bound behavior (e.g., Room SQLite migrations, Credential Manager/Auth boundaries, lifecycle sync triggers) requires a real instrumented test or explicit owner-approved runtime verification; JVM fakes are supplemental.
- If a required device/emulator is unavailable (and the owner has not specified CI/manual runtime verification for the task), record `Blocked` or `INCONCLUSIVE` — never pass by skip.

## IME, navigation & visual claims
- **IME**: Editable inputs must stay visible and reachable above the keyboard with obstructing bottom bars dismissed. Full runtime IME proof requires launching the production Activity and asserting bounds with the keyboard open and dismissed.
- **Navigation journeys**: Mount the production surface host, perform real UI actions across the return boundary, and assert the visible post-return state.
- **Visual claims**: Capture Android CLI/adb screenshots for UI tasks unless the owner explicitly owns runtime verification. Note that no Roborazzi task is currently configured in `SDO-Companion`; never fabricate a pixel-comparison PASS.
