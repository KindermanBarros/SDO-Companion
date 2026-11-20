# SDO Companion

Android companion (API 30+, target/compile SDK 37) for SDO RPG character sheets, campaigns, Session/GM play modes, and the system codex, featuring Standard and Cybergrunge visual modes, offline-first Room persistence, and Firebase Spark synchronization.

## Key Capabilities

- **Character Sheets**: guided creation wizard (with Heritage Points budget), full sheet management (attributes, skills, body/organs, inventory, powers, mystic abilities, conditions, transports, familiars, notes), and 1:1 cropped profile avatars.
- **Campaigns & Play Modes**: invites, active membership, shared campaign journal, compact Session Mode, and Historian/GM telemetry & quick actions.
- **Codex & Rules**: spoiler-free compendium catalogs plus 1,853 structured atomic rules from the Player's Handbook.
- **Spark Synchronization**: 100% Firebase Spark Firestore Protocol v2 (`protocolVersion = 2`) with offline local intents, per-field/item revisions, and explicit 3-way conflict resolution. Deployment procedure: [Firebase deploy](docs/firebase-deploy.md).

## Build

```powershell
.\gradlew.bat :app:assembleDevDebug
```

See [TECH_STACK](docs/engineering/TECH_STACK.md) for SDK/dependency versions and [VALIDATION](docs/engineering/VALIDATION.md) for test selection.

## Permissions

- `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` (no dangerous runtime permissions).

## Documentation

- [Documentation index](docs/README.md)
- [Agent workflow](AGENTS.md) and [ownership map](.agents/README.md)
- [Product backlog](docs/product/product.md) and [Memory](docs/MEMORY.md)
- [Architecture](docs/engineering/ARCHITECTURE.md) and [Operational gates](.agents/gates/project-contract.md)
