# Interface Cybergrunge — direção experimental

## Objetivo

A Interface Cybergrunge é uma apresentação alternativa do SDO Companion para celulares. Ela deve parecer
um terminal retrofuturista que continua funcionando enquanto sofre falhas de vídeo, perda de sinal e
interferências difíceis de explicar.

O horror não será contado por frases decorativas. Ele aparece na corrupção visual, no ruído, nos
glitches e na sensação de que alguma forma tenta atravessar a interface.

A Interface Padrão continua sendo a experiência estável. A Interface Cybergrunge aparece logo abaixo dela
nas configurações, marcada pelo selo amarelo **EXPERIMENTAL**. Enquanto a Interface Cybergrunge estiver
selecionada, a escolha de temas fica escondida porque sua paleta faz parte da própria direção artística.

## Princípios

1. **Interface antes de pôster.** A tela precisa parecer um aplicativo em funcionamento, nunca uma
   pintura, parede, cartaz impresso ou colagem de papel.
2. **Storytelling sem explicação.** Evitar frases sobre medo, destino, presença, corrupção ou
   personalidade do personagem. O efeito visual conta essa história sozinho.
3. **Arte antes da função.** O modo experimental deve ser agressivo, irregular e hostil. A operação
   continua possível, mas estabilidade, conforto e previsibilidade não limitam sua direção artística.
4. **Brutalismo na composição.** Usar escala, contraste, blocos rígidos, cortes e assimetria. Evitar
   transformar toda a superfície em textura desgastada.
5. **Glitch com função.** Interferências respondem a estados reais: campo vazio, toque, abertura,
   salvamento, erro, sincronização e mudança de página.
6. **Pouco texto decorativo.** Rótulos exibidos devem identificar dados, ações ou navegação.

## Linguagem visual

- fundo preto profundo, semelhante a vidro de monitor;
- branco de fósforo, vermelho de sinal e ciano frio usado com moderação;
- grade de terminal, scanlines e pixels visíveis;
- componentes retos ou com cortes duros, sem cartões arredondados convencionais;
- fonte monoespaçada de terminal retrô para leitura e controles;
- fonte distorcida somente em títulos de grande impacto;
- figuras anatômicas ou humanoides podem surgir como dados incompletos, nunca como ilustração limpa;
- ruído digital, separação RGB, blocos de compressão, duplicação de frames e rasgos de sinal;
- nada que pareça tinta, concreto, papel envelhecido ou sujeira aplicada sobre toda a tela.

## Campos vazios

Um campo vazio não recebe texto de exemplo nem instrução decorativa. O espaço é ocupado por uma
interferência visual mais violenta que o restante da tela, deixando claro que existe uma lacuna.

A ruptura pode combinar:

- blocos instáveis de pixels;
- fatias horizontais deslocadas;
- canais vermelho e ciano fora de registro;
- máscara fragmentada;
- pulsos de brilho e perda momentânea de sinal;
- contorno do campo tentando se recompor.

A interferência desaparece ou recua assim que o campo recebe conteúdo. Campos opcionais precisam
continuar distinguíveis de campos obrigatórios por sua estrutura e contexto, não por frases falsas.

## Intensidade de movimento

### Latente

Scanlines, pequenos desvios de canal, oscilações de brilho e dados fantasmas em baixa intensidade.

### Interferência

Acontece ao tocar, navegar, expandir, salvar ou alterar um valor. Partes do componente se deslocam,
duplicam ou recompõem por alguns frames.

### Ruptura

Reservada para campos vazios em destaque, falhas, condições graves e eventos importantes. Pode usar
shaders, máscaras, blur, partículas e deformações maiores, sem esconder a ação necessária para sair
daquele estado.

## Componentes

- **Painel:** estrutura rígida, dados limpos e interferência escapando das bordas.
- **Botão:** compressão, deslocamento de canais e retorno rápido ao estado estável.
- **Campo:** cursor e borda de terminal; vazio representado por ruptura, não placeholder.
- **Seletor:** opção ativa tratada como frequência capturada.
- **Navegação:** contador, direção e seção atual; sem frases artificiais de sistema.
- **Carregamento:** reconstrução progressiva de sinal.
- **Erro:** quebra localizada da composição, seguida de uma ação clara.
- **Card vazio:** área de sinal perdido, sem simular conteúdo inexistente.

## Celular

A composição é pensada primeiro para telas verticais:

- controles principais ao alcance do polegar;
- uma área de impacto visual por vez;
- detalhes densos concentrados nas margens e no fundo;
- conteúdo funcional em uma coluna principal;
- títulos grandes sem ocupar a maior parte da altura;
- glitches pesados atravessam fundo, chrome e cartões visíveis;
- cartões podem tremer continuamente, perder alinhamento e sofrer rupturas independentes;
- números, endereços, códigos e estados fantasmas ocupam o fundo como processos invasores.

## Movimento e processamento

O modo usa shaders, camadas, máscaras, blur, partículas e renderização dinâmica para simular um
cyberdeck possuído durante uma disputa na rede local. Glitches contínuos, tremor, separação de canais,
frames duplicados e dados invasores são parte central da experiência, não ornamentação ocasional.
Efeitos saem de execução junto com a tela ou componente e não deixam trabalhos ativos após a navegação.

A experiência padrão não assume esse custo. A Interface Cybergrunge é opt-in e identificada como
experimental.
