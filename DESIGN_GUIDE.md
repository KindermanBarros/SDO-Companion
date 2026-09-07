# SDO Companion — Design Guide

## Direção

O SDO Companion combina **Cyberpunk / Sci-Fi HUD**, **FUI operacional** e
**Acid Graphics / Neo-Brutalismo Digital**. A interface deve parecer um artefato
de campo: técnica, hostil, legível e deliberadamente imperfeita.

A base é utilitária e monoespaçada. O contraste vem de títulos editoriais
agressivos, cor ácida e sinais gráficos associados a telemetria, conformidade,
arquivos classificados e alertas.

## Princípios

1. **Dados antes de decoração.** Todo detalhe visual deve organizar, identificar
   estado ou reforçar hierarquia.
2. **Contraste antagônico.** Tipografia expressiva convive com legendas técnicas,
   números tabulares e componentes de terminal.
3. **Geometria tensionada.** Cortes diagonais substituem cantos arredondados
   convencionais; linhas e barras criam direção.
4. **Ácido com disciplina.** Verde e laranja são sinais, não fundos decorativos.
5. **Grunge controlado.** Ruído vem de grid, códigos, abreviações e repetição,
   mantendo leitura e toque acessíveis.

## Paleta

| Token | Hex | Uso |
| --- | --- | --- |
| Void | `#090B0B` | fundo global |
| Carbon | `#121515` | campos e células |
| Panel | `#191D1C` | painéis elevados |
| Grid | `#313936` | grid, divisores e bordas neutras |
| Acid | `#D7FF38` | ação primária, seleção e identificação |
| Signal | `#FF5A36` | risco, alterações locais e alertas |
| Cyan | `#57E6DE` | sincronização, integridade e proteção |
| Ice | `#DCE5DF` | texto principal |
| Muted | `#89938E` | metadados e texto secundário |

Use no máximo uma cor de sinal dominante por painel. `Acid` identifica ações
e estrutura; `Signal` nunca deve ser usado como confirmação positiva.

## Tipografia

- **Display:** serifada, preta e itálica, comprimida visualmente. É o espaço
  reservado para a futura fonte licenciada de inspiração metal/black-metal.
- **Interface:** monoespaçada em caixa alta.
- **Telemetria:** monoespaçada, pequena e com tracking amplo.
- Nomes de campos são diretos; metadados podem usar `//`, `.`, códigos e
  índices: `PLAYER_FILE // LIVE`, `ID.A3F901`, `LV.04`.
- Não usar a tipografia display em parágrafos ou valores críticos.

## Geometria e espaçamento

- Grid-base: **4 dp**.
- Ritmo principal: 8, 12, 16 e 24 dp.
- Área mínima de toque: **48 dp**.
- Painéis: `CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp)`.
- Campos: cortes menores, nunca pill-shaped.
- Bordas: 1 dp; use 2 dp apenas em foco crítico.
- O grid de fundo usa linhas menores a cada 12 dp e linhas principais a cada
  48 dp.

## Componentes

### HudBackground

Canvas global preto com grid milimetrado. Não colocar outro grid dentro de
cards; isso reduz a legibilidade.

### TechPanel

Contêiner principal com fundo `Panel`, borda de sinal e cortes diagonais.
Agrupa uma unidade semântica completa.

### SectionHeader

Índice ácido + título + linha técnica com três cortes de alerta. Seções devem
manter numeração estável dentro da ficha.

### TelemetryTag

Estado curto, em caixa alta. Exemplos:

- `SYNC_OK` em Cyan;
- `LOCAL_DELTA` em Signal;
- `PLAYER_ACCESS` em Acid.

### Barcode

Identificador gráfico derivado de um ID estável. É assinatura visual, não
substitui o texto do identificador e não deve ser usado como QR code.

### ComplianceMark

Marca ficcional `CE//SDO`; comunica integridade do arquivo e reforça a FUI.
Nunca imitar selos oficiais de segurança ou certificações reais.

### HudTextField

Campo escuro, recorte técnico, borda Acid no foco. Rótulo sempre em caixa alta.
Erros ficam abaixo do campo em Signal e devem explicar a correção.

## Estados

| Estado | Tratamento |
| --- | --- |
| normal | Grid/Muted |
| foco e ação | Acid |
| sincronizado | Cyan + `SYNC_OK` |
| alteração local | Signal + `LOCAL_DELTA` |
| erro | borda Signal + mensagem textual |
| desabilitado | Muted com contraste reduzido |

Cor nunca é o único sinal: sempre combinar com texto, ícone ou padrão.

## Conteúdo e voz

A linguagem é curta, operacional e em português. Termos ficcionais podem
acompanhar um significado reconhecível:

- `SALVAR // SINCRONIZAR`;
- `ROOM_LOCAL → FIRESTORE_REMOTE`;
- `MEMÓRIA DE CAMPO` para história;
- `MATRIZ DE PROTEÇÃO` para defesas.

Evite frases longas em caixa alta. Textos explicativos usam sentence case.

## Acessibilidade

- Contraste mínimo WCAG AA para informações essenciais.
- Não reduzir textos operacionais abaixo de 11 sp.
- Respeitar escala de fonte e rolagem vertical.
- Ícones interativos precisam de descrição.
- Animações futuras devem respeitar redução de movimento.
- O estilo metal aparece somente em títulos; dados permanecem monoespaçados e
  legíveis.

## Implementação

Tokens e componentes estão em
`app/src/main/java/com/kinderman/sdo/ui/SdoDesignSystem.kt`.
Não duplicar cores ou formas nas telas. Novos componentes devem ser construídos
a partir de `HudBackground`, `TechPanel`, `SectionHeader`,
`TelemetryTag`, `Barcode` e `HudTextField`.

