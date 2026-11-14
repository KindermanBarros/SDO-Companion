---
name: requirement-analysis
description: Analyze requirements, data/sync impact, UI states, and surface navigation.
---
# Skill — Requirement & Impact Analysis

Follow [product-documentation.md](../../rules/product-documentation.md). Summarize scope and decisions in `docs/MEMORY.md` ([spec-template.md](../../../harness/templates/spec-template.md), [summary-template.md](../../../harness/templates/summary-template.md)).

## Execution
1. **Exploratory QA handoff (if bug-fixing)**: Reuse the finding's steps, expected/actual outcome, and evidence from `report.json` without repeating the QA run.
2. **Impact & contract check**: Identify affected files across `presentation/`, `ui/`, `domain/`, `data/`, and `catalogs/`. Classify any Firestore/Room/catalog contract impact ([api-contract-rules.md](../../rules/api-contract-rules.md)).
3. **Rule Applicability**: Fill the 10-row matrix from [rule-applicability-template.md](../../../harness/templates/rule-applicability-template.md) (`Required`, `Not applicable — <reason>`, or `Exception`), loading only triggered rules.
4. **UI state & navigation**: Enumerate visually distinct screen states (loading, ready, empty, error, overlays, IME) and `AppSurface` transitions in `spec_v{N}.md` before handing off to [ux-design](../ux-design/SKILL.md) (if UI changes) or [implementation-plan](../implementation-plan/SKILL.md).
