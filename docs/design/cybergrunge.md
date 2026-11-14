# Cybergrunge interface

Mode-specific companion to the [design system](design_system.md). Preserve the approved alternative identity; it is not a dark recolor of Standard.

## Mode boundary

Standard remains the stable default. In Settings, Cybergrunge follows Standard and has a yellow EXPERIMENTAL badge only at the interface selector, not on mastheads or panels.
Hide theme selection while Cybergrunge is active; restoring Standard restores the stored theme.
The visual mode changes presentation, not data, permissions, RPG rules or navigation semantics.
The previous implementation plan described foundation work incorporated in PR #128; it is historical delivery guidance, not an outstanding ten-stage mandate. See [Memory](../MEMORY.md).

## Identity

A functioning retrofuturist terminal suffering video failure and intrusive signal corruption. Convey horror through graphics, not invented biographies or decorative phrases about fear/corruption.
Use rigid blocks, asymmetry, hard cuts, signal rails, scanlines, terminal grids, digital tearing, RGB separation, compression blocks and duplicated frames.
Keep the surface electronic: no aged paper, paint, concrete, all-over dirt, conventional rounded cards or poster-like illustrations.
Incomplete anatomical/humanoid forms may emerge as corrupted data; do not replace component chrome with clean illustration.
Represent a static phase of the actual layered effects in Pen; a red stroke on a black rectangle is insufficient fidelity.

## Typography and palette

- Press Start 2P is the functional terminal face, including Portuguese glyphs; functional sizes follow existing 10–14sp tokens before reading/system scaling.
- Acidic is for large headings; Drunk Fonts is for expressive mastheads. Missing decorative glyphs use the platform fallback.
- Preserve full readable text and the shared wrapping rule; single-word card headings autosize. The old truncation/two-line ellipsis allowance is retired.
- Use the current mode's semantic tokens: deep black/paper, signal red, restrained cold cyan, terminal green and rust/amber warnings.
- Functional red is #FF4D70 with #090608 button text; #E0003B remains structural red. Do not repurpose Standard palette meanings or hardcode isolated colors in screens.
- Preserve the theme's role mapping for primary/selected, positive telemetry, recoverable warning, destructive and disabled states.

## Interaction and components

Cybergrunge replaces Material ripple with bounded compression, brief signal displacement and recomposition feedback. This exception is owned here and scoped to this mode; feedback is never simply removed.
Navigation combines direction, current section and meaningful labels; compact destinations retain at least 64dp height. Follow shared accessibility and touch rules.
Panels keep rigid structures and clean data within disruptive edges. Buttons compress and return after interaction. Selection reads as a captured signal; loading reconstructs signal; errors show localized rupture plus a clear recovery action.
Cards may shake and fragment their leading/trailing border in phase with displacement, but effects belong to the component's actual bounds and cannot cover nested actions or hide essential data.
Use `Sdo*` shared contracts for actions, surfaces, fields, choices, navigation and empty/offline/error states. Screens declare content once; the design system selects mode-specific rendering.

## Empty fields

Use visual/typographic interference rather than example text or fictional instructions. Keep the real label, semantics, cursor and required/optional distinction.
An empty field may combine displaced pixel bands, separated red/cyan channels, fragmented masks and signal loss; populated fields immediately reduce the interference.
Long input keeps a readable central region. Preserve the implemented static high-contrast empty-field texture rather than imposing an unapproved continuous animation loop.

## Motion hierarchy

| Level | Meaning | Examples |
|---|---|---|
| Latent | Low-intensity background signal | Scanlines, small channel drift, phosphor fluctuation, ghost data |
| Interference | Actual interaction/state transition | Touch, navigation, expansion, save, value change; brief displacement/duplication/reassembly |
| Rupture | Prominent empty field, failure or major event | Local shaders, masks, blur, particles and stronger deformation; preserve the exit/recovery action |

Continuous low-amplitude corruption belongs to this opt-in identity, not Standard. Strong rupture is state/event-bound rather than permanently maximized on every element.
The dramatic composition remains usable: shared readability, accessibility, complete text and reachable actions take precedence over hiding information for effect.
Effects may use shaders, masks, layers, blur and particles when supported by existing implementation. Bind work to composition/lifecycle; release buffers and observers on exit.
Animate visible list items only; reuse resources; rapid interactions must not accumulate orphaned/infinite jobs. Standard must not compose Cybergrunge-only effects.
Honor existing render budgets; do not expand cost or add dependencies to satisfy the historical plan.

## Phone composition

Prioritize vertical phones, thumb-reachable controls and one dominant impact area. Concentrate dense signals at margins/background while retaining one functional content column.
Expressive headings must not consume most of the viewport. Ghost codes/data are background atmosphere, not fabricated user/domain content.
Affected wide/compact states follow the approved task contract; responsive layout does not prove additional device acceptance.

## Implementation and verification

| Responsibility | Existing source area |
|---|---|
| Mode selection, responsiveness and public tokens | `ui/core/` |
| Shared screen-facing contracts | `ui/shared/` |
| Alternative semantic colors/type/shape | `ui/cybergrunge/theme/` |
| Code-native bounded effects | `ui/cybergrunge/effects/` |
| Panels/actions/fields/choices/navigation | `ui/cybergrunge/components/` |
| Graphic assets | [CybergrungeAssets.kt](../../app/src/main/java/com/kinderman/sdo/ui/cybergrunge/CybergrungeAssets.kt) |

The Settings laboratory demonstrates actions, fields, choices, toggles and empty/offline/error states; it is a reference, not a separate design authority.
Use the current [validation policy](../engineering/VALIDATION.md). Existing render-budget tests and instrumented compilation are useful when affected; historical instructions to run every suite/video at every stage are superseded.
Approved Pen exports establish visual intent only. Record actual runtime evidence and missing coverage in the task's Memory entry.
