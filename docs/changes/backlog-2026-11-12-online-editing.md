# Registro de backlog — edição simultânea e conectividade

Adicionado BACKLOG-006 e a capacidade synchronization para registrar a solicitação de edição simultânea em tempo real, convergência sem conflito manual dentro da mesma conta, resolução de conflitos apenas entre contas diferentes e edição somente online. Estados de conexão/falha e modo offline somente leitura fazem parte do pedido. Nenhuma implementação ou arquitetura foi definida.

Skill documentation-and-adrs aplicada por leitura direta; a interface Skill não está disponível. Verificação: documentação de produto, links e tracker inicial vazio. Nenhum teste Android é necessário para este registro documental. O tracker de execução e a memória de features entregues permanecem inalterados.

## Product Requirement Reconciliation

| Capability file | Existing claim reviewed | Disposition | Current requirement location or retired status | Evidence / qualification |
|---|---|---|---|---|
| docs/product/requirements/campaigns.md | concorrência, reconexão e funcionamento offline. | Replace | Planned changes — CAMPAIGN-PLAN-004, referência a SYNC-PLAN-001 | O pedido novo fixa edição online obrigatória; somente os detalhes da consulta offline ficam para refinamento. |
| docs/product/requirements/campaigns.md | Players can use character sheets in campaigns. | Confirm unchanged | Requirements — CAMPAIGN-001 | Reason: a nova política é uma proposta futura e não substitui o objetivo de uso das fichas em campanha. |
