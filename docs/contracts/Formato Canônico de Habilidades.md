# Documento técnico do SDO-Companion

Este documento descreve decisões internas do aplicativo. As regras de RPG correspondentes permanecem no repositório de regras e devem ser escritas em linguagem de jogo.

---

# Formato Canônico de Magias, Runas, Cinzas e Poderes

#sistema/regras #sistema/modelo #magia #status/canonico

Formato usado nas regras, na ficha, nos catálogos e no SDO Companion. Este documento define o
**schema tipado** de cada campo. `Efeito` permanece texto livre por design — é onde a mecânica
específica de cada entrada vive. Todos os demais campos são tipados, com enums fechados,
inteiros ou objetos estruturados, para eliminar strings livres desnecessárias no app.

Os enums de personagem (`Attribute`, `Resource`, `BasicKnowledge`, `Protection`,
`SpecialKnowledgeBase` e subtipos) **não são redefinidos aqui**. Este documento importa por
referência de [[Glossário de Enums do Companion]] — qualquer alteração naqueles tipos vale
automaticamente para os schemas abaixo.

Convenção dos blocos de schema: sintaxe TypeScript-like, apenas para documentar tipos — não é
código executável. `?` marca campo opcional. `|` marca união de tipos (enum).

---

## Schema base — `Ability`

Todos os quatro tipos (`Spell`, `Rune`, `Ash`, `Power`) estendem esta base.

```typescript
type AbilityType = "Spell" | "Rune" | "Ash" | "Power";

interface AbilityBase {
  name: string;                  // Título. Usado na checagem de unicidade (ver Unicidade).
  type: AbilityType;
  source: Source;
  execution: Execution;
  range: RangeSpec;
  duration?: Duration;           // ausente quando execution.kind === "Passive"
  resistance: Resistance;
  effect: string;                // texto livre — única exceção de tipagem
}
```

## Fonte — `Source`

```typescript
type SourceKind =
  | "Knowledge"     // Conhecimento
  | "Race"          // Raça
  | "Path"          // Caminho
  | "Item"          // Item
  | "Background"    // Histórico
  | "Profession"    // Profissão
  | "Narrative";    // Narrativa (pacto, evento, recompensa)

interface Source {
  kind: SourceKind;
  sourceRef: SourceRef;
  // Preenchido somente quando kind === "Knowledge".
  knowledge?: {
    // Um Conhecimento Básico referencia BasicKnowledge (enum fechado); um Conhecimento
    // Especial referencia o `id` de um AcquiredKnowledge | ArcaneKnowledge | CombatTechnique
    // já registrado na ficha. Ver [[Glossário de Enums do Companion]].
    knowledgeRef: { basic: BasicKnowledge } | { specialId: string };
    level: number;         // inteiro, 0 até o nível atual do Conhecimento referenciado
  };
}

type SourceRef =
  | { kind: "Knowledge"; knowledgeId: SpecialKnowledgeId | BasicKnowledge }
  | { kind: "Race"; raceId: RaceId; powerCatalogId?: AbilityCatalogId }
  | { kind: "Path"; pathId: PathTemplateId; powerCatalogId?: AbilityCatalogId }
  | { kind: "Item"; itemInstanceId: ItemInstanceId }
  | { kind: "Background"; backgroundId: BackgroundId }
  | { kind: "Profession"; professionId: ProfessionId }
  | { kind: "Narrative"; narrativeSourceId: NarrativeSourceId };
```

Regras de preenchimento:

- `knowledge` só existe quando `kind === "Knowledge"`. Para as demais fontes, o campo fica ausente — nunca `null` residual.
- `sourceRef` é obrigatório e usa o mesmo discriminante de `kind`; a referência nunca é resolvida por nome livre. Para Item, use `sourceRef.itemInstanceId`.
- Alterações temporárias de Atributo ou Conhecimento nunca disparam remoção ou reavaliação automática de `Source`.
- Cinzas **não usam** `Source` no formato acima — ver seção própria [Fonte de Cinza](#fonte-de-cinza-ashorigin).

---

## Execução — `Execution`

```typescript
type ExecutionKind = "Action" | "Turn" | "Free" | "Reaction" | "Passive" | "Timed";

interface Execution {
  kind: ExecutionKind;
  // Preenchido somente quando kind === "Timed".
  timed?: {
    amount: number;               // inteiro positivo
    unit: "Minutes" | "Hours" | "Days";
  };
}
```

Em `Rune`, `Execution` descreve **somente a inscrição**. Gatilho e ativação ficam em `effect`.
`Execution` é campo de consulta — o Companion não controla economia de ações/turnos a partir dele.

---

## Alcance — `RangeSpec`

```typescript
type RangeBand = "Personal" | "Short" | "Medium" | "Long" | "Undefined";
// Personal: toque/self · Short: até 9m · Medium: 9–30m · Long: 30–90m
// Undefined: funcionamento especial descrito em effect; não significa apenas "mais de 90m"

interface RangeSpec {
  band: RangeBand;
  targetArea?: string;   // texto livre opcional — descrição de alvo/área, não normalizado
}
```

---

## Duração — `Duration`

```typescript
type DurationKind = "Instant" | "Turns" | "Scene" | "Session" | "Timed";

interface Duration {
  kind: DurationKind;
  // Preenchido somente quando kind === "Turns".
  turns?: number;                 // inteiro positivo
  // Preenchido somente quando kind === "Timed".
  timed?: {
    amount: number;               // inteiro positivo
    unit: "Hours" | "Days";
  };
}
```

`Duration` fica ausente no objeto `Ability` inteiro quando `execution.kind === "Passive"`.

---

## Resistência — `Resistance`

```typescript
// Protection vem do Glossário: "Geral" | "Esquiva" | "Postura" | "Mental" | "Arcana".
// Resistance reutiliza os mesmos literais e adiciona apenas "None", exclusivo daqui.
type Resistance = Protection | "None";
```

Mapa de legado, usado apenas por rotinas de migração de dados antigos:

| Legado | Resistance atual |
| --- | --- |
| `PG` | `Geral` |
| `PE` | `Esquiva` |
| `PP` | `Postura` |
| `PMental` | `Mental` |
| `PA` | `Arcana` |

A Resistência escolhida é a Proteção usada como CD quando a habilidade tem um NPC ou jogador como
alvo. O Companion apenas expõe esse valor — não resolve rolagem nem escolhe alvo.

---

## Custo por tipo

O recurso do custo é **derivado do tipo** da habilidade, nunca escolhido livremente.

```typescript
// SpendableResource vem de [[Glossário de Enums do Companion]]: "PV" | "PS" | "PM" | "PE" | "PD".
// Power nunca usa "PM" — o subconjunto válido é restringido explicitamente abaixo.
type PowerResource = Exclude<SpendableResource, "PM">;   // "PV" | "PS" | "PE" | "PD"

interface PowerCost {
  resource: PowerResource;
  amount: number;          // inteiro ≥ 0; passivo deve ser 0; ativo deve ser ≥ 1
  isDivineOrLuckBased: boolean;  // habilita PD como opção válida quando true
}

interface SpellCost {
  resource: "PM";
  amount: number;           // inteiro ≥ 0
}

interface RuneCost {
  resource: "PM";
  amount: number;           // pago integralmente na inscrição; ativação não tem custo novo
}

interface AshCost {
  resource: "Dose";
  amount: number;           // inteiro ≥ 1; consumido do item vinculado, sem seletor de recurso
}
```

Regras de validação:

- `Power`: `resource` é sempre `PE`, `PV`, `PS` ou `PD` — nunca `PM`. Se `execution.kind === "Passive"`, `amount` deve ser `0`. Caso contrário, `amount ≥ 1`. `PD` só é uma opção válida em `resource` quando `isDivineOrLuckBased === true`; a flag não obriga o uso de PD, apenas o habilita.
- `Spell`: `resource` é sempre `PM`.
- `Rune`: `resource` é sempre `PM`, pago antes do teste de inscrição.
- `Ash`: `resource` é sempre `Dose`, e o app não expõe seletor de recurso — o consumo é automático do item vinculado.
- Custos compostos (mais de um recurso por habilidade) não existem no modelo.
- O app bloqueia o uso quando o valor atual do recurso for menor que `amount`. PV e PS podem chegar a `0`, nunca abaixo. Nenhuma tentativa bloqueada altera a ficha.
- Dados importados ou persistidos com combinação inválida (ex.: `Power` com `resource: "PM"`) são normalizados para este contrato na leitura, nunca mantidos como estão.

---

## Magia — `Spell`

```typescript
interface Spell extends AbilityBase {
  type: "Spell";
  level: number;         // inteiro ≥ 1
  cost: SpellCost;
}
```

`Nível` é sempre um inteiro — nunca string ("nível 3+", por exemplo, é regra de texto em outra
fonte canônica, não um valor de campo).

---

## Runa — `Rune`

```typescript
interface Rune extends AbilityBase {
  type: "Rune";
  level: 1 | 2 | 3;
  cost: RuneCost;
}
```

Os componentes de composição de uma Runa — Direção, Intensidade, Forma e Sigilo — **não são campos
estruturados do formulário**. Eles fazem parte da descrição mecânica da Runa e são registrados
dentro de `effect`, junto com gatilho e limite de usos. Isso reflete que cada escola de Rúnicos usa
seu próprio compêndio de símbolos (ver [[Runas]]), o que tornaria qualquer enum fechado incompleto
ou incorreto para tradições futuras.

---

## Cinza — `Ash`

```typescript
type AshPurity = "Raw" | "Refined" | "Pure";     // Bruta, Refinada, Pura

type AshOrigin =
  | "Fire" | "Cold" | "Lightning" | "Healing" | "Oneiric" | "Acid" | "Concussive"
  | "Earth" | "Nature" | "Poison" | "Water" | "Air" | "Sound" | "Light" | "Darkness"
  | "Mental" | "Illusion" | "Blood" | "Technology" | "Decay" | "Death" | "Divine";

interface Ash extends Omit<AbilityBase, "source"> {
  type: "Ash";
  origin: AshOrigin;      // substitui Source — ver seção abaixo
  purity: AshPurity;
  cost: AshCost;
  linkedItemId: string;   // item de inventário com 0+ doses, criado automaticamente
}
```

### Fonte de Cinza — `AshOrigin`

Cinzas não usam `Source`. No lugar, usam `origin`, um enum fechado equivalente à "magia de origem"
que gerou o resíduo. `Rune` e `Spell` continuam usando `Source` normalmente.

### Unicidade e Pureza

- Cada combinação de `name` normalizado + `purity` é uma entrada **independente**, com `effect` e estoque (doses) próprios.
- Um mesmo `name` deve possuir três entradas de catálogo — uma por `purity` — quando publicado como conteúdo pronto. Criar uma não cria as demais automaticamente.
- `origin` **não** diferencia duplicatas: duas entradas de mesmo `name` + `purity`, mesmo com `origin` diferente, colidem.
- `purity` altera o texto de `effect`; não impõe dados ou potência fixa — cada entrada define sua própria mecânica.

### Ciclo de vida do item vinculado

- Criar uma `Ash` na seção Mística cria automaticamente `linkedItemId` no inventário com `doses: 0`.
- Adicionar pelo catálogo reutiliza a entrada existente de mesma chave (`name` normalizado + `purity`) em vez de criar outra.
- Renomear a `Ash` renomeia o item vinculado, preservando `linkedItemId`, doses e histórico.
- Carga é derivada, nunca persistida como valor solto:

```typescript
function ashCargo(purity: AshPurity, doses: number): number {
  if (doses === 0) return 0;
  const divisor = { Raw: 1, Refined: 2, Pure: 3 }[purity];
  return Math.ceil(doses / divisor);
}
```

- Uso consome diretamente `linkedItemId`, sem seletor. Doses insuficientes exibem **"Sem cinzas necessárias"** e não alteram a ficha.
- Uma `Ash` com `doses > 0` não pode ser apagada. A tentativa exibe **"Ainda existem essas cinzas no inventário"**.
- Vínculo, consumo e exclusão usam `linkedItemId` estável e operação atômica (tudo ou nada).

---

## Poder — `Power`

```typescript
type PowerSourceKind = SourceKind;   // mesmo enum de Source

interface Power extends AbilityBase {
  type: "Power";
  cost: PowerCost;
  grantsPermanentBonus?: boolean;    // só aplicável quando execution.kind === "Passive"
  permanentBonuses?: PermanentBonus[]; // presente somente se grantsPermanentBonus === true
  passiveState?: PassiveState;         // só aplicável quando execution.kind === "Passive"
}
```

### Bônus passivo estruturado — `PermanentBonus`

Quando um `Power` passivo marca `grantsPermanentBonus: true`, `effect` é substituído por uma lista
de modificadores estruturados — este é o único ponto do formato onde algo sai de texto livre para
dados tipados, porque o Companion precisa somar esses valores automaticamente.

Todos os tipos abaixo (`Attribute`, `SpendableResource`, `Protection`, `BasicKnowledge`) vêm de
[[Glossário de Enums do Companion]] — não são redeclarados aqui.

```typescript
type BonusTarget =
  | { kind: "Attribute"; attribute: Attribute }
  | { kind: "BasicKnowledge"; knowledge: BasicKnowledge }
  | { kind: "SpecialKnowledge"; specialKnowledgeId: string }   // id de AcquiredKnowledge | ArcaneKnowledge | CombatTechnique já possuído
  | { kind: "ResourceMax"; resource: SpendableResource }        // nunca "Exaustao" nem "CorrupcaoDivina"
  | { kind: "Protection"; protection: Protection };

interface PermanentBonus {
  target: BonusTarget;
  amount: number;   // inteiro; pode ser negativo
}
```

Regras:

- Exaustão e Corrupção Divina **não são alvos válidos** de `PermanentBonus` — são `TrackedMarker`, não `SpendableResource`, e portanto ficam fora de `BonusTarget.kind: "ResourceMax"` por construção do tipo, não apenas por convenção documental.
- `BonusTarget` distingue explicitamente `BasicKnowledge` (enum fechado) de `SpecialKnowledge` (referência por `id`, já que Adquiridos/Arcanos/Técnicas são abertos) — um `Power` nunca aponta para um Conhecimento por nome livre.
- Bônus de Poderes diferentes acumulam; nenhum sobrescreve o valor-base do alvo.
- Se um `SpecialKnowledge` referenciado em `BonusTarget` deixar de existir na ficha (ex.: apagado por edição), o `PermanentBonus` **permanece registrado e auditável** — não é apagado nem reescrito automaticamente.
- Se a desativação de um `PermanentBonus` reduzir um `ResourceMax` abaixo do valor atual, o valor atual cai imediatamente ao novo máximo e o excedente é perdido.

### Estado de ativação — `PassiveState`

```typescript
type PassiveState =
  | { mode: "ManualToggle"; active: boolean }   // Power passivo sem Fonte Item
  | { mode: "ItemBound" };                       // Power passivo de Item — sem toggle manual
```

- `ItemBound` é obrigatório quando `source.kind === "Item"`. Nesse caso o estado é **derivado**, não persistido: ativo enquanto o item referenciado por `source.sourceRef.itemInstanceId` estiver em estado `Equipped` ou `Wielded` (ver Contrato do SDO Companion), suspenso caso contrário.
- `ManualToggle` é o padrão para todo Power passivo sem Fonte Item, e começa com `active: false`.

---

## Unicidade

```typescript
interface UniquenessKey {
  type: AbilityType;
  normalizedName: string;   // ver normalização abaixo
  purity?: AshPurity;       // presente somente quando type === "Ash"
}
```

- `Spell`, `Rune` e `Power` são únicos por `normalizedName` dentro do próprio tipo. O mesmo `name` pode existir simultaneamente em tipos diferentes (uma `Spell` e um `Power` podem se chamar "Chama Arcana").
- `Ash` é única por `normalizedName + purity`; `origin` nunca entra na chave de unicidade.
- Normalização de nome: Unicode NFD → remover marcas diacríticas → minúsculas → remover tudo que não seja letra ou número. Comparação usa a chave normalizada completa, nunca um padrão livre fornecido pelo usuário.

Exemplo: `Chama Arcana`, `chama-arcana` e `Cháma  Arcana!` normalizam para a mesma chave; nomes
apenas parcialmente semelhantes não colidem.

### Concorrência e migração

- Catálogos usam `catalogId` estável; entradas manuais usam `persistentId` estável.
- Criar ou renomear para uma `UniquenessKey` já existente é bloqueado **antes de qualquer gravação**, exibindo **"Já existe uma entrada com esse nome"**. Nenhum registro é mesclado ou substituído.
- Toda entrada carrega um campo `revision: number`. Toda gravação compara a `revision` lida com a atual antes de persistir; divergência rejeita a escrita e preserva os dois estados para resolução explícita local × remoto. Nunca aplicar "última gravação vence" silenciosamente.
- Operações que envolvem `Ash` e seu estoque (`linkedItemId`, doses) usam uma única transação; qualquer falha aplica rollback completo.
- Na migração de dados legados, cada colisão de `UniquenessKey` bloqueia a conclusão até escolha explícita do usuário sobre qual registro manter. Após confirmação explícita de irreversibilidade, o registro não escolhido é apagado definitivamente — nunca de forma automática.

---

## Exemplos preenchidos

### Sutura das Cinzas (`Ash`)

```json
{
  "type": "Ash",
  "name": "Sutura das Cinzas",
  "origin": "Healing",
  "purity": "Refined",
  "cost": { "resource": "Dose", "amount": 1 },
  "linkedItemId": "item_sutura_cinzas_refinada",
  "execution": { "kind": "Action" },
  "range": { "band": "Personal", "targetArea": "uma criatura tocada" },
  "duration": { "kind": "Instant" },
  "resistance": "None",
  "effect": "Recupera 1d6 + POD de Vida. Novo uso antes do descanso causa 1 Exaustão."
}
```

### Runa do Silêncio Partido (`Rune`)

```json
{
  "type": "Rune",
  "name": "Runa do Silêncio Partido",
  "level": 2,
  "source": {
    "kind": "Knowledge",
    "knowledge": { "knowledgeRef": { "specialId": "arcane_runico_001" }, "level": 3 }
  },
  "cost": { "resource": "PM", "amount": 4 },
  "execution": { "kind": "Timed", "timed": { "amount": 1, "unit": "Hours" } },
  "range": { "band": "Medium", "targetArea": "área de ativação a 12 metros" },
  "duration": { "kind": "Turns", "turns": 2 },
  "resistance": "Mental",
  "effect": "Direção: para fora do ponto de inscrição. Intensidade: Fraco. Forma: área (traços cruzados). Sigilo: Ar. Gatilho: som acima de volume de conversa normal na área. Ao cumprir o gatilho, rompe a propagação de voz e som na área."
}
```

### Interface de Combate (`Power`, bônus permanente estruturado)

```json
{
  "type": "Power",
  "name": "Interface de Combate",
  "source": { "kind": "Item", "sourceRef": { "kind": "Item", "itemInstanceId": "implante_interface_combate_001" } },
  "cost": { "resource": "PE", "amount": 0, "isDivineOrLuckBased": false },
  "execution": { "kind": "Passive" },
  "range": { "band": "Personal" },
  "resistance": "None",
  "effect": "",
  "grantsPermanentBonus": true,
  "permanentBonuses": [
    { "target": { "kind": "Protection", "protection": "Geral" }, "amount": 0 },
    { "target": { "kind": "Attribute", "attribute": "AGI" }, "amount": 0 }
  ],
  "passiveState": { "mode": "ItemBound" }
}
```

*(valores de `amount` acima são ilustrativos; o Poder real define +1 em ataques físicos e
Iniciativa conforme [[Implantes e Exocorpos]] — o exemplo demonstra apenas a forma do schema.)*

---

## Migração

Preservar dados legados sem reclassificação silenciosa. Não inventar `Source`, `level`,
`purity`, `cost` ou `resistance` ausentes — entradas incompletas permanecem visíveis para
correção manual, nunca preenchidas com um palpite do sistema. Duplicatas normalizadas exigem
escolha explícita antes de qualquer exclusão irreversível.

### Migração específica de Cinzas legadas (registradas como itens comuns)

1. localizar candidatos por `normalizedName` + `purity`;
2. diante de correspondência única, vincular preservando `linkedItemId`, doses e histórico;
3. sem entrada `Ash` correspondente, criar a entrada e solicitar manualmente qualquer campo canônico ausente (`origin`, `cost`, etc.) — nunca inferir;
4. sem `purity` definida ou com múltiplos destinos possíveis, pausar a migração e exigir escolha explícita;
5. nunca mesclar, apagar ou redistribuir doses automaticamente;
6. gravar vínculo e trilha de auditoria na mesma transação.

Regras, schema e exemplos deste documento mudam sempre juntos — uma alteração de tipo sem o
exemplo correspondente atualizado é considerada uma migração incompleta.
