# Sincronização e conectividade

**Capability ID**: `synchronization`

Status: intenção de produto registrada no backlog em 2026-11-12; não implementada nem verificada em runtime.

## User goal

Permitir edição simultânea com atualização em tempo real e evitar problemas de sincronização, exigindo conexão para qualquer edição.

## Requirements

O aplicativo existente possui persistência local, sincronização e tratamento de conflitos, conforme os registros de adoção. Este documento não certifica o comportamento atual nem afirma que a política online abaixo já esteja aplicada.

## Planned changes

### SYNC-PLAN-001 — Edição simultânea e política online

Pedido direto do proprietário: adicionar edição simultânea em tempo real; reservar a resolução de conflito para edições de contas diferentes; exigir uso online para editar, com tela de conexão/falha e modo offline sem nenhuma edição.

| ID | Comportamento solicitado |
|---|---|
| SYNC-PLAN-001-A | Propagar edições simultâneas em tempo real entre as sessões participantes. |
| SYNC-PLAN-001-B | Edições de uma mesma conta, inclusive em sessões/dispositivos diferentes, devem convergir sem exigir resolução manual de conflitos. |
| SYNC-PLAN-001-C | Reservar a resolução de conflitos para edições concorrentes de contas diferentes; a sincronização normal entre contas não deve exigir intervenção quando não houver conflito. |
| SYNC-PLAN-001-D | Exigir conexão online para editar em todo o aplicativo. Sem conexão, nenhum fluxo pode permitir alterações, incluindo ficha, campanha, diário, fotos, ações de sessão, desenho e movimentação de tokens. |
| SYNC-PLAN-001-E | Apresentar estados/telas de conexão e falha de conexão, distinguindo indisponibilidade de uma operação concluída. |
| SYNC-PLAN-001-F | Oferecer modo offline somente leitura, sem permitir nenhuma edição. Não criar novas edições offline para envio posterior como alternativa ao bloqueio. |

O modo offline de consulta é a exceção à obrigatoriedade de uso online; não concede permissão para alterar dados. A nova política deve ser considerada no refinamento dos itens de diário, modos Sessão/Mestre, fotos e grid de batalha.

Decisões ainda para refinamento: quais dados estarão disponíveis para consulta offline; critério de conexão efetiva com o serviço, além da presença de rede; tratamento de uma edição em andamento quando a conexão cai; confirmação e recuperação de gravações cujo resultado é desconhecido; convergência de alterações simultâneas no mesmo campo pela mesma conta; granularidade dos conflitos entre contas e apresentação da comparação; autorização de escrita e retomada da edição após reconectar. A política não promete eliminar todas as falhas de rede nem fixa tecnologia, algoritmo de mesclagem ou modelo de armazenamento.

## References

- Solicitação direta do proprietário em 2026-11-12 sobre edição simultânea, conflitos apenas entre contas diferentes e uso online obrigatório para edição.
- `docs/current/onboarding-summary.md`: observações de adoção sobre Room, Firebase e sincronização existente.
- [Campanhas e modos de jogo](campaigns.md#planned-changes).
- [Fotos de personagens](character-sheets.md#planned-changes).
