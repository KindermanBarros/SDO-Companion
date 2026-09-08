# Fases 1 e 2 — contrato consolidado

Este documento é o critério de aceite da implementação consolidada. Ele substitui interpretações anteriores que tratavam Mestre como papel global ou transferível.

## Fase 1 — ficha e regras estruturadas

- Conhecimentos adquiridos, conhecimentos arcanos e técnicas de batalha usam catálogo pesquisável, detalhes, pré-requisitos, efeitos mecânicos e prevenção de duplicatas.
- Poderes de caminho e bônus de itens permanecem estruturados; valores derivados exibem a fórmula usada.
- Snapshots gravados na ficha preservam a versão escolhida mesmo quando o catálogo evolui.
- Inventário usa seletores controlados de tipo e destino para bônus mecânicos.

## Fase 2 — campanhas e sessão

### Autoridade

- Cada campanha tem exatamente uma Mestre: a conta que a criou (`campaign.ownerId`).
- Participações são sempre `PLAYER`. Uma mesma conta pode ser Mestre na campanha A e jogadora na campanha B.
- A responsabilidade da campanha não é promovida nem transferida por papel de membro.
- A Mestre vê e edita todas as fichas vinculadas à campanha ativa, mas não pode apagar ou desvincular fichas de outras pessoas.

### Convites

- A campanha recebe um código estático na criação.
- Pedir o convite novamente devolve o mesmo código; não há rotação, expiração ou revogação no fluxo normal.
- Toda entrada por convite cria uma participação `PLAYER`.

### Fichas

| Ação | Dono da ficha | Mestre da campanha | Administrador |
|---|---:|---:|---:|
| Ver ficha vinculada | Sim | Sim | Sim |
| Editar ficha vinculada ativa | Sim | Sim | Sim |
| Vincular/desvincular | Sim | Não | Conforme administração |
| Apagar a própria ficha | Sim, mesmo bloqueada/arquivada | Não | Sim |
| Criar e atribuir ficha a jogador ativo | — | Sim | Sim |

Vincular ou atribuir uma ficha nunca transfere sua propriedade.

### Sincronização e sessão

- Operações de sessão são imutáveis e idempotentes.
- O primeiro envio de uma operação não consulta antes um documento inexistente; isso evita o `PERMISSION_DENIED` observado no aplicativo real.
- Em uma repetição, a operação existente só é aceita como concluída quando ator e chave idempotente conferem.
- Falhas mantêm a alteração local para nova tentativa e a interface usa mensagens humanas em vez de códigos internos.

## Temas e acessibilidade

- **Violeta Rúnico**: novo nome de Violeta Onírico, mantendo compatibilidade com a preferência armazenada.
- **Edgerunners**: amarelo elétrico, ciano e vermelho sobre azul noturno, inspirado na linguagem cromática de Cyberpunk 2077.
- **Magenta Onírico**: rosa ácido, violeta e ciano.
- **Aperture White**: modo claro coerente em branco, azul escuro e laranja.

Superfícies, campos, barras do sistema, estados vazios e cartões devem consumir `MaterialTheme.colorScheme`. Textos claros fixos sobre superfícies claras e textos escuros fixos sobre cartões escuros não são permitidos. Títulos longos usam truncamento controlado, e códigos como `SYNC_OK`, `READ_ONLY` e `ACCOUNT_ACCESS` não devem ser a única explicação apresentada à pessoa usuária.

## Migração

- Room 14 → 15 normaliza papéis legados `MASTER`/`HISTORIAN` para `PLAYER`.
- O proprietário continua definido exclusivamente em `campaigns.ownerId`.
- A sincronização também normaliza participações legadas encontradas no Firestore, sem alterar o proprietário da campanha.

## Verificação mínima

- Testes unitários Android da Fase 1 e das políticas de acesso.
- Testes das regras Firestore para leitura, edição da Mestre, exclusão pelo dono, atribuição na criação, convite, imutabilidade de autoridade e histórico idempotente.
- Build Android e validação das regras nos checks da PR.
