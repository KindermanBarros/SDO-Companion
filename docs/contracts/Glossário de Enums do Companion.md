# Documento técnico do SDO-Companion

Este documento descreve decisões internas do aplicativo. As regras de RPG correspondentes permanecem no repositório de regras e devem ser escritas em linguagem de jogo.

---

# Glossário de Enums do Companion

#sistema/integracao #sistema/modelo #status/canonico

Este documento é a **fonte única** dos enums fechados e das estruturas extensíveis de
personagem usados por [[Contrato do SDO Companion]] e [[Formato Canônico de Habilidades para o
Companion]]. Nenhum dos dois documentos duplica um tipo definido aqui — ambos importam por
referência. Se um valor precisar mudar, muda apenas neste arquivo.

Convenção: sintaxe TypeScript-like, apenas para documentar tipos — não é código executável.

---

## Atributos — `Attribute`

```typescript
type Attribute = "FOR" | "VIG" | "AGI" | "POD" | "INT" | "CAR";
// Força · Vigor · Agilidade · Poder · Intelecto · Carisma
```

Enum fechado. Os seis nunca mudam de quantidade — toda regra que soma "+1 em um Atributo" resolve
contra este tipo.

---

## Recursos — `Resource`

```typescript
type Resource = "PV" | "PS" | "PM" | "PE" | "PD" | "Exaustao" | "CorrupcaoDivina";
// Vida · Sanidade · Arcano · Energia · Destino · Exaustão · Corrupção Divina
```

`Resource` é um enum **separado** de `Attribute` e de `Protection` porque Exaustão e Corrupção
Divina não interagem com Poderes, Escolas ou bônus estruturados — não podem ser alvo de
`PermanentBonus` nem de custo de habilidade. Isso é reforçado em ambos os documentos que
referenciam este glossário.

```typescript
type SpendableResource = "PV" | "PS" | "PM" | "PE" | "PD";
// subconjunto de Resource que pode ser gasto/pago por uma habilidade ou efeito
type TrackedMarker = "Exaustao" | "CorrupcaoDivina";
// subconjunto de Resource que é apenas rastreado, nunca pago nem alvo de bônus direto
```

---

## Conhecimentos Básicos — `BasicKnowledge`

Enum fechado de 24 valores, fixo por design (ver [[Criação de Personagem]]). Cada um pertence a
exatamente um `Attribute`.

```typescript
type BasicKnowledge =
  // FOR
  | "Atletismo" | "Brutalidade" | "Luta" | "Arremesso"
  // VIG
  | "Energia" | "Vitalidade" | "Tolerancia" | "Regeneracao"
  // AGI
  | "Furtividade" | "Reflexos" | "Movimento" | "Pontaria"
  // POD
  | "Arcano" | "Sentidos" | "Controle" | "Recuperacao"
  // INT
  | "Sanidade" | "Intuicao" | "Religiao" | "Raciocinio"
  // CAR
  | "Politica" | "Labia" | "Enganacao" | "Intimidacao";

const BASIC_KNOWLEDGE_ATTRIBUTE: Record<BasicKnowledge, Attribute> = {
  Atletismo: "FOR", Brutalidade: "FOR", Luta: "FOR", Arremesso: "FOR",
  Energia: "VIG", Vitalidade: "VIG", Tolerancia: "VIG", Regeneracao: "VIG",
  Furtividade: "AGI", Reflexos: "AGI", Movimento: "AGI", Pontaria: "AGI",
  Arcano: "POD", Sentidos: "POD", Controle: "POD", Recuperacao: "POD",
  Sanidade: "INT", Intuicao: "INT", Religiao: "INT", Raciocinio: "INT",
  Politica: "CAR", Labia: "CAR", Enganacao: "CAR", Intimidacao: "CAR",
};
```

`BasicKnowledge` nunca recebe recompensa de domínio (níveis 3/5) — ver [[Criação de
Personagem#Níveis de Conhecimento e Domínio]]. Isso é uma propriedade da categoria, não do enum
em si, e é reforçada onde `KnowledgeCategory` é usada (ver abaixo).

---

## Proteções — `Protection`

```typescript
type Protection = "Geral" | "Esquiva" | "Postura" | "Mental" | "Arcana";
```

Enum fechado de 5 valores, em português — este é o nome canônico usado em **todo** o app,
inclusive como alvo de bônus estruturado. O tipo `Resistance` do Formato Canônico de Habilidades
usa os mesmos 5 valores (mais `"None"`, exclusivo dele) e **não** os traduz — evita duas grafias
do mesmo enum coexistindo no código.

---

## Conhecimentos Especiais

Diferente de `BasicKnowledge`, as três categorias abaixo são **abertas por design** — o jogador
ou o Historiador cria novas entradas continuamente. Por isso não usam enum fechado de nomes; em
vez disso, definem uma **estrutura de campos obrigatória** que qualquer entrada nova deve
preencher, para que o app permaneça orientado a objetos mesmo com conteúdo ilimitado.

```typescript
type KnowledgeCategory = "Adquirido" | "Arcano" | "TecnicaDeCombate";

interface SpecialKnowledgeBase {
  id: string;              // identificador estável, gerado na criação da entrada
  name: string;             // nome livre, definido pelo jogador/Historiador
  category: KnowledgeCategory;
  primaryAttribute: Attribute;   // obrigatório; valor base permanente mínimo 1 no momento da liberação
  level: number;            // 0 a 5; nível 0 = liberado mas sem domínio
  unlockedAt: number;       // nível do personagem em que foi liberado, para auditoria
}
```

### Conhecimento Adquirido — `AcquiredKnowledge`

```typescript
interface AcquiredKnowledge extends SpecialKnowledgeBase {
  category: "Adquirido";
  domainRewardLevel3?: DomainReward;   // ver estrutura abaixo
  domainRewardLevel5?: DomainReward;
}
```

### Conhecimento Arcano — `ArcaneKnowledge`

```typescript
type ArcaneKnowledgeKind = "ArcaneSchool" | "ArcaneStudy";
// ArcaneSchool: uma Escola Arcana comum (Piromancia, Cura, Necromancia, etc.)
// ArcaneStudy: um Conhecimento Arcano com progressão própria e pacotes automáticos (ex.: Rúnico)

interface ArcaneKnowledge extends SpecialKnowledgeBase {
  category: "Arcano";
  kind: ArcaneKnowledgeKind;
  schoolName?: string;   // livre; preenchido quando kind === "ArcaneSchool"
                          // novas Escolas exigem aprovação do Historiador, mas o campo
                          // permanece string livre por design — não é enum fechado
  autoPackages?: AutoPackage[];   // preenchido quando kind === "ArcaneStudy" (ex.: Rúnico)
  domainRewardLevel3?: DomainReward;
  domainRewardLevel5?: DomainReward;
}

interface AutoPackage {
  unlockLevel: number;         // nível do ArcaneKnowledge que libera o pacote (ex.: 1, 3, 5)
  packageName: string;         // ex.: "Kit de Runas Básicas"
  grantedEntryIds: string[];   // IDs de Rune/Spell/Power concedidos integralmente
  countsAsDomainReward: boolean; // false para Rúnico: o pacote é adicional à recompensa normal
}
```

`ArcaneStudy` existe para capturar exatamente o caso do Conhecimento Rúnico: progressão 0–5,
pacotes automáticos por nível e recompensas de domínio que não substituem o pacote (ver [[Runas
#Progressão do Conhecimento Rúnico]]). Qualquer futuro Conhecimento Arcano com o mesmo padrão de
progressão usa `ArcaneStudy` em vez de precisar de um tipo novo.

### Técnica de Combate — `CombatTechnique`

```typescript
interface CombatTechnique extends SpecialKnowledgeBase {
  category: "TecnicaDeCombate";
  domainRewardLevel3?: DomainReward;
  domainRewardLevel5?: DomainReward;
  // Técnicas nunca concedem Magia ou Runa como recompensa de domínio — ver DomainReward.kind
}
```

### Recompensa de domínio — `DomainReward`

Estrutura comum às três categorias, tipando o que hoje era só uma célula de tabela em texto.

```typescript
type DomainRewardKind =
  | { kind: "Power"; powerId: string }
  | { kind: "SpecializedKnowledge"; specializationCategory: KnowledgeCategory }
  | { kind: "SpellOrRune"; abilityId: string };   // inválido para CombatTechnique

interface DomainReward {
  options: DomainRewardKind[];   // opções entre as quais o jogador escolhe UMA
  chosen?: DomainRewardKind;     // preenchido após a escolha do jogador
}
```

- Para `CombatTechnique`, `DomainRewardKind` nunca inclui `"SpellOrRune"` — a validação de escrita rejeita essa combinação.
- Uma especialização (`SpecializedKnowledge`) começa no nível 1, com seu próprio `SpecialKnowledgeBase`, e evolui de forma independente, recebendo suas próprias `DomainReward` nos níveis 3 e 5.

---

## Raças e Sub-raças

### Raça — `RaceId`

```typescript
type RaceId =
  | "Skayra" | "Humano" | "Elfo" | "Ascendido" | "Golm"
  | "Ciuvati" | "CriaDaNeblina" | "Kaltoch" | "Anao" | "Sonaris" | "Goblin"
  | "Ovaryn" | "Orc" | "Tritao" | "Fada" | "SangueVil" | "Aviano" | "Lumen";
```

Enum fechado de 18 valores (ver [[Raças]]). `Kaltoch` e `Elfo` são raças completas.

### Sub-raça — `SubraceKind`

```typescript
type SubraceKind =
  | "ElfoDoCrepusculo"
  | "Aumentado"
  | "Oraculo"
  | "BestialContaminado"
  | "BestialCompleto";

interface SubraceAssignment {
  kind: SubraceKind;
  keepsBasePower: "First" | "Second";
  substitutedPower: Power;
  bestialDna?: string;
  augmentedChoice?: "TecnologiaAprimorada" | "PosMortal";
  twilightElfChoice?: "InterfaceArcana" | "ConversaoDeEnergia";
}
```

Toda sub-raça mantém os valores e o bônus de Atributo da raça-base, conserva um Poder Racial e substitui o outro por um Poder de Sub-raça.

- `ElfoDoCrepusculo` só pode usar `Elfo` como raça-base.
- `Aumentado` pode usar qualquer `RaceId` como raça-base.
- `Oraculo` e as formas Bestiais seguem as regras descritas em [[Raças]].

### Poderes de Aumentado

O jogador escolhe **um** destes poderes ao adotar a sub-raça Aumentado:

- **Tecnologia Aprimorada:** concede um Conhecimento Adquirido apoiado por um implante.
- **Pós-Mortal:** concede 4 modificações ou implantes iniciais e torna as partes mecânicas imunes a venenos.

Quando a raça-base é Kaltoch, a imunidade total a venenos e doenças comuns continua pertencendo ao poder racial **Imunidade Mecânica**.

### Poderes de Elfos do Crepúsculo

O jogador escolhe **um** destes poderes ao adotar a sub-raça Elfos do Crepúsculo:

- **Interface Arcana:** concede +2 em um Conhecimento ligado a tecnologia, engenharia, artefatos ou magia.
- **Conversão de Energia:** permite recuperar 1 PM, uma vez por turno, sem ultrapassar o máximo.

---

## Referência cruzada

| Tipo | Onde é referenciado |
| --- | --- |
| `Attribute` | `BonusTarget.kind: "Attribute"` e `ProgressionBonusTarget.kind: "Attribute"` (ambos com campo `attribute`) |
| `Resource` / `SpendableResource` | `PowerCost.resource`, `AshCost`, `DerivedMax.resource`, `AbilityResourceRule` |
| `BasicKnowledge` | `BonusTarget.kind: "BasicKnowledge"`, `ProgressionBonusTarget.kind: "BasicKnowledge"`, `DisadvantageRule.knowledge` |
| `SpecialKnowledgeBase` e subtipos | `Source.knowledge.knowledgeRef.specialId`, `BonusTarget.kind: "SpecialKnowledge"`, `ProgressionBonusTarget.kind: "SpecialKnowledge"` |
| `Protection` | `BonusTarget.kind: "Protection"`, `Resistance` (Formato de Habilidades), `EquippedProtection.generalProtectionBonus` (Contrato) |
| `RaceId`, `SubraceKind` | `CharacterRaceSelection` (Contrato) |

Qualquer novo enum de personagem introduzido no futuro entra neste documento primeiro; os outros
dois só referenciam.
