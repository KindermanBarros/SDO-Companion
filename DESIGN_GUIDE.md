# SDO Companion — Design Guide

## Direção

O SDO Companion combina **Cyberpunk / Sci-Fi HUD**, **FUI operacional** e **Acid Graphics /
Neo-Brutalismo Digital**. A interface deve parecer um artefato de campo: técnica, hostil, legível e
deliberadamente imperfeita.

A base é utilitária e monoespaçada sobre um canvas escuro e profundo. O contraste vem de títulos
editoriais agressivos, brilho elétrico (Acid Cyan), alertas críticos em magenta e coral, e sinais
gráficos associados a telemetria, conformidade, arquivos classificados e contenção de dados.

---

## Princípios

1. **Dados antes de decoração.** Todo detalhe visual deve organizar, identificar estado ou reforçar
   hierarquia operacional.
2. **Contraste antagônico.** Tipografia expressiva convive com legendas técnicas, números tabulares
   e componentes de terminal de alta densidade.
3. **Geometria tensionada.** Cortes diagonais (`CutCornerShape`) substituem cantos arredondados
   convencionais; linhas guias e divisores criam direção vetorial.
4. **Cromática funcional disciplinada.** O elétrico **Acid Cyan (`#30C0B7`)** lidera ações, seleções
   e conexões ativas; o **Acid Magenta (`#EE227D`)** e o **Neon Coral (`#FD8083`)** comunicam dano,
   risco e colapso psicológico.
5. **HUD Grid estratificado.** O ruído visual é gerado por malhas milimetradas de duas densidades,
   carimbos normativos, códigos de barra e índices seriais.

---

## Sistema de Cores (Paleta Operacional)

### 1. Fundo de Tela e Superfícies Estruturais (Dark Canvas)

Criam a escuridão profunda do terminal digital sem recorrer ao preto puro, garantindo altíssimo
contraste com menor fadiga visual e suporte a hierarquias de contenção.

| Token              | Hex       | Função e Aplicação                                                                    |
|--------------------|-----------|---------------------------------------------------------------------------------------|
| `Void`             | `#040D1B` | Fundo absoluto da ficha e canvas primário do terminal.                                |
| `VoidDeep`         | `#061424` | Fundo secundário para variações de profundidade e contraste suave.                    |
| `Panel`            | `#191B1C` | Fundo de cartões primários e blocos estruturais de dados.                             |
| `ContainmentPanel` | `#1A060F` | Moldura da foto em pixel art e blocos de contenção de dados instáveis/perigosos.      |
| `ArcanePanel`      | `#261E3C` | Painel especial de alta hierarquia (cartões de linhagem mística ou perícias arcanas). |
| `MysticPanel`      | `#3B0855` | Destaque profundo para entidades transcendentes e grimórios.                          |
| `Carbon`           | `#132B49` | Fundo interno de tabelas de atributos, inputs e áreas preenchíveis.                   |
| `CarbonAlt`        | `#183451` | Variação de fundo para células ativas ou campos selecionados.                         |

---

### 2. Malha Técnica, Divisores e Wireframes (HUD Grid)

Estruturam a diagramação técnica, as linhas de mira e a malha vetorial do sistema.

| Token              | Hex       | Função e Aplicação                                                                      |
|--------------------|-----------|-----------------------------------------------------------------------------------------|
| `Grid`             | `#383B3D` | Grade milimetrada de fundo (linhas menores/secundárias a cada 12 dp).                   |
| `GridGuide`        | `#595F61` | Linhas guias principais da malha técnica (linhas a cada 48 dp).                         |
| `WireframeNeutral` | `#7B8285` | Molduras neutras, barras inativas de código de barras e contornos de caixas de seleção. |
| `WireframeLight`   | `#9BA3A8` | Ícones utilitários inativos e marcações técnicas auxiliares.                            |
| `TechCutDark`      | `#274D7D` | Divisores de tabelas e contornos funcionais de células.                                 |
| `TechCut`          | `#2C5784` | Setas de navegação (colunas de triângulos) e separadores de bloco.                      |
| `TechCutCyan`      | `#498099` | Faixas diagonais de corte técnico em cabeçalhos de seção.                               |

---

### 3. Tipografia e Hierarquia de Leitura

Estabelece a hierarquia entre brilho máximo (glow), dados funcionais e carimbos de auditoria.

| Token              | Hex       | Função e Aplicação                                                                                |
|--------------------|-----------|---------------------------------------------------------------------------------------------------|
| `Ice` / `TextGlow` | `#FCFCFD` | Texto de primeiro plano com brilho máximo (nome do personagem, totais numéricos, valores vitais). |
| `TextPrimary`      | `#EDEFF0` | Texto corrido de leitura primária e valores preenchidos em formulários.                           |
| `LabelFunctional`  | `#CADCF2` | Rótulos funcionais em caixa alta (`RAÇA`, `OCUPAÇÃO`, `IDADE`, `SEXO`).                           |
| `LabelLight`       | `#D8E6F6` | Variação iluminada de rótulos de identificação.                                                   |
| `Muted`            | `#C2C9CC` | Metadados secundários; usar tamanho e peso menores para preservar hierarquia.                     |
| `MetaStamp`        | `#D5D1E5` | Carimbos técnicos, códigos de auditoria e selos normativos (`CE//SDO`).                           |
| `MetalType`        | `#B0A8CE` | Tipografia de estilo metal/acid (`GERAL`, `ESQUIVA`, `POSTURA`) — aspecto cromado frio.           |
| `MetalDeep`        | `#8F82BA` | Sombra ou variação de baixa luz do estilo metal cromado.                                          |

---

### 4. Módulos de Sistema, Energia e Arcano (Tech Blues & Cyans)

Cores elétricas e dinâmicas para nós ativos, telemetria vital e fluxos energéticos.

| Token               | Hex       | Função e Aplicação                                                                                |
|---------------------|-----------|---------------------------------------------------------------------------------------------------|
| `AcidCyan` (`Acid`) | `#30C0B7` | Elemento elétrico de maior destaque: nós ativos, mira do HUD, conexões de rede e ações primárias. |
| `EnergyBlue`        | `#5690DA` | Preenchimento de barras de energia, slots de magia/mana e destaques de seleção.                   |
| `EnergyLight`       | `#5E9CDE` | Variação iluminada de energia e botões de ação secundária.                                        |
| `AuraBlue`          | `#91B6E6` | Gradientes luminosos internos para barras de progresso e auras de perícias místicas.              |
| `AuraLight`         | `#9CC1EA` | Pico luminoso de gradientes de barras arcanas.                                                    |
| `StatHeader`        | `#3B6FB0` | Cabeçalhos operacionais dos blocos de estatísticas vitais (`SANIDADE`).                           |
| `StatHeaderLight`   | `#407AB7` | Cabeçalhos de blocos vitais primários (`VIDA`, `DESTINO`).                                        |
| `ArcanePassive`     | `#6E5BA2` | Indicadores de estados arcanos passivos ou esferas de atributos latentes.                         |
| `ArcaneLatent`      | `#483B6D` | Fundo de receptáculos de poder arcano latente.                                                    |

---

### 5. Alertas, Dano e Tensão Psicológica (Acid Pinks & Corais)

Gama de advertência, desgaste biológico, loucura e status negativos.

| Token                    | Hex       | Função e Aplicação                                                                                    |
|--------------------------|-----------|-------------------------------------------------------------------------------------------------------|
| `AcidMagenta` (`Signal`) | `#EE227D` | Destaque agressivo para avisos críticos, valores de **Corrupção Divina**, efeitos negativos e ameaça. |
| `NeonCoral`              | `#FD8083` | Marcador de perda iminente de postura, medidor de **Exaustão** e alertas de dano recente.             |
| `PenaltyPink`            | `#D85E99` | Etiquetas de penalidade e subtotais de degradação.                                                    |
| `StressPink`             | `#E59BBA` | Sub-barras de estresse mental e destaques intermediários de aflição.                                  |
| `HostileHeader`          | `#852467` | Cabeçalhos de blocos hostis, status de insanidade e condições de penalidade grave.                    |
| `InsanityPink`           | `#A84876` | Indicadores de surto ou colapso cognitivo iminente.                                                   |
| `DamageTrack`            | `#773153` | Fundo vazio (trilha esgotada) de barras de dano acumulado ou sangramento/ferimento.                   |
| `DamageTrackDeep`        | `#46192F` | Fundo profundo de marcadores de ferimento crítico.                                                    |
| `HazardText`             | `#F2D1DD` | Contraste tipográfico sobre superfícies carmesim e rótulos de itens de risco extremo.                 |

---

## Mapeamento de Recursos Vitais

| Recurso       | Cor Ativa / Valor             | Cor da Trilha / Fundo         | Semântica                                 |
|---------------|-------------------------------|-------------------------------|-------------------------------------------|
| **VIDA**      | `StatHeaderLight` (`#407AB7`) | `DamageTrack` (`#773153`)     | Integridade biológica monitorada          |
| **SANIDADE**  | `StatHeader` (`#3B6FB0`)      | `DamageTrack` (`#773153`)     | Estabilidade psíquica / telemetria mental |
| **ARCANO**    | `AuraBlue` (`#91B6E6`)        | `ArcaneLatent` (`#483B6D`)    | Receptáculo místico de alta intensidade   |
| **ENERGIA**   | `EnergyBlue` (`#5690DA`)      | `TechCutDark` (`#274D7D`)     | Capacidade motora e reserva de ativação   |
| **DESTINO**   | `AcidCyan` (`#30C0B7`)        | `TechCutDark` (`#274D7D`)     | Fio condutor e sorte operacional          |
| **EXAUSTÃO**  | `NeonCoral` (`#FD8083`)       | `DamageTrack` (`#773153`)     | Degradação física e proximidade de falha  |
| **CORRUPÇÃO** | `AcidMagenta` (`#EE227D`)     | `DamageTrackDeep` (`#46192F`) | Contaminação divina / anomalia letal      |

---

## Geometria e Espaçamento

- Grid-base: **4 dp**.
- Ritmo principal: 8, 12, 16 e 24 dp.
- Área mínima de toque: **48 dp**.
- Painéis principais: `CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp)`.
- Cards de personagem: `CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp)`.
- Células e inputs: `CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp)`.
- Malha técnica de fundo:
    - Linhas menores a cada 12 dp em `Grid` (`#383B3D`, alpha `0.16f`).
    - Linhas mestras a cada 48 dp em `GridGuide` (`#595F61`, alpha `0.40f`).

---

## Tokens Semânticos

Componentes devem escolher primeiro uma função semântica e somente depois sua cor. Tokens brutos da
paleta não devem ser usados para inventar novos significados em telas isoladas.

| Função | Token base | Uso |
| --- | --- | --- |
| Ação primária / ativo | `AcidCyan` | salvar, adicionar, foco e sincronização concluída |
| Informação | `EnergyBlue` | recursos estáveis, seleção secundária e informação |
| Perigo / destrutivo | `AcidMagenta` | remover, corrupção, erro e ficha trancada |
| Atenção física | `NeonCoral` | exaustão, trauma e degradação |
| Texto primário | `Ice` / `TextPrimary` | valores e conteúdo principal |
| Texto secundário | `Muted` | metadados que não carregam a informação principal |
| Borda discreta | `TechCutDark` | campos inativos, tabelas e subdivisões |

Uma cor de recurso pode ser usada em borda, ícone e barra, mas o rótulo textual permanece em `Ice`
quando a combinação do token com o fundo não alcançar contraste suficiente.

## Tipografia

- **Display:** serifada pesada/itálica apenas em nomes e títulos curtos. Quando uma fonte acid/metal
  licenciada for adicionada, ela deve ficar empacotada no app e possuir fallback documentado.
- **Interface:** monoespaçada para telemetria, códigos, números e labels curtos.
- **Leitura:** sans-serif para história, efeitos, descrições e textos multilinha.
- Corpo mínimo: 12 sp; texto corrente recomendado: 14–16 sp; labels operacionais: mínimo 11 sp.
- Caixa alta é reservada a comandos, status e títulos; não usar em parágrafos.

## Hierarquia e Densidade

Cada viewport deve possuir um único foco elétrico dominante. O orçamento visual é:

1. `AcidCyan` para a ação/estado principal;
2. `AcidMagenta` somente quando houver risco, bloqueio ou destruição;
3. azuis para informação persistente;
4. grid e ornamentos sempre abaixo do contraste do conteúdo.

Códigos de barra, selos e cortes diagonais não devem aparecer juntos mais de uma vez no mesmo painel.
Se o ornamento competir com o nome, valor ou ação, reduza sua opacidade ou remova-o.

## Acessibilidade

- Texto normal deve buscar contraste mínimo de 4,5:1; texto grande, 3:1.
- Estado nunca depende somente de cor: usar texto (`LOCKED`, `SYNC_OK`), ícone e cor em conjunto.
- Área interativa mínima de 48 dp.
- Bloqueio afeta exclusão, não edição: os campos permanecem editáveis e o tipo do bloqueio fica explícito.
- Magenta sobre `Void` pode ser usado como texto em opacidade total; não reduzir sua opacidade em
  mensagens críticas.
- `StatHeader`, `StatHeaderLight` e `EnergyBlue` não devem ser usados como texto pequeno sobre
  `Carbon`; ficam restritos a bordas, ícones e barras.

## Estados dos Componentes

| Estado | Borda | Conteúdo | Indicador |
| --- | --- | --- | --- |
| Inativo | `TechCutDark` | `TextPrimary` | nenhum |
| Foco | `AcidCyan` | `Ice` | cursor cyan |
| Sincronizando | `EnergyBlue` | `Ice` | `LOCAL_DELTA` |
| Sincronizado | `AcidCyan` | `Ice` | `SYNC_OK` |
| Bloqueio pessoal | `AcidMagenta` | editável | cadeado + `LOCK.P` |
| Bloqueio do historiador | `AcidMagenta` | editável | cadeado + `LOCK.H` |
| Erro/destrutivo | `AcidMagenta` | `HazardText` | mensagem explícita |

`LOCK.P` impede o próprio jogador de excluir a ficha e pode ser removido pelo dono. `LOCK.H`
impede exclusão por jogadores e só pode ser alterado pelo historiador. O historiador mantém a ação
de exclusão nos dois estados.

## Layout Responsivo

- Telefones compactos: uma coluna; pares somente para campos curtos.
- Larguras maiores: conteúdo central limitado e painéis em duas colunas quando não quebrar a ordem
  canônica da ficha.
- Listas longas usam blocos repetíveis com ação de remoção no cabeçalho.
- A ordem das 13 seções segue o modelo canônico e não deve variar entre tamanhos de tela.

---

## Componentes do Design System

### HudBackground

Canvas de fundo `Void` (`#040D1B`) com grade vetorial milimetrada em duas densidades (`Grid` e
`GridGuide`). Cria a sensação tátil de um display CRT/OLED de campo.

### TechPanel

Contêiner estrutural com fundo `Panel` (`#191B1C`, 96% opacidade), corte diagonal chanfrado e borda
com a cor de acento do subsistema. Suporta acentos em `AcidCyan`, `HostileHeader`, `ArcanePanel` ou
`TechCutDark`.

### SectionHeader

Elemento composto por:

1. Emblema chanfrado em `AcidCyan` com texto `Void`.
2. Título em caixa alta em `Ice` (`#FCFCFD`).
3. Linha vetorial de divisão em `TechCutCyan` (`#498099`) finalizada com três cortes de alerta
   diagonal em `AcidMagenta` (`#EE227D`).

### TelemetryTag

Indicador de status compacto com borda técnica translúcida.

- Conexões e sincronização: `AcidCyan` (`SYNC_OK`, `PLAYER_ACCESS`).
- Alertas e deltas locais: `AcidMagenta` / `NeonCoral` (`LOCAL_DELTA`, `AUTH_GATE`).
- Metadados de nível: `NeonCoral` (`LV.01`).
- Classes de controle: `MetalType` (`PROFILE.P`, `OVERRIDE.M`).

### Barcode

Assinatura gráfica derivativa com barras neutras em `WireframeNeutral` (`#7B8285`) intercaladas com
estrias de sinal em `AcidMagenta` (`#EE227D`).

### ComplianceMark

Carimbo técnico ficcional `CE//SDO` em `AcidCyan` acompanhado de legenda de conformidade ativa em
`MetaStamp` (`#D5D1E5`).

### HudTextField

Campo de preenchimento estruturado:

- Fundo: `Carbon` (`#132B49`).
- Borda inativa: `TechCutDark` (`#274D7D`).
- Borda ativa / cursor: `AcidCyan` (`#30C0B7`).
- Rótulo inativo: `LabelFunctional` (`#CADCF2`).
- Texto preenchido: `Ice` (`#FCFCFD`) e `TextPrimary` (`#EDEFF0`).

---

## Implementação Técnica

Tokens e componentes fundamentais estão localizados em:

- [SdoDesignSystem.kt](app/src/main/java/com/kinderman/sdo/ui/SdoDesignSystem.kt)
- [Telas de personagem](app/src/main/java/com/kinderman/sdo/presentation/character)
- [Modelo canônico no domínio](app/src/main/java/com/kinderman/sdo/domain/model/Character.kt)
