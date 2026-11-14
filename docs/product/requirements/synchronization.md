# Synchronization & Connectivity

**Capability ID**: `synchronization`

Status: implemented in `InDevReview` (`BACKLOG-006` / `SYNC-PLAN-001`, 2026-11-14); JVM verification completed and emulator/device runtime verification pending.

## User goal

Enable simultaneous real-time editing and prevent sync issues by requiring an online connection for every mutation.

## Requirements

The existing app has local persistence, sync, and conflict handling per onboarding records. This document does not certify legacy runtime behavior beyond verified tests.

## Planned changes

### SYNC-PLAN-001 — Simultaneous editing and online-only policy

Direct owner request: add real-time simultaneous editing; reserve conflict resolution for edits from different accounts; require online connection to edit, with connection/failure states and read-only offline mode without any edits.

| ID | Requested behavior |
|---|---|
| SYNC-PLAN-001-A | Propagate simultaneous edits in real time across participating sessions. |
| SYNC-PLAN-001-B | Edits from the same account, including across different sessions/devices, must converge without requiring manual conflict resolution. |
| SYNC-PLAN-001-C | Reserve manual conflict resolution for concurrent edits by different accounts; normal cross-account sync must not prompt when there is no conflict. |
| SYNC-PLAN-001-D | Require online connection to edit across the entire app. When offline, no flow may allow changes, including sheets, campaigns, journal, photos, session actions, drawing, and token movement. |
| SYNC-PLAN-001-E | Display connection and connection-failure states/screens, distinguishing unavailability from a completed operation. |
| SYNC-PLAN-001-F | Offer read-only offline mode without allowing any edits. Do not create queued offline edits for later upload as an alternative to blocking. |

Read-only offline inspection is the sole exception to mandatory online use; it grants no write permission. This policy applies to journal, Session/GM modes, photos, and battle grid refinements.

Open refinement items: which data sets remain cached for offline inspection; effective backend reachability criteria beyond network presence; handling in-progress edits when connection drops; confirmation/recovery for uncertain writes; same-field simultaneous edits by the same account; cross-account conflict granularity/comparison UI; write authorization and resuming edits upon reconnect.

## References

- Direct owner request on 2026-11-12 for simultaneous editing, cross-account-only conflicts, and mandatory online editing.
- `docs/current/onboarding-summary.md`: onboarding observations on Room, Firebase, and existing sync.
- [Campaigns & play modes](campaigns.md#planned-changes).
- [Character photos](character-sheets.md#planned-changes).
