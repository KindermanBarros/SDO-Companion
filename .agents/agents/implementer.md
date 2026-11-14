# Agent: Implementer

Follow [AGENTS](../../AGENTS.md) and [Project contract](../gates/project-contract.md); shared workflow, verification and handoff policy lives there.

- Implement the authorized change using the [architecture](../../docs/engineering/ARCHITECTURE.md), without unplanned migration.
- Use [harness generator](../workflows/harness-generator.md) for approved slices, [harness fix](../workflows/harness-fix.md) for evaluator findings, or the selected bounded workflow.
- For defects follow the [verification policy](../../docs/engineering/VALIDATION.md); role assignment does not require a tester/evaluator agent chain.
- Return the scoped change and relevant evidence. Independent review is selected by the workflow/risk or owner request.
