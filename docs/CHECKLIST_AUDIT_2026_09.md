# Auditoria do checklist — setembro de 2026

Base auditada: `feat/auditoria-checklist-completa`, commit `3cf8b68`. Existem exatamente 20 commits do checklist sobre `main`. Na consulta inicial, essa branch ainda não tinha PR. A PR #26 é uma revisão diferente; este trabalho está na PR #27.

| Item | Evidência e resultado |
| --- | --- |
| 1. Apagar campanha arquivada | Corrigido o fluxo sequencial para batch atômico, estado terminal, fichas preservadas e proteção contra restauração por cliente antigo. Exige servidor e até 450 fichas. |
| 2. Código sempre visível | Card exibe o código; sincronização e leitura de convites agora abrangem membros autorizados, além da responsável. |
| 3. Card de campanha | Redesenho nos commits originais; inspeção visual em aparelho ainda necessária. |
| 4. Card recolhível | Corpo completo é expandido/recolhido pelo cabeçalho, com estado salvo. |
| 5. Aperture White | Conferidos #FF9A00, #27A7D8 e #B5AAAA. |
| 6. Aba Poderes | Edição por ID, expansão e seleção do catálogo presentes. Custos variáveis deixam de ser debitados por extração parcial. |
| 7. Catálogos canônicos | 250 exemplos publicados incorporam efeitos completos; JSONs com contrato explícito substituem extração textual. Efeitos raciais foram confrontados com Raças.md. |
| 8. 50 Conhecimentos por tipo | Contagem validada: 50 adquiridos, 50 arcanos, 50 técnicas, todos com IDs únicos. Especializações do Companion são identificadas como conteúdo autoral. |
| 9. Remover “Estudo de” | Nenhuma entrada começa com esse prefixo; escolas solicitadas presentes. |
| 10. Órgãos dinâmicos | Lista vazia inicialmente; inclusão, edição e remoção com IDs persistentes. |
| 11. Poderes pré-preenchidos | 100 exemplos em JSON possuem custo, ação, alcance, duração, efeito, condições, limite textual e aprimoramento explícitos. |
| 12. Custo único e sem usos máximos | Campos redundantes removidos; mantém frequência canônica textual sem contador de máximo. Custos variáveis e materiais exigem ajuste explícito. |
| 13. Magias pré-preenchidas | 50 magias, 50 cinzas e 50 runas normalizadas; efeito completo e origem preservados. |
| 14. Auditoria inicialmente desativada | Preferência padrão false, persistida em MainActivity. |
| 15. Pesquisa administrativa compacta | Busca e filtros compactos presentes; filtros agora sobrevivem ao retorno. |
| 16. Pesquisa de contas normais | Filtra somente o conjunto de fichas fornecido pelo repositório autorizado. |
| 17. Pesquisa de campanhas | Campo compacto nas campanhas disponíveis. |
| 18. Seleção de campanha | Substituído diálogo por dropdown; campanha selecionada restringe personagens e histórico e persiste no retorno. |
| 19. Ação rápida | Fluxo escolher/configurar/confirmar, com rolagem para telas pequenas; não permite ação em campanha arquivada. |
| 20. Histórico | Registros ordenados com personagem, valores anteriores/novos, data, ator, motivo e filtros. |
| 21. Biblioteca completa | Removido corte de 40; detalhes dos valores, raças, Caminhos, poderes e snapshots de fichas autorizadas. Modelos mostram payload e bônus. |
| 22. Backstack | Pilha de origem com ficha selecionada, retorno sequencial e preservação de estado das telas. |
| 23. Visão operacional | Card removido. |
| 24. Abas da ficha | Rótulos compactos em linha única, sem códigos redundantes. |

## Catálogo estruturado

`catalogs/README.md` documenta o contrato de 400 entradas e a complementação editorial proposta. `tools/generate_catalog.py --check` verifica campos, contagens e correspondência exata do Kotlin gerado. Room 15→16 preserva condições e aprimoramentos. A atualização do catálogo não sobrescreve snapshots de personagens existentes.

## Validação e limites

O commit `3013513` passou nos Actions Android (APK release e verificações existentes) e Firebase (emulador de regras). A etapa seguinte adiciona o catálogo estruturado, migração e validação determinística; consultar o resultado do commit final na PR.

O ambiente local não consegue baixar a distribuição Gradle; isso não é resultado de compilação. `git diff --check`, sintaxe JavaScript e verificação dos 400 registros executados localmente.

Nenhum APK foi executado em aparelho nesta sessão. Design e gestos precisam de aceite visual; as verificações de build não comprovam UX. As regras Firestore somente entram em produção pelo workflow de publicação após integração.
