# Working economically
Root AGENTS owns these rules; this file is a short execution checklist.

- Read root policy once, then only the active task/backlog, affected source and current-stage skill.
- A continuation starts from its task state, not from repository onboarding.
- Use rg and bounded reads. Print counts/errors/relevant excerpts rather than full status, JSON, logs or base64.
- One owner and one short plan for bounded work; no automatic multi-agent stage chain.
- Reuse authorization and unchanged evidence; a stage name does not create another approval/test/report requirement.
- Select risk-based checks once. Do not run JVM, device, coverage, formatting and full build for every copy/layout change.
- Honor owner CI/manual-test preferences and record their limits.
- Rerun only after a relevant source/config/runtime change or a diagnostic that requires it.
- Do not retry unchanged environmental failures; report the blocker and continue independent authorized work.
- One durable summary links canonical rules and CI evidence. Keep raw temporary output only in .scratch.
- Before every commit promote final artifacts and remove .scratch entirely; the hook enforces absence.
- Final output normally fits three bullets: change, validation/limit, commit/PR or next action.
