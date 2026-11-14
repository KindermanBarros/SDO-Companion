# UI contract — compact adaptive polish

Status: revised references approved by the owner (“Implementa! Pra mim tá bom”); implementation authorized. Preserve compatibility with all existing themes.
Source: [Requested scope](spec_v4.md). Existing design sources: docs/product/design_system.md, DESIGN_GUIDE.md and docs/interface-cybergrunge.md.
The owner explicitly requires preserving the established visual identity while compacting the UI. The existing design guides remain authoritative; compactness does not authorize replacing their typography, geometry or visual language with generic components.

## Identity acceptance criteria

Standard retains its operational HUD, Oxanium functional typography, MB Forever Raw fixed expressive headings, technical grid and asymmetric diagonal cuts.
Cybergrunge retains Press Start 2P functional typography, Acidic headings, Drunk Fonts expressive mastheads, rigid asymmetric structures and visible signal corruption.
Reduce selector footprint, excess copy and unnecessary vertical spacing before changing identity-bearing elements. Preserve 48dp touch targets.
Glitches follow actual component bounds, interaction and state; preserve the specified latent/interference/rupture hierarchy without hiding essential actions.
Review identity alongside density at compact sizes. A calmer generic card layout is not an acceptable substitute for either mode.
Latest owner rule: functional text is never shortened with ellipsis. Short labels prefer one line but may wrap between complete words when needed; widen/reflow the component first. Single-word Cybergrunge card titles autosize. Preserve full names, selections and counts, enlarged text and 48dp targets. No character-level splitting or clipped replacement.
Quick-access buttons and dashboard tab triggers show names without numeric prefixes or counters. Preserve section indices inside character sheets.
Library choices preserve the current enums: origin Tudo/Modelos/Catálogo; category Todas/Itens/Habilidades/Condições/Conhecimentos/Notas. Do not replace this taxonomy with power/magic/rune kinds.

## Shared behavior

Base references cover both modes at 360x800; main screens add 320x800, dashboard 720x360, and dashboard/library 840x800. CYBER-CONTROLS is Cybergrunge-specific. Runtime validation must cover adaptive overlays and enlarged text as well.
Use safe-content insets over a full-window theme background; do not consume additional navigation width on compact phones.
Keep 48dp interactive targets and support system font enlargement. Selected values and actions remain semantic, not color-only.
No new application dependency or navigation architecture is selected.

## State inventory

| State ID | Visible content / entry and exit | Verification focus | Pen source / node / role / PNG |
|---|---|---|---|
| DASH-READY | Dashboard quick access, visible search and compact current-filter triggers; normal navigation exits | All existing filters/reset/actions preserved | [Pen](design/compact-ui-identity-final.pen) / j2tYl, BsToP / full screen / [j2tYl](design/review-final/j2tYl.png), [BsToP](design/review-final/BsToP.png) |
| DASH-FILTER | Owner, campaign or status choice modal with current selection, scroll and dismiss | Narrow width, selection/dismiss/back and restored filter | [Pen](design/compact-ui-identity-final.pen) / u4XZ5M, bgRVJ / full screen / [u4XZ5M](design/review-final/u4XZ5M.png), [bgRVJ](design/review-final/bgRVJ.png) |
| SHEET-BODY | Sheet body/armor index 11 followed by organs 12 | Correct numbering and retained data/edit/read-only behavior | [Pen](design/compact-ui-identity-final.pen) / mhNTt, j7gV3 / full screen / [mhNTt](design/review-final/mhNTt.png), [j7gV3](design/review-final/j7gV3.png) |
| SESSION-READY | Character resources and compact ability summary rows | Name, kind/cost, availability/action remain scannable | [Pen](design/compact-ui-identity-final.pen) / C8Ee3, NCvpU / full screen / [C8Ee3](design/review-final/C8Ee3.png), [NCvpU](design/review-final/NCvpU.png) |
| SESSION-DETAIL | Expanded ability rules and use/confirmation; collapse returns to same row | No lost rule/cost, correct confirmation/read-only state | [Pen](design/compact-ui-identity-final.pen) / zWPx6, j0DylI / full screen / [zWPx6](design/review-final/zWPx6.png), [j0DylI](design/review-final/j0DylI.png) |
| LIBRARY-READY | Search, one Origin trigger, one Category trigger, archive/create controls and results | Every existing source/category still selectable | [Pen](design/compact-ui-identity-final.pen) / EUqg5, Edoja / full screen / [EUqg5](design/review-final/EUqg5.png), [Edoja](design/review-final/Edoja.png) |
| LIBRARY-CHOICE | Origin or category modal with current value and dismiss | Long labels, narrow/wide, scroll, selection and back | [Pen](design/compact-ui-identity-final.pen) / cHYlW, YwNEp, c1HBi, La8jZ / full screen / [cHYlW](design/review-final/cHYlW.png), [YwNEp](design/review-final/YwNEp.png), [c1HBi](design/review-final/c1HBi.png), [La8jZ](design/review-final/La8jZ.png) |
| RACE-READY | Wider race dialog, factual summary, subrace/attribute/power choices and Apply/Cancel | No invented lore, existing selection validity unchanged | [Pen](design/compact-ui-identity-final.pen) / HO9an, yaZ5E / full screen / [HO9an](design/review-final/HO9an.png), [yaZ5E](design/review-final/yaZ5E.png) |
| RACE-CHOICE | Race choice list with brief catalog-derived characteristics | Compare choices without hiding their full power details | [Pen](design/compact-ui-identity-final.pen) / MX8N1, PRaIv / full screen / [MX8N1](design/review-final/MX8N1.png), [PRaIv](design/review-final/PRaIv.png) |
| METADATA | Functional //-separated chunks in affected screens | Chunk accents, neutral delimiters and accessible semantics | [Pen](design/compact-ui-identity-final.pen) / j2tYl, BsToP / full screen / [j2tYl](design/review-final/j2tYl.png), [BsToP](design/review-final/BsToP.png) |
| CYBER-CONTROLS | Affected cards, selectors, fields, headers and navigation in their resting/pressed/selected/error states | Intentional edges, correct strokes, clipped/local effects and stable reading positions | [Pen](design/compact-ui-identity-final.pen) / fWRHR / full screen / [fWRHR](design/review-final/fWRHR.png) |
| RESTORED | Same route/entity/filter/draft/dialog after rotation | No state rollback or extra configuration-only external work | [Pen](design/compact-ui-identity-final.pen) / j2tYl, BsToP / full screen / [j2tYl](design/review-final/j2tYl.png), [BsToP](design/review-final/BsToP.png) |

## IME state coverage

Search fields: DASH-READY and LIBRARY-READY each require same-window keyboard-closed, keyboard-open and dismissed variants.
Any edited race/modal/amount/sheet input in the touched flow receives the same coverage.
Screen behavior: screen-no-toolbar while editing where persistent navigation would obscure actions; modal behavior: sheet-stays-open.
The open reference includes the focused field, essential actions and system-ime region. Dismissal may reuse the closed state only when geometry/content match.
Open dashboard references: PbnL5 (Standard), HhIWp (Cybergrunge); library: yWGRU (Standard), fnnPu (Cybergrunge). All are 360x800 in the same Pen source; PNGs are in design/review-final/<node>.png. Closed/dismissed references reuse their ready-state geometry with restored query/filter values; actual IME dismissal remains a runtime check.

## Dynamic data and fixtures

Use synthetic characters/campaigns, catalog-provided races and representative short/long labels.
Include empty results, active multiple filters, unavailable abilities, archived/read-only data, missing details, validation errors and enlarged text.
Time/user identifiers are fixture data. Never use production screenshots or a defect screenshot as an approved design golden.

## Approval

Initial Pen candidate and PNG exports exist, but the owner rejected their visual direction as losing the application's essence. Rejected drafts were removed by the owner-requested cleanup; the approved final reference remains and earlier artifacts remain in Git history.
Revised source: design/compact-ui-identity-final.pen; all 41 exports are in design/review-final. Fresh headless reopen/export passed with no font/asset warnings, clipping or unfinished placeholders; 994 short texts checked, none wrapped. All 39 original root IDs remain; two origin-choice screens were added.
Decorative title PNGs render the actual repository font files; functional text uses Oxanium/Press Start 2P. Lettering provenance is in design/assets/identity-lettering.json and identity-origin-lettering.json. These are design references, not a proposal to rasterize application text.
Approved review boards: [Standard](design/review-final/xyhIe.png), [Cybergrunge](design/review-final/FecBk.png). Runtime results belong in summary_v4.md; the Pen checks alone do not prove application behavior.

## Additional reference coordinates

All node IDs use the Pen source above and design/review-final/<node>.png; every reference is a full-window frame.
Dashboard 320: jqykr/e2hDMR; library 320: yNWi9/AxlQy; session 320: IU444/kJTZO (Standard/Cybergrunge).
Dashboard 720x360: lRJry/iQW5j. Dashboard 840: z8GkOZ/K3tgvw; library 840: W2BPXO/A46tYg.
Session confirmation overlay: SkM1T/u2t7t; opening it follows Use from the SESSION-DETAIL state.
RESTORED reuses the ready-state visual target; it does not constitute evidence that rotation/state restoration works.

## Owner review corrections

Owner authorized repair of the existing identity after supplied defect screenshots: angular Standard filter/library controls and inset item/ability cards; correct single outlines for Cybergrunge tabs/campaigns; keep all essential text outside signal masks; compact readable badge/navigation labels and library counters/archive control. Remove dashboard quick Session/Master only, preserving destination navigation. Existing canonical identity references remain authoritative. Supplied screenshots are defects, never new goldens.

Owner follow-up: operation quick action remains visible while character details are collapsed. Cybergrunge bottom bar uses centered one-line labels at reduced size with 48dp targets; full-height red background line removed. Dashboard settings sits beside sync.

## Whole-word wrapping rule
All themes wrap only between complete words, without automatic hyphenation or character-level word splitting. If a word does not fit, widen/reflow or autosize the component; keep one-word Cybergrunge card titles on one line. Preserve explicit newlines and semantic text. Native paragraph/no-hyphen settings are the baseline, not proof that an oversized token fits; component/runtime checks must cover long words and enlarged fonts.

Session equipment: name has full card width, category is secondary text below, state/load follows; category must never compress the name into fragments. Use compact inset card spacing. Session body surfaces retain theme outlines and display complete failure counts with autosizing rather than ellipsis.

Functional UI text must remain complete: no ellipsis for item names, category labels, selected values or status/counts. Correct available space first; wrap between words or autosize within legible limits. Clipping is not a substitute. The current equipment repair stacks full-width content and body-count autosize removes the previous ellipsis; unrelated legacy truncation is not certified by this repair.
