# Firebase CLI local setup

Requested by the owner on 2026-11-12. Applied the project-onboarding dependency setup guidance by reading its SKILL.md; no Skill invocation interface is exposed. No application implementation or deployment is part of this request.

- Existing Node.js: v26.13.0; npm: 11.20.0.
- Installed globally with `npm install --global firebase-tools@15.28.2`, matching firebase/package.json. Installation exited 0; `firebase --version` reports 15.28.2 and help works.
- The existing nvm Node directory exposes firebase.exe through PATH; no global PATH edit was necessary.
- Added .firebaserc with default project sdo-companion, matching the existing deployment documentation. Access to that project is not yet verified.
- `firebase login:list --json` found no account; `firebase projects:list --json` failed authentication. `firebase login` generated the browser authorization flow; the owner must finish that flow in their own terminal. No authorization codes or credentials are recorded here.
- npm reported a transitive superstatic engine warning on Node 26 (its declared majors are 20/22/24), deprecated dependencies, and blocked optional install scripts. CLI startup/help/auth-flow generation work; hosting, emulators and package install scripts have not been verified or enabled. No blanket npm allow-scripts policy was changed.

Next validation after login: `firebase projects:list`, then `firebase use` to confirm the configured project is accessible. No deploy, initialization overwrite, production write, credential-file read, local rules change or secret commit was performed. Existing firebase.json and firebase/package.json were preserved.

Installation reference: https://firebase.google.com/docs/cli

Opening the authentication page with Start-Process was rejected by automatic approval review (`blocked by policy`). No browser-opening fallback was attempted. The owner can run `firebase login` locally and follow its displayed link instead.

## Authentication verification

After the owner completed login, projects:list exited 0 and confirmed sdo-companion is ACTIVE and accessible. `firebase use` exited 0 and selected sdo-companion. CLI setup and project access are verified; no deployment was performed.

Security observation: login:list --json includes account credentials after authentication. Do not use that JSON mode for diagnostics or retain its raw output; no credential values were written into repository files. Use project-access checks or filtered/non-JSON account output instead.
