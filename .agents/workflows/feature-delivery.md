---
description: Deliver a bounded SDO feature through requirements, approved design/plan, implementation and evidence.
---
# Workflow: Feature Delivery

Read [Project contract](../gates/project-contract.md); use planner, implementer, tester and independent evaluator for their stages.

## Entry

Use for one cohesive feature/enhancement/API change. Multiple ordered deliveries use [Harness planning](harness-planning.md).
Resume the active approved docs/current spec, implementation plan, test plan and summary; preserve unrelated versions.
Establish a .docs task state linking those artifacts before editing.

## Stages

1. **Requirements — requirement-analysis.** Read affected living capability files; specify behavior, scope, data, navigation, edge cases and acceptance. Record the complete ten-row Rule Applicability matrix with supported triggers, decisions and evidence. Resolve material unknowns.
2. **UX — ux-design, when UI changes.** Create UI_contract.md state/viewport/control/fixture/IME inventory before Pen generation. Only design-reference fields may initially be Pending. Apply [Create UI](create-ui-and-verify.md) reference rules; obtain design approval before implementation planning proceeds.
3. **Plan — implementation-plan.** Record affected files, dependencies, risks and test commands in docs/current/implementation_plan_vN.md and test_plan_vN.md. Link the spec's rule matrix and each UI state's implementation/verification step. Obtain implementation approval; reuse authorization already covering the exact plan.
4. **Implement — android-implementation.** Change only approved data/domain/presentation/UI boundaries. Follow the selected persona's test-first rules. Production code must not select test fixtures. Require the debug build and applicable source gates.
5. **Test — android-testing.** Complete approved regressions; select the lowest sufficient layer. Require nonzero successful test counts, API/scenario receipts where applicable, real Android boundary tests and UI appearance evidence for their declared claims.
6. **Quality — code-quality-fix.** Apply all triggered [CI checks](../gates/ci-checks.md), including repository-wide source checks and coverage. A missing required checker blocks that stage.
7. **Reconcile.** Edit affected capability claims in place, preserve undelivered/backlog scope, and record the product reconciliation table under the existing summary. Run required product/lifecycle/metrics validators when available; absent prerequisites remain blocked.
8. **Runtime handoff.** For UI/platform/installation scope, install to the declared target runtime and repeat affected user journeys. Record build identity, serial, command, screenshots and outcomes; a missing target is blocked.
9. **Review and close.** Route to [Feature review](feature-review.md), update .docs, append history and enter InDevReview. Commit the validated task automatically; leave Done/merge/release to human approval.

## Stage gates

Use harness/scripts/check-stage-artifacts.sh with feature-delivery and the corresponding requirement-analysis, ui-contract, ux-design, implementation-plan and testing stage.
Run the affected journey registry, rendered-output, product-documentation and metrics validators required by [CI checks](../gates/ci-checks.md).
These commands are required when triggered; missing scripts are not passing evidence.

## Return routes

Requirement changes return to requirements; design feedback to UX; plan changes to planning.
Compilation/test/quality failures return to their owning stages within approved scope. Stop before downstream completion on any required gate failure.
