# Auditoria direcionada do Firestore

Escopo: inicialização Firebase, autenticação, sincronização offline-first, convites, campanhas, membros, personagens, operações, biblioteca, entregas e perfis administrativos.

## Consultas usadas pelo aplicativo

- `users`: listagem administrativa.
- `characters`: listagem administrativa, filtro por `ownerId` e por `campaignId`, além de leitura/escrita por ID.
- `campaigns`: filtro por `ownerId` e leitura/escrita por ID.
- `campaignMembers`: filtros por `userId` e `campaignId`, além de leitura/escrita pelo ID composto.
- `campaignInvites`: leitura por código/ID e filtro por `campaignId`.
- `campaignAudit`, `campaignLibrary` e `campaignDeliveries`: filtros por `campaignId`; entregas também filtram por `recipientId`.

## Modelo e autorização relevantes à correção

- Personagens possuem `ownerId`, `campaignId`, estado de bloqueio, exclusão lógica e `updatedAt`.
- O dono da ficha pode editar, desvincular e excluir a própria ficha conforme as demais validações.
- O dono da campanha pode editar fichas vinculadas enquanto a campanha estiver ativa.
- Para cumprir o contrato do aplicativo, o dono da campanha também pode desvincular uma ficha de terceiro, mas somente alterando `campaignId` para vazio e `updatedAt`.
- Essa permissão não permite mudar `ownerId`, conteúdo da ficha, exclusão lógica ou excluir fisicamente o documento.
- Administrador continua derivado exclusivamente do token autenticado com e-mail configurado e verificado; campos no documento `users` não concedem administração.

## Falhas silenciosas encontradas

- Inicialização do Firebase descartava a exceção sem log.
- Três rotinas de sincronização retornavam sucesso quando `Firebase.firestore` lançava exceção.
- Consulta remota de convite convertia indisponibilidade do Firebase em convite inexistente.
- Refresh de autenticação é propositalmente tolerante para preservar o modo offline, mas não produzia diagnóstico.

## Verificações adversariais exigidas para esta mudança

- Usuário externo não pode ler ou alterar ficha vinculada.
- Dono da campanha não pode mudar o proprietário da ficha.
- Dono da campanha não pode alterar conteúdo no mesmo write usado para desvincular.
- Dono da campanha não pode excluir a ficha de terceiro.
- Perfil global com campo de papel privilegiado não concede administração.
- Administrador exige identidade configurada e `email_verified` no token.
- Consultas reais continuam cobertas pela suíte do emulador.
