# Compact adaptive UI polish — test plan v4

Canonical applicability: [spec_v4.md#rule-applicability](spec_v4.md#rule-applicability).
Scope/authorization: [implementation plan](implementation_plan_v4.md); references: [UI contract](UI_contract.md).
This is a planned procedure; outcomes belong in summary_v4.md.

## Test Layer Selection

Use JVM tests for factual race guidance and pure policies, Compose instrumentation for control selection/layout/semantics, and shipped Activity for lifecycle/IME claims.
Preserve existing suites. No new testing dependency selected; no Roborazzi/golden acceptance claim without that infrastructure.

## Maintained Test Cases

| ID / state | Given / When / Then | Layer / owner |
|---|---|---|
| T-01 DASH-READY/FILTER | Narrow dashboard; open filters/change/reset/dismiss; every existing filter and result behavior retained, short controls single-line | Compose; dashboard owner |
| T-02 SHEET-BODY | Character sheet body; open section; index 11 precedes body and organs stays 12 | Compose/source; race/body owner |
| T-03 SESSION-READY/DETAIL | Mixed abilities; expand/use/cancel; correct cost/availability and complete rules remain available | Compose; session owner |
| T-04 LIBRARY-READY/CHOICE | Each existing source/category; select/dismiss; unchanged enum semantics/search/archive/create | Compose; dashboard owner |
| T-05 RACE-READY/CHOICE | Each race; inspect facts and pending selection; catalog-correct guidance and valid apply/cancel retained | JVM + Compose; race owner |
| T-06 METADATA | Multiple // chunks; render in both modes; neutral delimiters and differentiated semantic accents | Compose; shared owner |
| T-07 CYBER-CONTROLS | Both modes, narrow/wide/enlarged text; interact with labels; short labels one line, targets >=48dp, no clipping | Existing adaptive tests + focused Compose regression |
| T-08 RESTORED | Actual Activity, edit/search/dialog then recreate; route/entity/pending state retained and configuration-only external work not duplicated | Activity/behavior boundary |

## Commands and reports

RED: smallest owning JVM test via :app:testDebugUnitTest --tests <class>, or selected :app:connectedDebugAndroidTest instrumentation class/method.
GREEN: :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest.
Quality: :app:ktlintCheck :app:detekt :app:lintDebug :app:koverXmlReportDebug; report actual existing debt and applicable coverage.
Runtime: :app:connectedDebugAndroidTest (or documented direct adb instrumentation for the named class).
Reports: app/build/test-results/testDebugUnitTest and app/build/outputs/androidTest-results/connected; retained screenshots under run-scoped QA evidence.
Nonzero test counts and zero relevant failures are required. No skipped/unavailable runtime is treated as passed.

## IME Production Boundary

| States | Planned shipped entry / assertion |
|---|---|
| Dashboard search closed/open/dismissed | Launch production MainActivity with configured authenticated app; follow actual dashboard entry, tap search, wait for nonzero IME bounds, verify input/actions above keyboard, dismiss and verify restored query |
| Library search closed/open/dismissed | Navigate through actual campaign/library entry; repeat open/dismiss and verify category/origin/query retained |
| Race/session pending choices | Open actual flow, change pending value, recreate Activity, verify selected value; cancel/apply semantics retained |

Missing credentials/fixtures block the affected shipped-entry claim; isolated Compose tests supplement it without replacing it.

## Ephemeral Test Cases

| QA ID | Intent / preconditions | Expected outcome |
|---|---|---|
| QA-01 | Authenticated debug app on existing AVD; both modes at 320/360 and 840dp | Controls reachable, no blank strip, no short-label wrapping |
| QA-02 | Existing character, dashboard query and pending dialog; rotate portrait/landscape | Same route/entity/filter/pending values; no configuration-only sync duplication |

1. Record APK hash, build/auth identity, device/API/density and window dimensions; install the authenticated artifact.
2. Exercise QA-01 through actual available app entry points, capture before/after adb screenshots and compare to the mapped approved references; note fixture/appearance differences.
3. Exercise QA-02 with a search and pending race/session edit; rotate, inspect retained values, then return to portrait and dismiss/apply deliberately.
4. Capture actual IME open/dismiss states for editable search fields and record any blocked authenticated route rather than substituting demo.
5. Restore display/density/orientation settings; stop only the run-owned emulator without wiping data. Retain logs/screenshots and execution outcomes in summary_v4.md.

## API Integration Tests

Not applicable: no API/DTO/endpoint change. Do not call production Firebase for deterministic regression fixtures.

## Owner review regressions

- R-01 SharedVisualIdentityTest: Standard item/control angular geometry and stationary Cybergrunge content while decorative signal animates.
- R-02 CompactDashboardLibraryTest: quick Session/Master absent, settings retained; every modal filter remains selectable in both modes.
- R-03 SheetNavigatorCompactTest: 320dp Cybergrunge body navigation with 1.5 system font scale; position text stays on one line.
- R-04 ADB captures: synthetic sheet cards/body/attributes, dashboard/library and selected choice in both modes. Inspect outlines, masks, clipping, text contrast and short labels; screenshots are partial review evidence, not Roborazzi acceptance.
