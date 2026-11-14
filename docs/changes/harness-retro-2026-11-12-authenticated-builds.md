# Always deliver authenticated test APKs

The owner explicitly requested authenticated builds after receiving a demo APK.
Classification: WORKFLOW_GAP.
The old workflow generated demo branch APKs and restored protected inputs only on main.

## Change

- Removed the demo job and made authenticated debug APK delivery run on repository push or manual dispatch.
- Reused the existing Firebase/signing restoration and APK certificate verification actions.
- Missing inputs fail the build; no unauthenticated artifact is substituted.
- Retained read-only workflow permissions, certificate verification before upload and always-run protected-file cleanup.
- Persisted the preference in AGENTS.md, README.md, TECH_STACK and the shared harness contract.
- Application code, dependency versions and unrelated working-tree changes were preserved.

## Evidence

- Native structural validation and scoped git diff --check passed.
- Commit 78b7b99 was pushed to fix/cybergrunge-improvements.
- [Actions run 43355100969](https://github.com/KindermanBarros/SDO-Companion/actions/runs/43355100969) succeeded.
- Restore protected build files, authenticated debug build, certificate verification, artifact upload and protected-file cleanup all passed.
- Artifact name: sdo-companion-authenticated-debug-78b7b99a00b8d852a05d53384404fb0b44045481.
- Interactive Google sign-in and Firebase behavior on a physical smartphone were not tested by this CI packaging run.

## Handoff

Task is InDevReview. Automatic signed-off commits and the previously requested push publish the scoped configuration and evidence.
No merge, GitHub Release or production-data operation was requested or performed.
