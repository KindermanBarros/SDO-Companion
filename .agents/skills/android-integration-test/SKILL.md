---
name: android-integration-test
description: Implement Android integration tests below the UI layer.
---

# Skill — Android Integration Test

## Execute
- **Scope**: Cross-layer repository/use-case/ViewModel data flow, catalog/Firebase adapter parsing, cache/fallback, and persistence boundaries.
- **Fixtures**: Use structured scenario fixtures or deterministic fakes/in-memory stores; do not inline large raw payloads in test methods.
- **Assertions**: Assert `UiState` when testing ViewModel integration (`*IntegrationTest.kt`) or domain outputs when testing repository/use-case boundaries.

