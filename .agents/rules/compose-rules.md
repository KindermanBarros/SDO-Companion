# Compose Rules

Follow [.docs/CODE_STANDARDS.md](../../.docs/CODE_STANDARDS.md#ui) and [design_system.md](../../docs/product/design_system.md).

## Responsibilities & state
- Composables render state and invoke callbacks; they never call repositories, use cases, or data sources directly.
- Keep state transitions in ViewModels (`AppViewModel`, `AuthViewModel`), collected via `collectAsStateWithLifecycle()`.
- Keep one primary screen per file under `presentation/`; extract shared controls and theme primitives to `ui/`.
- Put `modifier: Modifier = Modifier` first among optional UI parameters.

## Theme, text & accessibility
- Preserve both `Standard` and `Cybergrunge` themes using existing SDO design tokens and `Sdo*` shared components; never hardcode raw `Color(...)` inline or invent `LocalAppColors` APIs.
- Use `stringResource()` for user-visible copy and provide `contentDescription` on interactive icons.
- Whole-word wrapping is mandatory in every theme: never split words across lines, insert automatic hyphens, or hide functional UI text (titles, names, labels, selected values, counters, statuses) with ellipsis. Reflow, wrap between words, or autosize single-word Cybergrunge card headings.
- Add stable, descriptive `Modifier.testTag(...)` to primary interactive elements and key containers; avoid transient indexes or random IDs.

## Lists & IME behavior
- Prefer `LazyColumn`/`LazyRow` with stable `key()` for dynamic lists.
- When editable text input is focused, apply `imePadding()` (or equivalent insets) so the focused field and primary actions stay visible above the keyboard, dismissing any obstructing bottom bar while `WindowInsets.isImeVisible` is true.
- Bottom sheets with text input must stay open above the keyboard while typing and use the platform drag handle without duplicate close buttons unless specified.
