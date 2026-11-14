---
name: pen-design-editor
description: Create and edit SDO .pen designs following the project styleguide via the headless pen.dev CLI.
---
# Pen Design Editor

Use whenever a `.pen` file is the design source of truth. Always use the headless CLI ([references/headless-cli.md](references/headless-cli.md)) — never edit `.pen` JSON directly or require the desktop GUI.

## Required SDO styleguide
Before creating or changing frames, read [DESIGN_GUIDE.md](../../../DESIGN_GUIDE.md): current interface, components, direction/principles, typography, geometry, and sections relevant to the target screen. [design_system.md](../../../docs/product/design_system.md) is an index, not a substitute for the guide.
For Cybergrunge, also read [the complete visual direction](../../../docs/interface-cybergrunge.md). It is a distinct interface, not a dark recolor of Standard.

- Inspect the actual [theme and typography](../../../app/src/main/java/com/kinderman/sdo/ui/core/SdoTheme.kt), [shape/spacing tokens](../../../app/src/main/java/com/kinderman/sdo/ui/core/SdoTokens.kt), and implementations of the components being represented. Read the matching existing screen; compare with an Android CLI screenshot when available, and record when runtime reference is unavailable.
- Map each reused component in `UI_contract.md` to its source, observed geometry/type/spacing/color roles, and Pen node. Reproduce these properties; naming a generic rectangle `SdoActionButton` does not establish fidelity.
- Standard retains its technical HUD hierarchy, guide typography, grid and diagonal cuts where specified by the component; do not substitute generic rounded cards, pill controls or arbitrary fonts.
- Cybergrunge retains asymmetric cuts, signal rails, layered scanlines/interference, terminal/editorial hierarchy and component chrome. Inspect [components](../../../app/src/main/java/com/kinderman/sdo/ui/cybergrunge/components/CybergrungeComponents.kt) and [assets](../../../app/src/main/java/com/kinderman/sdo/ui/cybergrunge/CybergrungeAssets.kt); a red line on a black frame is insufficient. Render a representative static phase of its visual layers; only motion itself may be deferred to runtime.
- Derive colors from active semantic theme roles, not isolated palette constants. Distinguish primary, secondary, destructive and disabled controls; preserve 48dp touch targets, whole-word wrapping and complete functional text.
- If guide, source and runtime disagree, identify the exact discrepancy before choosing a visual direction; do not silently replace the documented identity with tool defaults or a generic style archetype.

## Workflow
1. **Setup**: Verify `pen interactive --help`. Start headless on a candidate copy inside `.scratch/` (or workspace candidate path): `pen interactive --in <original.pen> --out <candidate.pen>`. Run `read_skill()`, `read_skill({ path: "execute.md" })`, `read_skill({ path: "pen-schema.md" })`, and `get_app_state()`.
2. **Inspect**: Apply the required SDO styleguide above; use `Get` to inspect hierarchy, screens, components, and node IDs. Export a before-image when modifying an existing screen.
3. **Edit**: Build one representative frame per affected visual mode first, export and inspect it against the guide/component mapping before generating the remaining states. Apply minimal `Insert`/`Update`/`Replace` operations matching `UI_contract.md`. Re-read modified nodes with `Get` and save to the candidate path with `save()`.
4. **Verify**:
   - **Visual**: Export PNG (`pen --in <candidate.pen> --export <png>`) and inspect styleguide fidelity (geometry, typography, hierarchy, graphic layers and semantic action states), alignment, contrast, whole-word wrapping, and absence of clipped/ellipsized text. A successful export or clean bounds check alone is not visual approval.
   - **Structural**: Confirm unrelated screens/nodes and shared components remain intact, and reopen the candidate in a fresh headless session.
5. **Finalize after approval**: Once the user approves, promote the validated candidate over the canonical `.pen` file, retain only the approved review PNGs, and remove temporary candidate/intermediate exports from `.scratch/` before commit.
