# Navigation Rules

Follow the custom navigation baseline in [.docs/ARCHITECTURE.md](../../.docs/ARCHITECTURE.md): `SdoApp` manages `AppSurface`, selected character state, and a `rememberSaveable` back stack without Jetpack `NavHost`.

## Rules
- Represent destinations through typed `AppSurface` entries and explicit callbacks; do not scatter raw route strings or boolean destination flags across UI state.
- Pass minimal serializable identifiers (such as character or campaign IDs) rather than full mutable domain graphs across surface transitions.
- Preserve back-stack restoration (`rememberSaveable`) and predictable system-back behavior when adding or modifying surfaces.
- Validate any external intent or deep-link input as untrusted before selecting a surface.
- When surface transitions or back-stack restoration change, verify the real entry-to-return flow.
