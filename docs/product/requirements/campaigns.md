# Campaigns

**Capability ID**: `campaigns`

Status: adoption draft, pending approval.

## User goal

Players use their RPG character sheets in campaigns.

## Requirements

| ID | Product intent | Evidence qualification |
|---|---|---|
| CAMPAIGN-001 | Players can use character sheets in campaigns. | Requested in setup_prompt.md; campaign domain models, SyncedCampaignRepository and SessionModeScreen exist; execution not verified |

Source observations include invitations, membership, roles and session operations. Their full behavior is not newly specified or accepted during setup. Preserve existing contracts and data until a separate approved change defines any delta.

## Compact UI review

Compact UI review: dashboard filters use compact modal choices and session abilities retain complete expandable rules, costs and use actions. See [approved UI contract](../../current/UI_contract.md). Component regressions pass; owner installed-app acceptance pending.

## Planned changes

Backlog solicitado pelo proprietário em 2026-11-12. São propostas não implementadas; cada item precisa de refinamento, especificação, design e critérios de aceite antes da entrega. Não representam aprovação de uma solução técnica nem comprovação do comportamento atual.

### CAMPAIGN-PLAN-001 — Diário compartilhado

Adicionar um diário compartilhado da campanha. Definir no refinamento autoria, permissões de leitura/escrita, organização dos registros e comportamento de edição/sincronização. Distinguir o diário das notas pessoais da ficha e do histórico técnico de operações já existentes.

Refinement started 2026-11-14. Owner confirmed shared text entries, member creation, author edit/delete and GM moderation. [Approved specification](../../changes/2026-11-14-shared-campaign-journal/spec.md); owner rejected the first visual candidates for styleguide noncompliance; redesign pending.

### CAMPAIGN-PLAN-002 — Evolução do Modo Sessão

Melhorar a interface e completar as funcionalidades do Modo Sessão, com foco em ações rápidas e apresentação compacta, preservando toda a informação necessária ao jogo. O inventário de ações, informações essenciais, atalhos e estados deve ser definido a partir dos fluxos existentes; o pedido não fixa ainda quais controles ou funcionalidades serão adicionados.

### CAMPAIGN-PLAN-003 — Evolução do Modo Mestre

Melhorar a interface e completar as funcionalidades do Modo Mestre, com foco em ações rápidas, controles compactos e acesso a toda a informação necessária para conduzir a campanha. Refinar quais informações precisam permanecer visíveis e quais ações exigem acesso imediato, sem presumir mudanças nas permissões existentes.

### CAMPAIGN-PLAN-004 — Grid de batalha em tempo real

Adicionar ao Modo Sessão um quadro de combate compartilhado, sobre um grid, no qual a pessoa mestre possa desenhar em tempo real e movimentar personagens representados por tokens em formato de moeda com suas fotos de perfil.

Escopo inicial solicitado:

- Grid e desenho sobre o quadro, com atualização compartilhada em tempo real.
- Movimentação dos personagens por tokens de moeda usando as fotos previstas em SHEET-PLAN-001.
- Cálculo de deslocamento.
- Representação e consideração de terrenos altos e baixos.
- Possibilidade de evolução com outras funcionalidades de combate, ainda a definir; a expressão “muitas outras coisas” não é tratada como escopo fechado.

Decisões para refinamento: escala/unidades do grid, diagonais e regras de deslocamento do SDO; significado e efeito de altura/terreno; quem pode mover cada token; ferramentas de desenho; persistência dos mapas; concorrência, reconexão e consulta offline. Edições offline ficam proibidas pela política [SYNC-PLAN-001](synchronization.md#planned-changes), também aplicável ao diário e aos modos Sessão/Mestre. Alcance, linha de visão, obstáculos, iniciativa, automação de regras e outras ferramentas poderão ser avaliados, mas não estão confirmados pelo pedido atual.

Dependências de produto identificadas: fotos dos personagens para os tokens; integração com a campanha e o Modo Sessão. A ordem de entrega e a tecnologia de sincronização ainda não foram escolhidas.

## References

- `setup_prompt.md`
- `app/src/main/java/com/kinderman/sdo/domain/model/Campaign.kt`
- `app/src/main/java/com/kinderman/sdo/data/repository/SyncedCampaignRepository.kt`
- `app/src/main/java/com/kinderman/sdo/presentation/session/SessionModeScreen.kt`

Owner review repair: preserve existing contracts while fixing theme outlines, clipped text and compact controls. Panel quick Session/Master removed; navigation retained. See [revision evidence](../../current/summary_v4.md). Owner explicitly accepted this UI delivery on 2026-11-13; automated evidence limitations remain in the linked summary.

Master operation exposes the existing quick-action dialog through a visible button on each collapsed character card; no command rules changed.
