---
name: harnesslens
description: Collect app-private runtime state and analytics evidence during exploratory QA with hlens and agent-device.
---

# Skill — HarnessLens

## Workflow
1. Confirm authorized debug build/device with HarnessLens state/analytics hooks installed. Never run on production accounts.
2. Create `.scratch/qa/<run-id>/`, copy approved contracts into `harnesslens/contracts/`, and record their SHA-256 hashes before UI actions.
3. Run `hlens capture start --platform <PLATFORM> --app-id <APP_ID> --device-id <DEVICE_ID>` and retain its native `run_id`.
4. Drive UI with `agent-device`. Before/after each required state checkpoint, export cumulative snapshots under `harnesslens/checkpoints/` and require fresh post-action state records.
5. Stop capture and export full `.scratch/qa/<run-id>/harnesslens/capture.ndjson` with matching `--run-id`.
6. Run `hlens state verify` and `hlens analytics verify` against approved contracts (see [Exploratory QA](../exploratory-qa/SKILL.md) and [HarnessLens reference](../exploratory-qa/references/harnesslens.md)).
7. Contract mismatch = `FAIL`; missing capture/hooks = `INCONCLUSIVE`. Never substitute manual declarations for verifier output or collect secrets/PII.
