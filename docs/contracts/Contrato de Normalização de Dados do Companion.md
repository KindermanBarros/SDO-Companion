# Documento técnico do SDO-Companion

Este documento descreve decisões internas do aplicativo. As regras de RPG correspondentes permanecem no repositório de regras e devem ser escritas em linguagem de jogo.

---

# Contrato de Normalização de Dados do SDO Companion

#sistema/integracao #sistema/modelo #status/canonico

Este contrato complementa [[Contrato do SDO Companion]], [[Formato Canônico de Habilidades]],
[[Glossário de Enums do Companion]] e [[Glossário de Itens do Companion]]. Ele define a fronteira
que impede regras executáveis de dependerem de texto livre, duplicação de dados ou snapshots.

## Princípios obrigatórios

1. Texto narrativo (`description`, `effect`, História e Anotações) explica; nunca é a única fonte
   de um comportamento que o Companion calcula, aplica, filtra ou valida.
2. Catálogos são entidades versionadas e imutáveis por revisão. Uma alteração publica nova revisão;
   instâncias existentes continuam na revisão que receberam.
3. Instâncias guardam somente estado mutável e referências tipadas. Itens personalizados são
   entradas de catálogo em escopo de personagem ou campanha, não snapshots paralelos.
4. Todo identificador possui domínio nominal; um ID de condição não pode ocupar campo de item.
5. Valores derivados não são persistidos. São recalculados por `formulaId` fechado.

```typescript
type AbilityCatalogId = string & { readonly __brand: "AbilityCatalogId" };
type ItemCatalogId = string & { readonly __brand: "ItemCatalogId" };
type ItemInstanceId = string & { readonly __brand: "ItemInstanceId" };
type ConditionId = string & { readonly __brand: "ConditionId" };
type ConditionInstanceId = string & { readonly __brand: "ConditionInstanceId" };
type SpecialKnowledgeId = string & { readonly __brand: "SpecialKnowledgeId" };
type CharacterId = string & { readonly __brand: "CharacterId" };

interface CatalogReference<Id> { id: Id; revision: number; }
interface CatalogEntry<Id> {
  id: Id;
  revision: number;
  scope: "Canonical" | "Campaign" | "Character";
  ownerId?: CharacterId;
  publishedAt: string;
}
```

## Efeito mecânico estruturado

Uma habilidade, item, condição ou evento pode manter `effect: string`, mas seus resultados
executáveis usam objetos compartilhados. Um efeito é uma composição; não se cria um campo novo
em cada tipo de conteúdo para dano, cura ou condição.

```typescript
type Dice = { count: number; die: 4 | 6 | 8 | 10 | 12 | 20; bonus?: number };
type EffectOperation =
  | { kind: "Damage"; damageType: DamageType; amount: Dice | number; target: TargetSpec }
  | { kind: "Healing"; resource: SpendableResource; amount: Dice | number; target: TargetSpec }
  | { kind: "ApplyCondition"; condition: ConditionPayload; target: TargetSpec }
  | { kind: "AddModifier"; modifier: ActiveModifier }
  | { kind: "SpendResource"; resource: SpendableResource; amount: number }
  | { kind: "ConsumeItemState"; itemInstanceId: ItemInstanceId; amount: number; state: "Dose" | "Charge" | "Ammo" };

interface MechanicalEffect {
  operations: EffectOperation[];
  usage?: UsageLimit;
  trigger?: TriggerSpec;
}
```

`effect` não deve ser interpretado para preencher ou corrigir `MechanicalEffect` automaticamente.
Conteúdo legado incompleto fica visível para correção manual.

## Condições e modificadores

```typescript
type ConditionKind =
  | "Abalado" | "Caido" | "Cego" | "Surpreendido" | "Imobilizado" | "Sangramento"
  | "Queimando" | "Envenenado" | "Paralisia" | "Asfixia" | "Dor" | "Intoxicacao"
  | "Dependencia" | "Exaustao" | "Inconsciente";

interface ConditionPayload {
  kind: ConditionKind;
  intensity?: number;
  damage?: Dice;
  difficultyClass?: number;
  duration?: Duration;
  cadence?: "OnApply" | "EndOfTurn" | "StartOfTurn" | "Daily";
  targetRegion?: BodyRegionSlot | OrganSlot;
  endsWhen?: EndCondition[];
}

interface ConditionInstance extends ConditionPayload {
  instanceId: ConditionInstanceId;
  source: Source;
  appliedAt: string;
}

interface ActiveModifier {
  modifierId: string;
  target: BonusTarget;
  amount: number;
  source: Source;
  duration?: Duration;
  stacking: "Add" | "HighestOnly" | "ReplaceBySource";
  activation?: TriggerSpec;
}
```

Condições iguais não acumulam exceto quando sua definição disser que intensidade é acumulável.
O cálculo de atributos, recursos, proteções e conhecimentos considera `PermanentBonus` e
`ActiveModifier` sem mutar o valor-base.

## Corpo, órgãos e dano localizado

```typescript
type OrganSlot = "Brain" | "HeartOrCore" | "LungsOrRespiratory" | "LiverOrFilter" | "Other";
type BodyState = "Intact" | "Stabilized" | "Damaged" | "Destroyed" | "Missing";

interface InjuryEvent {
  eventId: string;
  source: Source;
  occurredAt: string;
  damage?: { type: DamageType; amount: number };
  failuresAdded: number;
}

interface BodyRegionState {
  region: BodyRegionSlot;
  state: BodyState;
  failures: number;
  protection: EquippedProtection;
  implantInstanceIds: ItemInstanceId[];
  prosthesisInstanceId?: ItemInstanceId;
  injuries: InjuryEvent[];
}

interface OrganState {
  organ: OrganSlot;
  state: BodyState;
  failures: number;
  implantInstanceId?: ItemInstanceId;
  injuries: InjuryEvent[];
}
```

## Criação e progressão auditáveis

O estado final continua sendo a fonte de jogo; o histórico abaixo explica sua formação e permite
revalidar limites sem duplicar bônus no valor-base.

```typescript
type CharacterChoice =
  | { kind: "AttributePoint"; attribute: Attribute; amount: number; phase: "Creation" | "LevelUp" | "Milestone5" }
  | { kind: "KnowledgePoint"; target: ProgressionBonusTarget; amount: number; phase: "Creation" | "LevelUp" }
  | { kind: "FreeAcquiredKnowledge"; knowledgeId: SpecialKnowledgeId }
  | { kind: "RacialOverflowTransfer"; from: Attribute; to: Attribute; amount: number }
  | { kind: "DomainReward"; knowledgeId: SpecialKnowledgeId; reward: DomainRewardKind }
  | { kind: "PathMilestone"; pathId: PathTemplateId; powerId: AbilityCatalogId };

interface CharacterProgression { choices: CharacterChoice[]; }
```

Uma especialização é um `SpecialKnowledgeBase` próprio com `parentKnowledgeId` obrigatório. Ela
não reaproveita nem altera o nível do conhecimento-pai.

## Itens e estados consumíveis

```typescript
interface StackState { quantity: number; groupingKey?: string; }
interface ConsumableState { dosesCurrent: number; chargesCurrent?: number; }
interface AmmoState { ammoTypeId: AmmoTypeId; loaded: number; capacity: number; }
interface ContainerState { contentItemInstanceIds: ItemInstanceId[]; }

interface ItemRuntimeState {
  stack?: StackState;
  consumable?: ConsumableState;
  ammo?: AmmoState;
  container?: ContainerState;
  reload?: { state: "Loaded" | "Empty" | "Reloading"; completesAt?: string };
}

interface InstalledPart {
  installationId: string;
  catalog: CatalogReference<ItemCatalogId>;
}
```

`AmmoTypeId`, `SpecialRuleId`, `SpecialEffectId`, `SpecialAttackRuleId`, propriedades aleatórias
de gema e condições concedidas referenciam catálogos tipados. Nunca são resolvidos por nome.

## Fórmulas, validação e persistência

```typescript
type DerivedFormulaId =
  | "VidaMax" | "SanidadeMax" | "ArcanoMax" | "EnergiaMax" | "DestinoMax"
  | "ProtecaoGeral" | "ProtecaoEsquiva" | "ProtecaoPostura" | "ProtecaoMental"
  | "ProtecaoArcana" | "CargaMaxima" | "LimiteImplantes" | "Deslocamento";

interface DerivedValue { formulaId: DerivedFormulaId; current?: number; }
```

Cada gravação valida: tipos nominais, revisão de catálogo existente, referências ativas, limites
de domínio, requisitos de equipamento e compatibilidade de estado. Dados derivados são
recalculados na leitura. Migrações legadas não inferem valores ausentes nem descartam dados sem
confirmação explícita e trilha de auditoria.
