# Localization Rules

Follow [.docs/CODE_STANDARDS.md](../../.docs/CODE_STANDARDS.md#resources-and-strings).

## Rules
- Define user-visible strings in `app/src/main/res/values/strings.xml` and access them via `stringResource()` or `getString()`; do not add new hardcoded UI literals in Composables.
- Use descriptive `snake_case` keys (`<screen>_<element>_<type>`), `pluralStringResource()` for count-dependent copy, and typed format arguments (`%1$s`, `%1$d`) with explicit `Locale` where formatting numbers/dates.
- Provide `contentDescription` on interactive non-text icons and controls for accessibility.
- Verify changed copy and resource usage through `:app:lintDebug` and diff review.
