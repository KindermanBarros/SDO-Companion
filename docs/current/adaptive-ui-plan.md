# Adaptive UI update — bounded delivery plan

Date: 2026-11-12. Stage owner: independent planner. Implementation is present; CI verification is pending. No runtime/screenshot acceptance is asserted before the published jobs complete.

## Context and authorization

The user requests installation/use of `android/skills/jetpack-compose/adaptive` and improved positioning/adaptation of the existing Standard and Cybergrunge interfaces. The preceding explicit instruction delegates Android validation to CI rather than local device/build gates. This plan uses that validation boundary; it does not claim that previously unavailable runtime or visual gates passed. Read root AGENTS, planner role, product documentation flow, architecture/testing/Compose rules, living sheet/campaign requirements and the upstream adaptive skill. The repository's instruction-invocation fallback is used because no repository Skill invocation API is exposed.

## Scope facts and route

One cohesive shared-layout enhancement: retain all existing destinations, screen callbacks, sheet order, domain rules and data. Layout changes are concentrated in shared responsive primitives, the application frame/navigation and selected existing screen layout rows. No Navigation3 migration, experimental Grid/FlexBox adoption, dependency/toolchain upgrades, backend work or two-pane navigation is included. The upstream skill permits a subset of its steps; adaptation/navigation positioning and preservation of existing saved state are the applicable subset. Its Navigation3 migration prerequisite is identified as a separate future migration, not silently introduced in this patch.

Apply the bounded Feature Delivery route with explicit user-authorized CI-only validation. The user's update request authorizes this scope; this document is not a fabricated full-harness gate/approval receipt.

## Product Requirements

- `docs/product/requirements/character-sheets.md`, SHEET-002: retain sheet management, canonical page/section ordering, callbacks and read-only explanation. SHA-256 `3c3231b2b58e0d2585daa0f25d44b9273256fe68065375316ed6a6007fb514e0`.
- `docs/product/requirements/campaigns.md`, CAMPAIGN-001: retain campaign sheet usage and authority semantics. SHA-256 `0f341cc1e14d5186a688826b3358ca259c6cc1a076b5fc9dddd53bbda0230e5a`.
- `DESIGN_GUIDE.md`, Layout Responsivo: compact phones use one column; larger widths have bounded centered content and multiple columns when canonical order is retained.

## Observed limitations

- `SdoResponsiveFrame` classifies 600/840dp widths, but most screen bodies remain unbounded `fillMaxSize` lists; tablet content stretches rather than forming a readable centered surface.
- `SdoApp` uses the navigation rail only for EXPANDED (840dp+) even though MEDIUM tablets already have sufficient edge-navigation space.
- `SdoResponsiveGrid` currently bases columns only on raw width/minimum width; enlarged text cannot reserve additional width for action labels or numeric modules.
- Dashboard quick actions/filter rows use fixed side-by-side weights, making narrow windows and text enlargement compete for space.
- Cybergrunge navigation uses visual selection with `clickable`; explicit selection semantics and minimum touch dimensions should be shared layout requirements.

## Proposed implementation order

1. **Responsive policy and centered content.** Add a small pure layout-policy helper for width class/content padding/content maximums, preserving 600/840dp classes. Provide reusable centered content bounds in the existing shared responsive file; constrain settings/form content more narrowly than dashboard/session lists. Apply where existing bodies stretch (dashboard, historian, settings and sheet body) without relocating global navigation or changing callbacks.
2. **Adaptive navigation.** Use rail for MEDIUM/EXPANDED when window height has sufficient space for the existing destinations (minimum 480dp); use bottom navigation for COMPACT and short landscape windows. Retain compact sheet full-screen navigation behavior and all existing destination/back-stack state. Add selectable-group/selected semantics to Cybergrunge controls, >=48dp targets, consistent width/alignment and safe system-bar treatment. Preserve Standard Material components and Cybergrunge codes/recuts/colors.
3. **Reflow shared controls.** Scale effective grid minimum width with relevant text scale; cap columns according to available width, retaining existing order and spacing. Convert crowded bounded action groups (dashboard filters/quick actions and similar selected rows) to the existing responsive grid or wrapping behavior. Never blindly migrate every Column to a lazy grid.
4. **Hierarchy for both modes.** Maintain expressive title faces and industrial palette; ensure functional labels can wrap/read at enlarged font scale rather than shrinking them into 6–9sp to preserve a single line. Avoid turning all content into equal-emphasis inset panels. Keep critical values and access explanations visible.
5. **Testing and review.** Add JVM tests for boundary widths/grid-policy edge cases; add instrumentation semantics/bounds coverage for both modes at compact/medium/expanded widths and enlarged text. CI must compile instrumentation and execute JVM/lint/catalog checks. A passing compilation is not screenshot/device proof; state any instrumentation not executed by CI as pending. Independent evaluator reads final source and CI output before readiness is reported.

## Acceptance criteria

| ID | Observable contract | Planned evidence |
|---|---|---|
| ADAPT-001 | Compact width retains bottom navigation; widths >=600dp with height >=480dp use rail; short windows retain bottom navigation, preserving existing destinations. | Pure width/height-policy JVM boundaries 599/600/839/840dp and 479/480dp + instrumented selection semantics; current PR CI |
| ADAPT-002 | Screen content has readable centered maximum width on tablet/desktop windows, and retains usable width at 320/360dp. | Shared frame policy tests and Compose bounds tests in both visual modes |
| ADAPT-003 | Grid/action reflow preserves item order, no zero columns and larger text gets equal or fewer columns. | JVM grid tests for 320/360/600/840/1200dp, text scales 1/1.3/2 |
| ADAPT-004 | Cybergrunge navigation announces selected destination and has >=48dp touch targets. | Compose semantics and bounds tests for both modes |
| ADAPT-005 | Existing sheet callbacks, page order, read-only state and saveable navigation/scroll behavior are retained. | Focused source review + existing test suites; runtime state recreation remains pending if not executed by CI |
| ADAPT-006 | Build/check outcome is tied to published PR head. | GitHub Actions URLs, commit SHA, successful catalog/lint/JVM/instrumentation compilation; no claim of device testing from assembleDebugAndroidTest |

## Rule Applicability

| Rule ID | Rule document | Decision | Trigger / rationale | Planned evidence |
|---|---|---|---|---|
| ARCH | android-architecture.md | Required | Shared UI-only policies; domain/data ownership unchanged. | Final diff review and source-rule evidence |
| IMPL | implementation-rules.md | Required | Real responsive behavior and callbacks must remain intact. | Policy tests, source review and CI build |
| TEST | testing-strategy.md | Required; local Android execution replaced by CI per user instruction | Layout policy plus maintained Compose behavior change. | JVM tests, CI lint/build and honest runtime qualification |
| SUI | compose-rules.md | Required | Layout, adaptive navigation, text and accessibility. | Bounds/semantics tests, mode matrix, review |
| L10N | localization-rules.md | Required | Labels and accessibility content may be touched; avoid new hardcoded copy. | Reuse existing resources/copy; resource/checker review |
| NAV | navigation-rules.md | Required | Destination representation changes bar/rail; routes and back-stack must remain stable. | Width policy tests and navigation-semantic inspection; no new destination |
| API | api-contract-rules.md | Not applicable — no endpoints, DTOs or contracts change | UI-only layout. | Diff review |
| OBS | observability.md | Not applicable — no asynchronous/recovery/storage boundary change | No new diagnostics required. | Diff review |
| ANL | analytics-rules.md | Not applicable — analytics: none | No product-approved analytics delta. | Diff review |
| SEC | android-security.md | Not applicable — no auth/storage/IPC/network/release-security delta | Existing protected build workflow remains unchanged. | Diff review |

## Product Requirement Reconciliation

| Capability file | Existing claim reviewed | Disposition | Current requirement location or retired status | Evidence / qualification |
|---|---|---|---|---|
| docs/product/requirements/character-sheets.md | Players can manage their character sheets. | Confirm unchanged | SHEET-002 | Unchanged because only presentation positioning changes; callbacks/canonical semantics are retained and runtime evidence is not implied. |
| docs/product/requirements/campaigns.md | Players can use character sheets in campaigns. | Confirm unchanged | CAMPAIGN-001 | Unchanged because campaign domain, authority and data are untouched. |

## Verification commands

CI-owned Android checks: `./gradlew lintDebug testDebugUnitTest assembleDebugAndroidTest --build-cache --stacktrace`; all three catalog validators; shared source-rule bundle where configured. Lightweight local diff/document checks are allowed but do not substitute Android CI. No APK installation, screenshot approval, emulator replay or full-harness lifecycle completion may be marked passing without actual evidence.

## Implementation handoff

The requested Adaptive skill was installed from android/skills commit `5815d27fda564aa9f07ebdcb5e57ed3e8d904e0d`, including its references and Apache license. Applied the compatible subset described above; no dependency or Navigation3 migration.

Implemented shared bounded content, width/height-aware navigation, font-scale-aware grid reflow, adaptive Settings panels, wrapping Dashboard filters/section actions, Cybergrunge accessible selection and fields, functional typography/contrast and non-shrinking two-line action labels. Colors/fonts/recuts and existing callbacks remain recognizable and intact.

Added four JVM policy tests and 24 parameterized component tests, plus execution of the two existing archived-sheet tests through the harness direct instrumentation runner in a new emulator CI workflow. PNG captures are evidence for manual review, not approved golden screenshot comparisons. Full production navigation/IME/fold-posture journeys are not claimed.

Local static evidence: git diff --check, Bash syntax and YAML parse passed; check-product-docs.py --summary docs/current/adaptive-ui-plan.md passed. Android tests are delegated to CI under the explicit user instruction. Full harness lifecycle/coverage acceptance remains distinct from the targeted PR checks and is not claimed.
