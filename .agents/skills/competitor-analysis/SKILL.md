---
name: competitor-analysis
description: Compare competitors with primary evidence and recommend one PR-sized feature.
---
# Competitor Analysis

## Guardrails
- Read-only repository inspection: do not edit source, docs, or git state unless explicitly asked.
- Browse current official docs/releases/store listings for 5–8 maintained competitors (label any adjacent product `Adjacent`). Cite direct URLs beside material claims.
- Label `Fact`, `Inference`, and `Opinion`. Distinguish `Absent` (contradicted by source) from `Unverified` (score conservatively and prefix reason with `Unverified`).

## Workflow & output contract
Start the response with:
```text
Project category: <category>
Research date: <YYYY-MM-DD>
Competitor-selection method: <selection basis and maintenance evidence>
```
Then present in order:
1. **Baseline & comparability notes**: SDO Companion baseline (with repo file links) and platform/audience differences.
2. **Metric rubrics**: Table `Metric | 1/10 | 5/10 | 10/10` for 4–6 metrics (covering core capability, usability, and trust/reliability).
3. **Auditable scorecard**: Table `Name of app | Metric | Metric score /10 | Reason for the score` for every product-metric pair, including `Current project — SDO Companion`.
4. **Evidence limits**: Unverified, paywalled, or platform-mismatched gaps.
5. **Feature opportunities (3–7 vertical slices)**: Table `Feature | Description | ROI /10 | Reasons for ROI | Complexity to create a PR /10 | Confidence to create it and pass harness /10`.
6. **Proposed next feature**: Recommend **one** feature only, detailing its implementation boundary (aligned with `.docs/ARCHITECTURE.md`), verification checks, and primary risk/mitigation. End with: `Would you like me to create a PR for **<feature>**?`
