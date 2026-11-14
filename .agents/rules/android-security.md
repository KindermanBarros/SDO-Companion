# Android Security Code Rules

Apply this rule (`SEC: Required`) whenever a change touches authentication, session state, local storage, `Intent`/`Bundle`/URI inputs, exported components, network/TLS, Firestore security rules, WebView, or release signing/shrinking. Otherwise record `SEC: Not applicable — no security boundary changed`.

## Trust-boundary invariants
1. **Untrusted input**: Treat user text, remote Firestore/auth payloads, `Intent` extras, deep links, URIs, and imported files as untrusted. Validate type, bounds, scheme, and allowed values before use; fail closed with a safe fallback.
2. **Secrets & PII**: Never hardcode or commit API keys, signing credentials, `google-services.json` secrets, or tokens. Never log tokens, passwords, credentials, or full user sheet/campaign payloads in Logcat, exceptions, or telemetry. Clear sensitive in-memory and local session state on logout.
3. **Transport & backend**: Enforce HTTPS/TLS in production (`usesCleartextTraffic="false"`). Any local emulator/loopback test endpoint must be strictly gated to debug/test runs (`127.0.0.1` / `10.0.2.2`) and never fall back to production. Client checks do not replace Firestore security rules.
4. **Component exposure**: Keep Android components `android:exported="false"` unless required (`MainActivity` launcher is the only exported component). Validate any incoming intent data before privileged actions.
5. **Release hardening**: Release builds must not be debuggable or bundle test hooks/staging endpoints. Delivered test/release APKs must be authenticated with the configured Firebase project and signed with the registered certificate.

## Evidence when triggered
- Document the affected trust boundary and failure/fallback behavior.
- Add unit/integration tests for input validation, auth/sync failure handling, or Firestore rules (`npm --prefix firebase run test:rules` when rules change), plus real runtime proof when platform/IPC behavior changes.
