---
name: project-onboarding
description: Discover an Android repository, configure the harness, and adopt or create an approved project foundation.
---

# Project onboarding

Stage order, approvals and gates belong to
[`project-onboarding.md`](../../workflows/project-onboarding.md). This skill owns
the procedures below; it never grants implementation authorization.

## Discover and reconcile

Inspect Gradle settings/build files, version catalogs, manifests, packages, tests and architecture signals directly. Record paths and source/configuration hashes. The observed map is not desired policy; missing evidence is unknown.

Inspect settings, module build files, version catalogs, convention plugins,
projectDir mappings and source-set declarations. Correct the proposed
`harness-project.json`; discovery deliberately never overwrites an existing profile.
Account for every production and test module/source set. Keep file paths relative
to the consumer root; reject external/symlink escapes. Use `main`, `test`, and
`android_test` root arrays and explicit `layers` mappings for module-level or
nonstandard layouts. An empty project is expected to have an invalid draft profile
until AI creates the approved foundation; report pending, never passing.

Inspect navigation conventions. Configure the production destination/graph/host
and instrumented tests with `navigation` file paths and required/optional argument
maps. Do not copy notes-app destinations into a different product. If navigation
requires a different supported architecture, propose the exact checker/rule changes
for approval; do not omit required contracts to get a green result.

## Adopt or build

Use the approved `docs/current/implementation_plan_v<N>.md` and its linked spec,
test plan, applicability decisions and approved UX references as context for the
reused Android stage skills. Do not generate a second competing foundation plan.

For adoption, determine which existing conventions satisfy policy and which need
an explicit exception or approved migration. Do not overwrite existing `AGENTS.md`,
setup prompts, product documentation or application source during installation.

For creation, use the approved product specification, UX references and onboarding
plan to author build settings, pinned toolchain/dependencies, wrapper, modules,
manifest, actual entry point and first real vertical behavior. AI authors this
foundation; deterministic scripts must not replace it with a canned notes app.
Use existing Android implementation/testing skills and generate real tests.
Do not claim Gradle wrapper JARs, Android SDKs, signing files or runtime evidence
exist until verified. Use trusted dependency tooling for binary artifacts.

Configure `commands.build`, `commands.unit_tests`, and available quality commands as
argument arrays. Commands are executed without shell interpolation; invoke `bash`
or `cmd /c` explicitly when a platform wrapper requires it. Only the active CI catalog defines executable gates.
Use clean/rerun test tasks so executed tests produce fresh XML, and include all
approved test modules. Product documentation must be authored from requirements
and approved; installation does not invent it.

## Refresh and verify

After source/build/profile changes, inspect and update observed source-map facts within the approved scope. Preserve unrelated edits and document resolutions before clearing unknowns. No legacy discovery/refresh script gate applies.

Run the workflow's gates and record command, exit code, executed test count,
fingerprints, log/report locations and remaining blockers in onboarding summary.
Preserve existing application sources, approvals, evidence and shared tools. Cleanup requires explicit scope and must not delete durable records as temporary setup.
