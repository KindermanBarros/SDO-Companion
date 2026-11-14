---
name: security-and-hardening
description: Harden Android security boundaries for input, storage, auth, IPC, and network.
---
# Security and Hardening

Operational companion to [android-security.md](../../rules/android-security.md). Apply whenever a change touches auth, local storage, intents/URIs, exported components, Firestore sync, or release configuration.

## Core practices
1. **Input & URI validation**: Validate all IDs, enum strings, route/intent arguments, `content://` URIs, and remote Firestore documents before domain/UI mapping; fail closed on malformed data.
2. **Auth & local storage**: Clear in-memory and local session state on logout; keep credentials, tokens, and keystore values out of source, logs, exceptions, and UI diagnostics.
3. **Exported surface & IPC**: Keep components `android:exported="false"` (except `MainActivity` launcher) and use minimal manifest permissions (`INTERNET` only).
4. **Network & release**: Enforce HTTPS (`usesCleartextTraffic="false"`), keep debug/test hooks out of release builds, and verify R8/signing configuration on release artifacts.

## Verification
- Add unit/integration tests for malformed payloads, auth/permission failures, and safe fallbacks.
- When Firestore rules change, run `npm --prefix firebase run test:rules`.
- Review manifest, Gradle build types, and diff against [android-security.md](../../rules/android-security.md).
