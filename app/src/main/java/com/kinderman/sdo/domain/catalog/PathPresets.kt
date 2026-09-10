package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.Power

data class PathPreset(
    val catalogId: String,
    val motto: String,
    val keywords: List<String>,
    val pillars: List<String>,
    val powers: List<PathPower>,
)

data class PathPower(
    val name: String,
    val effect: String,
    val cost: String = "Sem custo",
    val action: String = "Passiva",
    val range: String = "Pessoal",
    val duration: String = "Instantânea",
    val limit: String = "Sem limite adicional",
    val category: String = "Poder de Caminho",
    val activationCondition: String = "Sempre ativo",
    val enhancements: String = "Sem aprimoramento publicado",
    val deactivationCondition: String = "Não aplicável",
    val costType: com.kinderman.sdo.domain.model.AbilityCostType = com.kinderman.sdo.domain.model.AbilityCostType.NONE,
    val costValue: Int = 0,
    val destinyCostEligible: Boolean = false,
    val executionType: com.kinderman.sdo.domain.model.AbilityExecution = com.kinderman.sdo.domain.model.AbilityExecution.PASSIVE,
    val rangeType: com.kinderman.sdo.domain.model.AbilityRange = com.kinderman.sdo.domain.model.AbilityRange.PERSONAL,
    val durationType: com.kinderman.sdo.domain.model.AbilityDuration = com.kinderman.sdo.domain.model.AbilityDuration.INSTANT,
    val durationValue: Int = 0,
    val durationUnit: com.kinderman.sdo.domain.model.AbilityTimeUnit = com.kinderman.sdo.domain.model.AbilityTimeUnit.HOURS,
    val resistance: com.kinderman.sdo.domain.model.AbilityResistance = com.kinderman.sdo.domain.model.AbilityResistance.NONE,
)

object PathPresets {
    val entries = listOf(
        PathPreset(
            catalogId = """path.engrenagens""".trimIndent(),
            motto = """Nada é lixo, só está no lugar errado.""".trimIndent(),
            keywords = listOf("""Reparar""".trimIndent(), """Criação""".trimIndent(), """Perseverança""".trimIndent()),
            pillars = listOf(
                """Consertar ou Tentar — Se algo ainda pode funcionar, vale a pena tentar salvá-lo.""".trimIndent(),
                """Inovação — Todo mundo conserta máquinas. Eu quero construir uma que ninguém nunca imaginou.""".trimIndent(),
                """Persistência — Se não deu certo, é porque ainda falta uma peça.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Arquiteto da Sucata""".trimIndent(),
                    effect = """gerar um item temporário com a sucata que tiver ao seu redor. O item não pode ser modificado nem adornado e dura 1 cena.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    duration = "1 cena",
                    durationType = com.kinderman.sdo.domain.model.AbilityDuration.SCENE,
                ),
                PathPower(
                    name = """Eureka!""".trimIndent(),
                    effect = """Ao realizar uma análise utilizando Engenhocaria, receba +5 no teste.

Para cada falha nessa análise, você pode utilizar 1 sucata para cada 1 no custo daquele item para desfazê-la.

Observação: a segunda parte foi preservada como está na ficha; “falha nessa análise” e “cada 1 no custo” não são definidos nela.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.personagem""".trimIndent(),
            motto = """O Maior dos Papéis, também requer o maior dos sacrifícios.""".trimIndent(),
            keywords = listOf("""Atuar""".trimIndent(), """Plateia""".trimIndent(), """Desenvolvimento""".trimIndent()),
            pillars = listOf(
                """Ato I — Introduzir o público ao cenário e à trama.""".trimIndent(),
                """Ato II — O protagonista enfrenta obstáculos, conflitos crescentes e tenta alcançar seu objetivo, geralmente encontrando aliados e inimigos pelo caminho.""".trimIndent(),
                """Ato III — É o momento de clímax, onde o conflito principal atinge o seu ponto mais alto e é resolvido. A história é concluída, mostrando as consequências da jornada para o personagem e seu mundo.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Falas Preparadas""".trimIndent(),
                    effect = """Durante um descanso, você pode escolher não recuperar Energia. Caso faça isso, você prepara dois objetos alterados em até itens incomuns até o fim do dia.""".trimIndent(),
                ),
                PathPower(
                    name = """Instrumento de Cenário""".trimIndent(),
                    effect = """Você pode gastar 3 de Energia em vez de fazer o teste normal de Lábia. Você cria um objeto inexistente no cenário até alguém notar que ele não estava lá.""".trimIndent(),
                    cost = "3 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 3,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.necrocamminus""".trimIndent(),
            motto = """A morte não fala, ela sussurra.""".trimIndent(),
            keywords = listOf("""Silêncio""".trimIndent(), """Morte""".trimIndent(), """Empatia""".trimIndent()),
            pillars = listOf(
                """Descanso — Descrição ainda não preenchida.""".trimIndent(),
                """Compaixão — Descrição ainda não preenchida.""".trimIndent(),
                """Ritos — Descrição ainda não preenchida.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Ritos de Passagem""".trimIndent(),
                    effect = """executar um ritual de 3 turnos para acalmar um morto em sua presença. Se for uma criatura morto-vivo, sua forma física se desfaz.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                ),
                PathPower(
                    name = """Instrumentos do Ofício""".trimIndent(),
                    effect = """Você inicia com uma arma de truque que se torna um instrumento de sua escolha.

Na ficha, o equipamento relacionado registrado é Tripidante, com 1d10 de dano e forma Pá — Instrumento.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.herdeiro_ruinas""".trimIndent(),
            motto = """O tempo enterra impérios. Eu desenterro suas histórias.""".trimIndent(),
            keywords = listOf("""História""".trimIndent(), """Memória""".trimIndent(), """Legado""".trimIndent()),
            pillars = listOf(
                """As Pedras Lembram — As ruínas preservam a memória dos impérios que o tempo tentou apagar. Cada pedra, símbolo e vestígio guarda uma história esperando para ser redescoberta.""".trimIndent(),
                """Ecos do Passado — Toda relíquia carrega o legado de quem a criou. Valar acredita que esses vestígios devem ser compreendidos e preservados, nunca tratados como simples espólios.""".trimIndent(),
                """Conhecimento é Poder — O maior tesouro não é o ouro, mas o conhecimento. Cada descoberta fortalece a compreensão do passado e revela caminhos para o futuro.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Leitor de Ruínas""".trimIndent(),
                    effect = """Recebe +2 de Linguística.""".trimIndent(),
                ),
                PathPower(
                    name = """Memória Onírica""".trimIndent(),
                    effect = """Ao entrar em contato físico com um objeto ou local, captar a impressão emocional mais forte deixada naquele lugar.

O poder não pode ser usado no mesmo objeto ou local mais de uma vez por dia.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "Toque",
                    limit = "Uma vez por dia",
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.la_befana""".trimIndent(),
            motto = """Vem dos montes a noite profunda.""".trimIndent(),
            keywords = listOf("""Culinária""".trimIndent(), """Cuidado""".trimIndent(), """Tradição""".trimIndent()),
            pillars = listOf(
                """Vingança — "A melhor vingança é a indiferença."

Carlo passou grande parte da vida sendo definido por aqueles que o aprisionaram,
transformaram e exploraram.

Sua vingança não é necessariamente destruir todos que lhe fizeram mal, mas
construir uma vida que eles não conseguiram tirar dele.

Viver bem, tornar-se alguém e carregar Fabrizio consigo é uma forma de negar
aos seus opressores a vitória final.""".trimIndent(),
                """Cozinhar — "Cozinhar é relembrar. Enquanto eu cozinhar, Fabrizio não morre em minha memória."

A culinária começou com Fabrizio, tornou-se uma ferramenta de sobrevivência
e vingança nas minas e finalmente se transformou em propósito através dos
ensinamentos de Xantia.

Carlo aprende o mundo através da comida.

Novos ingredientes, técnicas e receitas são também novas histórias.""".trimIndent(),
                """Felicidade — "Se existe alguma para mim nesse mundo, vou procurar em todo lugar — e, quando encontrá-la, vou dividi-la como uma bela Carbonara."

Carlo passou grande parte da vida apenas sobrevivendo.

Agora procura experiências, pessoas, sabores e lugares capazes de mostrar que
existir pode significar mais do que simplesmente continuar respirando.

Encontrar felicidade não é suficiente.

Ele quer aprender a compartilhá-la.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Ingrediente Secreto""".trimIndent(),
                    effect = """Carlo aprendeu que ingredientes carregam propriedades que vão além de sabor e
valor nutricional.

Características marcantes de animais, monstros, plantas, fungos e outros
ingredientes podem sobreviver ao processo culinário quando preparados
corretamente.

Analisar Ingrediente

Ao encontrar um ingrediente incomum, Carlo pode examiná-lo utilizando:

INT + Degustação

O Mestre define a dificuldade conforme a raridade e complexidade do ingrediente.

Em caso de sucesso, Carlo identifica uma Propriedade Culinária coerente com
a natureza do ingrediente.

Exemplos:

• carne de uma criatura extremamente resistente → Resistência;
• olhos de um predador → Percepção;
• carne de uma criatura veloz → Agilidade;
• fungo arcano → Arcano;
• criatura venenosa → Tolerância;
• animal adaptado ao frio → Resistência ao Frio;
• criatura capaz de escalar → Escalada;
• criatura noturna → Visão ou Sentidos.

A propriedade precisa existir ou ser claramente representada pelo ingrediente.

Extrair Propriedade

Ao preparar uma refeição utilizando um ingrediente analisado, Carlo pode realizar um teste de:

INT + Culinária

Em caso de sucesso, escolha uma criatura que consumir o prato.

Até o final da próxima cena relevante, ela recebe:

+2 em testes diretamente relacionados à Propriedade Culinária extraída.

Exemplo:

Carlo prepara carne de uma criatura conhecida por sua visão excepcional.

A propriedade identificada é Percepção.

Quem consumir o prato pode receber +2 em testes de Sentidos baseados em visão
durante a duração do efeito.

Limites

• apenas 1 Propriedade Culinária pode ser extraída por prato;
• uma criatura só pode possuir 1 efeito de Ingrediente Secreto ativo;
• o poder não copia literalmente Poderes Raciais, magias ou habilidades completas;
• ele traduz uma característica do ingrediente em um benefício culinário limitado.
•""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    category = "Ofício / Sobrenatural",
                ),
                PathPower(
                    name = """Prato Favorito""".trimIndent(),
                    effect = """Xantia ensinou Carlo que conhecer ingredientes não é suficiente.

Uma refeição pode carregar lembranças, conforto e significado.

Carlo precisa conhecer o prato favorito da pessoa e possuir ingredientes
razoavelmente adequados para prepará-lo.

Durante um descanso ou período seguro, Carlo pode preparar esse prato.

A criatura que consumir a refeição:

remove 1 ponto adicional de Exaustão

e recebe:

+2 no primeiro teste diretamente relacionado a medo, desespero, trauma ou resistência mental realizado antes do próximo descanso.

Uma criatura só pode receber os benefícios de Prato Favorito uma vez por dia.

Descobrindo um Prato Favorito

Carlo não pode simplesmente declarar qual é o prato favorito de outra pessoa.

Ele precisa descobrir isso através de:

• conversa;
• convivência;
• observação;
• história compartilhada;
• investigação;
• ou preparação de refeições para aquela pessoa.

Isso transforma conhecer os outros em parte do próprio Caminho.

A Carbonara de Fabrizio

A Carbonara possui significado especial para Carlo.

Quando Carlo prepara Carbonara para si mesmo, ela conta automaticamente como
seu Prato Favorito.

Quando a prepara para alguém que conheça a história de Fabrizio e compartilhe
a refeição com Carlo, ela também pode ser tratada como Prato Favorito daquela
pessoa caso exista uma ligação emocional construída em jogo.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por dia",
                    category = "Social / Sobrenatural",
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.festival_eterno""".trimIndent(),
            motto = """A pessoa mais completa é aquela livre para rir, lamentar e gritar.""".trimIndent(),
            keywords = listOf("""Impressionar""".trimIndent(), """Motivar""".trimIndent(), """Alegrar""".trimIndent()),
            pillars = listOf(
                """Dualidade na Vida — A tragédia e o horror produzidos pelo mundo devem ser equilibrados pela comédia, pelo romance, pelo espetáculo e pelos momentos de alegria criados pelo Circo.""".trimIndent(),
                """Liberdade Plena — A maior dádiva entregue aos mortais é o direito de existir e tornar-se aquilo que desejam. A liberdade deve ser protegida.""".trimIndent(),
                """Estude e Impressione — Conhecimento também é espetáculo. Estudo e técnica permitem criar desde apresentações até máquinas, fogos de artifício e invenções.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Palco de Batalha""".trimIndent(),
                    effect = """Uma vez por cena, enquanto possuir uma Araninha Dançarina funcional a até 10 metros, iniciar uma apresentação sincronizada.

Escolha até 3 aliados que consigam perceber a música ou apresentação.

Cada um recebe:

+2 no próximo teste realizado até o início do seu próximo turno.

O bônus não acumula com Ajuda no mesmo teste.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "10 metros",
                    rangeType = com.kinderman.sdo.domain.model.AbilityRange.MEDIUM,
                    limit = "Uma vez por cena",
                    category = "Suporte / Performance",
                    enhancements = """Grande Espetáculo

O benefício dura até o final do próximo turno de cada aliado.""".trimIndent(),
                ),
                PathPower(
                    name = """O Show Tem Que Continuar""".trimIndent(),
                    effect = """Uma vez por cena, quando você ou um aliado a até 10 metros falhar em um teste por uma diferença de até 2 pontos, transformar a falha em:

sucesso parcial.

A ação funciona, mas deve gerar uma complicação, custo ou consequência menor apropriada.""".trimIndent(),
                    cost = "1 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 1,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "10 metros",
                    rangeType = com.kinderman.sdo.domain.model.AbilityRange.MEDIUM,
                    limit = "Uma vez por cena",
                    category = "Motivação / Resiliência",
                    enhancements = """Encore

O poder pode ser usado duas vezes por cena, mas nunca duas vezes no mesmo teste.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.mao_justica""".trimIndent(),
            motto = """Proteger os necessitados, ensinar os fracos, punir os opressores.""".trimIndent(),
            keywords = listOf("""Justiça""".trimIndent(), """Remissão""".trimIndent(), """Punição""".trimIndent()),
            pillars = listOf(
                """Justiça — A força deve ser usada para impedir que inocentes sejam abandonados diante da violência.""".trimIndent(),
                """Remissão — Bismarck busca compensar os próprios pecados através de proteção, disciplina e serviço.""".trimIndent(),
                """Punição — Quem insiste em oprimir os fracos deve enfrentar consequências.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Guilhotina de Bromir""".trimIndent(),
                    effect = """Uma vez por cena, ao atingir com um ataque corpo a corpo uma criatura que esteja:

com 25% ou menos de sua Vida Máxima

transformar o golpe em uma execução direcionada.

O ataque recebe:

+1d6 de dano físico

e ignora a penalidade de Ataque Direcionado contra uma região corporal, caso esteja mirando cabeça, pescoço ou outro ponto vital apropriado.

Se o dano gerar uma Falha Corporal ou consequência crítica suficiente para matar o alvo segundo as regras normais, a execução ocorre.""".trimIndent(),
                    cost = "3 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 3,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por cena",
                    category = "Combate / Execução",
                    enhancements = """Sentença

Quando utilizar Guilhotina de Bromir, o dano adicional aumenta para:

+1d8""".trimIndent(),
                ),
                PathPower(
                    name = """Provocação Intensificada""".trimIndent(),
                    effect = """Uma vez por cena, escolha uma criatura a até 10 metros que esteja atacando ou ameaçando um aliado e Faça um Teste Resistido apropriado de intimidação, provocação ou domínio contra o alvo.

Em caso de sucesso, até o início do seu próximo turno:

• o alvo recebe −4 em ataques contra criaturas que não sejam você;
• você recebe +2 na Proteção apropriada contra o primeiro ataque realizado por ele contra você.

O alvo ainda pode escolher agir contra outra criatura; o poder não remove sua agência.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "10 metros",
                    rangeType = com.kinderman.sdo.domain.model.AbilityRange.MEDIUM,
                    limit = "Uma vez por cena",
                    category = "Controle / Proteção",
                    enhancements = """Olhe Para Mim

A duração passa até o final do seu próximo turno.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.redencao""".trimIndent(),
            motto = """Da mais densa escuridão, surgirá a mais brilhante luz.""".trimIndent(),
            keywords = listOf("""Laços""".trimIndent(), """Reparação""".trimIndent(), """Salvação""".trimIndent()),
            pillars = listOf(
                """Devo Criar Laços — Robert decidiu deixar de existir isoladamente e construir relações verdadeiras com aqueles ao seu redor.""".trimIndent(),
                """Vou me Redimir — Depois de ter ferido pessoas por egoísmo e violência, busca compensar o dano que causou.""".trimIndent(),
                """Irei Salvar — Habilidades antes usadas para caçar e matar agora devem servir para preservar vidas.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Coadjuvante""".trimIndent(),
                    effect = """Quando usar a ação Ajuda em um teste no qual possua um Conhecimento relevante, você concede:

+5 nesse teste em conjunto.

Você não pode ser o personagem principal daquele teste.""".trimIndent(),
                    category = "Cooperação",
                    enhancements = """Eu Cubro Você

Uma vez por cena, quando já estiver participando diretamente da mesma tarefa, pode usar Ajuda sem gastar uma ação.""".trimIndent(),
                ),
                PathPower(
                    name = """Sentidos Aprimorados""".trimIndent(),
                    effect = """+2 em testes de Sentidos para perceber ameaças, rastros ou alterações no ambiente.

Uma vez por cena, quando estiver conscientemente procurando algo, transforme o bônus em:

+4 para um único teste.""".trimIndent(),
                    limit = "Uma vez por cena",
                    category = "Percepção",
                    enhancements = """Caçador Reformado

Quando obtiver sucesso usando o bônus de +4, pode também determinar uma informação adicional plausível sobre aquilo que percebeu: direção, quantidade aproximada, tempo de passagem ou estado geral.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.conhecimento_juvenil""".trimIndent(),
            motto = """Deve-se olhar para as coisas com a curiosidade de uma criança e a sabedoria de um pesquisador.""".trimIndent(),
            keywords = listOf("""Conhecimento""".trimIndent(), """Curiosidade""".trimIndent(), """Exploração""".trimIndent()),
            pillars = listOf(
                """Sabor da Vida — Conhecimento torna a existência mais interessante. Passado e presente ajudam a compreender aquilo que ainda está por vir.""".trimIndent(),
                """Admiração Infantil — Aquilo que parece simples ainda pode esconder algo novo, estranho ou divertido quando observado sem preconceitos.""".trimIndent(),
                """Exploração — A ficha não apresenta um terceiro Pilar formalmente nomeado, mas suas situações de favorecimento enfatizam desmontar tecnologia, explorar ambientes e compreender conceitos desconhecidos.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Quero Entender Isso""".trimIndent(),
                    effect = """Uma vez por cena, depois de realizar um teste para:

• estudar;
• identificar;
• compreender;
• desmontar;
• interpretar;

algo que Eloah nunca tenha encontrado antes, receba:

+5 no teste.""".trimIndent(),
                    cost = "1 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 1,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por cena",
                    category = "Conhecimento / Investigação",
                    enhancements = """Mais Uma Pergunta

Em caso de sucesso, Eloah também pode descobrir uma informação secundária verdadeira diretamente relacionada ao objeto estudado.""".trimIndent(),
                ),
                PathPower(
                    name = """Desmontar Para Aprender""".trimIndent(),
                    effect = """Durante um período seguro, Eloah pode desmontar ou analisar profundamente um objeto, dispositivo, artefato ou mecanismo acessível.

Faça um teste apropriado.

Em caso de sucesso, escolha um benefício:

• produzir anotações que permitam qualquer um receber +2 em um teste diretamente relacionado;
• identificar uma peça, função ou princípio essencial do objeto.""".trimIndent(),
                    category = "Ofício / Pesquisa",
                    enhancements = """Caderno de Descobertas

Uma vez por sessão, um bônus obtido por esse poder é:

+5.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.apostador""".trimIndent(),
            motto = """São as possibilidades que me fazem continuar; não é a certeza, e muito menos a dúvida. Essa é uma espécie de aposta que faço comigo mesmo.""".trimIndent(),
            keywords = listOf("""Risco""".trimIndent(), """Análise""".trimIndent(), """Controle""".trimIndent()),
            pillars = listOf(
                """Propenso a Riscos — O risco não é apenas ameaça; é a possibilidade de encontrar algo que não existiria sem apostar.""".trimIndent(),
                """Comportamento — Vitória e derrota fazem parte do mesmo jogo. Nenhuma das duas deve dominar completamente suas decisões.""".trimIndent(),
                """Analítico — Serabatte não joga cegamente. Probabilidade, leitura do adversário e intuição são partes da aposta.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Cara ou Coroa""".trimIndent(),
                    effect = """Uma vez por cena, antes de realizar um teste próprio, jogue uma moeda.

Cara:

o teste é tratado como um Crítico Automático positivo.

Coroa:

o teste é tratado como uma Falha Crítica automática.

Você não rola o d20 para esse teste.

Este poder não pode receber Vantagem, pois o resultado já é substituído por um Crítico Automático ou Falha Crítica automática.""".trimIndent(),
                    cost = "3 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 3,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    destinyCostEligible = true,
                    limit = "Uma vez por cena",
                    category = "Destino / Risco",
                    enhancements = """Dobrar a Aposta

Depois de obter Coroa, você pode gastar 1 Destino para jogar novamente a moeda.

O segundo resultado é obrigatório.""".trimIndent(),
                ),
                PathPower(
                    name = """Rodada da Casa""".trimIndent(),
                    effect = """Uma vez por sessão, depois de realizar um teste, antes de sua resolução final, peça ao Mestre para rolar:

1d20 secreto.

Depois que ele informar apenas o resultado total alternativo, você escolhe entre:

• seu resultado original;
• o resultado da Casa.

Todos os modificadores normais do teste são aplicados à rolagem da Casa.""".trimIndent(),
                    limit = "Uma vez por sessão",
                    category = "Destino",
                    enhancements = """A Casa Sempre ganha

Você pode usar Rodada da Casa duas vezes por sessão, mas nunca mais de uma vez na mesma cena.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.pedra_rocha""".trimIndent(),
            motto = """Do sangue se cria a pedra; do suor, a rocha.""".trimIndent(),
            keywords = listOf("""Dinheiro""".trimIndent(), """Sossego""".trimIndent(), """Esforço""".trimIndent()),
            pillars = listOf(
                """Suor Nobre — Quem trabalha e se desgasta merece colher o valor de seu próprio esforço.""".trimIndent(),
                """Descanso Eterno — A paz e o espaço pessoal devem ser respeitados.""".trimIndent(),
                """Vontade de Leão — Um objetivo escolhido de verdade não deve ser abandonado facilmente.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Combate Bêbado""".trimIndent(),
                    effect = """Enquanto estiver sob efeito narrativo de álcool suficiente para estar Bêbado, você recebe:

Vantagem em ataques corpo a corpo desarmados.

Quando estiver sóbrio, o poder simplesmente não funciona; ele não aplica a antiga penalidade de −2.

Combate Bêbado não concede Crítico Automático e não pode ser combinado com outro efeito que transforme a mesma rolagem em Crítico Automático.""".trimIndent(),
                    category = "Combate",
                    enhancements = """Briga de Taverna

Uma vez por cena, ao acertar um ataque beneficiado por Combate Bêbado, você pode gastar:

2 PE

para causar:

+1d6 de dano de impacto.""".trimIndent(),
                ),
                PathPower(
                    name = """Impaciência Furiosa""".trimIndent(),
                    effect = """Uma vez por cena, para entrar em fúria até o final da cena.

Durante esse período:

• recebe Redução de Dano 1 contra Impacto e Perfuração;
• recebe +2 em testes baseados em FOR.

O bônus não aumenta dano diretamente.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    duration = "1 cena",
                    durationType = com.kinderman.sdo.domain.model.AbilityDuration.SCENE,
                    limit = "Uma vez por cena",
                    category = "Resistência / Esforço",
                    enhancements = """Cabeça Dura

Recebe um aumento de dano igual a sua brutalidade.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.podio""".trimIndent(),
            motto = """Entre todos aqueles que vivi e conheci, eu apenas serei o único que permanecerá de pé no final.""".trimIndent(),
            keywords = listOf("""Arrogância""".trimIndent(), """Assimilação""".trimIndent(), """Autopreservação""".trimIndent()),
            pillars = listOf(
                """Arrogante — Krux associa respeito a autonomia, força e capacidade de permanecer de pé diante dos outros.""".trimIndent(),
                """Comportamento — Confiança precisa ser demonstrada. Fraqueza exposta pode custar influência e controle.""".trimIndent(),
                """Assimilar — Toda oportunidade de aprimorar corpo, equipamento ou posição pode ser aproveitada, independentemente de quão agradável seja o processo.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Protético""".trimIndent(),
                    effect = """Você recebe:

+5 em testes diretamente relacionados à instalação, adaptação ou aceitação de implantes no próprio corpo.

Quando um implante ancestral exigiria um teste para rejeição ou instabilidade, repetir o teste.

O segundo resultado deve ser mantido.

O Poder não reduz permanentemente sua Energia por implante. Custos de manutenção pertencem às regras do próprio implante.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    category = "Tecnologia / Corpo",
                    enhancements = """Corpo Modular

Uma vez por descanso, durante manutenção apropriada, você pode receber +2 em um teste para reparar ou recalibrar um dos seus próprios implantes.""".trimIndent(),
                ),
                PathPower(
                    name = """Sempre de Pé""".trimIndent(),
                    effect = """Uma vez por cena, quando sofrer um efeito que tentaria:

• derrubá-lo;
• empurrá-lo;
• imobilizá-lo;
• fazê-lo largar um objeto;
• forçá-lo a recuar;

receber:

+4 no teste ou resistência apropriada.

Se o efeito não permitir teste, reduza pela metade a distância de movimento forçado, quando aplicável.""".trimIndent(),
                    cost = "1 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 1,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por cena",
                    category = "Autopreservação",
                    enhancements = """Terceiro Lugar

Uma vez por sessão, quando seria reduzido a 0 Vida, pode gastar 1 Destino para permanecer com 1 Vida.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.desmanche""".trimIndent(),
            motto = """O lixo de uns é o tesouro de outros.""".trimIndent(),
            keywords = listOf("""Inventar""".trimIndent(), """Construir""".trimIndent(), """Melhorar""".trimIndent()),
            pillars = listOf(
                """Mente Criativa — Onde outras pessoas enxergam lixo, Óleo enxerga peças, mecanismos e novas possibilidades.""".trimIndent(),
                """Dedicação ao Projeto — Um projeto promissor não deve ser abandonado apenas porque a primeira tentativa falhou.""".trimIndent(),
                """Ainda Não, Posso Melhorar — Erros são dados. Cada falha informa como a próxima versão pode funcionar melhor.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Mestre das Máquinas""".trimIndent(),
                    effect = """Uma vez por cena, ao realizar um teste de Reparo em:

• mecanitos;
• máquinas;
• implantes;
• construtos;
• dispositivos mecânicos;

receber:

+5 no teste.

Se o reparo recuperar Vida de uma entidade mecânica, em caso de sucesso ela recupera:

+2 Vida adicional.

Isso não remove Falhas Corporais ou Falhas de Órgão mecânicas automaticamente.""".trimIndent(),
                    cost = "1 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 1,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por cena",
                    category = "Reparo / Tecnologia",
                    enhancements = """Não Joga Fora Ainda

O bônus de recuperação aumenta de +2 para +4 Vida.""".trimIndent(),
                ),
                PathPower(
                    name = """Peça de Reposição""".trimIndent(),
                    effect = """Uma vez por cena, usando sucata ou material mecânico plausível, improvise uma peça temporária capaz de substituir um componente comum quebrado ou ausente.

A peça permite que o objeto volte a funcionar:

até o fim da cena.

Ela não reproduz componentes:

• únicos;
• mágicos;
• divinos;
• de tecnologia ancestral excepcional;
• cujo funcionamento dependa de material que Óleo não possua.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação completa",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por cena",
                    category = "Improviso / Ofício",
                    enhancements = """Quase Original

O reparo temporário dura até o próximo descanso.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.peao""".trimIndent(),
            motto = """Pela Ordem, por Exomathis.""".trimIndent(),
            keywords = listOf("""Servidão""".trimIndent(), """Doutrina""".trimIndent(), """Obediência""".trimIndent()),
            pillars = listOf(
                """Servidão — Eu sou apenas uma peça neste vasto plano. A vontade da Deusa está acima de qualquer pessoa ou coisa.

O indivíduo existe como parte de algo maior. O dever vem antes do desejo pessoal.""".trimIndent(),
                """Doutrina — Todos devem ser convertidos ao entendimento de que a máquina é o destino inevitável de toda vida.

A verdade de Exomathis não deve apenas ser conhecida, mas propagada.""".trimIndent(),
                """Obediência — A palavra da Máquina é lei. Devo tornar a carne em algo superior, sem questionamentos, pois este é o plano superior.

A transformação da carne é entendida como parte inevitável do propósito da Máquina.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Cavaleiro""".trimIndent(),
                    effect = """Você recebe acesso à Técnica de Batalha:

Lâminas Gêmeas

Ela conta como uma Técnica de Batalha adquirida normalmente e segue suas próprias regras.

Este Poder não concede bônus adicionais além do acesso à técnica.""".trimIndent(),
                    category = "Combate",
                ),
                PathPower(
                    name = """Bispo""".trimIndent(),
                    effect = """Você recebe:

+2 em testes diretamente relacionados a Exomathis.

O bônus pode se aplicar a assuntos como:

• doutrina;
• símbolos;
• ritos;
• história da ordem;
• identificação de práticas religiosas;
• interpretação dos dogmas de Exomathis.

Não se aplica automaticamente a qualquer ação realizada em nome da religião.""".trimIndent(),
                    category = "Doutrina",
                    enhancements = """Voz da Máquina

Uma vez por cena, transforme o bônus de +2 em:

+4 para um único teste.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.governante""".trimIndent(),
            motto = """Com a Coroa vem sua glória, mas também seu peso.""".trimIndent(),
            keywords = listOf("""Governar""".trimIndent(), """Liberdade""".trimIndent(), """Povo""".trimIndent()),
            pillars = listOf(
                """Rei — Serei um governante que ama seu povo, os fazendo felizes a qualquer custo.

Governar significa assumir responsabilidade pelo bem-estar daqueles sob sua proteção.""".trimIndent(),
                """Livre — Um governante não é escravo de seu povo e nem o contrário. Serei livre para tomar minhas decisões.

Autoridade sem autonomia não é liderança.""".trimIndent(),
                """Povo — Todos aqueles que são considerados o meu povo irei ajudar da melhor forma possível, com a menor perda possível.

O poder deve produzir segurança e estabilidade para aqueles que dependem dele.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Intuição de Governante""".trimIndent(),
                    effect = """Uma vez por cena, depois de realizar um teste de Intuição relacionado diretamente a:

• política;
• lealdade;
• hierarquia;
• disputa de poder;
• intenção de um governante;
• comportamento de uma facção;

receber:

+4 no resultado.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    limit = "Uma vez por cena",
                    category = "Social / Competência",
                    enhancements = """Leitura da Corte

Você pode usar o poder duas vezes por cena.""".trimIndent(),
                ),
                PathPower(
                    name = """Entrada Triunfal""".trimIndent(),
                    effect = """Uma vez por cena, ao entrar em um ambiente social onde sua presença possa ser percebida, fazer uma apresentação, anúncio, discurso ou demonstração pública.

Escolha uma criatura ou grupo que tenha presenciado a entrada.

Você recebe:

+4 no próximo teste social contra esse alvo ou grupo até o final da cena.

O benefício termina após ser utilizado.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    duration = "1 cena",
                    durationType = com.kinderman.sdo.domain.model.AbilityDuration.SCENE,
                    limit = "Uma vez por cena",
                    category = "Social",
                    enhancements = """Presença Real

O bônus pode ser aplicado aos dois primeiros testes sociais realizados contra o público afetado.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.guardia_segredos""".trimIndent(),
            motto = """O passado fala, e eu sou sua voz.""".trimIndent(),
            keywords = listOf("""Relíquias""".trimIndent(), """Sabedoria""".trimIndent(), """Proteção""".trimIndent()),
            pillars = listOf(
                """Guardiã das Relíquias — Relíquias dracônicas e outros artefatos de valor inestimável devem ser preservados e protegidos de quem possa destruí-los ou utilizá-los de maneira irresponsável.""".trimIndent(),
                """Voz dos Ancestrais — Desvendar os segredos das civilizações antigas permite recuperar tradições, acontecimentos e conhecimentos que o tempo tentou apagar.""".trimIndent(),
                """Sabedoria Oculta — Conhecimento é mais valioso quando compreendido. Saphire busca interpretar o passado para tomar decisões melhores no presente.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Escudo de Escamas""".trimIndent(),
                    effect = """Uma vez por cena, manifestar uma barreira de escamas dracônicas.

Por 1d4 turnos, você recebe:

+5 em Proteção Geral  
+5 em Proteção Arcana

A barreira acompanha seus movimentos.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    duration = "1d4 turnos",
                    durationType = com.kinderman.sdo.domain.model.AbilityDuration.TURNS,
                    limit = "Uma vez por cena",
                    category = "Sobrenatural / Defesa",
                    enhancements = """Escamas Ancestrais

Durante a duração do poder, uma vez, ao sofrer dano físico ou mágico, reduza esse dano em:

10 pontos.""".trimIndent(),
                ),
                PathPower(
                    name = """Visão das Cinzas""".trimIndent(),
                    effect = """Ao tocar um objeto antigo, ruína ou local historicamente relevante, receber um fragmento sensorial verdadeiro ligado ao passado daquele alvo.

O fragmento pode mostrar, por exemplo:

• uma pessoa que manipulou o objeto;
• um acontecimento marcante;
• uma emoção intensa;
• uma imagem;
• um som;
• um momento importante relacionado à relíquia.

O poder não fornece uma reconstrução completa do passado.

O mesmo objeto ou local só pode ser afetado:

1 vez por dia.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "Toque",
                    category = "Sobrenatural / Investigação",
                    enhancements = """Ecos Mais Nítidos

Ao usar Visão das Cinzas, você pode fazer uma pergunta objetiva sobre o fragmento percebido. A resposta deve ser verdadeira dentro das informações disponíveis naquele eco.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.dispariedade""".trimIndent(),
            motto = """Do caos, eu forjo minha melodia; com as sombras, eu crio minha luz.""".trimIndent(),
            keywords = listOf("""Imparcial""".trimIndent(), """Sagaz""".trimIndent(), """Resiliente""".trimIndent()),
            pillars = listOf(
                """Justo? Justo — Geralmente tento fazer o que é correto perante a sociedade, mas, se precisar quebrar alguns ovos para alcançar meus objetivos, não hesitarei.

Damien tenta fazer aquilo que considera correto, mas não trata moralidade como uma regra absoluta.""".trimIndent(),
                """Volume Máximo — A música é minha válvula de escape para as incertezas que me assombram. Um legado que não me deixa virar um monstro por completo.

Sua música é expressão, memória e controle emocional.""".trimIndent(),
                """Confiança — Não cresci da maneira tradicional. Sempre lutei para sobreviver e confiei somente em meu pai, que poderia simplesmente ter me abandonado como outros fizeram.

Confiança é rara e, por isso, possui grande valor.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Volume Máximo""".trimIndent(),
                    effect = """A música é minha válvula de escape para as incertezas que me assombram. Um legado que não me deixa virar um monstro por completo.

Sua música é expressão, memória e controle emocional.

Confiança

Não cresci da maneira tradicional. Sempre lutei para sobreviver e confiei somente em meu pai, que poderia simplesmente ter me abandonado como outros fizeram.

Confiança é rara e, por isso, possui grande valor.

Volume Máximo

Uma vez por cena, enquanto toca ou executa uma performance audível.

Escolha até 3 aliados a até 10 metros que consigam ouvir você.

Cada alvo recebe:

+2 no próximo teste realizado até o início do seu próximo turno.

O bônus não acumula com outro benefício de Ajuda para o mesmo teste.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "10 metros",
                    rangeType = com.kinderman.sdo.domain.model.AbilityRange.MEDIUM,
                    limit = "Uma vez por cena",
                    category = "Performance / Suporte",
                    enhancements = """Refrão

Os aliados podem utilizar o bônus até o final do seu próximo turno, em vez do início.""".trimIndent(),
                ),
                PathPower(
                    name = """Só Confio em Poucos""".trimIndent(),
                    effect = """Escolha uma criatura com quem você tenha estabelecido confiança real.

Uma vez por cena, quando realizar um teste diretamente para:

• proteger;
• alcançar;
• ajudar;
• resgatar;
• compreender;

essa criatura, receba:

+2 no teste.

Apenas uma criatura pode ser seu Vínculo por vez. A mudança deve ser consequência de desenvolvimento narrativo.""".trimIndent(),
                    limit = "Uma vez por cena",
                    category = "Vínculo / Sobrevivência",
                    enhancements = """Não Vou Perder Você

Uma vez por cena, em vez do bônus de +2, depois de falhar nesse teste você pode gastar:

1 PE

para rerrolar. O segundo resultado deve ser mantido.""".trimIndent(),
                ),
            ),
        ),
        PathPreset(
            catalogId = """path.sangue_carmesim""".trimIndent(),
            motto = """Todos são ligados pelo Sangue.""".trimIndent(),
            keywords = listOf("""Cuidar""".trimIndent(), """Nutrir""".trimIndent(), """Sangrar""".trimIndent()),
            pillars = listOf(
                """Cuidar — Cuide daqueles que são abençoados pelo sangue.

O sangue representa vida e vínculo. Preservar a vida é parte central da prática de Estienne.""".trimIndent(),
                """Nutrir — Nutra o corpo e evite enfraquecer a carne.

Curar não significa apenas fechar ferimentos, mas preservar a capacidade do corpo de continuar vivendo.""".trimIndent(),
                """Sangrar — Sangre se necessário para proteger o que é seu.

O próprio corpo é um recurso, mas sacrificá-lo só possui sentido quando protege outra vida.""".trimIndent(),
            ),
            powers = listOf(
                PathPower(
                    name = """Cicatrização Forçada""".trimIndent(),
                    effect = """Uma vez por turno, perca:

1d6 HP

Esse dano ignora Redução de Dano e não pode reduzir Estienne abaixo de 1 HP.

Escolha outra criatura a até 3 metros.

Ela recupera:

o dobro do HP perdido por Estienne.

Este poder:

• não remove Falhas Corporais;
• não remove Falhas de Órgão;
• não restaura membros;
• não pode ter o custo de HP reduzido ou prevenido.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    range = "3 metros",
                    rangeType = com.kinderman.sdo.domain.model.AbilityRange.SHORT,
                    limit = "Uma vez por turno",
                    category = "Sobrenatural / Cura",
                    enhancements = """Sangue Compartilhado

O alcance aumenta para:

10 metros""".trimIndent(),
                ),
                PathPower(
                    name = """Fio Coagulado""".trimIndent(),
                    effect = """Quando acertar um ataque corpo a corpo com uma arma que possa ser coberta pelo próprio sangue, formar um fio ou lâmina coagulado ao redor da arma.

Some Sangromancia no dano desse ataque.""".trimIndent(),
                    cost = "2 PE",
                    costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                    costValue = 2,
                    action = "1 ação",
                    executionType = com.kinderman.sdo.domain.model.AbilityExecution.ACTION,
                    category = "Sobrenatural / Combate",
                    enhancements = """Lâmina Rubra

Ao usar Fio Coagulado, escolha uma das opções:

• causar +2 de dano adicional; ou
• considerar o ataque mágico para superar resistências apropriadas.""".trimIndent(),
                ),
            ),
        ),
    )

    fun find(catalogId: String): PathPreset? = entries.firstOrNull { it.catalogId == catalogId }
}

fun Character.withPathPreset(entry: CatalogEntry): Character {
    val preset = PathPresets.find(entry.id) ?: return copy(pathName = entry.name)
    val retainedPowers = powers.filterNot { it.origin.startsWith("Caminho — ") }
    val pathPowers = preset.powers.map { power ->
        Power(name = power.name, origin = "Caminho — ${entry.name}", effect = power.effect)
    }
    return copy(
        pathName = entry.name,
        pathMotto = preset.motto,
        pathKeywords = preset.keywords,
        pathPillars = preset.pillars,
        powers = retainedPowers + pathPowers,
    )
}

