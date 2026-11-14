---
name: code-review-and-quality
description: Review code correctness, architecture, security, performance, and simplicity.
---
# Code Review and Quality

Follow [Project contract](../../gates/project-contract.md) and [Review checklist](../../gates/review-checklist.md). For complex multi-slice evaluation reports, use [code-review-template.md](../../../harness/templates/code-review-template.md).

## Five review axes
1. **Correctness**: Matches approved requirements/ACs, handles null/empty/error/cancellation paths, and has meaningful test assertions.
2. **Simplicity & readability**: Smallest diff that solves the task; clear names, no dead code, no speculative abstractions, no `TODO()`/stub paths ([implementation-rules.md](../../rules/implementation-rules.md)).
3. **Architecture**: Preserves [.docs/ARCHITECTURE.md](../../../.docs/ARCHITECTURE.md) (`ui`/`presentation` → `domain` ← `data`, manual DI in `SdoApplication`, custom `AppSurface` navigation).
4. **Security**: Validates untrusted inputs, protects secrets/PII, and enforces Firestore/Auth boundaries ([android-security.md](../../rules/android-security.md) when triggered).
5. **Performance & UI**: Uses `LazyColumn` keys, lifecycle-aware Flow collection, Standard/Cybergrunge tokens, whole-word wrapping, and no functional ellipsis ([compose-rules.md](../../rules/compose-rules.md)).

## Execution
1. Read only the active task/spec, diff, and triggered rules — do not preload unrelated rules or rerun valid fingerprint-matched checks.
2. Categorize findings clearly: **Critical** (blocks handoff: security, data loss, broken behavior), **Required** (must fix in scope), **Nit / Optional** (non-blocking), or route out-of-scope observations to [living backlog](../../../docs/product/product.md#product-backlog).
3. Verify that triggered checks from [CI checks](../../gates/ci-checks.md) ran and passed, or that limitations are explicitly stated.
