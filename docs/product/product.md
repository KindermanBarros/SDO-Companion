# SDO Companion

Status: onboarding draft, pending approval. Product intent comes from setup_prompt.md; implementation observations are not release acceptance.

SDO Companion helps players create and manage RPG character sheets and use them in campaigns. The growing beta also includes a codex for the SDO RPG system. No additional release exclusions have been requested.

## Capability index

| Capability | Living requirements | Evidence status |
|---|---|---|
| Character sheets | [Character sheets](requirements/character-sheets.md) | Source and tests present; runtime acceptance unverified |
| Campaign use | [Campaigns](requirements/campaigns.md) | Source and tests present; runtime acceptance unverified |
| RPG codex | [Codex](requirements/codex.md) | Bundled catalog and screens present; runtime acceptance unverified |
| Sincronização e conectividade | [Sincronização](requirements/synchronization.md) | Nova política registrada no backlog; não implementada |

## Delivery direction

First establish an accurate adoption baseline. Propose specific architecture, quality and test repairs using that evidence before selecting a new product feature. Detailed requirements belong in the capability files.

## Backlog de produto

Solicitações registradas em 2026-11-12. Itens ainda não implementados, sem prioridade ou ordem de entrega definida. O detalhamento está nos requisitos das capacidades; este backlog vivo participa da seleção, execução e reconciliação do workflow; registrar um item não autoriza sua implementação.

| ID | Item | Requisitos |
|---|---|---|
| BACKLOG-001 | Diário compartilhado da campanha | [Campanhas — CAMPAIGN-PLAN-001](requirements/campaigns.md#planned-changes) |
| BACKLOG-002 | Modo Sessão mais completo, compacto e com ações rápidas | [Campanhas — CAMPAIGN-PLAN-002](requirements/campaigns.md#planned-changes) |
| BACKLOG-003 | Modo Mestre mais completo, compacto e com ações rápidas | [Campanhas — CAMPAIGN-PLAN-003](requirements/campaigns.md#planned-changes) |
| BACKLOG-004 | Fotos de perfil dos personagens nas fichas, listas e modos de jogo | [Fichas — SHEET-PLAN-001](requirements/character-sheets.md#planned-changes) |
| BACKLOG-005 | Grid de batalha compartilhado em tempo real no Modo Sessão | [Campanhas — CAMPAIGN-PLAN-004](requirements/campaigns.md#planned-changes) |
| BACKLOG-006 | Edição simultânea em tempo real e uso online obrigatório, com offline somente leitura | [Sincronização — SYNC-PLAN-001](requirements/synchronization.md#planned-changes) |

BACKLOG-001 selected by owner on 2026-11-14; specification approved; UI approved with minor Standard color refinement (implementation pending). [Task state](../../.docs/OngoingTasks/shared_campaign_journal_task_state.md), [change record](../changes/2026-11-14-shared-campaign-journal.md). Other backlog items retain their unordered status.

Owner correction 2026-11-14: journal Pen designs rejected for styleguide noncompliance; explicitly requested enforcing the styleguide in the Pen skill. [Change record](../changes/2026-11-14-shared-campaign-journal.md): redesigned journal approved by owner with minor Standard color refinement; implementation pending. No product priorities changed.

## Workflow do backlog
Owner request 2026-11-13: publish the approved main-branch fixes as v0.51.1 using signed release CI. [Release task](../../.docs/CompletedTasks/release_0_51_1_task_state.md): Done; owner accepted 2026-11-14; published with verified signed APK, CI run 43540829981 successful; existing product priorities unchanged.
Owner request 2026-11-14: create feature branch with Companion improvements and optimize workflows, skills, rules, templates and tools for lower token usage. [Optimization task](../../.docs/CompletedTasks/lean_workflows_and_tools_task_state.md): Done; owner accepted 2026-11-14; on `feature/lean-workflows-and-tools`; ~6,250 lines removed across 73 harness files and 114 documents validated by `validate-harness.ps1`.

- Ao começar, selecionar o pedido autorizado e consultar o requisito relacionado; prioridades não definidas continuam sem ordem inventada.
- Acrescentar pedidos e achados úteis com origem, estado e dúvidas, sem expandir a tarefa atual.
- Ao entregar, atualizar o item para refletir pendência, bloqueio, revisão ou aceite; somente o proprietário aprova o encerramento.
- Melhorias visuais em revisão não concluem o escopo amplo dos modos Sessão/Mestre.

## Entregas aprovadas
| Pedido | Registro | Estado |
|---|---|---|
| Compactação e correções da UI | [Task state](../../.docs/CompletedTasks/compact_ui_polish_task_state.md) | Done; aceite explícito em 2026-11-13 |
| Limpeza e workflow econômico | [Task state](../../.docs/CompletedTasks/workflow_cleanup_task_state.md) | Done; aceite explícito em 2026-11-13 |

## Harness Feature Tracker

No complex work package is selected during onboarding. The empty tracker records no delivered or accepted features.

<!-- HARNESS_TRACKER_START -->
| ID | Feature | Workspace | Status | Updated | Notes |
|---|---|---|---|---|---|
<!-- HARNESS_TRACKER_END -->
