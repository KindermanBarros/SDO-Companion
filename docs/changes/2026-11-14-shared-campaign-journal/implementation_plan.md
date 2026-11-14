# Shared journal implementation and verification plan

- Scope: [Approved specification](spec.md); [UI contract](UI_contract.md); BACKLOG-001 only.
- Status: planning ready; visual references await owner approval. Existing start authorization covers implementation after the visual decision.
- Reuse Android/Firebase/coroutines/manual wiring. No new library, architecture migration, app-wide connectivity rewrite, or backend deployment authorized here.

## Delivery steps
1. Reachable read-only journal: add domain journal model/repository, Firestore listener/defensive mapping, account-scoped cache, ViewModel state, campaign-card callback and JOURNAL AppSurface. List/detail must be reachable from Dashboard and pass build before proceeding.
2. Online create/edit/delete: wire editor/validation/confirmation and transactional repository operations, with server-enforced member/author/GM permissions and campaign lifecycle. No local mutation queue; success only after confirmed commit.
3. Concurrency/recovery: revisions, last editor and operation IDs; stale different-account edits return comparison; same-account retries converge automatically; uncertain writes reconcile by stable identity. Keep drafts while reconnecting and clear restricted content after permission/account loss.
4. Finish all UI states and lifecycle behavior: reuse Sdo controls from the mapping, localized strings, complete text, IME/Back handling, saved draft/list position scoped to account/campaign, offline/archived screens, theme variants and accessibility.
5. Run the selected security/domain/state/runtime checks, inspect actual changed diff and record evidence/limitations once; reconcile backlog and hand off InDevReview with scoped signed-off commit.

## Anticipated files
| Boundary | Create / modify |
|---|---|
| Domain | domain/model/CampaignJournalEntry.kt; domain/repository/CampaignJournalRepository.kt; focused permission/validation tests |
| Data | data/repository/FirestoreCampaignJournalRepository.kt; FirestoreCollections.kt; account-scoped journal cache implementation; mapping/revision/recovery tests |
| Wiring | SdoApplication.kt and existing ViewModel factory wiring; retain manual DI |
| Presentation | presentation/journal/JournalViewModel.kt and JournalScreen.kt; SdoApp.kt; dashboard campaign-card callback; localized strings.xml |
| Service boundary | firebase/firestore.rules; firebase/tests/firestore.rules.test.mjs; additive Firestore contract documentation |
| Runtime tests | app/src/androidTest/.../JournalJourneyTest.kt; maintained fixture-backed state captures |
Kotlin paths are relative to app/src/main/java/com/kinderman/sdo/. No file deletion or existing campaign schema removal planned.

## Data contract and risks
Proposed additive collection: campaignJournal. Concrete schema must be recorded with the API-contract skill before coding.
Fields: stable entry/campaign/author identity, author display snapshot, title/body, server creation/update timestamps, revision, last editor, operation identity and deletion marker.
Use transactions that read campaign/member/entry state and enforce matching immutable identity and revision. Transactions must fail without service access rather than enqueue offline writes.
Retries preserve entry/operation IDs; deletion markers support repeat-safe reconciliation. Tombstone retention/purge is not implemented without an explicit data-retention decision.
Prefer existing Firebase cache for read-only snapshots; filter by active account/campaign, reject pending local mutations and clear UI/drafts on session changes. Add Room storage only if the approved cache behavior cannot be met without it; document migration before introducing one.
Offline access can only reflect the last confirmed membership; revocation discovered on reconnect immediately clears restricted content. It cannot certify remote membership while disconnected.
Last-writer/revision handling must preserve original authorship during GM moderation and prevent another account's silent overwrite, including transaction retries.

## Requirements and checks
| Spec coverage | Primary evidence |
|---|---|
| FR-001 / AC-001-003 | authorized list/detail, ordering, empty state and account switching tests; denied outsider/removed-member service reads |
| FR-002 / AC-004-005 | blank/limits validation; forged author/campaign rejection; server-confirmed creation with fixed timestamps |
| FR-003 / AC-006-008 | author versus other member/GM writes, immutable authorship, destructive cancellation, archived/deleted campaign service rules |
| FR-004 / AC-009-012 | offline cached/uncached states, no queued mutation, draft retention, uncertain commit/retry without duplicate, permission loss |
| FR-005 / AC-013-015 | two connected sessions, live create/update/delete, stale cross-account comparison/reload/replace, same-account convergence |

## Rule mapping
| Required rules from spec | Concrete work and verification |
|---|---|
| ARCH / IMPL | repository contracts, manual wiring, ViewModel-owned transitions; scoped architecture review and focused JVM tests |
| TEST / API / SEC | schema/rules/repository tests; emulator authorization and revision tests; cross-campaign forged requests; cancellation preserved |
| SUI / L10N | existing Sdo controls and resource strings; screenshot/contrast/complete-text review in both modes |
| NAV | AppSurface journal route; saved context, Back, discard, rotation and IME instrumented journeys |
| OBS | explicit network/persistence/error results; no draft text, IDs, credentials or user content in diagnostic logs |
ANL remains not applicable; add no analytics.

## UI State Stages
All J01-J26 rows and both variants in UI_contract.md are required. Maintain one fixture-backed runtime capture per state, with stable journal-* tags; J22/J23 additionally exercise a real Android IME.
No Roborazzi task/dependency exists in this repository. Do not invent a method-scoped verification task or claim pixel comparison; use Android CLI captures and reference review until any screenshot-test infrastructure is separately approved.
Representative production journey: Dashboard > campaign > Abrir diário > Novo registro > save confirmed > open > edit/cancel/delete > return to Dashboard. Assert persisted content and original campaign context.

## Selected commands and evidence limits
- Current design/docs: Pen export inspection and fresh reopen; validate-harness.ps1; git diff --cached --check; scratch cleanup hook.
- Implementation: .\gradlew.bat :app:assembleDebug; focused :app:testDebugUnitTest --tests selectors for journal models/repository/state; :app:lintDebug; configured ktlint/detekt with pre-existing failures reported separately.
- Service: npm --prefix firebase run test:rules using the existing Firestore emulator command; no production deployment during tests.
- Coverage: :app:koverXmlReportDebug for changed domain/ViewModel coverage; keep project thresholds and report baseline gaps honestly.
- Android: focused JournalJourneyTest via :app:connectedDebugAndroidTest; adb screencap for approved state references; two-session service updates and offline/reconnect checks.
- No adb device is currently connected. Device/IME/live propagation are future required evidence, not covered by design rendering or a JVM fake.
