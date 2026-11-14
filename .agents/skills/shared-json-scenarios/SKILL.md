---
name: shared-json-scenarios
description: Define or consume Android/iOS shared JSON test scenarios.
---

# Skill — Shared JSON Scenarios

## Purpose
Define or consume structured JSON test scenarios (`id`, `description`, `apiMocks`, `expected.domain`, `expected.ui`, `tags`) for cross-layer or cross-platform contract verification when applicable.

## Rules
- Each scenario asserts logical state (`itemCount`, `statusLabel`, `actionEnabled`, `selectedId`, `destinationScreen`, `emptyStateVisible`) rather than pixel-exact styling or widget hierarchy.
- Assert `expected.ui` when testing ViewModel-exposed `UiState`; assert `expected.domain` when testing repository/use-case outputs below UI.
- Do not embed large raw mock payloads inline inside test methods when a scenario fixture owns the contract.


