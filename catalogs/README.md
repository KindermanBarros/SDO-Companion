# Catálogos plug-and-play — SDO Companion

Os JSONs são a fonte estruturada dos 500 registros. A mesma edição é proposta no repositório `solidao-dos-oprimidos`, em `03 - Regras/Catalogos estruturados`. Não é necessário extrair valores de Markdown, preencher campos na primeira seleção ou acessar a rede durante o uso do app.

| Arquivo | Registros |
| --- | ---: |
| powers.json | 100 poderes |
| magic.json | 50 magias |
| ashes.json | 150 cinzas: 50 nomes × 3 purezas |
| runes.json | 50 runas |
| knowledge.json | 50 adquiridos + 50 arcanos + 50 técnicas, no contrato especializado v2 |

## Contrato

Poderes usam `catalog.schema.json`. Magias, runas e cinzas usam `abilities.schema.json`; conhecimentos usam `knowledge.schema.json`. Os contratos especializados seguem o formato plano de grimório Roll20: um documento nomeado contém uma lista homogênea, e cada registro possui apenas campos do seu domínio. O gerador converte os três contratos para `CatalogEntry`; na ficha, Conhecimentos viram `SpecialKnowledge`. IDs de catálogo são estáveis e IDs de instância são criados ao adicionar à ficha.

| Campos | Destino |
| --- | --- |
| id, version | catalogEntryId, catalogVersion |
| name, group, source, ruleReference | nome, categoria, origem e referência |
| costType, costValue, execution, range, targetArea, duration, resistance | campos canônicos usados diretamente pela UI |
| knowledge, level | vínculo obrigatório da Magia ou Runa ao Conhecimento possuído |
| source, purity | Fonte e Pureza da Cinza |
| mechanicalEffect | effect |
| prerequisites, activationCondition, limit, enhancements, deactivationCondition | detalhes do Poder |
| attribute, initialLevel | attribute, value do Conhecimento |
| maxLevel | limite validado do catálogo (5) |
| summary, keywords, repeatable | descrição, busca e duplicidade |

Na ficha, custo é formado por um tipo canônico e um valor inteiro. `Nenhum` não possui valor, `Dose` é exclusivo de Cinzas e Runas usam Arcano. Execução e Duração possuem valores de tempo independentes. Magias, Runas e Cinzas são carregadas diretamente dos campos tipados, sem inferência textual.

## Proveniência e complementação

Textos-base conferidos em `solidao-dos-oprimidos`. Efeitos e aprimoramentos dos exemplos publicados são preservados. Cada Pureza de uma Cinza é uma entrada independente, pois muda seu efeito e sua carga. Os 150 Conhecimentos têm nível inicial 1, máximo 5 e Atributo principal editável na ficha. Conhecimentos Arcanos incluem escolas e também conhecimentos gerais de magia, como disciplinas, ofícios e fenômenos sobrenaturais. As técnicas não concedem ataques extras ou dano implícito.

A proposta de formato não se torna cânone publicado antes da integração da PR do repositório de regras. Fichas já criadas preservam seus snapshots e alterações pessoais.

## Atualizar

1. Edite os JSONs completos, preservando IDs, e incremente o `catalogVersion` correspondente quando alterar o contrato publicado.
2. Execute `python3 tools/generate_catalog.py` na raiz do Companion.
3. Execute `python3 tools/generate_catalog.py --check`.
4. Commite JSONs e o Kotlin gerado juntos. O Actions rejeita divergências.

O gerador valida campos, tipos, contagens, IDs únicos e preenchimento antes de gerar Kotlin tipado. Não infere custos, condições ou mecânicas. A migração Room 17→18 adiciona os campos estruturados somente ao catálogo local; as fichas existentes de todas as contas permanecem intactas.
