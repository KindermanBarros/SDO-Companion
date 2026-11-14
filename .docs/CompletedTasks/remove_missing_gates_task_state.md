# Remove gates backed by absent scripts - Done

- Date time: 2026-11-12 22:23 America/La_Paz
- Task objective: Remove retired legacy-script gates and their active callers while keeping available verification commands and honest evidence requirements.
- Open questions: None; the owner explicitly requested removal.
- Shared dependencies and references: D1. Current .agents gates and callers, existing Gradle tasks and .docs validation policy; confirmed existing inputs. No added application dependency.
- Sub-tasks:
    - 1 Inventory and scope - Done
        - Confirm missing paths, inventory gates/callers and preserve historical reports and unrelated changes.
        - Dependencies: See D1
        - Technology and module changes: None
        - Definition of done:
            - [x] harness/scripts does not exist; executable local structural gate and Gradle wrapper exist.
    - 2 Remove retired gates and update callers - Done
        - Delete the obsolete contract inventory, remove script-backed commands and prerequisites from active workflows/rules/skills/prompts, and align the current CI and validation documents.
        - Dependencies: See D1
        - Technology and module changes: None
        - Definition of done:
            - [x] No active .agents instruction or configured command requires a retired script; actual test failures remain non-passing.
    - 3 Verify and hand off - Done
        - Run structural validation, regression fixtures, script-reference scan and scoped whitespace checks; append history, record limitations and commit automatically.
        - Dependencies: See D1
        - Technology and module changes: None
        - Definition of done:
            - [x] Structural checks, 10 structural fixtures, 4 CI shell-control fixtures, JSON parsing, shell syntax and scoped whitespace pass.

Runtime limitation: adb devices reports no connected runtime; the actual Android CI journey was not executed locally.
The adaptive CI script builds through Gradle and invokes adb directly, preserving screenshots without the absent shared runner.

[Maintenance record](../../docs/changes/harness-retro-2026-11-12-remove-missing-gates.md)

Owner explicitly approved closure of all five tasks, including UI, on 2026-11-13. Historical check limitations remain unchanged.
