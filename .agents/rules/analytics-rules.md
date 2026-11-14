# Analytics Rules

`SDO-Companion` does not require analytics events by default. Unless the owner explicitly requests tracking, record `ANL: Not applicable — analytics: none` and add no tracking code.

## Rules when explicitly requested
- Fire events from ViewModels or domain coordinators, never directly inside Composables.
- Use consistent `<object>_<action>` naming.
- Never include auth tokens, credentials, PII (email, user ID), or user-created character/campaign text in event parameters.
