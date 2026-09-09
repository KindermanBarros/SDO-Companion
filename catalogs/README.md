# Catálogos plug-and-play — SDO Companion

Os JSONs são a fonte estruturada dos 400 registros. A mesma edição é proposta no repositório `solidao-dos-oprimidos`, em `03 - Regras/Catalogos estruturados`. Não é necessário extrair valores de Markdown, preencher campos na primeira seleção ou acessar a rede durante o uso do app.

| Arquivo | Registros |
| --- | ---: |
| powers.json | 100 poderes |
| magic.json | 50 magias |
| ashes.json | 50 cinzas |
| runes.json | 50 runas |
| knowledge.json | 50 adquiridos + 50 arcanos + 50 técnicas |

## Contrato

Cada entrada corresponde a `CatalogEntry`. Poderes são convertidos em `Power`, magias/cinzas/runas em `MysticAbility` e Conhecimentos em `SpecialKnowledge`. IDs de catálogo são estáveis; IDs de instância são criados ao adicionar à ficha. Não existem `structuredCost`, `maxUses` ou contador de usos máximos no contrato.

| Campos | Destino |
| --- | --- |
| id, version | catalogEntryId, catalogVersion |
| name, group, source, ruleReference | nome, categoria, origem e referência |
| cost, action, range, duration | campos homônimos da habilidade |
| mechanicalEffect | effect |
| prerequisites, activationCondition, limit, enhancements, deactivationCondition | detalhes do Poder |
| relatedAttribute, initialValue | attribute, value do Conhecimento |
| summary, keywords, repeatable | descrição, busca e duplicidade |

`cost` é o único custo visível e persistido. Custos fixos como `2 PM + 1 PV` podem ser debitados automaticamente. Conversões, custos variáveis, doses e materiais mantêm sua expressão completa e exigem ajuste explícito durante o uso. A frequência textual de uma regra é preservada em `limit`; isso não cria um campo de usos máximos na ficha.

## Proveniência e complementação

Textos-base conferidos em `solidao-dos-oprimidos@45e1ebe6b32251fca4043b1bab1e15966f544710`. Efeitos e aprimoramentos dos 250 exemplos publicados são preservados. Ação, alcance, duração e encerramento que antes estavam implícitos recebem uma normalização editorial explícita nesta proposta: bônus passivos permanecem vinculados à fonte; ações desencadeadas identificam a ação/alvo existente; não se cria uma distância numérica sem fonte. Os 150 Conhecimentos são identificados como especializações do Companion, com nível inicial 1 e atributo mínimo 1; as técnicas não concedem ataques extras ou dano implícito.

A proposta de formato não se torna cânone publicado antes da integração da PR do repositório de regras. Fichas já criadas preservam seus snapshots e alterações pessoais.

## Atualizar

1. Edite os JSONs completos, preservando IDs. Incremente `catalogVersion`, `version` das entradas e `BuiltInCatalog.VERSION` ao publicar uma nova edição.
2. Execute `python3 tools/generate_catalog.py` na raiz do Companion.
3. Execute `python3 tools/generate_catalog.py --check`.
4. Commite JSONs e o Kotlin gerado juntos. O Actions rejeita divergências.

O gerador valida campos, tipos, contagens, IDs únicos e preenchimento antes de gerar Kotlin tipado. Não infere custos, condições ou mecânicas. A migração Room 15→16 preserva os campos estruturados novos; o catálogo local é atualizado sem reescrever as fichas.
