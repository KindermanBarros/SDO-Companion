package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind

/** Catálogo autoral ampliado: cada linha ancora uma prática em povos, lugares ou ofícios de Nulvara. */
internal object ExpandedKnowledgeCatalog {
    private fun rows(kind: CatalogKind, values: String): List<CatalogEntry> =
        values.trimIndent().lineSequence().filter(String::isNotBlank).map { row ->
            val (slug, name, category, attribute, lore) = row.split('|', limit = 5)
            CatalogEntry(
                id = "knowledge.${kind.name.lowercase()}.$slug",
                kind = kind,
                name = name,
                group = category,
                summary = lore,
                relatedAttribute = attribute,
                initialValue = 1,
                mechanicalEffect = "Pode fundamentar testes de $name quando a especialização e a experiência do personagem forem relevantes.",
                source = "Catálogo do Companion — especializações de Nulvara",
                version = BuiltInCatalog.VERSION,
                ruleReference = "03 - Regras/Criação de Personagem/Criação de Personagem.md",
                keywords = listOf(category, attribute, name),
            )
        }.toList()

    private val acquired = rows(CatalogKind.ACQUIRED_KNOWLEDGE, """
        arqueologia|Arqueologia|Ciência histórica|INT|Escava ruínas dos Silos e distingue camadas anteriores à Tragédia Planar.
        ferraria|Ferraria|Ofício|INT|Forja e recupera metal nas oficinas quentes de Durnkhaz.
        medicina|Medicina|Ciência aplicada|INT|Diagnostica doenças e traumas com a disciplina dos hospitais de Marevon.
        mecanica|Mecânica|Ofício técnico|INT|Mantém motores, próteses e mecanismos kaltoch em condições adversas.
        sobrevivencia|Sobrevivência|Exploração|VIG|Lê abrigo, água e perigo nas rotas hostis de Nulvara.
        atuacao|Atuação|Arte|CAR|Constrói personagens e emoções nos palcos populares de Essolhen.
        cartografia|Cartografia|Ciência geográfica|INT|Registra fronteiras móveis, rotas e distorções do continente.
        navegacao|Navegação|Exploração|INT|Orienta viagens por estrelas, correntes, marcos e instrumentos.
        alquimia_pratica|Alquimia Prática|Ofício arcano|INT|Prepara reagentes estáveis sem confundir fórmula com conjuração.
        botanica|Botânica|Ciência natural|INT|Reconhece flora medicinal, predatória e contaminada pela Bruma.
        zoologia|Zoologia|Ciência natural|INT|Classifica animais e seus hábitos antes que virem bestas de lenda.
        geologia|Geologia|Ciência natural|INT|Interpreta rocha, solo, falhas e a memória mineral das Serras de Keth.
        mineralogia|Mineralogia|Ofício científico|INT|Avalia minérios, gemas e ligas negociadas entre Caldera e Skratz.
        historia_nulvara|História de Nulvara|Humanidades|INT|Relaciona impérios, migrações e cicatrizes deixadas pela Tragédia Planar.
        politica_capitais|Política das Capitais|Ciência social|CAR|Lê alianças, rivalidades e interesses das oito capitais.
        direito_marevon|Direito de Marevon|Ciência social|INT|Conhece contratos portuários, jurisdição e precedentes da capital costeira.
        comercio_skratz|Comércio de Skratz|Ofício social|CAR|Negocia preços, favores e riscos nos mercados goblins.
        navegacao_dravan|Navegação Dravan|Exploração|AGI|Domina recifes, tempestades e rotas entre as Ilhas Dravan.
        engenharia_kaltoch|Engenharia Kaltoch|Tecnologia|INT|Compreende chassis, energia e protocolos dos corpos mecânicos.
        cultura_goblin|Cultura Goblin|Humanidades|INT|Interpreta clãs, humor, sucata e etiqueta improvisada de Skratz.
        tradicoes_anas|Tradições Anãs|Humanidades|INT|Reconhece juramentos, linhagens de ofício e códigos subterrâneos.
        etiqueta_elfica|Etiqueta Élfica|Ciência social|CAR|Evita ofensas em pactos longos, salões e círculos arcanos élficos.
        cultos_menores|Cultos Menores|Religião|INT|Identifica santos locais, entidades periféricas e ritos sem templo.
        bestiologia|Bestiologia|Ciência natural|INT|Estuda criaturas alteradas, seus sinais, territórios e fraquezas plausíveis.
        anatomia|Anatomia|Ciência aplicada|INT|Mapeia corpos orgânicos e suas variações raciais sem tratar diferença como doença.
        farmacia|Farmácia|Ofício científico|INT|Calcula dose, interação e conservação de remédios e toxinas.
        agricultura|Agricultura|Ofício rural|INT|Cuida de solo, safra e pragas nas terras disputadas de Nulvara.
        pecuaria|Pecuária|Ofício rural|CAR|Cria, conduz e trata animais domésticos e montarias.
        pesca|Pesca|Ofício|AGI|Lê águas, redes e temporadas de rios, lagos e litoral.
        caca|Caça|Exploração|AGI|Prepara emboscadas, abate e aproveitamento responsável de presas.
        rastreamento|Rastreamento|Exploração|POD|Reconstrói passagens por marcas, silêncio e alterações mínimas do ambiente.
        marcenaria|Marcenaria|Ofício|INT|Constrói e repara estruturas de madeira, do cais ao abrigo de viagem.
        alvenaria|Alvenaria|Ofício|FOR|Ergue, avalia e desmonta estruturas de pedra e tijolo.
        serralheria|Serralheria|Ofício|INT|Produz fechaduras, ferragens e mecanismos de contenção.
        costura|Costura|Ofício|AGI|Modela e repara tecidos, couro leve e vestimentas de proteção.
        joalheria|Joalheria|Ofício|AGI|Lapida gemas e identifica trabalhos delicados ou falsificações.
        cervejaria|Cervejaria|Ofício|VIG|Fermenta bebidas e reconhece contaminações nas tavernas de rota.
        musica|Música|Arte|CAR|Executa repertórios que carregam memória entre povos e fronteiras.
        oratoria|Oratória|Ciência social|CAR|Estrutura discursos para multidões, conselhos e julgamentos.
        investigacao|Investigação|Ciência aplicada|INT|Conecta vestígios, depoimentos e contradições sem inventar certeza.
        criptografia|Criptografia|Tecnologia|INT|Cria e rompe cifras usadas por guildas, governos e contrabandistas.
        arquivologia|Arquivologia|Humanidades|INT|Organiza registros e encontra o documento que alguém tentou apagar.
        demolicao|Demolição|Ofício técnico|INT|Calcula cargas e pontos estruturais para abrir caminho com controle.
        pilotagem|Pilotagem|Tecnologia|AGI|Conduz veículos terrestres, aéreos ou arcanotécnicos sob pressão.
    """)

    private val arcane = rows(CatalogKind.ARCANE_KNOWLEDGE, """
        necromancia|Necromancia|Escola arcana|POD|Investiga morte, espíritos e decadência sem prometer restauração verdadeira.
        abjuracao|Abjuração|Escola arcana|POD|Constrói barreiras, dissipações e selos de negação.
        invocacao|Invocação|Escola arcana|POD|Chama matéria, ferramentas e presenças temporárias mediante vínculo.
        encantamento|Encantamento|Escola arcana|POD|Influencia emoções e impulsos sem apagar identidade.
        ilusao|Ilusão|Escola arcana|POD|Molda percepção sensorial e enfrenta observadores atentos.
        transmutacao|Transmutação|Escola arcana|POD|Altera propriedades materiais sem criar riqueza permanente.
        electromancia|Electromancia|Escola arcana|POD|Conduz eletricidade, nervos e velocidade pelos circuitos do mundo.
        litomancia|Litomancia|Escola arcana|POD|Trabalha pedra, peso e estrutura com precisão mineral.
        natureza_arcana|Natureza Arcana|Escola arcana|POD|Negocia com plantas, animais e ciclos orgânicos vivos.
        piromancia|Piromancia|Escola arcana|POD|Controla fogo, calor, combustão e luz intensa.
        aqueomancia|Aqueomancia|Escola arcana|POD|Move água, pressão, névoa e frio úmido.
        arqueomancia|Arqueomancia|Escola arcana|POD|Reativa padrões perdidos inscritos em relíquias e ruínas.
        aeromancia|Aeromancia|Escola arcana|POD|Orienta ar, vento, pressão e movimento atmosférico.
        geomancia|Geomancia|Escola arcana|POD|Escuta terreno, areia, cristal e linhas telúricas.
        criomancia|Criomancia|Escola arcana|POD|Extrai calor e organiza umidade em gelo e conservação.
        umbramancia|Umbramancia|Escola arcana|POD|Trabalha sombra, ocultação, silêncio e medo.
        luminomancia|Luminomancia|Escola arcana|POD|Produz luz, revelação, miragens e radiação.
        cronomancia|Cronomancia|Escola arcana|POD|Dobra percepção temporal, atraso e aceleração breve.
        gravimancia|Gravimancia|Escola arcana|POD|Altera peso, queda, órbita e pressão local.
        espaciomancia|Espaciomancia|Escola arcana|POD|Manipula distância, portais curtos e bolsões espaciais.
        sonoromancia|Sonoromancia|Escola arcana|POD|Controla vibração, música, eco e ruptura.
        ferromancia|Ferromancia|Escola arcana|POD|Comanda metal refinado, magnetismo e armas.
        biomancia|Biomancia|Escola arcana|POD|Adapta carne, sentidos e formas vivas com consequências.
        alquimancia|Alquimancia|Escola arcana|POD|Transforma reagentes e propriedades por equivalências arcanas.
        tecnomancia|Tecnomancia|Escola arcana|POD|Interage com máquinas, circuitos e interfaces mágicas.
        astromancia|Astromancia|Escola arcana|POD|Lê estrelas, orientação, presságios e energia celeste.
        adivinhacao|Adivinhação|Escola arcana|POD|Busca indícios remotos e possibilidades, nunca futuro imutável.
        metamancia|Metamancia|Disciplina arcana|POD|Modifica alcance, forma e custo de outras magias.
        espiritismo|Espiritismo|Disciplina arcana|POD|Contata almas e memórias residuais sem confundir contato com domínio.
        oniromancia|Oniromancia|Escola arcana|POD|Navega sonhos, mente e manifestações do Plano Onírico.
        cura_arcana|Cura Arcana|Escola arcana|POD|Regenera, purifica e estabiliza dentro dos limites da vida.
        teoria_cinzas|Teoria da Pureza|Disciplina arcana|INT|Classifica Cinzas brutas, refinadas e puras por risco e potência.
        coleta_cinzas|Coleta de Vestígios|Disciplina arcana|INT|Extrai doses antes que uma manifestação se dissipe.
        refinamento_cinzas|Refinamento de Cinzas|Ofício arcano|INT|Converte vestígios instáveis em reagentes confiáveis.
        sintaxe_runica|Sintaxe Rúnica|Disciplina arcana|POD|Combina sigilo, forma, direção, intensidade, gatilho e limite.
        desarme_runico|Desarme Rúnico|Disciplina arcana|POD|Interrompe inscrições sem acionar seu defeito oculto.
        paleografia_arcana|Paleografia Arcana|Disciplina arcana|INT|Decifra símbolos de tradições mortas em ruínas e relíquias.
        cartografia_onirica|Cartografia Onírica|Disciplina arcana|INT|Registra caminhos mutáveis do Plano Onírico por âncoras simbólicas.
        manifestos_oniricos|Manifestos Oníricos|Bestiologia arcana|INT|Reconhece entidades nascidas de sonho, trauma e desejo.
        materia_onirica|Matéria Onírica|Ofício arcano|POD|Molda substância instável trazida de sonhos consolidados.
        energia_onirica|Energia Onírica|Disciplina arcana|POD|Mede e canaliza tensão entre realidade e imaginação.
        corrupcao_divina|Corrupção Divina|Teologia arcana|INT|Identifica marcas de poder divino que deformam corpo e vontade.
        pactos|Pactos e Vínculos|Teologia arcana|CAR|Formula obrigações sobrenaturais e reconhece cobranças ocultas.
        reliquias|Relíquias de Nulvara|Arqueologia arcana|INT|Avalia origem, função e perigo de artefatos anteriores às capitais.
        contramagia|Contramagia|Disciplina arcana|POD|Reconhece uma conjuração a tempo de negar ou deformar seu efeito.
        concentracao|Concentração Arcana|Disciplina arcana|VIG|Sustenta efeitos sob dor, distração e pressão de combate.
    """)

    private val techniques = rows(CatalogKind.BATTLE_TECHNIQUE, """
        aparar|Aparar|Defesa marcial|AGI|Desvia golpes com arma preparada e leitura de tempo.
        contra_ataque|Contra-ataque|Resposta marcial|AGI|Transforma uma abertura defensiva em resposta imediata.
        guarda_alta|Guarda Alta|Postura|VIG|Protege cabeça e tronco sem abandonar terreno.
        guarda_baixa|Guarda Baixa|Postura|AGI|Mantém mobilidade contra ataques às pernas.
        escudo_companheiro|Escudo do Companheiro|Defesa marcial|VIG|Interpõe proteção entre perigo e aliado adjacente.
        muralha_ana|Muralha Anã|Tradição marcial|VIG|Firma escudo e juramento como fazem as linhas subterrâneas.
        faca_skratz|Faca de Skratz|Tradição marcial|AGI|Usa distração, terreno e lâmina curta sem duelo honrado.
        lanca_dravan|Lança Dravan|Tradição marcial|AGI|Controla distância em convés, praia e formação apertada.
        arco_marevon|Arco de Marevon|Tradição marcial|AGI|Prioriza precisão entre mastros, telhados e multidões.
        martelo_caldera|Martelo de Caldera|Tradição marcial|FOR|Quebra guarda com a cadência das grandes forjas.
        duas_maos|Arma em Duas Mãos|Técnica com armas|FOR|Converte base firme e alavanca em impacto decisivo.
        espada_escudo|Espada e Escudo|Técnica com armas|VIG|Alterna cobertura, pressão e corte em formação.
        tiro_preciso|Tiro Preciso|Técnica à distância|AGI|Respira, mede e escolhe um ponto exposto.
        tiro_cobertura|Tiro de Cobertura|Técnica à distância|AGI|Controla passagem e obriga inimigos a respeitar a linha de fogo.
        saque_rapido|Saque Rápido|Técnica com armas|AGI|Prepara arma no instante em que a ameaça se revela.
        desarme|Desarme|Manobra|AGI|Ataca empunhadura, equilíbrio ou vínculo em vez do corpo.
        derrubar|Derrubar|Manobra|FOR|Usa alavanca e impulso para tirar o alvo da base.
        agarrar|Agarrar|Manobra|FOR|Controla membros e deslocamento a curta distância.
        imobilizar|Imobilizar|Manobra|FOR|Converte um agarrão estabelecido em contenção segura.
        empurrar|Empurrão Tático|Manobra|FOR|Move o alvo para abrir linha, queda ou retirada.
        romper_formacao|Romper Formação|Assalto|FOR|Ataca o intervalo entre defensores coordenados.
        investida|Investida Controlada|Assalto|VIG|Acumula deslocamento sem entregar todo o equilíbrio.
        passo_lateral|Passo Lateral|Mobilidade|AGI|Sai da linha de ataque mantendo ameaça próxima.
        recuo_coberto|Recuo Coberto|Mobilidade|AGI|Abandona posição sem oferecer perseguição gratuita.
        salto_obstaculo|Salto de Obstáculo|Mobilidade|AGI|Cruza cobertura baixa preservando ritmo de combate.
        combate_cego|Combate às Cegas|Percepção marcial|POD|Luta por som, ar e intenção quando a visão falha.
        leitura_oponente|Leitura do Oponente|Percepção marcial|INT|Reconhece hábito, alcance e intenção antes do golpe.
        finta|Finta|Engano marcial|CAR|Cria uma expectativa falsa e ataca a reação.
        provocacao|Provocação|Pressão marcial|CAR|Atrai atenção hostil para proteger um plano ou aliado.
        comando_campo|Comando de Campo|Liderança|CAR|Distribui ordens curtas que organizam aliados sob ruído.
        manter_linha|Manter a Linha|Formação|VIG|Recusa avanço inimigo enquanto houver companheiros ao lado.
        avancar_juntos|Avançar Juntos|Formação|CAR|Coordena deslocamento para que ninguém fique isolado.
        troca_posicao|Troca de Posição|Formação|AGI|Substitui um aliado exposto sem romper a linha.
        proteger_conjurador|Proteger Conjurador|Formação|VIG|Cria espaço para concentração e gestos arcanos.
        cacador_monstros|Caçador de Monstros|Caça|INT|Adapta postura depois de reconhecer anatomia e padrão da criatura.
        quebrador_construtos|Quebrador de Construtos|Assalto|FOR|Procura juntas, conduítes e pontos de manutenção.
        duelista|Duelista|Técnica com armas|AGI|Controla medida e iniciativa em confronto singular.
        brigao_taverna|Brigão de Taverna|Combate improvisado|FOR|Transforma bancos, garrafas e aperto em vantagem plausível.
        arma_improvisada|Arma Improvisada|Combate improvisado|INT|Reconhece alcance e fragilidade de qualquer ferramenta à mão.
        combate_montado|Combate Montado|Montaria|AGI|Ataca sem perder controle da montaria em movimento.
        queda_segura|Queda Segura|Mobilidade|AGI|Absorve impacto e escolhe onde terminar após ser derrubado.
        resistencia_dor|Resistência à Dor|Condicionamento|VIG|Mantém ação apesar de ferimentos sem negar suas consequências.
        respiracao_tatica|Respiração Tática|Condicionamento|VIG|Recupera foco entre explosões de esforço.
        vigilia|Vigília de Campanha|Condicionamento|POD|Permanece atento em turnos de guarda e viagens longas.
        retirada|Retirada Ordenada|Liderança|CAR|Transforma fuga em movimento coordenado e defensável.
        rendicao|Rendição Segura|Diplomacia marcial|CAR|Encerra violência preservando reféns, armas e termos claros.
        captura|Captura sem Morte|Controle marcial|INT|Prioriza contenção, desarme e dano não letal.
        ultimo_bastiao|Último Bastião|Postura|VIG|Sustenta passagem estreita quando recuar destruiria o grupo.
    """)

    val entries: List<CatalogEntry> = acquired + arcane + techniques
}
