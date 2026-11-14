# Standard interface

Mode-specific companion to the [design system](design_system.md); shared rules live there.

## Identity

An operational cyberpunk / sci-fi HUD with acid graphics and digital neo-brutalism: a technical field terminal, not a generic Material dashboard.
Use a deep canvas, technical grid, asymmetric diagonal cuts, electrical accents and restrained industrial audit marks.
Data leads decoration. Each viewport has one dominant electrical focus; reduce ornament when it competes with a name, value or action.
Do not stack barcodes, seals and diagonal cuts together more than once in the same panel.

## Typography

- Oxanium: functional titles, fields, buttons and reading.
- MB Forever Raw: large fixed uppercase expressive headings only; never paragraphs, character names or critical numbers.
- System monospace fallback: codes, IDs and telemetry. Uppercase is for commands/status/headings, not paragraphs.
- Body minimum 12sp; reading usually 14–16sp; operational labels minimum 11sp before system scaling.
- Bundle fonts and preserve licenses under `licenses/fonts/`: Oxanium SIL OFL 1.1; MB Forever Raw personal/commercial use without modification.

## Geometry and density

- Base grid 4dp; primary rhythm 8/12/16/24dp.
- Main panels: `CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp)`.
- Character cards: top-end 24dp / bottom-start 12dp; cells and inputs: 12dp / 8dp.
- Background grid: minor lines every 12dp (`Grid`, alpha 0.16), major guides every 48dp (`GridGuide`, alpha 0.40).
- Compact density removes excess spacing, never legibility, complete labels or touch area.
- Enlarged reading uses the existing preference (1.16 multiplier); preserve system scaling.

## Theme roles

Choose a semantic role before a color. Primary is the main action/selection/focus; secondary is stable information; error is danger/deletion/lock, never decoration.
Background covers canvas/system bars; surface covers panels; surfaceVariant covers inputs/subdivisions; onSurface/onSurfaceVariant separate primary/secondary text; outline roles express neutral borders.
See [theme and token values](standard_tokens.md). Screens consume `MaterialTheme.colorScheme`, not raw historical palette values.
The light Aperture White theme uses orange actions, blue information, dark text and subtle gray borders; never white-on-white or pale functional text.

## Component contracts

| Need | Existing contract / representation |
|---|---|
| Actions and primitives | `SdoActionButton`, `SdoButton`, `SdoTextButton`, `SdoIconButton`, `SdoOutlinedButton` |
| Clickable surfaces | `SdoCard`; isolate nested actions |
| Filters and responsive actions | `SdoFilterChip`, `SdoResponsiveGrid` |
| Panels and choices | `CyberPanel`, `TechPanel`, `SdoChoicePanel`, `SdoTogglePanel` |
| Text input | `SdoField`, `HudTextField` |
| Empty / error / offline | `SdoEmptyState`, `SdoErrorState`, `SdoOfflineState` |
| Navigation | `SdoNavigationBar`, `SdoNavigationRail` |

Screens prefer these contracts; Material internals/required structural slots remain implementation details. Extract repeated local styling rather than cloning components.
`HudBackground` draws the Void canvas and two grid densities. `TechPanel` uses semantic surface at 96% opacity, chamfered cuts and a 72% accent border.
`SectionHeader` combines a chamfered badge, uppercase heading and technical divider; `TelemetryTag` uses compact technical borders and explicit state text.
`Barcode` uses neutral bars/signal accents; `ComplianceMark` uses `CE//SDO` as restrained fictional branding, not a functional message.
`HudTextField` maps Carbon background, TechCutDark inactive border, primary focus/cursor, LabelFunctional labels and readable foreground text through current semantic roles.

## Content and interactions

- Campaign headers expand/collapse as one action, with accessible direction state. Name/status/role and selectable/copyable invitation code remain visible; metrics/description/actions expand inside.
- Archived campaigns use a neutral border and explicit archive status. Destructive actions stay isolated and require confirmation.
- Long sheet sections use the existing collapsible preference and saveable expansion. Never hide critical errors, save feedback or confirmation actions in a collapsed block.
- Character cards open the sheet and expose identity, race/occupation/level, campaign and sync/lock state; administrative owner details remain permission-dependent.
- Owner transfer is a separate Historian action with searchable profiles, explicit selection/confirmation, first name, disambiguating email and UID metadata; no accidental parent-card activation.
- Lock states govern deletion, not editing; `LOCK.P` and `LOCK.H` remain explicit. Domain/security requirements own authorization details.
- Local visual preferences never mutate character, campaign or Firebase data.

## Motion

| Token | Duration | Purpose |
|---|---:|---|
| RESPONSE | 180ms | Focus, selection, touch |
| TRANSITION | 300ms | Panel/state entry and exit |
| SIGNAL_PULSE | 700ms | Reversible activity pulse |
| TELEMETRY_SCAN | 1100ms | Linear indeterminate progress |

Movement communicates state, not continuous unrelated decoration. Preserve visible Material ripple as specified in the shared guide.
Initial authentication/character loading uses a dedicated state; later sync stays within the panel without discarding context.
Indeterminate processes use a specific textual description, never fabricated percentages; finish immediately without minimum loading delays.
Do not mix more than two rhythms per component. Keep readable content stable; expose progress semantics and respect system animation scaling.
No strobe, shake or rapid high-contrast flashes in Standard; Cybergrunge's separate effects do not apply here.
`CyberLoadingIndicator` uses 15 telemetry segments, scanner and pulse with functional Oxanium/monospace text.

## Responsive layout

- Compact phones use one column; pairs are reserved for short fields. Increase grid cell width for enlarged text.
- Side navigation starts at 600dp width and 480dp usable height; smaller/short windows use bottom navigation. Compact sheets retain their no-bottom-bar behavior.
- Main content maximum width 1120dp; login 640dp. Navigation stays outside that limit and respects safe insets.
- Settings uses adaptive panels at least 360dp before text scaling, with full-width heading/local warning. Filters reflow complete account/campaign names.
- Preserve the current canonical sheet order, page navigation and saved per-page scroll; the approved task contract owns scoped index refinements, not an old page-number table.

## Implementation

Read [core/theme](../../app/src/main/java/com/kinderman/sdo/ui/core/SdoTheme.kt), shared contracts under `ui/shared/`, and [Standard components](../../app/src/main/java/com/kinderman/sdo/ui/standard/components/StandardControls.kt).
