---
name: android-unit-test
description: Implement and verify Android unit-test coverage.
---

# Skill — Android Unit Test

## Execute
- **Scope**: Domain rules, use cases, ViewModels, mappers, and formatters in `app/src/test/`.
- **Stack**: JUnit 4 + Kotlin `runTest` + deterministic fake repositories/services (see [.docs/TECH_STACK.md](../../../.docs/TECH_STACK.md)).
- **Conventions**: File ends with `Test.kt`; follow AAA pattern; one primary behavior per test; assert observable state/outputs rather than internal implementation details.

