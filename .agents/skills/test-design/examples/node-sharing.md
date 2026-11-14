# Test Plan — Node Sharing (Illustrative Example)

Illustrative ephemeral acceptance test plan (`20261104-node-sharing-android-01`). Obtain approved product requirements, build identity, and user approval before running.

## Scope & Sources
- `sharedContracts/openapi.yaml` and `sharedContracts/test-scenarios/note_share_*.json`.
- Execution layer: ephemeral `agent-device` via `exploratory-qa` against real staging.

## Acceptance Coverage Overview

| AC | Approved behavior | Case | Priority | API mode |
|---|---|---|---|---|
| AC-01 | Inviting a collaborator records a pending read-only share and shows success. | NS-01 | P0 / critical | Real staging |
| AC-02 | Inviting the same email returns 409 conflict and shows duplicate-access error. | NS-02 | P0 / critical | Real staging |
| AC-03 | Updating a collaborator to full access persists the role and updates the member list. | NS-03 | P0 / critical | Real staging |

## Ephemeral Test Cases

### NS-01 — Invite a collaborator (`note_share_invite_001.json`)
1. Open the QA note sharing flow, enter approved QA email, select read-only, and submit via real UI.
2. Verify invite success UI (`expected.ui`), capture screenshot/snapshot, and confirm `POST` returned `201` with `accessRole: read_only, status: pending`.
3. Verify fresh HarnessLens state and analytics contract (if in scope).

### NS-02 — Reject duplicate invitation (`note_share_duplicate_invite_001.json`)
1. Submit an invitation for an already-shared QA email on the same note.
2. Verify `POST` returns `409`, duplicate-access error is shown (`expected.ui`), and no duplicate membership exists in staging.

### NS-03 — Update collaborator access (`note_share_update_role_001.json`)
1. Open the shared-user list, select the collaborator, and change role to full access.
2. Verify `PATCH` returns `200`, UI shows full-access label (`expected.ui`), and staging API confirms `accessRole: full_access`.

## Session Cleanup & Coverage Gaps
- Export/verify capture and request evidence before resetting owned mappings and cleaning isolated QA data.
- Resolve build/device/environment, product ACs, and tracking contracts before execution.

## Exploratory Risk Targets *(20 min / 60 actions budget)*

| Target | Risk hypothesis / transition | Suggested fault condition | Priority |
|---|---|---|---|
| R-01 | Repeated invite taps bypass loading guards (`ready -> submitting -> result`). | Double-tap at loading boundary. | High |
| R-02 | Backgrounding loses pending invite identity (`pending -> background -> foreground`). | Background while pending, then restore. | High |
| R-03 | Delayed member-list refresh fails recovery (`loading -> disconnected -> restored`). | Delay read, then restore network. | High |
