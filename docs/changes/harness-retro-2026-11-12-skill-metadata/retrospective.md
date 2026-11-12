# HarnessLens skill metadata repair

Classification: HARNESS_ENVIRONMENT.

Incident: the skill loader rejected `.agents/skills/harnesslens/SKILL.md` with `missing field description`. Its frontmatter used `summary` instead of the required `description` key.

Invariant: every repository SKILL.md must declare a nonempty name and description in its YAML frontmatter.

Repair: rename the incorrect key, preserving its text and all workflow instructions. The reported loader error supplies the failing reproduction; validate the repaired header and scan all skill headers for the same defect. No application code or runtime behavior changes, so Android build/runtime tests are not relevant.

The repair is delivered through the existing harness setup PR. Commit identity uses the repository owner's configured GitHub identity. No stage or product-acceptance status is advanced.
