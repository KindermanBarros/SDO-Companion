# Documento técnico do SDO-Companion

Este documento descreve decisões internas do aplicativo. As regras de RPG correspondentes permanecem no repositório de regras e devem ser escritas em linguagem de jogo.

---

# Contrato do SDO Companion

#sistema/integracao #status/canonico

Este documento registra somente a fronteira entre as regras e o aplicativo. Valores e
procedimentos continuam em suas fontes canônicas de [[Regras]]. Este documento define o
**schema tipado** dessa fronteira: enums fechados, objetos estruturados e tabelas de constantes
no lugar de texto corrido, para eliminar ambiguidade de implementação.

Os enums de personagem (`Attribute`, `Resource`, `BasicKnowledge`, `Protection`, `RaceId`,
`SubraceKind` e os tipos de Conhecimento Especial) **não são redefinidos aqui**. Este documento
importa por referência de [[Glossário de Enums do Companion]].

Convenção dos blocos de schema: sintaxe TypeScript-like, apenas para documentar tipos — não é
código executável.

---

## Personagem

`RaceId`, `SubraceKind` e `SubraceAssignment` vêm de [[Glossário de Enums do
Companion]].

```typescript
interface CharacterRaceSelection {
  raceId: RaceId;
  subrace?: SubraceAssignment;          // ausente quando o jogador não escolhe Sub-raça
}

interface PathAssignment {
  pathTemplateId?: string; // referência a um Caminho pronto, se usado
  name: string;
  motto: string;
  keywords: [string, string, string];
  pillars: [string, string, string];
  powers: Power[];         // ver [[Formato Canônico de Habilidades]]
}
```

- `raceId` é sempre um valor de `RaceId` (enum fechado de 18).
- `subrace` é opcional. `Aumentado` pode ser escolhido por qualquer raça; `ElfoDoCrepusculo` só pode ser escolhido por Elfo.
- Um `pathTemplateId` preenche automaticamente `name`, `motto`, `keywords`, `pillars` e `powers` a partir do template — os campos continuam editáveis depois de preenchidos.

### Bônus e máximos derivados

`Attribute`, `BasicKnowledge` e `Resource` vêm de [[Glossário de Enums do Companion]].

```typescript
type ProgressionBonusTarget =
  | { kind: "Attribute"; attribute: Attribute }
  | { kind: "BasicKnowledge"; knowledge: BasicKnowledge }
  | { kind: "SpecialKnowledge"; specialKnowledgeId: string };

interface ProgressionBonus {
  source: "LevelUp" | "EvenLevel" | "Milestone5";
  target: ProgressionBonusTarget;
  amount: number;
}

interface DerivedMax {
  resource: Resource;
  current: number;
  max: number;              // sempre recalculado, nunca editado diretamente
  formulaId: DerivedFormulaId;
}

type DerivedFormulaId =
  | "VidaMax" | "SanidadeMax" | "ArcanoMax" | "EnergiaMax" | "DestinoMax"
  | "ProtecaoGeral" | "ProtecaoEsquiva" | "ProtecaoPostura" | "ProtecaoMental"
  | "ProtecaoArcana" | "CargaMaxima" | "LimiteImplantes" | "Deslocamento";
```

- Bônus permanentes de progressão (`ProgressionBonus[]`) são armazenados **separados** de valores temporários (buffs/debuffs de cena). Nunca ficam no mesmo array nem no mesmo campo.
- Todo `DerivedMax` é recalculado pela tabela canônica identificada por `formulaId`; seus insumos são lidos do estado tipado atual, nunca de `Record<string, number>` aberto nem de um número solto persistido.
- Quando um recálculo reduz `max` abaixo de `current`, `current` é imediatamente igualado a `max` e o excedente é perdido.

---

## Equipamentos

Os tipos concretos de Item (Arma, Armadura, Acessório, Implante, Exocorpo, Montaria,
Consumível, Munição, Material, Modificação, Gema, Qualidade) **não são redefinidos aqui**. Este
documento importa por referência de [[Glossário de Itens do Companion]]. Esta seção define apenas
as regras de fronteira: estados de inventário, limites e derivações que dependem do estado de
sessão, não da forma do item em si.

### Estados de inventário — `InventoryState`

```typescript
type InventoryState = "Equipped" | "Wielded" | "QuickAccess" | "Backpack" | "Stored";
// Equipado (E) · Empunhado (W) · Acesso Rápido (R) · Mochila (M) · Guardado (G)

interface InventoryStateLimits {
  QuickAccess: { maxItems: 2; maxCargoPerItem: 1 };
  Wielded: { maxHands: 2 };
}
```

- `Wielded` é restrito a armas e escudos e é validado contra `InventoryStateLimits.Wielded.maxHands`.
- `QuickAccess` aceita no máximo `InventoryStateLimits.QuickAccess.maxItems` itens, cada um com Carga até `maxCargoPerItem`.

### Mãos e itens Pesados

`HandRequirement` vem de [[Glossário de Itens do Companion]] (`WeaponEntry.hand`) — não é
redeclarado aqui.

### Recipientes de carga e Mochila

```typescript
interface CargoContainerRule {
  onlyOneActiveGrantsCapacity: true;
  duplicateOrUnsupportedBehavior: "SanitizeToStored"; // rebaixa para InventoryState "Stored"
}
```

- Somente um Recipiente de Carga em `Equipped` concede capacidade adicional e habilita `Backpack` como estado válido.
- Recipientes duplicados ou sem suporte válido são normalizados para `Stored` automaticamente (nunca ficam em estado inconsistente).

### Proteção e regiões

`BodyRegionSlot` vem de [[Glossário de Itens do Companion]].

```typescript
interface EquippedProtection {
  region: BodyRegionSlot;
  // generalProtectionBonus soma à Proteção "Geral" (ver Protection no Glossário de Enums) quando
  // validamente equipado; localProtection é a PL, vinculada apenas a esta região.
  generalProtectionBonus: number;
  localProtection: number;
}
```

- PG válida soma à Proteção Geral global; PL nunca "vaza" para outra região.
- Escudos só concedem seus bônus de proteção enquanto estiverem em `Wielded`.

### Itens quebrados

```typescript
interface BrokenItemEffect {
  effectsSuspended: true;
  unequippedFromAllRegions: true;
  forcedState: "Stored";
}
```

### Poderes concedidos por item (gemas)

`EmbeddedGem` vem de [[Glossário de Itens do Companion]]. Ao ser instalada, a Gema **deixa de
existir como `ItemInstance` avulso** no inventário — passa a ser sub-objeto embutido do item
hospedeiro, consistente com o princípio de composição definido lá (Material, Modificação e Gema
se fundem ao hospedeiro; Munição não, por ser contável).

```typescript
interface EquippableItemInstance extends ItemInstance {
  gemSlots: ItemSlot[];              // de [[Glossário de Itens do Companion]]
  installedGems: EmbeddedGem[];      // tamanho ≤ número de slots do tipo "Gema" disponíveis
  // ... demais campos de WeaponEntry/ArmorEntry/AccessoryEntry referenciados via catalogEntryId
}
```

- `installedGems[].grantedPower` só existe ativo na ficha enquanto `state` do item hospedeiro for `Wielded` ou `Equipped`.
- Remover a gema recria um `ItemInstance` de Gema solto no inventário do personagem e remove a entrada de `installedGems` — não há necessidade de sincronizar contra uma tabela externa de poderes ativos.

### Componentes instaláveis e descrição estrutural

```typescript
interface EquippableItemInstance extends ItemInstance {
  gemSlots: ItemSlot[];
  technologySlots: ItemSlot[];
  installedGems: EmbeddedGem[];
  installedTechnologies: EmbeddedTechnology[];
}
```

- Cada Gema ou Tecnologia ocupa um espaço do tipo correspondente.
- Instalar e remover componentes recompõe `mechanicalEffects` e os Poderes de Item ativos.
- `description` nunca serializa Categoria, Material, Qualidade, PG, PL, LA, Carga, Durabilidade ou espaços; cada dado usa seu campo tipado.
- Munição usa `quantity`; sua Carga efetiva é `ceil(quantity / 10)` e quantidade 0 produz Carga 0.
- Criar uma arma à distância cria atomicamente um lote padrão compatível de 10 munições.

### Sobrecarga — constantes tipadas

`SpendableResource` e `BasicKnowledge` vêm de [[Glossário de Enums do Companion]].

```typescript
type EncumbranceState = "Normal" | "Overloaded" | "Immobile";

interface EncumbranceThresholds {
  overloadedRange: { minOver: 1; maxOver: 3 };   // 1 a 3 acima do máximo
  immobileMinOver: 4;                            // 4 ou mais acima do máximo
}

interface DisadvantageRule {
  attribute: Attribute;
  knowledge: BasicKnowledge;
}

interface EncumbrancePenalties {
  Overloaded: {
    movementPenaltyMeters: 5;      // -5m de deslocamento
    dodgeProtectionPenalty: 2;     // -2 na Proteção de Esquiva ("Esquiva" em Protection)
    runExtraCost: { resource: Extract<SpendableResource, "PE">; amount: 1 };
    disadvantageOn: DisadvantageRule[];   // [{AGI,Movimento}, {AGI,Furtividade}, {FOR,Atletismo}]
  };
  Immobile: {
    blocksMovementActions: true;
    blocksDodge: true;
  };
}
```

- `EncumbranceState` é derivado exclusivamente da Carga ativa, **excluindo** itens em `Stored`.
- Nenhum desses valores é hardcoded solto em texto de UI — toda penalidade referencia esta tabela.

### Orçamento de criação (PH)

```typescript
interface CreationBudgetRule {
  blockPurchaseAboveBalance: true;   // compras acima do saldo de PH são bloqueadas
  shopAndBuilderDisabledAtZero: true; // loja e construtor por PH ficam indisponíveis em saldo 0
  freeBuilderExemptFromBudget: true;  // construtor livre aceita custo "#" fora do orçamento
}
```

---

## Habilidades

Magias, Runas, Cinzas e Poderes seguem integralmente [[Formato Canônico de Habilidades]]. Catálogos
disponíveis para seleção direta devem fornecer todos os campos obrigatórios do schema definido lá
— o Companion não preenche campo obrigatório ausente com valor padrão.

### Validação de recurso por tipo

`SpendableResource` vem de [[Glossário de Enums do Companion]].

```typescript
type AbilityResourceRule =
  | { abilityType: "Power"; allowedResources: Exclude<SpendableResource, "PM">; passiveAmount: 0; activeMinAmount: 1 }
  | { abilityType: "Spell"; allowedResources: Extract<SpendableResource, "PM"> }
  | { abilityType: "Rune"; allowedResources: Extract<SpendableResource, "PM">; consumedAt: "Inscription" }
  | { abilityType: "Ash"; allowedResources: "Dose"; consumedFrom: "LinkedItem" };
  // "Dose" não é um SpendableResource — é exclusivo de Ash, ver Formato Canônico de Habilidades
```

- O app valida o recurso pelo tipo da habilidade, sem seletor livre de recurso em nenhum formulário.
- `Power` nunca usa `PM`. `PD` só é uma opção habilitada quando a habilidade estiver marcada como divina ou ligada a sorte/Destino/probabilidade (ver `isDivineOrLuckBased` no Formato Canônico).
- Dados importados ou persistidos com combinação inválida (ex.: `Rune` com recurso diferente de `PM`) são normalizados para este contrato na leitura.

---

### Vínculo de fichas e campanhas

- Jogadores só podem vincular fichas das quais sejam proprietários e apenas quando elas não pertençam a outra campanha.
- Uma ficha vinculada pode ser desvinculada pelo proprietário da ficha ou pelo proprietário da campanha.
- Administradores podem vincular qualquer ficha a qualquer campanha.
- Administradores podem desvincular qualquer ficha de qualquer campanha.
- Quando um administrador transfere uma ficha, o vínculo anterior é removido antes de registrar o novo, sem alterar a propriedade da ficha.
- Vincular uma ficha atualiza também a lista de personagens da associação do proprietário quando ela existir.

## Persistência

```typescript
interface MigrationRequirement {
  justification: string;       // por que o dado precisa ser persistido
  legacyHandling: string;      // como dados antigos são tratados
  validation: string;          // como a integridade é validada pós-migração
  rollbackPlan: string;        // como reverter em caso de falha
}
```

- Não persistir no banco nenhum valor que seja derivável do modelo atual (ex.: `DerivedMax.max`, `ashCargo`, `EncumbranceState`) — esses são sempre recalculados na leitura.
- Toda migração de schema exige um `MigrationRequirement` preenchido nos quatro campos antes de ser aplicada.

---

## Referência cruzada de constantes

Esta tabela existe apenas como índice — os valores completos e suas condições ficam nos blocos de
schema acima, para evitar duas fontes divergentes do mesmo número.

| Constante | Bloco de origem |
| --- | --- |
| Limite de mãos ao empunhar | `InventoryStateLimits.Wielded` |
| Limite de itens em Acesso Rápido | `InventoryStateLimits.QuickAccess` |
| Faixas de Sobrecarga | `EncumbranceThresholds` |
| Penalidades de Sobrecarga | `EncumbrancePenalties` |
| Regras de orçamento de criação | `CreationBudgetRule` |
| Recurso válido por tipo de habilidade | `AbilityResourceRule` |
| Atributos, Recursos, Conhecimentos Básicos, Proteções, Raças, Sub-raças | [[Glossário de Enums do Companion]] |
| Armas, Armaduras, Acessórios, Implantes, Exocorpos, Montarias, Consumíveis, Munição, Material, Modificação, Gema, Qualidade | [[Glossário de Itens do Companion]] |
