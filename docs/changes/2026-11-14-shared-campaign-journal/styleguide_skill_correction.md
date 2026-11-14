# Pen styleguide correction - InDevReview

- Date: 2026-11-14 01:21 America/La_Paz.
- Owner request: journal designs rejected as inconsistent with the styleguide; add the styleguide to the skill responsible for Pen generation.
- Applied skills: skill-creator and pen-design-editor; no application behavior or product scope changes.
- Cause: the Pen skill required SDO themes without directly requiring the canonical guide/component comparison. Execution also failed to read the full relevant guide sections before batching frames; clean bounds were incorrectly treated as sufficient visual evidence.
- Observed failures: rounded Standard cards, arbitrary typography, Cybergrunge reduced to a dark palette/red line, missing cuts/chrome/graphic layers, generic action styling.
- Invariant: each Pen mode must reproduce the canonical guide and actual component properties before bulk state generation.
- Change: [pen-design-editor](../../../.agents/skills/pen-design-editor/SKILL.md) now links the guide, alternate direction, theme, tokens, components and assets; requires a source-to-Pen mapping and inspection of one representative frame per mode before generating the rest.
- Validation: skill-creator quick_validate.py PASS after supplying PyYAML in temporary scratch storage; validate-harness.ps1 PASS (116 documents); scoped staged diff PASS. Temporary validator dependencies and rejected visual artifacts removed.
- Disposition: rejected generated candidates/exports discarded; approved [specification](spec.md) retained. No rejected visual is an approved reference.
- Limitation: instruction validation does not prove a corrected design. Journal remains InProgress at design; redraw and owner visual review are still pending.
- Next action: apply the corrected skill to one representative journal frame per mode before expanding its state inventory.
