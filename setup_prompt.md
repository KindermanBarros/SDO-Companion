# First setup prompt

Edit the preferences below, then give this entire file to your AI coding agent from
the Android project's root. The initializer copies it there if it is absent and
preserves your edits on reruns. This file is an instruction prompt, not a script;
initialization does not contact an AI service or run it automatically.

## My project

- Product name: SDO Companion
- What people should be able to do: Create RPG character sheets and use them in campaigns
- First release scope: Create Character Sheets, Manage them and have a codex for my RPG System
- Outside the first release: Nothing for now, this is a growing beta
- Existing app or new project: auto-detect
- Package/application ID for a new app: auto-detect
- Minimum Android version and supported devices: auto-detect
- Backend/API, authentication, offline and storage needs: uses firebase cli
- Design references and accessibility/localization needs: There is a docs folder
- Architecture/DI/module preferences: auto-detect
- Toolchain constraints: auto-detect
- Setup dependency preference: install necessary dependencies after approval
- Cleanup preference: remove unchanged temporary setup files after verification;
  retain harness runtime files, my edited prompt, approvals, architecture and evidence.

## Instructions to the AI

1. Read `AGENTS.md` and the L1 rules. Use
   `.agents/workflows/project-onboarding.md` and its `project-onboarding` skill.
   If the harness is only present at `.harness/`, read those files there first.
   The onboarding workflow, not this editable prompt, owns stage order and gates.
2. Inspect the repository before asking technical questions. Run
   `python3 harness/scripts/project_onboarding.py discover` after installing the
   entry points, or invoke that script through `.harness/` before installation.
   Read the observed architecture map and explain unknowns. Do not treat source
   matches or draft configuration as approved architecture.
3. Adopt existing code without silently migrating its architecture. For an empty
   project, help me define the product and propose a minimal production foundation.
   Ask about unresolved product choices; infer technical facts from evidence.
4. Draft `docs/current/implementation_plan_v1.md` and `harness-project.json`.
   Include exact modules/source roots, architecture decisions, build/test commands,
   policy gaps, setup dependencies, acceptance checks and cleanup ownership.
   Link the approved specification and all ten Rule Applicability decisions.
5. Present the plan and exact protected instruction files for approval before
   changing them or writing application code. Record only approvals I actually give.
6. After approval, implement the real project foundation using the existing Android
   implementation/testing skills. Do not make me manually scaffold a project as a
   prerequisite. Keep required build, test, source and runtime gates enforced.
7. Resolve discovery unknowns, refresh the architecture map, and run onboarding
   verification. Show command results and remaining blockers honestly. Successful
   verification cleans only unchanged temporary files owned by this installation.
   Do not delete this prompt, user files, the shared harness, or approval evidence.
8. Finish with what was created/adopted, verification results and the next product
   delivery step. Do not claim a complete app when only its foundation was built.
