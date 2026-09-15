# Plano de implementação — Interface Cybergrunge

Este plano transforma o modo experimental em uma interface própria, com componentes, movimento e
renderização separados da Interface Padrão. A implementação deve avançar por etapas revisáveis, sem
misturar regras do RPG com decisões visuais.

## Resultado esperado

- Interface Padrão preservada como experiência estável e personalizável por temas.
- Interface Cybergrunge opt-in, com paleta fixa e selo amarelo **EXPERIMENTAL**.
- Componentes Cybergrunge próprios, não apenas cores diferentes sobre componentes Material.
- Storytelling criado por falhas de sinal, animações e corrupção visual, sem frases decorativas.
- Campos vazios representados por interferência intensa, sem placeholder.
- Experiência desenhada primeiro para celular vertical.
- Efeitos encerrados corretamente quando a tela ou componente deixa de existir.

## Organização da UI

A pasta deve convergir para esta estrutura:

```text
ui/
├── core/            seleção do modo, tokens, tipografia e contratos
├── shared/          comportamento comum e conteúdo independente da aparência
├── standard/        componentes e movimento da Interface Padrão
├── cybergrunge/
│   ├── components/  campos, painéis, botões, seletores e navegação
│   ├── effects/     shaders, máscaras, partículas, blur e falhas de sinal
│   ├── motion/      tempos, sequências e intensidade das animações
│   ├── theme/       paleta, tipografia, formas e dimensões
│   └── preview/     laboratório visual e estados de demonstração
└── SdoDesignSystem.kt
```

As telas continuam declarando conteúdo e ações uma única vez. O design system escolhe a apresentação
Padrão ou Cybergrunge. Nenhuma tela deve repetir a regra `if modo == ...` para cada componente.

## Etapa 1 — Fundação e seletor

- Renomear integralmente o modo para Cybergrunge.
- Mostrar Interface Padrão primeiro.
- Mostrar o selo amarelo **EXPERIMENTAL** imediatamente acima da opção Interface Cybergrunge.
- Esconder o seletor de temas enquanto Cybergrunge estiver ativo.
- Restaurar os temas ao voltar para Interface Padrão.
- Separar arquivos por responsabilidade e criar os pontos de entrada dos dois modos.
- Criar testes para nome, ordem e disponibilidade de temas.

**Aceite:** alternar o modo muda imediatamente a apresentação das configurações e não altera a
preferência de tema armazenada.

## Etapa 2 — Laboratório visual

Criar uma tela interna com todos os estados dos componentes:

- normal, pressionado, focado, selecionado e desabilitado;
- vazio, preenchido, inválido e salvando;
- carregando, sem conexão, sincronizado e com falha;
- card recolhido e expandido;
- navegação anterior, próxima e escolha direta;
- intensidades Latente, Interferência e Ruptura.

O laboratório é a referência visual antes de migrar telas reais.

## Etapa 3 — Motor de animação e glitch

### Movimento latente

- varredura lenta de scanlines;
- oscilação curta de fósforo;
- pixels presos temporários;
- canal de cor deslocado em baixa amplitude;
- sombra digital com atraso sutil.

### Interferência por interação

- botão comprime e separa canais no toque;
- card recompõe borda e conteúdo ao abrir;
- troca de página produz cortes horizontais direcionais;
- seleção captura a opção como uma frequência;
- salvamento alinha camadas antes dispersas.

### Ruptura

- shaders de deslocamento por faixa;
- máscaras fragmentadas;
- blocos de framebuffer;
- blur localizado e duplicação de frame;
- partículas de sinal;
- silhuetas incompletas compostas por raster e dados.

Ruptura não roda continuamente em todos os elementos. Ela é disparada por um estado ou evento e volta
a uma intensidade latente.

## Etapa 4 — Campos e espaços vazios

Criar `CybergrungeField` com:

- texto em fonte terminal;
- cursor de fósforo;
- borda que reage a foco e erro;
- vazio sem placeholder textual;
- textura de glitch violenta enquanto vazio;
- recuo imediato do glitch quando houver valor;
- distinção estrutural entre opcional e obrigatório;
- animação própria para validação e salvamento.

Campos longos usam ruído nas margens e preservam uma área limpa para leitura.

## Etapa 5 — Componentes fundamentais

Implementar pares visuais selecionados pelo design system:

| Contrato | Interface Padrão | Interface Cybergrunge |
| --- | --- | --- |
| painel | `StandardPanel` | `CybergrungePanel` |
| botão | `StandardAction` | `CybergrungeAction` |
| campo | `StandardField` | `CybergrungeField` |
| escolha | `StandardChoice` | `CybergrungeChoice` |
| navegação | `StandardNavigator` | `CybergrungeNavigator` |
| carregamento | `StandardLoading` | `CybergrungeSignalLoading` |
| estado vazio | `StandardEmptyState` | `CybergrungeVoidState` |
| diálogo | `StandardDialog` | `CybergrungeInterruption` |

A API pública permanece estável para evitar duas versões das telas.

## Etapa 6 — Ficha de personagem

Migrar a ficha como primeira experiência completa:

1. Perfil e recursos.
2. Atributos e conhecimentos.
3. Caminho e poderes.
4. Místico.
5. Inventário.
6. Corpo e estado.
7. Anotações e história.

Cada página recebe uma assinatura visual, mas usa os mesmos componentes fundamentais. Poderes,
Místico e Estado aceitam rupturas mais fortes; Perfil e Inventário mantêm maior estabilidade.

## Etapa 7 — Demais telas

- Login: aquisição de sinal e autenticação.
- Painel: terminal operacional de fichas e campanhas.
- Campanha: membros e personagens como dados conectados.
- Historiador: monitoramento e ações de sessão.
- Configurações: calibração dos dois modos.
- Catálogos: busca e leitura como arquivo indexado.

Não adicionar mensagens fictícias ou biografias para preencher espaço.

## Etapa 8 — Fontes e recursos

- Selecionar uma fonte terminal retrô com português completo para leitura.
- Selecionar fontes distorcidas semelhantes a Acidic e Drunk somente para títulos.
- Confirmar licença e formato antes de adicionar ao aplicativo.
- Preferir shaders e desenho nativo para efeitos animados.
- Usar imagens apenas quando forem parte real do conteúdo ou uma textura impossível de reproduzir
  satisfatoriamente em tempo real.

## Etapa 9 — Ciclo de vida e estabilidade

A arte tem prioridade, mas o efeito não pode continuar existindo fora da tela:

- animações vinculadas à composição;
- shaders e buffers liberados ao sair;
- nenhum loop solto ou observador abandonado;
- listas animam apenas itens visíveis;
- recursos gráficos reutilizados;
- eventos rápidos não acumulam animações infinitas;
- Interface Padrão não carrega recursos exclusivos do modo experimental.

## Etapa 10 — Verificação

A cada etapa:

- executar testes unitários;
- executar lint;
- validar em celular compacto e celular alto;
- verificar troca de modo e retorno ao tema anterior;
- inspecionar texto ampliado sem alterar a direção artística;
- procurar animações sobrevivendo à navegação;
- registrar vídeo curto dos estados de movimento;
- atualizar este plano quando uma decisão visual mudar.

## Ordem dos commits

1. seletor, nome e visibilidade de temas;
2. organização da pasta UI;
3. laboratório visual;
4. motor de animação;
5. campos vazios;
6. componentes fundamentais;
7. ficha;
8. demais telas;
9. estabilização e revisão visual.

Cada commit deve deixar o aplicativo compilável.
