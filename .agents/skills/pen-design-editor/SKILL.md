---
name: pen-design-editor
description: Inspect and edit .pen designs via the headless pen.dev CLI.
---
# Pen Design Editor

Use whenever a `.pen` file is the design source of truth. Always use the headless CLI ([references/headless-cli.md](references/headless-cli.md)) — never edit `.pen` JSON directly or require the desktop GUI.

## Workflow
1. **Setup**: Verify `pen interactive --help`. Start headless on a candidate copy inside `.scratch/` (or workspace candidate path): `pen interactive --in <original.pen> --out <candidate.pen>`. Run `read_skill()`, `read_skill({ path: "execute.md" })`, `read_skill({ path: "pen-schema.md" })`, and `get_app_state()`.
2. **Inspect**: Use `Get` to inspect hierarchy, screens, components, and node IDs. Export a before-image when modifying an existing screen.
3. **Edit**: Apply minimal `Insert`/`Update`/`Replace` operations matching `UI_contract.md` states and SDO themes. Re-read modified nodes with `Get` and save to the candidate path with `save()`.
4. **Verify**:
   - **Visual**: Export PNG (`pen --in <candidate.pen> --export <png>`) and inspect alignment, typography, contrast, whole-word wrapping, and absence of clipped/ellipsized text.
   - **Structural**: Confirm unrelated screens/nodes and shared components remain intact, and reopen the candidate in a fresh headless session.
5. **Finalize after approval**: Once the user approves, promote the validated candidate over the canonical `.pen` file, retain only the approved review PNGs, and remove temporary candidate/intermediate exports from `.scratch/` before commit.
