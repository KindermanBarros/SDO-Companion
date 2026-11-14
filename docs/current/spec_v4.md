# Compact adaptive UI polish — requested scope

Status: design approved; owner explicitly authorized implementation and GitHub push with signed CI delivery.
Owner request covers the existing dashboard, sheets, session, reference library, race selection and shared Standard/Cybergrunge presentation.
This is one coordinated UI-polish delivery using the existing architecture. It does not implement the broader product backlog.

## Product Requirements

Existing capability context: character-sheets, campaigns and codex under docs/product/requirements/.
The latest owner message is the source of this delta; existing RPG behavior and rules remain authoritative.
Do not mark the broader Session/Master backlog delivered merely because this request improves component density.

## Requirements

| ID | Requested outcome |
|---|---|
| UI-01 | Dashboard search/filter panel below quick access occupies less height while preserving every current filter, search, reset and selected-value indication. |
| UI-02 | Corpo e armadura carries index 11, consistent with inventory 10 and organs 12. |
| UI-03 | Session powers, magic, runes and ashes are compact and scannable; costs, availability, use/confirmation and complete details remain accessible. |
| UI-04 | Reference-library origin and category each use one selected-value control opening a choice modal, rather than a grid of every option. |
| UI-05 | Race choices expose concise factual characteristics derived from existing catalog stats and power descriptions; preserve racial/subrace selection constraints. |
| UI-06 | Race dialog uses more available width with safe margins and bounded width on large windows; actions and keyboard states remain usable. |
| UI-07 | Meaningful chunks separated by // have differentiated accessible accent colors; separators remain neutral and never become the only semantic cue. |
| UI-08 | Cybergrunge uses restrained functional typography/copy and deliberate outlines/lines/glitch placement; preserve its identity while prioritizing usable controls and readable content. |
| UI-09 | Narrow phones retain content width and reachable controls; wide layouts use space deliberately. Investigate/fix the reported gray left strip using runtime evidence. |
| UI-10 | Rotation preserves route, entity, filters, selections, in-progress drafts, dialogs and scroll as appropriate; avoid duplicate external window/sync work caused only by recreation. Normal Compose recomposition is expected. |
| UI-11 | Apply shared fixes to both Standard and Cybergrunge, honor system font scale and retain minimum accessible touch targets. |

## Design direction to validate

- Compact selectors reduce visible choice count, not touch target size; retain at least 48dp interactive targets.
- Search stays visible; selected filters become short triggers with badges/labels and modal selection.
- Session summary rows prioritize name, kind/cost and action; long rules remain behind details.
- Library gets one Origin trigger and one Category trigger, plus existing archive/search/create functionality.
- Race modal exposes facts and powers without invented lore; width follows current window constraints.
- Functional content uses readable body styles; large distressed headings and constant effects do not compete with every control.
- Short landscape windows keep bottom navigation when the existing rail policy excludes them.
- Use existing Compose APIs and breakpoints; no Navigation 3 or experimental Grid migration is proposed.

## Rule Applicability

| Rule | Decision | Trigger / planned evidence |
|---|---|---|
| ARCH | Required | Shared component/state ownership; source review against current manual wiring/custom navigation |
| IMPL | Required | Compose/state/window changes; scoped code review and build |
| TEST | Required | Real regressions for defects plus behavior assertions |
| SUI | Required | Both modes, compact/wide and input/modal states; runtime interaction and screenshots |
| L10N | Required | Labels/characteristics and formatted metadata; resource/content review |
| NAV | Required | Recreation, back behavior and retained screen/entity state; production-entry tests |
| API | Not applicable | No backend/Firestore contract change; preserve existing save/sync policy beyond configuration-only duplicate-work correction |
| OBS | Not applicable | No telemetry or logging feature requested |
| ANL | Not applicable | No analytics contract introduced |
| SEC | Not applicable | No new permission, credential, network or external-input boundary proposed; reassess if scope changes |

## Acceptance and boundaries

At 320/360dp portrait, short landscape and wide 840dp, in both modes: all existing actions/filters remain reachable; no unintended blank strip or clipping; selectors use modals as requested.
Keyboard-open/dismissed states keep selected values, focused controls and required actions reachable.
Rotation during search, race selection, session amount editing and sheet editing retains the intended state and avoids duplicate configuration-only side effects.
Race guidance must match catalog facts; section numbering and metadata colors remain legible with enlarged text.
Do not promise literal elimination of recomposition or invent a screenshot/renderer pass.

## Open execution decision

The owner approved the revised Pen design and requested implementation: “Implementa! Pra mim tá bom ... Pode ir botar a mão na massa”. Short labels prefer one line; the latest no-truncation rule permits complete-word wrapping when necessary. Quick-access buttons have no numbering/counters.
The owner subsequently requested GitHub push and registered-certificate CI delivery because the signing keystore is not local. Use configured CI secrets; do not deliver the local debug-signed test fixture.

## Owner defect review and reproduction

Owner requested all recorded screenshot findings fixed together: remove only dashboard quick Session/Master; restore angular Standard selectors/inset cards; correct Cybergrunge tab/campaign outlines and masks clipping Body/Attributes text; keep short labels readable and fix library archive contrast, selection, counters and kind labels. No data/API/permission change.
RED on API 36 before relevant fix:
- SharedVisualIdentityTest.standardCardAndOutlinedControlUseAngularShapes: angular card assertion failed.
- CompactDashboardLibraryTest.dashboardFiltersCommitAndCancelPendingStatus: quick Session unexpectedly existed.
- SheetNavigatorCompactTest.shortChoiceValueRemainsReadableInsideNarrowSheet: short choice ellipsized.
- SharedVisualIdentityTest.clickableHeaderDoesNotMaskItsContentCorners: bottom-left content pixel masked by sdoClickable's cut-corner clip.
The latter precisely reproduces the owner-corrected mask diagnosis; removing panel motion alone was insufficient. Owning cards retain shape/border; clickable rows must not clip glyphs.
