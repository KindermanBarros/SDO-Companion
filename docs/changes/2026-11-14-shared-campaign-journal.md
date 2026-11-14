# Shared Campaign Journal — BACKLOG-001 (`CAMPAIGN-PLAN-001`)

- **Date / Status**: `2026-11-14` / Spec & UI approved (with Standard blue/cyan accent refinement); Implementation `InProgress`.
- **Visual Assets**: [journal.pen](assets/2026-11-14-shared-campaign-journal/journal.pen), [review-board-v2.png](assets/2026-11-14-shared-campaign-journal/review-board-v2.png), [visual_references.json](assets/2026-11-14-shared-campaign-journal/visual_references.json), [previews-v2/](assets/2026-11-14-shared-campaign-journal/previews-v2/).

## 1. Objective & Scope
Campaign participants record their shared story separately from personal notes and technical operation history.
- **In scope (v1)**: Chronological plain-text entries (`title`, `body`, `author`, `date`) visible to all current campaign participants, honoring `SYNC-PLAN-001` (online writes, read-only offline cache, live updates, conflict handling).
- **Out of scope**: Images/attachments, private entries, rich text, and full-app `BACKLOG-006` rollout.

## 2. Rule Applicability
| Rule ID | Rule document | Decision | Planned evidence |
|---|---|---|---|
| ARCH | `android-architecture.md` | Required | Existing layers/manual wiring; scoped review |
| IMPL | `implementation-rules.md` | Required | Repository/state tests |
| TEST | `testing-strategy.md` | Required | Unit, service rules and runtime journeys |
| SUI | `compose-rules.md` | Required | UI contract, accessibility and screenshots |
| L10N | `localization-rules.md` | Required | Resource strings and date formatting |
| NAV | `navigation-rules.md` | Required | Campaign entry, Back/cancel, restoration |
| API | `api-contract-rules.md` | Required | Additive schema and compatibility tests |
| OBS | `observability.md` | Required | Network/save recovery; no journal text in logs |
| ANL | `analytics-rules.md` | Not applicable — analytics: none | No tracking requested |
| SEC | `android-security.md` | Required | Server-enforced access and authorship tests |

## 3. Functional Requirements & Acceptance Criteria
- **FR-001 (Read)**: Current participants read their campaign journal newest-first (`AC-001`); non-members/removed members cannot fetch and logout/campaign switch clears restricted content (`AC-002`); empty campaigns show empty state + authorized create action (`AC-003`).
- **FR-002 (Create)**: Members and GM create entries online; saving valid title/body returns server-confirmed entry with authenticated author & server creation date (`AC-004`). Reject blank fields and enforce `120` char title / `20,000` char body limits on client & service (`AC-005`).
- **FR-003 (Edit/Delete/Moderate)**: Authors edit/delete their own entries; other members cannot (`AC-006`). GM edits/deletes any entry while preserving original authorship & creation time; deletion requires confirmation and cancellation preserves entry (`AC-007`). Archived campaigns are read-only; deleted campaigns deny normal access (`AC-008`).
- **FR-004 (Connectivity & No Offline Queue)**: Cached entries readable offline with label & disabled mutations; uncached shows unavailable/retry (`AC-009`). Disconnecting preserves draft, blocks submission, permits retry on reconnect; cancel confirms discarding unsaved changes (`AC-010`). Uncertain/failed saves never show confirmed success; retry uses same entry/operation identity without duplicates (`AC-011`). Permission loss prevents writes and clears restricted content (`AC-012`).
- **FR-005 (Live Sync & Conflicts)**: Connected participants see create/update/delete live (`AC-013`). Concurrent cross-account edits detect stale revisions and offer compare/reload/replace without silent overwrite (`AC-014`). Same-account sessions converge automatically (`AC-015`).

## 4. UI Contract & States (`J01–J26`, Standard + Cybergrunge)
- **Styleguide**: Reuse `SdoTheme` (`Oxanium`/`MB Forever Raw` in Standard; `Press Start 2P`/`Acidic`/`Drunk Fonts` in Cybergrunge), `HudBackground`, `TechPanel` / `CyberGrungePanel`, `SdoActionButton` / `CyberGrungeAction`, `SdoField` / `CyberGrungeField`. Standard information panel borders use secondary blue `#5690DA` at 72%, toolbar context secondary blue, list count primary cyan. Whole-word wrapping, no ellipsis, `>= 48dp` touch targets.
- **States**: `J01` Loading, `J02` Empty, `J03` List, `J04` Cached offline, `J05` Uncached offline, `J06` Fetch error, `J07` Archived list, `J08` Access revoked, `J09` Author detail, `J10` Other member detail, `J11` GM detail, `J12` Offline detail, `J13` New editor, `J14` Edit editor, `J15` Validation, `J16` Saving, `J17` Editor disconnected, `J18` Uncertain save, `J19` Cross-account conflict, `J20` Delete confirmation, `J21` Discard confirmation, `J22` Title IME, `J23` Body IME, `J24` Campaign entry, `J25` Replace confirmation, `J26` Archived detail.
- **Copy**: Status (`"Carregando diário"`, `"O diário está vazio"`, `"Offline · somente leitura"`, `"Campanha arquivada · somente leitura"`, `"Você não tem mais acesso a este diário"`), Labels (`"Título"`, `"Registro"`), Invalid (`"Informe o título e o texto do registro."`), Failure (`"Não foi possível confirmar o salvamento. Seu rascunho está preservado."`), Disconnect (`"Sem conexão. Seu rascunho está preservado. Reconecte-se para salvar."`), Conflict (`"Este registro foi alterado por outra pessoa."`), Delete (`"Excluir este registro? Esta ação não pode ser desfeita."`), Discard (`"Descartar alterações? Seu rascunho não foi salvo."`), Replace (`"Substituir a versão atual? A versão comparada será substituída pelo seu texto."`), Removed author fallback (`"Ex-participante"`).
- **IME & Navigation**: Tags `journal-title`, `journal-body`, `journal-save`, `journal-back`; focus order `Back -> title -> body -> Save`; IME variants omit nonessential masthead and keep focused field + Save reachable. Dashboard campaign card (`Abrir diário`) -> List -> Detail -> Edit/Delete; Back restores campaign context; rotation preserves drafts.

## 5. Implementation & Verification Plan
- **Delivery Slices**:
  1. **Read-only journal**: `domain/model/CampaignJournalEntry.kt`, `domain/repository/CampaignJournalRepository.kt`, `data/repository/FirestoreCampaignJournalRepository.kt` (collection `campaignJournal`), `FirestoreCollections.kt`, account-scoped cache, `presentation/journal/JournalViewModel.kt` + `JournalScreen.kt`, `SdoApplication.kt` manual DI, `SdoApp.kt` `JOURNAL` `AppSurface`, campaign-card `Abrir diário` callback, `strings.xml`.
  2. **Online create/edit/delete**: Editor, validation (`120`/`20,000`), delete/discard confirmations, transactional Firestore writes checking campaign/member/entry state (no offline mutation queue).
  3. **Concurrency, recovery & lifecycle**: `revision`, `lastEditorId`, `operationId`, `deletedAt` tombstone; cross-account compare/reload/replace (`J19`/`J25`); same-account convergence; uncertain save reconciliation; offline/archived/revoked screens; IME/rotation/account-switch cleanup.
  4. **Service & runtime verification**: `firebase/firestore.rules` + `firebase/tests/firestore.rules.test.mjs`, `:app:assembleDebug`, focused `:app:testDebugUnitTest`, `npm --prefix firebase run test:rules`, `:app:lintDebug`, `:app:koverXmlReportDebug`, `JournalJourneyTest` (when device connected).
- **Design & Skill Notes**: `journal.pen` passed 52-frame headless validation (`430x932`, 0 layout issues) and owner approved UI with Standard blue/cyan accents; `pen-design-editor` skill was updated to enforce styleguide mapping before bulk generation.
- **Implementation & Verification (2026-11-14 02:02 America/La_Paz — `InDevReview`)**:
  - Delivered `CampaignJournalEntry`, `CampaignJournalRepository`, `FirestoreCollections.CAMPAIGN_JOURNAL`, `FirestoreCampaignJournalRepository`, `JournalViewModel`, `JournalScreen`, `journal_strings.xml`, `SdoApplication`/`ViewModelFactories`/`SdoApp`/`DashboardScreen` wiring, `firebase/firestore.rules` + `firebase/tests/firestore.rules.test.mjs`, `CampaignJournalPolicyTest`, `JournalViewModelTest`, and `JournalJourneyTest`.
  - Passed: `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:testDebugUnitTest` (257/257 passing, 0 failed, 0 skipped, including 9 new journal policy/ViewModel tests), `:app:lintDebug`, `:app:koverXmlReportDebug` (59.71% line coverage).
  - Limitations: `:app:ktlintCheck` and `:app:detekt` failed on pre-existing baseline issues (plus style findings in new journal files); `npm --prefix firebase run test:rules` skipped/failed locally because `firebase/node_modules` is absent; `JournalJourneyTest` compiled only (no `adb` device connected).
