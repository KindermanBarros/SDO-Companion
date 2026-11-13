---
name: code-quality-fix
description: Run static analysis, formatting, lint checks, and fix violations.
---

# Skill — Code Quality Fix

## Purpose

Run all static check suites, lint rules, and custom compliance rules. Resolve all violations. Fix root causes — do not suppress rules.

---

## Load

Read the approved Rule Applicability matrix from the active specification or generated
complex-slice contract, then load a conditional rule only when its decision is
`Required` or an approved exception. Do not re-read the matrix from a summary file.

---

## Principles

- Minimum code that solves the problem — nothing speculative.
- Touch only what you must. Don't "improve" adjacent code or formatting.
- Every changed line should trace directly to the user's request.
- Surgical changes over large refactors.

---

## Execute

### 1. Run All Quality Checks
Read the active specification's Rule Applicability matrix first. Run the baseline checks
below and the checks needed for every `Required` row; retain explicit non-applicable
and exception rationales. Do not add analytics or logs merely to change a decision.

Execute the following baseline checks:
```bash
./gradlew assembleDebug
./gradlew ktlintCheck
./gradlew detekt
./gradlew lintDebug
./gradlew :app:koverXmlReportDebug
```

Generate the Kover report in this stage so coverage review uses evidence from the
current source and test state. The Testing skill runs only related tests; repository-wide
coverage is checked here.

Use the configured Gradle quality tasks and review the remaining architectural, Compose, localization and assertion concerns directly. No legacy source-script gate applies.

Additionally check for dummy code:
```bash
grep -rn "TODO()\|NotImplementedError\|// dummy\|// placeholder\|// stub" app/src/main/ sharedContracts/
```
Must return zero matches.

On Windows use gradlew.bat for the configured tasks.

### 2. Auto-Fix Formatting Issues
To resolve Ktlint style and formatting errors automatically, run:
```bash
./gradlew ktlintFormat
```

### 3. Diagnose and Fix Violations
For any remaining failures or warnings:
- Analyze `detekt` and Android `lint` reports.
- Review Compose, localization, and architectural check outputs.
- Fix root causes, do not suppress rules.

### 4. Record Results
**Update `summary_{feature_id}.md`** (or `summary_v<N>.md`) to mark **Code Quality Fix** ✅ with notes and timestamp once all checks pass with 0 violations.

---

## Done When

- [ ] `./gradlew ktlintCheck` — exit code 0
- [ ] `./gradlew detekt` — exit code 0
- [ ] `./gradlew lintDebug` — exit code 0
- [ ] Fresh coverage XML counts and thresholds are reviewed and recorded.
- [ ] No dummy code found
- [ ] Every Rule Applicability decision has required check/review evidence or its approved rationale
- [ ] Summary updated and marked complete

**APPROVED →** Return to the active workflow file and proceed to the next stage defined there.
