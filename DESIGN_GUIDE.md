# SDO Companion — Design Guide

## Revisão responsiva — setembro de 2026

- As cores históricas abaixo descrevem o tema industrial, não valores fixos para todas as telas.
  Tokens públicos agora resolvem papéis do tema ativo, incluindo modais e barras.
- Aperture White usa azul Portal como destaque; vermelho fica reservado a erros e exclusão.
- Campos compactos: mínimo de 48 dp, padding interno vertical de 8 dp, texto de 14 sp.
  Preservar os intervalos entre itens e permitir crescimento para fontes ampliadas.
- Cabeçalhos recebem largura limitada e podem quebrar em duas linhas; decoração nunca disputa
  espaço com o título. Estados vazios não usam códigos de barras.
- Seções recolhíveis preservam estado durante navegação. Auditoria é opcional em Configurações.
- Acessos rápidos usam ícones com descrição acessível. Salvamento usa ícone e toast sob demanda.
- Exclusão de campanha exige confirmação e conexão. Fichas são desvinculadas, não apagadas;
  um estado terminal impede restauração por clientes antigos.

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

- **Assinatura black metal — MB Forever Raw:** lettering ornamental espinhoso usado apenas em
  títulos fixos, grandes e em caixa alta. Nunca aplicar a campos, parágrafos, números críticos ou
  nomes de personagens; a baixa legibilidade é intencional e funciona como imagem de marca.
- **Interface — Oxanium:** sans quadrada e futurista usada em títulos funcionais, campos, botões e
  leitura. Sua construção geométrica faz a ponte entre o logo orgânico e a grade técnica.
- **Telemetria:** fallback monoespaçado do sistema para códigos, IDs, números e labels curtos.
- As fontes ficam empacotadas no APK. Oxanium usa SIL OFL 1.1; MB Forever Raw permite uso pessoal
  e comercial, sem modificação. Avisos completos e o `ReadMe` original ficam em `licenses/fonts/`.
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

O identificador `OWNER` no card é uma ação exclusiva do historiador. Ele mostra o primeiro nome da
conta Google como informação primária e o UID truncado apenas como telemetria. Ao tocar, abre uma
lista pesquisável de todos os perfis já registrados; a troca de owner exige confirmação pela
seleção explícita de outra pessoa.

## Motion Design

Movimento comunica mudança de estado; não é decoração contínua fora de processos ativos.

| Token | Duração | Uso |
| --- | ---: | --- |
| `RESPONSE` | 180 ms | foco, seleção e resposta direta ao toque |
| `TRANSITION` | 300 ms | entrada/saída de painéis e mudança de estado |
| `SIGNAL_PULSE` | 700 ms | pulso reversível de atividade |
| `TELEMETRY_SCAN` | 1100 ms | varredura linear de progresso indeterminado |

- Autenticação inicial, autenticação Google e carga inicial de personagens usam tela dedicada;
  sincronizações posteriores usam o indicador dentro do painel, preservando o contexto.
- Processos sem progresso mensurável usam animação indeterminada e sempre exibem uma descrição
  textual específica. Nunca simular porcentagem.
- A varredura mantém velocidade linear; o pulso usa aceleração/desaceleração suave. Não misturar
  mais de dois ritmos no mesmo componente.
- Evitar deslocar conteúdo já legível. A animação fica contida no indicador e não bloqueia leitores
  de tela: o componente publica semântica de progresso e descrição do processo.
- Não usar flashes, strobe, tremor ou alternância rápida de alto contraste. As animações Compose
  acompanham a escala de animação configurada pelo sistema.
- Ao concluir, trocar o estado imediatamente; não impor duração mínima artificial ao loading.

## Layout Responsivo

- Telefones compactos: uma coluna; pares somente para campos curtos.
- Larguras maiores: conteúdo central limitado e painéis em duas colunas quando não quebrar a ordem
  canônica da ficha.
- Listas longas usam blocos repetíveis com ação de remoção no cabeçalho.
- A ficha usa seis páginas com swipe, abas roláveis, contador e controles anterior/próxima.
- Somente a página atual e páginas adjacentes entram na composição; cada página usa sua própria
  `LazyColumn`, evitando medir as 13 seções simultaneamente.
- A ordem das 13 seções segue o modelo canônico e não deve variar entre tamanhos de tela:

| Página | Seções canônicas |
| --- | --- |
| Perfil | 01 Identidade, 02 Recursos, 03 Traços |
| Aptidões | 04 Atributos, 05 Conhecimentos Especiais, 06 Proteções |
| Caminho | 07 Caminho e Poderes |
| Corpo | 08 Inventário, 09 Corpo, 10 Órgãos |
| Místico | 11 Magias, Runas e Cinzas |
| Registro | 12 Condições, 13 História e Notas |

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

### CyberLoadingIndicator

Indicador indeterminado formado por 15 segmentos de telemetria, scanner horizontal e pulso de
sinal. Usa `AcidCyan` como energia, `AcidMagenta` somente nos marcos de alerta e texto funcional
em Oxanium/monoespaçada. Possui variantes de autenticação e carregamento de personagens.

### OwnerPickerDialog

Painel modal do historiador para transferência de ficha. Mantém busca sempre visível, lista
preguiçosa limitada à altura útil, primeiro nome em destaque, e-mail para desambiguação e UID como
metadado. O owner atual recebe `OWNER.ATUAL`; resultados vazios usam `NO_SIGNAL`.

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
