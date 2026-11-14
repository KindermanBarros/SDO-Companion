---
name: analytics-event-validator
description: Validate app-side analytics events against approved tracking contracts.
---
# Analytics Event Validator

Standalone or [exploratory-qa](../exploratory-qa/SKILL.md) helper for verifying app-side analytics events when tracking is explicitly in scope. Follow [references/contracts.md](references/contracts.md) and [references/android-capture.md](references/android-capture.md).

## Workflow
1. **Scenario & contract**: Create `.scratch/qa/<run-id>/scenario.json` (`{"scenario_id":"<id>","app_id":"com.kinderman.sdo"}`) and `.scratch/qa/<run-id>/analytics/contract.json` from approved tracking requirements.
2. **Optional backend mocks**: When controlled responses are needed, apply mappings via [api-mocking](../api-mocking/SKILL.md) before arming capture and verify hits before cleanup.
3. **Inspect, collect & validate**:
   ```bash
   python3 .agents/skills/analytics-event-validator/scripts/capture.py inspect --run-dir .scratch/qa/<run-id> --serial <serial>
   python3 .agents/skills/analytics-event-validator/scripts/capture.py reset --run-dir .scratch/qa/<run-id> --serial <serial> --contract .scratch/qa/<run-id>/analytics/contract.json
   # Trigger declared UI actions, then flush and collect:
   python3 .agents/skills/analytics-event-validator/scripts/capture.py collect --run-dir .scratch/qa/<run-id> --serial <serial> --contract .scratch/qa/<run-id>/analytics/contract.json
   python3 .agents/skills/analytics-event-validator/scripts/validate.py --run-dir .scratch/qa/<run-id> --contract .scratch/qa/<run-id>/analytics/contract.json
   ```
4. **Verdict & cleanup**: Exit codes: `0` PASS, `1` FAIL, `2` INCONCLUSIVE, `3` INVALID_INPUT. Promote only the final summary verdict to durable docs and remove `.scratch` before commit.
