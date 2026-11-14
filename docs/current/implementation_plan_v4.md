# Compact adaptive UI polish — implementation plan v4

## Feature / Bug

Implement the approved compact UI while preserving all Standard themes and Cybergrunge identity.
Authorization: owner approved the visual references and explicitly requested implementation without another approval stop: “Implementa! Pra mim tá bom ... Pode ir botar a mão na massa”.
This authorization covers this implementation/test plan within the established scope; no separate review of these files is claimed.
Canonical references: [UI contract](UI_contract.md), [requirements](spec_v4.md).
Deleted legacy templates are unavailable; retain the existing repository plan headings and actual native gates.

## Requirement summary

UI-01–UI-11: compact dashboard/session/library, body index, factual race guidance/wider dialog, colored metadata, responsive insets/state and Cybergrunge polish.
Short control labels prefer one line and wrap complete words rather than truncate when needed; quick access has no numbering/counters. Existing game rules, theme choices and architecture remain authoritative.

## Rule Applicability Implementation

Canonical decisions: [spec_v4.md#rule-applicability](spec_v4.md#rule-applicability).
ARCH/IMPL: keep existing component/theme and state ownership; inspect scoped diff and compile.
TEST: maintained behavioral regressions in [test plan](test_plan_v4.md).
SUI: instrumented compact/wide/overlay checks and adb captures against approved references.
L10N: resource-backed new copy, catalog-derived facts, semantic theme accents.
NAV: saveable selection/modal state, recreation and real lifecycle distinction; production-bound evidence where available.
analytics: none. Data/DTO/API/OpenAPI changes: not applicable; no endpoint or schema changes.

## Impact summary and exact file scope

| Ownership | Files / responsibility |
|---|---|
| Dashboard/library implementation | presentation/dashboard/DashboardScreen.kt; presentation/historian/HistorianDashboardScreen.kt; scoped tests and a dedicated strings resource file |
| Race/body implementation | presentation/character/RacePickerDialog.kt; CharacterAdvancedSections.kt; scoped race guidance/selection tests and strings |
| Session implementation | presentation/session/SessionModeScreen.kt; scoped tests and strings |
| Shared UI/root | ui/shared/Sdo*Components.kt, ui/standard/components/StandardControls.kt, ui/core/SdoTheme.kt and SdoResponsive.kt, ui/cybergrunge/components/effects only as needed for actual defects |
| Configuration/state | presentation/SdoApp.kt, AppViewModel.kt, character/CharacterSheetScreen.kt only for source-confirmed recreation/state loss |
| Validation | Existing src/test and src/androidTest suites; focused new regression tests; no new dependency selected |
| Documentation | UI contract/spec/summary/task state, README and relevant living capability requirements; append history |

Do not change data schemas/catalog rules, replace fonts, rasterize application text, migrate navigation or commit pre-existing unrelated edits/deletions.

## UiState design

Preserve current flows/custom saveable navigation. Modal visibility and pending choices use saveable stable values keyed to owning entity.
Preserve existing filter/search/reset/read-only semantics. Long descriptions expand; name/kind/cost/action stay visible in compact ability rows.
Race summaries derive from existing stats/powers without new lore or persisted fields.

## UI State Stages

Every row uses its named Pen node/PNG in the approved contract; exact runtime methods are in the test plan.

| Stage | Implementation | Verification |
|---|---|---|
| DASH-READY | Compact visible search/filter triggers, unnumbered actions | Dashboard controls, 320/840 and theme checks |
| DASH-FILTER | Saveable filter modal preserving every selection/reset | Modal choice/dismiss and restoration |
| SHEET-BODY | Existing section index 11 | Sheet index and screenshot |
| SESSION-READY | Compact keyed ability rows; derive descriptions once per data change | Cost/availability/action and narrow text |
| SESSION-DETAIL | Expand complete rules without losing use flow | Expand/collapse and use/confirmation |
| LIBRARY-READY | One origin/category trigger each | Existing enums and archive/search/create controls |
| LIBRARY-CHOICE | Full choice modal for each enum | Every enum remains selectable |
| RACE-READY | Window-bounded wider modal and saveable pending choices | Selection/apply/cancel and recreation |
| RACE-CHOICE | Factual brief guidance per race | Catalog-consistent summary and narrow layout |
| METADATA | Theme-semantic chunk accents, neutral separators | Annotated spans and rendering |
| CYBER-CONTROLS | Preserve fonts/cuts/effects; single-line controls, compact functional spacing | Both modes, enlarged text, component bounds |
| RESTORED | Key window work to inputs; configuration-aware lifecycle handling | Real recreation/pending state and background behavior |

## Navigation Flow

No routes added. Dialog Back/dismiss returns to unchanged screen and saved query/selection. Keep current width/height rail policy.

## Test plan

[test_plan_v4.md](test_plan_v4.md). Run focused RED before each behavioral fix, then combined unit/build/instrumented gates.
Build with configured Firebase/authentication; never deliver demo fallback. No push requested in this turn.
Runtime repair: the existing API 37 image fails inside Espresso InputManager before assertions. Preserve baseline APKs and run RED/GREEN on an isolated workspace-local stable Android test image using checksum-verified official Android CLI tools; do not upgrade app dependencies to mask the runtime failure.

## Risks and compatibility

Use semantic Material theme roles, existing Sdo component contracts and bundled typography. Do not hard-code prototype colors into screens.
Recomposition itself is normal; fix duplicate external work/state loss. Reproduce inset and rotation reports before making acceptance claims.
Existing quality debt must be reported separately; do not add suppressions or weaken gates. Runtime fixtures must not use production backend data.

## Files to delete and cleanup

After approval, remove only this run's rejected/intermediate Pen/export artifacts after retaining the approved source/images and provenance. Preserve unrelated user files and deletions.
Stop only run-owned emulator processes; preserve AVD data. Automatic scoped commit follows validation; no branch/push without authorization.

## Out of scope

Broader backlog, game mechanics, Firebase contract/security/signing changes, dependencies, production data and navigation migrations.

## Owner defect-review revision

Authorization: owner supplied Standard/Cybergrunge screenshots, corrected the outline/mask diagnosis, then requested all recorded fixes together. This is a repair of the approved identity, not a new visual direction.
1. RED: shared angular card/control regression, missing quick-access regression and narrow enlarged sheet-navigation text regression; use existing API 36 runtime.
2. Shared fixes: SdoPrimitiveControls, CybergrungeComponents/effects honor caller shape/border, theme card background and stable unmasked content. Preserve signal animation in decoration.
3. Screen fixes: DashboardScreen/ HistorianDashboardScreen compact rows, theme selectors/modal choices, archive contrast, human kind labels and removal of quick Session/Master; keep bottom navigation.
4. Sheet fixes: CharacterSheetPager compact single-line navigator; AbilitySection, CharacterAdvancedSections and PhaseOneStrictInventory replace direct rectangular surfaces with SdoInsetCard and semantic metadata; responsive TwoFields prevents short choice truncation.
5. GREEN: focused Android tests, existing 44 UI regressions, JVM/build/lint/static/coverage commands; adb captures and independent review. No new dependency or route/data change.
6. Update this PR and signed authenticated CI APK using existing authorization. Failed baseline quality/coverage and missing pixel/production-journey evidence remain explicitly non-passing.
Dependencies: existing Compose/Material/AndroidX tests, existing shared components and registered CI secrets only. No module or technology change.

Follow-up authorized by owner: expose operation quick action outside expansion; reduce/center Cybergrunge bar while keeping 48dp targets; delete background vertical line; relocate settings to sync row. Reuse existing callbacks/tokens; build and one affected test class only, per owner request.
