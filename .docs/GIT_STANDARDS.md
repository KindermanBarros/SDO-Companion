# GIT_STANDARDS

> **Sealed.** Edit only when the user explicitly asks. Read for requested git actions.

## Rules

- The owner prefers automatic commits after applicable checks pass; stage only the completed task's files.
- Create branches, pushes and pull requests only when requested.
- Preserve unrelated working-tree changes. Never stage secrets, signing keys, local.properties, build outputs or IDE caches.
- Jira prefix: None; no confirmed Jira ID exists. Do not invent one.
- Until an owner supplies a Jira ID, only lab branches are allowed under this harness.
- Existing fix/cybergrunge-improvements predates setup and is preserved.

## Branches

Use lab/ followed by a short lowercase kebab-case description when there is no Jira ID.
Feature, bugfix and hotfix branch conventions remain Pending until Jira policy is supplied.

## Commits

Use a concise imperative subject of at most 72 characters, and sign off with git commit -s.
Include only subject and Signed-off-by trailer unless the user requests otherwise.
Jira subject format: Pending; no Jira-prefixed subject may be fabricated.

## Remote and pull requests

Platform: GitHub, host github.com.
Repository: https://github.com/KindermanBarros/SDO-Companion.git.
Base branch: main, from refs/remotes/origin/HEAD.

Only when requested, fetch origin/main and rebase before pushing; stop for conflicts or unrelated dirty work.
Use gh pr create --base main with a concrete descriptive title.
Use gh auth status to inspect authentication when needed.

### PR body template

No .github/pull_request_template.md exists. Use these sections in order:

- Related Issue/s ID: None unless supplied.
- Task description
- Test Scenario
- Known Issues / Limitations
- Changes
- PR Checklist
- Screenshots/Videos/Documentation (mandatory for UI tasks)
