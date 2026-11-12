# CODE_STANDARDS — SDO Companion

> **Sealed.** Edit only when the user explicitly asks. If you think a standard should change, propose it in your summary.

Main language: Kotlin. Base guides: [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide). Rules below take precedence. Tests: [VALIDATION](VALIDATION.md).

## Naming

- UpperCamelCase for types and for composables that return Unit; lowerCamelCase for functions, properties and locals; UPPER_SNAKE_CASE for constants.
- Kotlin file names match their main declaration. Source paths match packages under the existing `src/main/java/` (or `kotlin/`) roots.
- Private mutable Flow backing properties use `_state` / `_effect`; expose read-only flows.
- Keep the naming of existing feature packages. If they break Android's lowercase convention, don't rename them in unrelated work.

## Formatting and linting

- Follow `kotlin.code.style` from `gradle.properties` (`official` if set). Four spaces, conventional braces, no semicolons.
- Wrap long parameter lists one per line; follow the trailing-comma usage nearby.
- Prefer the 100-column limit in new code without reformatting existing long lines.
- Use explicit imports.
- Ktlint 14.2.0, Detekt 2.0.0-alpha.6 and Android Lint are configured; existing onboarding reports contain failures, not a clean baseline.

## Language idioms

- Prefer `val`, read-only collections, immutable state data classes and exhaustive `when` expressions.
- Use sealed interfaces for intents and effects where the code already does.
- Use safe calls, early returns and validation instead of new unchecked assertions (`!!`).
- Keep related declarations together in the same file when the code already does so.

## UI

- Render with Compose (or the UI toolkit the app uses). Keep state transitions in ViewModels; pass state and intent callbacks to screens.
- Put `modifier: Modifier = Modifier` first among optional UI parameters in new APIs. Keep useful previews.
- Preserve each screen's actual theme. Do not impose a theme rule the code doesn't follow.

## Resources and strings

- Visible text goes in `strings.xml`, read through `stringResource` or `getString`. Formatted resources for placeholders.
- Use Android resource naming (`snake_case`). Use explicit units in names and explicit `Locale` for stored filenames or formatted numbers and dates.

## Error handling

- Validate sizes, headers, formats and nullable inputs before reading payloads or using them.
- Match the nullable and Boolean failure boundaries already in use, and handle outcomes at the callers.
- Log actionable failures without secrets or captured user content. Preserve coroutine cancellation: never swallow `CancellationException`.

## Concurrency

- Use structured coroutines and Flow. Tie UI collection to the lifecycle (`repeatOnLifecycle`, `lifecycleScope`) and cancel owned jobs and observers on teardown.
- Do not create unowned scopes (`GlobalScope` or a scope nobody cancels).
- Do not add a coroutine-test or Turbine dependency without recording it in TECH_STACK first.

## Documentation

- Use concise KDoc for contracts, for declarations used outside their module, and for units, threading or lifecycle behavior that isn't obvious.
- Do not require KDoc on every declaration. Do not add comments that restate the code.

## Dependencies

- Declare external dependencies through `gradle/libs.versions.toml` aliases; current plugin versions are pinned in root `build.gradle.kts`.
- Keep local artifacts (`*.aar`, `*.jar`) that the build already references. Record them in TECH_STACK.
- Record every new dependency decision in [TECH_STACK](TECH_STACK.md) before you add it.

## Forbidden

- Secrets, tokens or signing material in source code.
- Fabricated APIs, versions or library names.
- Renaming packages or moving files unrelated to the task.
- State transitions inside composable rendering.
- Unowned coroutine scopes and global mutable dispatchers.
- Duplicate success paths for the same user action.
