# PR136 integration evidence

2026-11-12. Implemented; CI verification pending. User explicitly directed implementation and publication with CI-only validation after the local harness prerequisite failures. This exception permits proceeding without local runtime/RED gates; it does not certify tests as passing.

## Scope and resolution

- Rebased both PR commits cleanly onto main eda8cd9501336c9567ac765fc6e6adaa38fb0202.
- Preserve the access-state subtitle in both interfaces; hide only the redundant character name in Cybergrunge.
- Branch debug artifacts are explicitly demo-only in their name and build summary and receive no credentials.
- Authenticated debug packaging runs only on trusted main, restores existing protected Firebase/signing files, checks the certificate, and always cleans up files. No new authentication provider, credential or database behavior.
- Both debug jobs execute the checked-in Gradle wrapper.

## Harness and validation

Read AGENTS.md, bug-fixing workflow, requirement-analysis, android-implementation and android-testing guidance, templates and living character-sheets requirements. No Skill invocation interface is exposed; repository instruction fallback used.

Local baseline: lifecycle validator exit 1 (tracker has no feature rows); adb and agent-device exit127 (not installed); Java21 instead of25; wrapper assembleDebug --offline exit1 (distribution uncached; Network is unreachable). These failures are retained rather than reported passing.

Added CharacterSheetAccessInstrumentedTest with two cases mounting the actual CharacterSheetScreen under Standard and Cybergrunge and asserting the archived/read-only explanation. Existing PR CI compiles instrumentation but does not run an emulator: these tests remain runtime-unverified until executed. CI lint/JVM/build results must be read on the final head before integration. A main-only authenticated artifact cannot be validated by untrusted PR packaging.

## Product Requirement Reconciliation

| Capability file | Existing claim reviewed | Disposition | Current requirement location or retired status | Evidence / qualification |
|---|---|---|---|---|
| docs/product/requirements/character-sheets.md | Players can manage their character sheets. | Confirm unchanged | Requirements SHEET-002 | Unchanged because this fix restores the existing ability to manage sheets without expanding capability; runtime verification remains pending CI/device. |

## Remaining gates

CI checks on published head; independent review; instrumented runtime for the two added cases; authenticated main packaging after integration. Not ready for automatic merge until required checks pass. No local evidence receipt or lifecycle acceptance is fabricated.
