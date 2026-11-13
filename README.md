# SDO Companion

Android companion for creating and managing SDO RPG character sheets, using campaigns and consulting the system codex. Source includes Google sign-in, Room persistence, Firebase synchronization and Standard/Cybergrunge visual modes; source presence does not establish release acceptance.

## Target devices

- Android API 30 minimum; compile and target API 37.
- Responsive phone/tablet window layouts exist. Confirmed target hardware: smartphone (owner confirmation); specific models and physical-device acceptance: Pending.

## Features

- Character creation, sheets, progression, equipment and access rules.
- Campaigns, invitations, membership, session operations and historian screens.
- Bundled codex catalogs, local persistence, synchronization and conflict handling.
- Shared journal, character photos, battle grid and online-only editing are planned, not delivered: see [Product overview](docs/product/product.md).

## Build

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Gradle selects JetBrains JDK 25; Android SDK 37 is required. Google-services configuration enables Firebase integration when present; signing reads optional external credentials. See [TECH_STACK](.docs/TECH_STACK.md) for exact versions and configuration.

Test APKs delivered by CI are always authenticated with the configured Firebase project and registered signing certificate. Missing protected inputs fail the build; no demo APK is substituted.

## Permissions

- android.permission.INTERNET; no dangerous runtime permission is declared.
- MainActivity is the exported launcher; no other component is declared.

## Documentation

- [Agent workflow](AGENTS.md)
- [Architecture](.docs/ARCHITECTURE.md)
- [Validation](.docs/VALIDATION.md)
- [Operational workflows and gates](.agents/gates/project-contract.md)
- [Existing onboarding evidence and blockers](docs/current/onboarding-summary.md)

This documentation setup does not resolve the earlier onboarding quality or coverage blockers.
