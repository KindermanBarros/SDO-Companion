# Always deliver authenticated test APKs - InAgentValidation

- Date time: 2026-11-12 22:45 America/La_Paz
- Task objective: Replace demo CI artifacts with authenticated debug APKs, persist the owner's preference, and publish a verified test artifact.
- Open questions: None. Owner requested authenticated builds; prior push/build authorization covers the corrected test delivery.
- Shared dependencies and references: D1. Existing restore-android-secrets and verify-android-signature composite actions; existing repository secrets, no new dependency. D2. Existing Gradle debug signing uses release credentials when supplied.
- Sub-tasks:
    - 1 Inspect build paths and authorization - InDevReview
        - Verify trigger, secrets restoration, package identity, signing checks and current branch; preserve unrelated changes.
        - Dependencies: See D1 and D2
        - Technology and module changes: None
        - Definition of done:
            - [x] Existing composite actions support authenticated debug builds.
    - 2 Configure authenticated delivery - InDevReview
        - Remove the demo job, enable authenticated APK builds for repository push/manual events, keep signature verification and cleanup, and update the permanent policy.
        - Dependencies: See D1 and D2
        - Technology and module changes: None
        - Definition of done:
            - [x] Test APK delivery has no demo fallback and uses validated Firebase/signing inputs.
    - 3 Verify and deliver - InAgentValidation
        - Run harness checks, commit only scoped files, push, and verify the actual Actions build and artifact.
        - Dependencies: See D1
        - Technology and module changes: None
        - Definition of done:
            - [ ] Successful signature-verified authenticated APK artifact linked to the owner.

Local validation: native structural gate and scoped git diff --check pass. Actual protected-input restoration and signature verification require the GitHub Actions run after push.
