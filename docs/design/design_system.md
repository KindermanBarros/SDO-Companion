# SDO Companion design system

Canonical entry point for the existing Standard and Cybergrunge identities. This consolidation changes documentation ownership, not the approved appearance or application behavior.

## Ownership and precedence

| Topic | Owner |
|---|---|
| Shared interaction, accessibility and text rules | This document |
| Standard identity, geometry, typography and motion | [Standard](standard.md) |
| Standard theme/palette and resource mapping | [Standard tokens](standard_tokens.md) |
| Cybergrunge identity, effects and mode-specific feedback | [Cybergrunge](cybergrunge.md) |
| Implemented token/component values | [UI core](../../app/src/main/java/com/kinderman/sdo/ui/core/SdoTheme.kt), shared and mode-specific component sources |
| Approved task-specific states and visual references | The task's UI contract and linked Pen nodes/exports |

Shared rules apply to both modes; explicit mode-specific rules apply only to that mode.
An approved task contract can refine scoped states without replacing the established identity. A design change requires explicit owner authorization.
If source, approved contract and guide disagree, record the exact discrepancy and resolve it before changing visual direction. Source observation alone is not design approval.
Historical plans and screenshots are evidence, not an additional rule owner. See [Memory](../MEMORY.md) for deliveries and acceptance.

## Shared rules

- Keep complete functional text: no ellipsis, clipping, automatic hyphenation or character-level wrapping. Reflow/widen components or wrap complete words; preserve explicit user newlines. Autosize single-word Cybergrunge card headings without making them illegible.
- Preserve at least 48dp touch targets, system text scaling, semantic labels, focus/selection and non-color status indicators.
- Normal text targets 4.5:1 contrast; large text targets 3:1. Decoration must not hide essential data or actions.
- Use semantic theme roles and shared `Sdo*` contracts. Do not substitute generic fonts, rounded cards, pills or arbitrary colors for the approved identity.
- Primary, secondary, destructive and disabled states remain distinct. Theme/mode changes do not change permissions, content or RPG rules.
- Tap feedback belongs to the smallest complete action. A card and checkbox representing one selection share one toggle handler; nested actions must not trigger their parent.
- Decorative icons are not independent actions. Interactive non-text controls have accessible descriptions; empty/error/offline states preserve a clear next action without fabricated data.
- Editable fields and primary actions stay reachable above the IME; dismiss obstructing bottom navigation. Input sheets remain open above the keyboard with the platform drag handle.
- Use safe-content insets over a full-window themed background. Preserve route, entity, filter, draft and scroll state across supported configuration changes.
- Read code/resource implementation rules in [Code standards](../engineering/CODE_STANDARDS.md#ui) and [Compose rules](../../.agents/rules/compose-rules.md); do not maintain another copy here.

## Feedback by mode

Standard operational controls retain visible Material ripple/indication, contained within the action bounds.
Cybergrunge intentionally replaces Material ripple with bounded compression/signal displacement; accessible feedback and usable targets remain mandatory. This is a mode-specific exception, not permission to remove feedback globally.

## Design and verification

Use [Create UI and Verify](../../.agents/workflows/create-ui-and-verify.md). New visual directions use the UX contract and Pen workflow; bounded identity-preserving repairs reuse approved references.
Map represented components to actual source geometry, typography, semantic roles and Pen nodes. Inspect a representative frame per affected mode before expanding states.
Pen exports prove the design artifact, not runtime behavior. Select proportional checks through [Validation](../engineering/VALIDATION.md); no historical per-stage test mandate applies.
Responsive layouts do not establish physical tablet acceptance: smartphone is the confirmed target; record additional device evidence separately.
