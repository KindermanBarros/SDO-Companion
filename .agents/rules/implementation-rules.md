# Implementation Rules

Every line of production code in `app/src/main/` must implement the actual approved requirement — never a placeholder, stub, or dummy that merely compiles.

## Prohibited in production sources
- **Stub returns**: Returning hardcoded constants or `emptyList()` where calculation, persistence, or repository calls are required.
- **Unimplemented markers**: `TODO(...)`, `NotImplementedError(...)`, or comments like `// dummy`, `// placeholder`, `// stub for now`.
- **No-op handlers**: Empty `{}` or log-only lambdas where user actions require state transitions, persistence, or navigation.
- **Unreachable/partial wiring**: Functions or state branches that compile but are never wired to a reachable production call site.

## Allowed exceptions
- Test sources (`app/src/test/`, `app/src/androidTest/`) may use fakes, stubs, and test fixtures.
- Interfaces, `abstract` declarations, and `@Preview` composables (which may pass sample state for preview rendering).
- Explicitly documented owner-approved scope exclusions.
