# Retire gates backed by absent scripts

Date: 2026-11-12 22:31 America/La_Paz.

## Authorization, classification and invariant

The owner explicitly requested removal of gates whose scripts are absent.
Classification: HARNESS_ENVIRONMENT.
Active instructions/configuration must not require retired scripts. Available checks still need actual execution and successful evidence.

## Scope

- Deleted the four obsolete source/runtime contract-inventory documents.
- Removed legacy source, stage-artifact, lifecycle, traceability, receipt, journey, platform, visual-artifact, onboarding and harness self-test script gates from active .agents callers.
- Updated workflows, affected skill/rule paragraphs, the evaluation prompt and the current CI catalog.
- Removed source_rules and coverage script commands from harness-project.json; kept available Gradle commands and the coverage report.
- Preserved meaningful acceptance mappings, reviews, human approvals, real runtime evidence and numerical coverage standards.
- Updated the existing adaptive CI script to build the same APKs/classes with Gradle and execute AndroidJUnitRunner with adb directly; logs and screenshot collection remain.
- Added a structural regression for a retired gate hidden in a skill and four isolated shell-control regressions for the adaptive runner.
- Kept application source, versions, product requirements, historical reports and prior tooling deletions unchanged.
- Existing historical onboarding findings are not declared resolved; retired script requirements no longer block future workflows.

## Verification

- Native structural gate: exit 0; active instruction links/limits/imports and retired-script scan pass.
- Structural regression suite: exit 0; 10 cases, including rejection of a retired caller inside a skill.
- Adaptive runner regressions: exit 0; positive, test failure, zero-test and adb-transport cases.
- The mocked instrumentation failure returns shell success to exercise the real false-pass risk; the runner still rejects that result.
- Adaptive shell syntax: Git Bash -n exit 0.
- harness-project.json parses successfully and contains no retired command.
- Active .agents Markdown and executable configuration scan: no retired-script references.
- Scoped git diff --check: exit 0.
- Real Android runtime check: adb devices returned an empty device list. No emulator/device journey or Android build was claimed.

## Handoff

Task: InDevReview. Owner preference authorizes an automatic signed-off commit after validation; no branch, push, PR, merge or release is performed.
Product behavior: Confirmed unchanged. The CI runner's actual device execution remains for its configured GitHub emulator environment.
Skills applied: harness-router and harness-retrospective, read from local SKILL.md instructions.
