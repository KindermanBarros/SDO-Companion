# Runtime Observability Rules

Do not add logging merely to satisfy a checklist (`OBS: Not applicable` when no new diagnostic boundary is needed).

## Rules when logging is needed
- Use `android.util.Log` with a consistent `SDO/<Component>` tag.
- Use `DEBUG` for development state, `INFO` for lifecycle milestones, `WARN` for recoverable sync/cache fallbacks, and `ERROR` for unexpected exceptions or persistence/auth failures.
- Never log PII (emails, user IDs), auth tokens, credentials, or user-authored character/campaign content.
- Keep technical failure details in logs rather than exposing raw exception payloads in UI state.
