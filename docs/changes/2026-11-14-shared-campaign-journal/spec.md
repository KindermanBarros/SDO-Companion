# Shared campaign journal - BACKLOG-001

- Date / Status: 2026-11-14 / Approved by owner ("Do it").
- Requirement: CAMPAIGN-PLAN-001. Owner selected this item and answered permissions/content questions.

## Objective and scope
Campaign participants record their shared story separately from personal notes and technical operation history.
First version: chronological plain-text entries with title, body, author and date, visible to all current campaign participants.
Images, attachments, private entries, rich text and full-app BACKLOG-006 rollout are outside this delivery.
Journal operations honor SYNC-PLAN-001: online writes, read-only offline access, live updates and conflict handling.

## Rule applicability
| Rule ID | Rule document | Decision | Planned evidence |
|---|---|---|---|
| ARCH | android-architecture.md | Required | Existing layers/manual wiring; scoped review |
| IMPL | implementation-rules.md | Required | Repository/state tests |
| TEST | testing-strategy.md | Required | Unit, service rules and runtime journeys |
| SUI | compose-rules.md | Required | UI contract, accessibility and screenshots |
| L10N | localization-rules.md | Required | Resource strings and date formatting |
| NAV | navigation-rules.md | Required | Campaign entry, Back/cancel, restoration |
| API | api-contract-rules.md | Required | Additive schema and compatibility tests |
| OBS | observability.md | Required | Network/save recovery; no journal text in logs |
| ANL | analytics-rules.md | Not applicable - analytics: none | No tracking requested |
| SEC | android-security.md | Required | Server-enforced access and authorship tests |

## Requirements and acceptance criteria
- FR-001: Current participants can read their campaign journal; retain existing administrator support access.
  - AC-001: Members and GM see that campaign's entries newest first with title, author and date.
  - AC-002: Non-members/removed members cannot fetch entries; logout/campaign switching clears restricted content.
  - AC-003: Empty campaigns show an empty state and authorized create action.
- FR-002: Members and GM create entries in active campaigns while online.
  - AC-004: Saving valid title/body displays a server-confirmed entry with authenticated author and server creation date; author/campaign cannot be forged.
  - AC-005: Reject blank fields and enforce proposed limits of 120 title characters and 20,000 body characters on client/service.
- FR-003: Authors edit/delete their entries; GM moderates all. Preserve original authorship and creation time.
  - AC-006: Authors can edit with an updated timestamp; another member cannot edit/delete, including direct service requests.
  - AC-007: GM can edit/delete any entry; deletion requires confirmation and cancellation preserves it.
  - AC-008: Archived campaigns are read-only; deleted campaigns deny normal journal access.
- FR-004: Writes require service connectivity and current authorization; no queued offline mutations.
  - AC-009: Cached entries are readable offline with a label and disabled mutations; uncached journals show unavailable/retry.
  - AC-010: Disconnecting preserves the draft, blocks submission and permits retry after reconnect; cancel confirms discarding unsaved changes.
  - AC-011: Failed/uncertain saves never show confirmed success; retry uses the same entry identity and reconciles server outcome without duplicates.
  - AC-012: Permission loss prevents writes and clears restricted content.
- FR-005: Committed changes propagate to connected participants without manual refresh.
  - AC-013: A second online participant sees create/update/delete changes in the open journal.
  - AC-014: Concurrent edits from different accounts detect stale revisions and offer compare/reload before explicit replacement, without silent overwrite.
  - AC-015: Same-account sessions converge without manual conflict resolution or offline queuing.

## Screen states and navigation
Campaign context provides a Journal entry point; Session/Master can reuse it without broad redesign.
List: loading, empty, content, error/retry, cached offline and unavailable offline.
Editor: new/edit, validation, saving, failed/uncertain save, disconnected, conflict and permission loss.
Back returns to campaign context; editor Back/cancel confirms discarding unsaved changes; successful save returns to journal.
Preserve list position and draft across configuration changes; account/campaign changes must not expose another context's drafts.

## Data, assumptions and verification
Reuse Firebase authentication and campaign membership/GM ownership; no new dependency or service deployment at this stage.
Additive storage must preserve old clients and existing data. Concrete persistence/cache/revision contracts follow during planning.
Proposed defaults for approval: limits above, newest-first order, archived read-only, original author retained during moderation and explicit conflict replacement.
Owner confirmed: members create; authors edit/delete their own; GM moderates all; shared text-only entries with title, author and date.
Open questions: None; owner approved specification and proposed defaults on 2026-11-14.
Verification: focused domain/state tests; Firestore rules tests for unauthorized access, immutable fields and revisions; two-session propagation; offline/reconnect with no queued writes; Android navigation and CLI screenshots.
Current evidence: requirements/model/rules inspection only. Implementation and runtime acceptance pending.
