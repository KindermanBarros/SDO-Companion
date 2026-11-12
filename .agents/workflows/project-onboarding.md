---
description: Adopt an Android app or let AI create its approved foundation before feature delivery.
---

# Project onboarding

This workflow owns first setup and deliberate onboarding refreshes. It does not
replace product specification, UI design approval, normal delivery, or acceptance.
Invoke `project-onboarding` through the available skill interface; if unavailable,
read its `SKILL.md` and record that interface limitation in onboarding evidence.

## 1. Discover

Read root `setup_prompt.md` if present, `AGENTS.md`, and L1 rules. User preferences
persist, but a prompt cannot fabricate approval or turn a failed gate into a pass.
Run `python3 harness/scripts/project_onboarding.py discover` from the consumer root.
Inspect `docs/architecture/architecture-map.md` and its JSON source evidence.
Use `adopt` for an existing app and `create` when a project foundation is absent;
static detection is only a proposal. Distinguish a broken existing build from an
empty project. A broken baseline must not be relabeled greenfield to bypass repair.

## 2. Define and approve the foundation

Use the existing requirements/specification skills and Rule Applicability template.
Record the complete ten-row matrix in the approved specification. For UI scope,
follow the existing UX stage and design-system approval before UI implementation.
Write `docs/current/implementation_plan_v1.md`, linking the canonical specification
and its `#rule-applicability` anchor. Cover:

- adoption/creation mode, product boundaries, architecture decisions and exceptions;
- actual Gradle module names, directories, source sets, layer roots, navigation
  contracts, build commands and test reports in `harness-project.json`;
- any root-module builds, convention plugins, aliases, projectDir remaps, Java-only
  modules and custom source sets that automatic discovery could not resolve;
- required tools/dependencies, first-build acceptance, runtime boundaries and setup
  file ownership/cleanup; do not install a browser unless the approved design path needs it;
- exact existing deliverables and protected instruction files requiring changes.

Review and resolve every profile unknown with source evidence or a documented
decision. Profiles configure paths/contracts, never source-rule exclusions.
Keep the canonical specification and planned tests in `docs/current/spec_v<N>.md`
and `docs/current/test_plan_v<N>.md` for the reused Android stage skills. Include
their exact paths and approval-time hashes in the implementation plan, so the same
user approval covers the foundation and its tests. Increment versions for revisions
and set `approval.json.plan` to the exact approved implementation-plan version.
Present the plan and profile for explicit user approval before application source
changes. Approval of initial product scope is not approval of an unseen scaffold.
Record actual consent in `docs/onboarding/approval.json` using the approval template,
with the approved plan/profile SHA-256 hashes. Reapproval is required when those
approved inputs change. Never fill approval fields on the user's behalf before consent.

## 3. Adopt or create

Follow the `project-onboarding` skill. In adoption, preserve existing source and
identify policy mismatches for explicit resolution, rather than silently migrating.
In creation, AI writes the approved Gradle/application/test foundation, including a
real first behavior and meaningful tests. Use `android-implementation` and
`android-testing`; load their required stage rules. No dummy production callbacks.
This stage is the explicit exception to requiring an existing green build *before*
creating a new project. It does not waive any gate after creation.

Use the approved command arrays rather than assuming `:app`. Existing custom
instrumented/visual runners must be adapted through separately authorized files
when they cannot execute the approved module/runtime boundary; never record their
legacy `app` configuration as evidence for a different module.

## 4. Verify

Refresh the observed map with `discover --refresh`, review it against the profile,
and run `python3 harness/scripts/project_onboarding.py check`.
Run `python3 harness/scripts/project_onboarding.py verify`. It requires matching
plan/profile approvals, product documentation, fresh fingerprints, the lifecycle
gate, full source checks, a build and non-zero passing Gradle test XML evidence.
Run other required gates from the approved applicability decisions, including
quality, coverage, design and real runtime checks when triggered. The base verifier
does not substitute for these gates. Missing devices/toolchains/dependencies remain
blocked. Keep results under `.harness-state/` and reference them in
`docs/current/onboarding-summary.md` with paths and one-line excerpts.

## 5. Cleanup and handoff

Only successful base verification automatically removes unchanged temporary setup
files from the installation manifest. Runtime harness links/files, architecture,
approvals and evidence remain. User edits are preserved. Failed or interrupted
setup retains its files for diagnosis and can be rerun.

For deliberate removal, first review
`python3 harness/scripts/project_onboarding.py cleanup --uninstall`, then use
`--uninstall --apply` when removal is authorized. Uninstall removes only unchanged
owned files/links, never recursively deletes directories or shared dependencies,
and retains its manifest/receipt. Scheduled jobs, SDKs and shared browser caches
are outside this installer and require their own explicit removal.

Handoff to the appropriate delivery workflow only when the approved onboarding
gates pass. The generator retains its normal baseline, approval and review gates.
