package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind

object BuiltInCatalog {
    const val VERSION = 2

    private fun canonicalReference(source: String): String = when (source) {
        "Catálogos canônicos de Caminhos" -> "03 - Regras/Caminhos"
        "50 Exemplos de Poderes Mágicos" -> "03 - Regras/Magia/50 Exemplos de Poderes Mágicos.md"
        "50 Exemplos de Poderes de Profissão e Conhecimento" -> "90 - Modelos/Exemplo de 50 Poderes.md"
        "50 Exemplos de Magias" -> "03 - Regras/Magia/50 Exemplos de Magias.md"
        "50 Exemplos de Cinzas" -> "03 - Regras/Magia/50 Exemplos de Cinzas.md"
        "50 Exemplos de Runas" -> "03 - Regras/Magia/50 Exemplos de Runas.md"
        else -> error("Fonte canônica não registrada: $source")
    }

    private fun rows(kind: CatalogKind, source: String, values: String): List<CatalogEntry> =
        values.trimIndent().lineSequence().filter(String::isNotBlank).map { row ->
            val p = row.split('|')
            CatalogEntry(
                id = "${kind.name.lowercase()}.${p[0]}", kind = kind, name = p[1], group = p[2],
                summary = p.getOrElse(3) { "" }, cost = p.getOrElse(4) { "" },
                action = p.getOrElse(5) { "" }, range = p.getOrElse(6) { "" },
                duration = p.getOrElse(7) { "" }, source = source, version = VERSION,
                ruleReference = canonicalReference(source),
            )
        }.toList()

    private val paths = rows(CatalogKind.PATH, "Catálogos canônicos de Caminhos", """
        engrenagens|Caminho das Engrenagens|Solidão dos Oprimidos|Reparar, criação e perseverança.
        personagem|Caminho do Personagem|Solidão dos Oprimidos|Caminho pessoal de Robert Julian Dobber.
        necrocamminus|Necrocamminus|Solidão dos Oprimidos|Morte, travessia e transformação.
        herdeiro_ruinas|Herdeiro das Ruínas|Solidão dos Oprimidos|Relíquias, legado e exploração.
        la_befana|Caminho da La Befana|Solidão dos Oprimidos|Culinária, cuidado e tradição.
        festival_eterno|Caminho do Festival Eterno|Silêncio dos Oráculos|Impressionar, motivar e alegrar.
        mao_justica|Caminho da Mão da Justiça|Silêncio dos Oráculos|Lei, proteção e julgamento.
        redencao|Caminho da Redenção|Silêncio dos Oráculos|Culpa, reparação e esperança.
        conhecimento_juvenil|Caminho do Conhecimento Juvenil|Silêncio dos Oráculos|Curiosidade, estudo e descoberta.
        apostador|Caminho do Apostador|Silêncio dos Oráculos|Risco, leitura e oportunidade.
        pedra_rocha|Caminho da Pedra e da Rocha|Silêncio dos Oráculos|Resistência, construção e impacto.
        podio|Caminho do Pódio|Silêncio dos Oráculos|Competição, vitória e reconhecimento.
        desmanche|Caminho do Desmanche|Silêncio dos Oráculos|Sucata, desmontagem e sobrevivência.
        peao|Caminho do Peão|Sons do Orgulho|Servidão, doutrina e obediência.
        governante|Caminho do Governante|Sons do Orgulho|Autoridade, estratégia e responsabilidade.
        guardia_segredos|Guardiã dos Segredos|Sons do Orgulho|Sigilo, memória e proteção.
        dispariedade|Caminho da Dispariedade|Sons do Orgulho|Contraste, ruptura e identidade.
        sangue_carmesim|Caminho do Sangue Carmesim|Sons do Orgulho|Sangue, sacrifício e legado.
    """)

    private val powers = rows(CatalogKind.POWER, "50 Exemplos de Poderes Mágicos", """
        canal_economico|Canal Econômico|Conjuração|Uma vez por cena, reduza em 1 PM uma Magia da Escola escolhida.
        conjuracao_cautelosa|Conjuração Cautelosa|Conjuração|Ação Completa concede +4 na conjuração.
        reserva_oculta|Reserva Oculta|Recurso|Aumenta o Arcano máximo em 2 enquanto a fonte estiver ativa.
        mente_ancorada|Mente Ancorada|Concentração|Receba +2 para manter concentração.
        canal_doloroso|Canal Doloroso|Recurso|Troque 2 PV por 1 PM, até POD por cena.
        escola_favorita|Escola Favorita|Conhecimento|Escolha uma Escola e receba +1 nos ataques dela.
        forma_familiar|Forma Familiar|Conhecimento|Escolha uma Magia e receba +2 para conjurá-la.
        alcance_estendido|Alcance Estendido|Metamancia|Gaste +1 PM para dobrar alcance sem ampliar área.
        area_esculpida|Área Esculpida|Metamancia|Exclua até POD aliados de uma área própria.
        eco_arcano|Eco Arcano|Conjuração|Gaste +2 PM para repetir efeito reduzido em outro alvo.
        cinzeiro_nato|Cinzeiro Nato|Cinzas|Receba +4 para coletar Cinzas da origem escolhida.
        refinador|Refinador|Cinzas|Refinamento leva metade do tempo.
        paladar_cinzas|Paladar de Cinzas|Cinzas|Identifique origem e pureza por exame breve.
        mao_estavel|Mão Estável|Runas|Receba +2 ao inscrever Runas sob condições adequadas.
        ativador_leigo|Ativador Leigo|Runas|Ativar Runa custa −1 PM, mínimo 1.
        sigilo_pessoal|Sigilo Pessoal|Runas|Suas Runas reconhecem você sem componente adicional.
        inscricao_rapida|Inscrição Rápida|Runas|Uma vez por sessão, inscreva Runa 1 em Ação Completa.
        runa_duravel|Runa Durável|Runas|Uma Runa 1 recebe uma carga adicional.
        desarmador_runico|Desarmador Rúnico|Runas|Receba +4 para desativar Runa identificada.
        defeito_intencional|Defeito Intencional|Runas|Inclua resposta secreta contra adulteração.
        sangue_chave|Sangue é Chave|Sangromancia|Use 1 PV no lugar de 1 PM uma vez por turno.
        hemostasia|Hemostasia Instintiva|Sangromancia|Ignore sangramento gerado por custos de Sangromancia.
        memoria_ancestral|Memória Ancestral|Arqueomancia|Receba +4 para reconhecer tradição arcana antiga.
        emprestimo_reliquia|Empréstimo da Relíquia|Arqueomancia|Reproduza função menor de relíquia estudada uma vez por sessão.
        passo_leve|Passo Leve|Aeromancia|Ignore os primeiros 3 m de queda e vento difícil.
        pulmao_arcano|Pulmão Arcano|Aeromancia|Prenda a respiração por uma cena e ignore fumaça comum.
        corpo_frio|Corpo Frio|Criomancia|Receba +2 contra efeitos de gelo e frio.
        combustao_controlada|Combustão Controlada|Piromancia|Exclua um objeto inflamável de incêndio criado por você.
        raiz_companheira|Raiz Companheira|Natureza|Uma planta vinculada alerta sobre perigo evidente.
        voz_mortos|Voz dos Mortos|Necromancia|Cadáveres reconhecem sua pergunta como não hostil.
        curador_campo|Curador de Campo|Cura|Cure +2 PV em alvo com metade da Vida ou menos.
        cura_sem_cicatriz|Cura sem Cicatriz|Cura|Receba +2 em estabilização e evite marcas indesejadas.
        sonhador_lucido|Sonhador Lúcido|Oniromancia|Receba +4 para reconhecer sonho ou intrusão onírica.
        pesadelo_domado|Pesadelo Domado|Oniromancia|Transforme instabilidade onírica em imagem inofensiva uma vez por sessão.
        sombra_habitual|Sombra Habitual|Umbramancia|Receba +2 Furtividade após usar magia de sombra.
        farol_interior|Farol Interior|Luminomancia|Emita luz sem custo enquanto consciente.
        segundo_percebido|Segundo Percebido|Cronomancia|Receba +2 em Iniciativa.
        peso_familiar|Peso Familiar|Gravimancia|Ignore penalidade de gravidade alterada comum.
        geometria_intima|Geometria Íntima|Espaciomancia|Receba +4 para perceber portais e distorções.
        ouvido_harmonico|Ouvido Harmônico|Sonoromancia|Receba +4 para localizar som ou falha por vibração.
        afinidade_metalica|Afinidade Metálica|Ferromancia|Sinta a direção de objeto metálico vinculado em 30 m.
        corpo_adaptavel|Corpo Adaptável|Biomancia|Uma vez por cena, +4 em Tolerância contra ambiente.
        reagente_substituto|Reagente Substituto|Alquimancia|Substitua componente comum por equivalente plausível uma vez por sessão.
        interface_intuitiva|Interface Intuitiva|Tecnomancia|Receba +4 para operar dispositivo arcano desconhecido.
        navegante_astral|Navegante Astral|Astromancia|Nunca se perca sob céu visível ou mapa adequado.
        contrafeitico_reflexo|Contrafeitiço Reflexo|Abjuramancia|Reação e 3 PM concedem +4 PA contra efeito percebido.
        pacto_menor|Pacto Menor|Narrativa|Entidade concede +2 em situação estreita e cobra condição explícita.
        grimorio_vinculado|Grimório Vinculado|Item|Acesse uma Magia registrada enquanto portar o livro.
        foco_cristal|Foco de Cristal|Item|Uma vez por cena, +2 em ataque da Escola gravada.
        cicatriz_onirica|Cicatriz Onírica|Histórico|Receba +2 PA contra Manifestos Oníricos.
    """)

    private val professionPowers = rows(CatalogKind.POWER, "50 Exemplos de Poderes de Profissão e Conhecimento", """
        prof_olho_ferreiro|Olho de Ferreiro|Profissão: Ferreiro / Ofício|Identifica defeitos, reparos e qualidade; uma vez por cena, +4 para avaliar, reparar ou modificar metal.
        prof_reforco_emergencia|Reforço de Emergência|Profissão: Ferreiro ou Armeiro / Ofício|2 PE e Ação Completa tornam um equipamento danificado funcional até o fim da cena.
        prof_ferramenta_certa|Ferramenta Certa|Profissão: Artesão ou Mecânico / Ofício|Uma vez por cena, declare possuir uma ferramenta pequena e comum plausível para o ofício.
        prof_mao_carpinteiro|Mão de Carpinteiro|Profissão: Carpinteiro / Ofício|+2 para construir, reparar, desmontar ou avaliar madeira; identifica um ponto estrutural relevante por cena.
        prof_pedra_sobre_pedra|Pedra Sobre Pedra|Profissão: Pedreiro ou Construtor / Ofício|+2 para avaliar estruturas de pedra; identifica rachadura, passagem ou risco estrutural por cena.
        prof_costura_campo|Costura de Campo|Profissão: Alfaiate ou Costureiro / Ofício|1 PE e uma ação reparam tecido ou couro leve para uso normal durante a cena.
        prof_maos_joalheiro|Mãos de Joalheiro|Profissão: Joalheiro ou Lapidador / Ofício|+4 para avaliar gemas, metais preciosos e trabalhos delicados; reconhece falsificação grosseira.
        prof_tranca_familiar|Tranca Familiar|Profissão: Chaveiro ou Serralheiro / Ofício|1 PE concede +4 ao trabalhar em fechadura, algema ou trava mecânica.
        prof_no_marinheiro|Nó de Marinheiro|Profissão: Marinheiro ou Cordoeiro / Ofício|+4 para amarrar, prender, içar ou improvisar estruturas com cordas.
        prof_remendo_sapateiro|Remendo de Sapateiro|Profissão: Sapateiro ou Coureiro / Ofício|Durante descanso, repara desgaste comum em couro; por cena, +2 Movimento quando o equipamento resolver o problema.
        prof_memoria_arquivista|Memória de Arquivista|Profissão: Arquivista ou Bibliotecário / Conhecimento|Uma vez por cena, +4 para recordar informação plausível de registros, livros ou documentos.
        prof_leitura_dinamica|Leitura Dinâmica|Profissão: Escriba ou Estudioso / Conhecimento|Com uma ação, identifica tema, estrutura, nomes e trechos relevantes de um texto extenso.
        prof_citacao_precisa|Citação Precisa|Profissão: Pesquisador ou Professor / Conhecimento|1 PE concede +2 em Conhecimento treinado; com referência imediata, o bônus é +4.
        prof_didatica|Didática|Profissão: Professor ou Tutor / Conhecimento|Ao Ajudar em Conhecimento ou ofício dominado, concede +4 em vez de +2.
        prof_codigo_legado|Código Legado|Profissão: Programador / Tecnologia|+2 para compreender código alheio; 1 PE ignora penalidade simples de legado ou incompatibilidade.
        prof_depuracao|Depuração|Profissão: Programador ou Engenheiro de Sistemas / Tecnologia|Após falhar ao programar, reparar ou configurar, 2 PE permitem repetir o teste.
        prof_automacao_improvisada|Automação Improvisada|Profissão: Programador ou Tecnomago / Tecnologia|2 PE e Ação Completa automatizam tarefa repetitiva simples até o fim da cena.
        prof_criptografo|Criptógrafo|Profissão: Escriba, Espião ou Programador / Conhecimento|+4 para reconhecer, criar ou quebrar cifras simples; identifica códigos amadores com tempo.
        prof_cartografia_mental|Cartografia Mental|Profissão: Cartógrafo / Conhecimento|Reconstrói de memória áreas percorridas e recebe +4 para não se perder onde já explorou.
        prof_contabilidade|Contabilidade|Profissão: Contador, Administrador ou Mercador / Conhecimento|+4 para encontrar inconsistências em registros financeiros, inventários, salários e impostos.
        prof_comida_verdade|Comida de Verdade|Profissão: Cozinheiro / Conhecimento|No descanso, prepara refeição para até 5 criaturas; cada uma remove +1 Exaustão na próxima recuperação.
        prof_aproveitar_tudo|Aproveitar Tudo|Profissão: Cozinheiro ou Açougueiro / Conhecimento|Recursos para três refeições alimentam quatro sem reduzir a qualidade.
        prof_paladar_treinado|Paladar Treinado|Profissão: Cozinheiro ou Provador / Conhecimento|+4 para identificar ingredientes, deterioração e alterações; detecta veneno evidente sem teste.
        prof_primeiros_socorros|Primeiros Socorros|Profissão: Curandeiro ou Médico de Campo / Conhecimento|Uma vez por cena, 1 PE concede +4 ao estabilizar uma criatura ferida.
        prof_diagnostico|Diagnóstico|Profissão: Médico ou Curandeiro / Conhecimento|Exame identifica sinais evidentes; uma vez por cena, +4 para diagnosticar condição específica.
        prof_dose_correta|Dose Correta|Profissão: Boticário ou Apotecário / Conhecimento|1 PE reduz em 2 penalidade de dosagem, aplicação ou condições improvisadas.
        prof_anfitriao|Anfitrião Experiente|Profissão: Estalajadeiro ou Hospedeiro / Social|+2 para acalmar conflitos e lidar com hóspedes; concede +2 ao próximo teste social de aliado.
        prof_ouvido_taverna|Ouvido de Taverna|Profissão: Taverneiro ou Garçom / Social|Uma vez por sessão, após circular em local movimentado, obtém um rumor local plausível.
        prof_cervejeiro|Cervejeiro|Profissão: Cervejeiro ou Vinicultor / Ofício|+4 para produzir, avaliar ou identificar fermentados; percebe adulteração evidente.
        prof_acougueiro|Açougueiro|Profissão: Açougueiro / Conhecimento|+2 para identificar cortes, ossos, musculatura e anatomia prática conhecida.
        prof_previsao_tempo|Previsão do Tempo|Profissão: Fazendeiro, Marinheiro ou Pastor / Conhecimento|Após observar o ambiente, pergunta uma vez por dia a tendência natural do clima nas próximas horas.
        prof_mao_fazendeiro|Mão de Fazendeiro|Profissão: Agricultor / Conhecimento|+4 para reconhecer plantações, solo, pragas comuns e ferramentas agrícolas.
        prof_tratador_animais|Tratador de Animais|Profissão: Pastor ou Cavalariço / Conhecimento|+4 para acalmar, conduzir ou cuidar de animais domésticos ou treinados.
        prof_cavaleiro_estrada|Cavaleiro de Estrada|Profissão: Cavalariço ou Mensageiro / Movimento|+2 para controlar montaria; 1 PE ignora penalidade pequena de terreno em manobra montada.
        prof_pescador|Pescador|Profissão: Pescador / Conhecimento|+4 para pescar, usar redes, reconhecer águas produtivas ou lidar com barco pequeno.
        prof_rastreador|Rastreador|Profissão: Caçador ou Batedor / Conhecimento|Uma vez por cena, +4 ao examinar rastros, pegadas ou sinais de passagem.
        prof_cacador_paciente|Caçador Paciente|Profissão: Caçador / Combate|Após observar oculto ou imóvel por um turno, 2 PE concedem +2 no ataque contra o alvo.
        prof_lenhador|Lenhador|Profissão: Lenhador / Conhecimento|+4 para derrubar árvores, usar machado como ferramenta ou cruzar vegetação densa.
        prof_minerador|Minerador|Profissão: Minerador / Conhecimento|+4 para reconhecer minérios, estabilidade, escavação e perigos de minas.
        prof_guia_caravana|Guia de Caravana|Profissão: Guia ou Caravanista / Conhecimento|+2 para escolher rotas e organizar marcha; permite repetir um teste coletivo de viagem por dia.
        prof_guarda_atento|Guarda Atento|Profissão: Guarda ou Vigia / Conhecimento|Enquanto de guarda, +4 em Sentidos contra aproximações, invasões e comportamento suspeito.
        prof_postura_brigao|Postura de Brigão|Profissão: Lutador, Segurança ou Taverneiro / Combate|Uma vez por turno, 1 PE concede +2 em um ataque desarmado.
        prof_aguentar_tranco|Aguentar o Tranco|Profissão: Carregador, Mineiro ou Trabalhador Braçal / Conhecimento|Uma vez por cena, 2 PE permitem rerrolar um teste falho de esforço físico.
        prof_trabalho_equipe|Trabalho em Equipe|Profissão: Soldado, Operário ou Marinheiro / Conhecimento|Em tarefa física cooperativa, sua ação Ajuda concede +4 em vez de +2.
        prof_porteiro|Porteiro|Profissão: Segurança ou Guarda de Portão / Conhecimento|+4 para perceber contradições em justificativas de entrada, documentos ou histórias improvisadas.
        prof_mao_pesada|Mão Pesada|Profissão: Ferreiro, Lenhador ou Trabalhador Braçal / Combate|Uma vez por turno, ao usar ferramenta pesada como arma improvisada, 2 PE causam +1d6 de dano físico em um acerto.
        prof_pechincha|Pechincha|Profissão: Mercador / Social|Uma vez por cena, 1 PE concede +4 ao negociar preço, pagamento ou troca.
        prof_avaliacao_mercado|Avaliação de Mercado|Profissão: Mercador ou Leiloeiro / Conhecimento|Estima faixa de valor e recebe +4 para reconhecer falsificação, abuso de preço ou qualidade incomum.
        prof_repertorio_bardo|Repertório de Bardo|Profissão: Músico, Bardo ou Artista / Social|Após uma ação de performance, concede +2 ao próximo teste social de um aliado contra quem assistiu.
        prof_conversa_barbeiro|Conversa de Barbeiro|Profissão: Barbeiro, Cabeleireiro ou Prestador de Serviços / Social|Uma vez por sessão durante o serviço, obtém de uma pessoa uma informação cotidiana, rumor ou relação social que ela revelaria casualmente.
    """)

    private val magic = rows(CatalogKind.MAGIC, "50 Exemplos de Magias", """
        faisca_condutora|Faísca Condutora|Electromancia 1|1d6 + POD elétrico; desregula mecanismo simples.|2 PM|1 ação|3 m|instantânea
        impulso_acelerado|Impulso Acelerado|Electromancia 2|Movimento +5 m e +2 na próxima Iniciativa ou Reflexos.|3 PM|1 ação|pessoal|1 cena
        punho_pedra|Punho de Pedra|Litomancia 1|Próximo ataque desarmado causa +1d6 físico.|1 PM|1 ação|pessoal|1 ataque
        muro_improvisado|Muro Improvisado|Litomancia 2|Ergue cobertura alta de 15 PV.|4 PM|1 ação|8 m|1 cena
        vinha_prendedora|Vinha Prendedora|Natureza 1|Imobiliza; escapar exige FOR + Atletismo CD 12.|2 PM|1 ação|8 m|até escapar
        sussurro_fauna|Sussurro da Fauna|Natureza 2|Animais pequenos observam, distraem ou carregam objeto.|3 PM|1 ação|20 m|1 cena
        chama_palma|Chama na Palma|Piromancia 1|Luz, ignição ou 1d6 + POD ao toque.|1 PM|1 ação|toque|1 cena
        rajada_brasas|Rajada de Brasas|Piromancia 2|Cone causa 2d6 + POD de fogo.|4 PM|1 ação|cone 5 m|instantânea
        jato_cortante|Jato Cortante|Aqueomancia 1|1d6 + POD físico e empurra 1 m.|2 PM|1 ação|8 m|instantânea
        nevoa_cega|Névoa Cega|Aqueomancia 2|Área bloqueia visão e impõe Cego.|3 PM|1 ação|área 3 m a 10 m|1 rodada
        toque_drenante|Toque Drenante|Necromancia 1|1d6 + POD; recupera metade, máximo POD.|2 PM|1 ação|toque|instantânea
        sussurro_ossos|Sussurro dos Ossos|Necromancia 2|Cadáver revela memória curta da morte.|3 PM|1 ação|3 m|instantânea
        toque_restaurador|Toque Restaurador|Cura 1|Recupera 1d6 + POD PV; não remove Falhas.|2 PM|1 ação|toque|instantânea
        purificacao|Purificação|Cura 2|Remove veneno fraco, sangramento ou condição leve.|3 PM|1 ação|toque|instantânea
        sussurro_alheio|Sussurro Alheio|Oniromancia 1|Planta pensamento curto contra Proteção Mental.|2 PM|1 ação|8 m|instantânea
        veu_mental|Véu de Névoa Mental|Oniromancia 2|Ilusão sensorial simples em pequena área.|4 PM|1 ação|10 m|1 cena
        fio_rubro|Fio Rubro|Sangromancia 1|Rastreia por sangue durante uma hora.|1 PM + 1 PV|1 ação|20 m|1 hora
        coagulo_imperativo|Coágulo Imperativo|Sangromancia 2|2d6 + POD e reduz Movimento em 5 m.|3 PM + 2 PV|1 ação|8 m|1 rodada
        leitura_vestigio|Leitura de Vestígio|Arqueomancia 1|Revela idade, tradição e uso arcano de inscrição.|2 PM|1 ação|toque|instantânea
        eco_reliquia|Eco da Relíquia|Arqueomancia 2|Reproduz função menor de relíquia estudada.|4 PM|1 ação|toque|1 rodada
        sopro_vetorial|Sopro Vetorial|Aeromancia 1|Empurra 2 m e apaga chama comum.|1 PM|1 ação|linha 6 m|instantânea
        passo_vendaval|Passo de Vendaval|Aeromancia 2|Desloca 10 m e ignora terreno difícil.|3 PM|1 ação|pessoal|instantânea
        sentir_fratura|Sentir Fratura|Geomancia 1|Detecta cavidades, solo instável e vibrações.|1 PM|1 ação|raio 10 m|instantânea
        dente_cristal|Dente de Cristal|Geomancia 2|2d6 + POD físico e terreno difícil.|4 PM|1 ação|10 m|1 cena
        geada_subita|Geada Súbita|Criomancia 1|1d6 + POD e −2 m Movimento.|2 PM|1 ação|6 m|1 rodada
        casulo_gelo|Casulo de Gelo|Criomancia 2|Imobiliza; casulo tem 12 PV.|4 PM|1 ação|8 m|1 rodada
        manto_penumbra|Manto de Penumbra|Umbramancia 1|+4 Furtividade em sombra.|2 PM|1 ação|pessoal|1 cena
        passagem_silenciosa|Passagem Silenciosa|Umbramancia 2|Abafa sons do grupo até alguém atacar.|3 PM|1 ação|área 3 m|1 cena
        clarao_revelador|Clarão Revelador|Luminomancia 1|Revela ocultação e pode impor Cego.|1 PM|1 ação|cone 6 m|1 rodada
        lanca_prismatica|Lança Prismática|Luminomancia 2|2d6 + POD luminoso; ignora sombra.|4 PM|1 ação|15 m|instantânea
        instante_alongado|Instante Alongado|Cronomancia 1|+2 em reação, Reflexos ou tarefa imediata.|2 PM|1 ação|pessoal|1 teste
        atraso_causal|Atraso Causal|Cronomancia 2|Atrasa efeito visível até fim do próximo turno.|4 PM|1 ação|8 m|1 rodada
        ancora_peso|Âncora de Peso|Gravimancia 1|Impede empurrão e reduz Movimento em 3 m.|2 PM|1 ação|8 m|1 rodada
        queda_lateral|Queda Lateral|Gravimancia 2|Move 4 m; colisão causa 2d6.|4 PM|1 ação|linha 8 m|instantânea
        mao_distante|Mão Distante|Espaciomancia 1|Manipula objeto de até 2 kg a distância.|1 PM|1 ação|10 m|concentração
        salto_fenda|Salto de Fenda|Espaciomancia 2|Teleporta para ponto visível.|4 PM|1 ação|12 m|instantânea
        estalo_ressonante|Estalo Ressonante|Sonoromancia 1|1d6 + POD sônico; dobro em objeto frágil.|2 PM|1 ação|8 m|instantânea
        camara_eco|Câmara de Eco|Sonoromancia 2|Sons não entram nem saem da área.|3 PM|1 ação|área 4 m|1 cena
        desvio_magnetico|Desvio Magnético|Ferromancia 1|+2 PG contra um ataque metálico.|2 PM|reação|pessoal|1 ataque
        danca_laminas|Dança das Lâminas|Ferromancia 2|Até três alvos sofrem 2d6 + POD.|4 PM|1 ação|raio 6 m|instantânea
        olhos_adaptados|Olhos Adaptados|Biomancia 1|Concede visão no escuro ou aquática.|1 PM|1 ação|toque|1 cena
        pele_reativa|Pele Reativa|Biomancia 2|Reduz em 2 o primeiro dano físico da rodada.|4 PM|1 ação|toque|1 cena
        separar_mistura|Separar Mistura|Alquimancia 1|Separa componentes comuns de recipiente pequeno.|2 PM|1 ação|toque|instantânea
        troca_propriedade|Troca de Propriedade|Alquimancia 2|Objeto fica flexível, rígido, aderente ou quebradiço.|4 PM|1 ação|toque|1 cena
        diagnostico_fantasma|Diagnóstico Fantasma|Tecnomancia 1|Identifica falha e atividade recente de dispositivo.|1 PM|1 ação|toque|instantânea
        comando_intruso|Comando Intruso|Tecnomancia 2|Força máquina simples a executar função válida.|4 PM|1 ação|8 m|instantânea
        norte_celeste|Norte Celeste|Astromancia 1|Revela direção, hora e rota sob referência astral.|1 PM|1 ação|pessoal|instantânea
        mau_pressagio|Mau Presságio|Astromancia 2|Alvo sofre −2 no próximo teste.|4 PM|1 ação|12 m|1 teste
        pele_selo|Pele de Selo|Abjuramancia 1|Concede +2 Proteção Arcana.|2 PM|1 ação|toque|1 rodada
        ruptura_encanto|Ruptura de Encanto|Abjuramancia 2|Teste contra 12 + nível encerra efeito até nível 2.|4 PM|1 ação|10 m|instantânea
    """)

    private val ashes = rows(CatalogKind.ASH, "50 Exemplos de Cinzas", """
        brasa_rubra|Brasa Rubra|Fogo / Refinada|2d6 de fogo a 6 m.
        sopro_forja|Sopro de Forja|Fogo / Bruta|Aquece metal pequeno por uma cena.
        sol_engarrafado|Sol Engarrafado|Luz / Pura|Ilumina 15 m e revela sombras mágicas.
        estopim_cinzento|Estopim Cinzento|Fogo / Refinada|Acende material inflamável tocado.
        coracao_caldeira|Coração de Caldeira|Calor / Pura|Resistência a frio por uma cena.
        geada_vidro|Geada de Vidro|Gelo / Refinada|2d6 frio e −2 m Movimento.
        sal_preservacao|Sal de Preservação|Gelo / Bruta|Conserva alimento ou cadáver por um dia.
        nevoa_invernal|Névoa Invernal|Gelo / Refinada|Névoa fria em raio de 3 m.
        prisao_cristalina|Prisão Cristalina|Gelo / Pura|Imobiliza por uma rodada; gelo 15 PV.
        passo_sem_pegadas|Passo sem Pegadas|Gelo / Refinada|Congela e apaga rastros por 10 m.
        po_restaurador|Pó Restaurador|Cura / Refinada|Recupera 2d6 PV.
        selo_coagulante|Selo Coagulante|Cura / Bruta|Interrompe sangramento comum.
        folego_dourado|Fôlego Dourado|Cura / Pura|Estabiliza e recupera 3d6 PV.
        lavagem_purificante|Lavagem Purificante|Cura / Refinada|Remove veneno fraco ou condição leve.
        vigilia_ambar|Vigília Âmbar|Cura / Refinada|Ignora 1 Exaustão como penalidade por uma cena.
        veu_translucido|Véu Translúcido|Ilusão / Refinada|Imagem estática de até 2 m.
        eco_emprestado|Eco Emprestado|Mental / Bruta|Repete perfeitamente um som curto.
        poeira_esquecimento|Poeira do Esquecimento|Mental / Pura|Apaga lembrança dos últimos segundos.
        olho_mercurio|Olho de Mercúrio|Percepção / Refinada|+4 para notar ilusão ou detalhe oculto.
        perfume_confianca|Perfume de Confiança|Mental / Refinada|−2 à resistência em conversa não hostil.
        fuligem_sem_luz|Fuligem sem Luz|Sombra / Refinada|Escurece raio de 3 m.
        manto_violeta|Manto Violeta|Sombra / Refinada|+4 Furtividade em penumbra.
        veneno_sombra|Veneno da Sombra|Trevas / Pura|3d6 e Abalado por uma rodada.
        passagem_muda|Passagem Muda|Sombra / Bruta|Abafa passos por uma rodada.
        marca_ausente|Marca do Ausente|Sombra / Pura|Oculta objeto pequeno por uma hora.
        prata_sonho|Prata de Sonho|Onírica / Pura|Visão breve e simbólica do Plano Onírico.
        ancora_desperto|Âncora do Desperto|Onírica / Refinada|+4 contra efeito onírico.
        sono_alheio|Sono Alheio|Onírica / Pura|Alvo dorme por uma rodada ou até sofrer dano.
        memoria_condensada|Memória Condensada|Onírica / Refinada|Guarda lembrança sensorial curta.
        passo_nevoa|Passo de Névoa|Onírica / Pura|Teleporta até 6 m.
        ferrugem_faminta|Ferrugem Faminta|Decadência / Refinada|2d6 a metal ou −1 Durabilidade.
        po_ossario|Pó de Ossário|Morte / Bruta|Aponta para cadáver ou energia necrótica.
        frio_sepulcral|Frio Sepulcral|Morte / Refinada|2d6 necrótico a 6 m.
        voz_postuma|Voz Póstuma|Espírito / Pura|Cadáver responde uma pergunta fragmentária.
        repouso_selado|Repouso Selado|Morte / Refinada|Impede reanimação por um dia.
        areia_ascendente|Areia Ascendente|Ar / Bruta|Rajada empurra objetos e revela formas.
        pulmao_tempestade|Pulmão de Tempestade|Ar / Refinada|Ignora fumaça comum por uma cena.
        trovao_seco|Trovão Seco|Som / Refinada|Cone causa 2d6 sônico.
        queda_pluma|Queda de Pluma|Ar / Refinada|Anula queda de até 15 m.
        frasco_vendaval|Frasco de Vendaval|Ar / Pura|Linha empurra alvos 4 m.
        limalha_viva|Limalha Viva|Metal / Refinada|Forma ferramenta metálica simples.
        peso_montanha|Peso de Montanha|Pedra / Pura|Imobiliza objeto ou criatura.
        pele_cascalho|Pele de Cascalho|Pedra / Refinada|Reduz próximo dano físico em 2d6.
        cristal_eco|Cristal de Eco|Pedra / Bruta|Registra e repete frase curta.
        semente_instantanea|Semente Instantânea|Natureza / Refinada|Faz planta comum crescer rapidamente.
        esporos_alerta|Esporos de Alerta|Natureza / Bruta|Mudam de cor diante de aproximação.
        seiva_predatoria|Seiva Predatória|Natureza / Pura|Vinhas Imobilizam em área de 3 m.
        sangue_desperto|Sangue Desperto|Sangue / Refinada|Identifica parentesco direto.
        rastro_carmesim|Rastro Carmesim|Sangue / Pura|Indica direção do dono da amostra.
        circuito_fantasma|Circuito Fantasma|Tecnologia / Pura|Alimenta dispositivo simples por uma cena.
    """)

    private val runes = rows(CatalogKind.RUNE, "50 Exemplos de Runas", """
        aquecimento|Aquecimento|Nível 1 / Fogo|Recipiente aquece líquido frio.
        centelha|Centelha|Nível 1 / Fogo|Ferramenta produz chama por comando.
        alarme_termico|Alarme Térmico|Nível 1 / Fogo|Porta emite clarão quando cruzada.
        lamina_morna|Lâmina Morna|Nível 1 / Fogo|Próximo acerto causa +1d6 fogo.
        selo_cauterizante|Selo Cauterizante|Nível 1 / Cura|Bandagem interrompe sangramento.
        dissipacao_impacto|Dissipação de Impacto|Nível 2 / Pedra|Parede reduz dano em 2d6.
        solo_firme|Solo Firme|Nível 1 / Pedra|Bota ignora terreno difícil uma rodada.
        tranca_mineral|Tranca Mineral|Nível 2 / Pedra|Funde porta ao batente, CD 17.
        pilar_emergencia|Pilar de Emergência|Nível 2 / Pedra|Sustenta estrutura por uma cena.
        memoria_rocha|Memória da Rocha|Nível 3 / Pedra|Mostra vibração marcante do último dia.
        frescor|Frescor|Nível 1 / Gelo|Conserva recipiente por um dia.
        passo_congelante|Passo Congelante|Nível 1 / Gelo|Congela 1 m sob a sola.
        ferrolho_gelo|Ferrolho de Gelo|Nível 2 / Gelo|Cria camada de gelo de 12 PV.
        armadilha_geada|Armadilha de Geada|Nível 2 / Gelo|2d6 + POD e reduz Movimento.
        camara_invernal|Câmara Invernal|Nível 3 / Gelo|Mantém sala fria enquanto selada.
        curativo_reativo|Curativo Reativo|Nível 1 / Cura|Ao sofrer dano, recupera 1d6 + POD.
        marca_pureza|Marca de Pureza|Nível 1 / Cura|Frasco muda de cor diante de toxina.
        leito_restaurador|Leito Restaurador|Nível 2 / Cura|Descanso recupera +2d6 PV.
        selo_estabilizacao|Selo de Estabilização|Nível 2 / Cura|Estabiliza portador ao chegar a 0 PV.
        santuario_regenerativo|Santuário Regenerativo|Nível 3 / Cura|Aliados em 3 m recuperam 3d6 + POD.
        passos_silenciosos|Passos Silenciosos|Nível 1 / Sombra|Botas abafam passos até atacar.
        fechadura_cega|Fechadura Cega|Nível 1 / Ilusão|Oculta visualmente uma fechadura.
        manto_parede|Manto da Parede|Nível 2 / Ilusão|Capa concede +4 Furtividade imóvel.
        corredor_falso|Corredor Falso|Nível 2 / Ilusão|Intruso percebe passagem errada.
        arquivo_inexistente|Arquivo Inexistente|Nível 3 / Sombra|Oculta cofre por uma cena.
        mensagem_eco|Mensagem de Eco|Nível 1 / Som|Reproduz mensagem de dez segundos.
        sino_mudo|Sino Mudo|Nível 1 / Som|Alerta portador quando fio rompe.
        ruptura_harmonica|Ruptura Harmônica|Nível 2 / Som|+2d6 contra objeto frágil.
        camara_silenciosa|Câmara Silenciosa|Nível 2 / Som|Sons não atravessam uma sala.
        voz_anfiteatro|Voz do Anfiteatro|Nível 3 / Som|Amplifica voz com clareza.
        retorno_magnetico|Retorno Magnético|Nível 1 / Metal|Arma arremessada retorna à mão.
        desvio_projetil|Desvio de Projétil|Nível 2 / Metal|Escudo concede +4 contra projétil metálico.
        inventario_fiel|Inventário Fiel|Nível 1 / Metal|Caixa alerta quando item sai.
        corrente_obediente|Corrente Obediente|Nível 2 / Metal|Corrente Imobiliza alvo adjacente.
        arsenal_vinculado|Arsenal Vinculado|Nível 3 / Metal|Convoca arma marcada no edifício.
        raiz_guarda|Raiz de Guarda|Nível 1 / Natureza|Planta aponta para intruso.
        madeira_renovada|Madeira Renovada|Nível 1 / Natureza|Repara quebra simples uma vez.
        cerca_espinhosa|Cerca Espinhosa|Nível 2 / Natureza|Barreira vegetal de 5 m e 15 PV.
        fruto_vigilia|Fruto de Vigília|Nível 2 / Natureza|Fruto remove 1 Exaustão uma vez ao dia.
        jardim_sentinela|Jardim Sentinela|Nível 3 / Natureza|Plantas Imobilizam invasores.
        ancora_onirica|Âncora Onírica|Nível 3 / Onírica|Impede teleporte e deslocamento planar.
        cofre_memoria|Cofre de Memória|Nível 2 / Onírica|Cristal guarda lembrança sensorial.
        despertador_pesadelo|Despertador de Pesadelo|Nível 1 / Onírica|Amuleto acorda diante de intrusão mental.
        janela_sonho|Janela de Sonho|Nível 3 / Onírica|Comunicação entre dois espelhos.
        lacre_antimanifesto|Lacre Antimanifesto|Nível 2 / Onírica|Manifesto vence PA 16 para cruzar.
        heranca_rubra|Herança Rubra|Nível 1 / Sangue|Confirma parentesco direto.
        juramento_carmesim|Juramento Carmesim|Nível 2 / Sangue|Marca quem quebra cláusula registrada.
        guardiao_reliquia|Guardião de Relíquia|Nível 2 / Arqueomancia|Relíquia fere e repele usuário não autorizado.
        traducao_recursiva|Tradução Recursiva|Nível 3 / Arqueomancia|Traduz símbolos antigos conhecidos.
        portal_oficina|Portal de Oficina|Nível 3 / Espaço|Abre passagem entre arcos pareados.
    """)

    val entries: List<CatalogEntry> = paths + powers + professionPowers + magic + ashes + runes + ItemCreationRules.catalog
}
