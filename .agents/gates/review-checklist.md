# Review checklist

Read [Project contract](project-contract.md) and select the triggered [CI checks](ci-checks.md).

## Authority and scope

- [ ] Read root policy, approved spec/plan or sprint contract and the linked task state.
- [ ] Preserve unrelated changes, product backlog, previous approvals and evidence.
- [ ] Architecture matches .docs/ARCHITECTURE.md or an explicitly approved scoped change.
- [ ] Generic Hilt, NavHost and single-UiState guidance is not reported as implemented SDO behavior.
- [ ] Existing domain Activity dependency and manual wiring remain explicit observations, not silent policy waivers.
- [ ] No speculative refactor, toolchain upgrade or package move was introduced.

## Application review when source changes

- [ ] Review business-rule ownership, repository boundaries, mappings, auth/session and error/cancellation behavior.
- [ ] State transitions have an owner and loading/error/empty/success behavior is covered.
- [ ] Preserve Standard/Cybergrunge tokens and the approved design system; do not invent LocalAppColors APIs.
- [ ] Visible text, resource names, imports and concurrency follow .docs/CODE_STANDARDS.md.
- [ ] Load android-security rules only for triggered boundaries; verify untrusted input, IPC, storage and network risks.
- [ ] No credentials, tokens, personal data or signing material are exposed.
- [ ] Relevant ViewModel/use-case/mapper/API regressions have meaningful assertions.
- [ ] Applicable build, tests, Lint, static analysis, source and coverage gates pass with actual evidence.

## Runtime and visual claims

- [ ] Required production-entry journey tests exercise real actions, return boundaries and visible post-return results.
- [ ] Platform-bound behavior uses the real shipped Android boundary; fake-only proof is supplemental.
- [ ] IME tests exercise shipped-Activity open/dismiss geometry and successful named JUnit cases.
- [ ] UI screenshots are captured and retained separately from approved design references.
- [ ] Declared visual/rendered-text comparisons have the required configured renderer, method, reference and bounds proof.
- [ ] Missing devices, services, models, templates or checkers remain non-passing.

## Documentation and handoff

- [ ] Documentation-only work passes the native structural gate and its regression fixtures.
- [ ] Changed product claims are reconciled in their existing capability files; planned work is not called delivered.
- [ ] Technical docs, task index and append-only history match the change.
- [ ] Independent evaluator did not implement or test the submitted work.
- [ ] Task enters InDevReview with blockers stated; only developer approval moves it to Done.
- [ ] Automatic commit contains only the validated task scope and a sign-off; no unsolicited push/PR/branch/merge/release.
- [ ] Human approval remains required before merge or release; a commit is not acceptance.
