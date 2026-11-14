---
name: api-mocking
description: Control backend responses during QA using session-owned WireMock fixtures and cleanup.
---
# API Mocking

Use real backends by default; choose this skill only when approved QA cases require synthetic responses or fault injection. Follow [references/routing.md](references/routing.md) and [references/usage.md](references/usage.md#discovery-and-validation).

## Run
Use [exploratory-qa's execution scope](../exploratory-qa/SKILL.md#target-and-scenario) and [artifact layout](../exploratory-qa/references/artifact-layout.md).
1. Inspect routing ([references/routing.md](references/routing.md)) and use `--session <run-id>` with `hlens mock`.
2. Trace the in-scope action's method/path and local Room/cache side effects before applying fixtures.
3. Apply reviewed mappings with `hlens mock apply` and supply [mock coverage](../exploratory-qa/SKILL.md#network-mode).
4. Trigger the app action, then run `hlens mock requests` and `hlens mock verify --name <mapping>` to retain sanitized receipts under `wiremock/`.
5. Verify and export evidence **before cleanup**, then run `hlens mock reset` in a finally path and restore app/device routing. For acceptance replanning when real fixtures are blocked, follow [feature-acceptance plan review](../../workflows/feature-acceptance.md#1-plan--test-design).

## Ownership and evidence
- Use one unique session per run (`--exclusive` or `--scope-header X-QA-Session`). Never invoke global WireMock reset.
- WireMock proves app-side handling of synthetic responses only; it never proves UI rendering, analytics ingestion, or real Firebase/backend behavior. Keep unsupported backend claims `INCONCLUSIVE`.
- Coordinates with [exploratory-qa](../exploratory-qa/SKILL.md) and [analytics-event-validator](../analytics-event-validator/SKILL.md).
