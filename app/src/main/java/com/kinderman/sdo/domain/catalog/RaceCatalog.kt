package com.kinderman.sdo.domain.catalog

data class RacialPower(val name: String, val effect: String)

data class RaceDefinition(
    val name: String,
    val attribute: String,
    val hp: Int,
    val sanity: Int,
    val arcane: Int,
    val energy: Int,
    val powers: List<RacialPower>,
    val organic: Boolean = true,
)

data class SubRaceDefinition(
    val name: String,
    val powers: List<RacialPower>,
    val organicOnly: Boolean = false,
)

object RaceCatalog {
    private fun power(name: String, effect: String) = RacialPower(name, effect)
    private fun race(name: String, attribute: String, hp: Int, sanity: Int, arcane: Int, energy: Int, organic: Boolean = true, vararg powers: RacialPower) =
        RaceDefinition(name, attribute, hp, sanity, arcane, energy, powers.toList(), organic)

    val races = listOf(
        race("Skayra", "POD", 1, 1, 2, 0, powers = arrayOf(power("Sombra Autônoma", "1 PM: interaja com objeto ou mecanismo simples a até 5 m pela sombra."), power("Marca do Pacto", "Uma vez por cena, 2 PM concedem +4 em Proteção Mental ou Arcana."))),
        race("Humanos", "Qualquer", 1, 1, 1, 1, powers = arrayOf(power("Versatilidade", "Escolha 1 Conhecimento Adquirido adicional."), power("Adaptação", "Uma vez por cena, 2 PE permitem repetir um teste falho; mantenha o segundo resultado."))),
        race("Elfos", "POD", 0, 1, 2, 1, powers = arrayOf(power("Afinidade Arcana", "+2 em testes de uma tradição, escola ou aplicação de magia escolhida."), power("Passo Gracioso", "Uma vez por turno após Movimento, 1 PE desloca mais 5 m; não vale para Corrida."))),
        race("Ascendidos", "VIG", 2, 0, 1, 1, powers = arrayOf(power("Asas Manifestas", "2 PE: plane, ignore quedas ou voe 10 m terminando apoiado."), power("Sopro Dracônico", "Uma vez por cena, 2 PM: linha de 6 m, POD + Arcano, 1d6 mágico."))),
        race("Elfos do Crepúsculo", "INT", 0, 1, 2, 1, powers = arrayOf(power("Interface Arcana", "+2 em um Conhecimento ligado a tecnologia, engenharia, artefatos ou magia."), power("Conversão de Energia", "Uma vez por turno, converta 2 PE em 1 PM ou 2 PM em 1 PE."))),
        race("Golms", "VIG", 3, 1, 0, 0, organic = false, powers = arrayOf(power("Corpo Construído", "Imune a sangramento, venenos e doenças comuns; não respira; cura biológica pela metade."), power("Matéria Resistente", "Escolha Pedra, Metal, Cristal ou Cerâmica para obter a resistência correspondente."))),
        race("Ciuvati", "POD", 0, 2, 2, 0, powers = arrayOf(power("Anatomia Impossível", "Uma vez por cena, 2 PM reduzem em um grau Falha Corporal, mutilação ou desmembramento."), power("Presença Anômala", "Uma vez por cena, 3 PM: teste resistido; alvo sofre -2 contra você até o próximo turno."))),
        race("Crias da Neblina", "CAR", 2, 1, 0, 1, powers = arrayOf(power("Pedra Viva", "Membros resistem a +1 Falha Corporal; críticos não desmembram automaticamente."), power("Reparo Dourado", "Imune a sangramento; recebe resistência crescente conforme Falhas de Órgão."))),
        race("Kaltoch — Andarilho", "VIG", 1, 1, 1, 1, organic = false, powers = arrayOf(power("Reparo", "Corpo mecânico: recupera HP por Reparo e começa com Reparo [0]."), power("Imunidade Mecânica", "Imune a venenos e doenças comuns."))),
        race("Kaltoch — Aumentado", "INT", 1, 1, 1, 1, powers = arrayOf(power("Pós-Mortal", "Partes mecânicas são imunes a venenos; começa com 4 modificações ou implantes."), power("Tecnologia Aprimorada", "Escolha 1 Conhecimento Adquirido suportado por um implante."))),
        race("Anões", "FOR", 2, 0, 0, 2, powers = arrayOf(power("Nascidos da Terra", "+4 em Sentidos no subterrâneo ou em estruturas de pedra/terra."), power("Resistência Anã", "RD 2 contra corte, impacto e perfuração; RD 4 abaixo da metade do HP."))),
        race("Sonaris", "POD", 0, 2, 1, 1, powers = arrayOf(power("Tímpano Vivo", "+5 em Sentidos por audição e localização sonora em 10 m; vulnerável a som extremo."), power("Memória Emocional", "2 PM captam a impressão emocional mais forte de objeto ou local."))),
        race("Goblins", "AGI", 2, 0, 0, 2, powers = arrayOf(power("Remendo Genial", "Uma vez por dia, improvise um item comum que dura 1 cena."), power("Instinto Paranoico", "+4 em Iniciativa e ignora penalidade de PG por surpresa no primeiro turno."))),
        race("Ovaryn", "VIG", 2, 1, 0, 1, powers = arrayOf(power("Passo Montanhês", "+4 para escalar, equilibrar e cruzar terreno íngreme ou instável."), power("Investida de Chifres", "Após mover 5 m em linha reta, 1 PE causa +1d4 corpo a corpo."))),
        race("Orcs", "FOR", 2, 0, 0, 2, powers = arrayOf(power("Ímpeto Brutal", "Após mover 5 m, 1 PE causa +2 dano corpo a corpo."), power("Recusar a Queda", "Uma vez por cena, receba +1 Exaustão para ficar com 1 HP em vez de 0."))),
        race("Tritões", "VIG", 1, 1, 1, 1, powers = arrayOf(power("Anfíbio", "Respira dentro e fora d'água; nada com deslocamento normal."), power("Sentidos Abissais", "+4 em Sentidos debaixo d'água e visão em profundidade até 10 m."))),
        race("Fadas", "CAR", 0, 2, 2, 0, powers = arrayOf(power("Voo Feérico", "2 PE: voe 10 m e termine apoiado."), power("Glamour", "Uma vez por cena, 2 PM criam ilusão sensorial simples em área de 5 m."))),
        race("Sangue-Vil", "Qualquer", 1, 0, 2, 1, powers = arrayOf(power("Herança", "Ao acertar, 1 PM causa +1d6 mágico do tipo definido na criação."), power("Olhar Amaldiçoado", "Uma vez por cena, 4 PE permitem intimidar até 3 criaturas adicionando POD."))),
        race("Avianos", "AGI", 0, 1, 0, 3, powers = arrayOf(power("Asas", "Plane e ignore quedas; 2 PE permitem voar 10 m terminando apoiado."), power("Visão de Caça", "+4 em Sentidos visuais além de 10 m; penalidade de distância reduzida em 2."))),
        race("Lúmens", "POD", 0, 2, 2, 0, organic = false, powers = arrayOf(power("Corpo Energético", "Imune a sangramento, venenos e doenças comuns."), power("Desfase", "Uma vez por cena, 3 PM atravessam até 3 m de matéria sólida não viva."))),
    )

    val subRaces = listOf(
        SubRaceDefinition("Oráculo", listOf(
            power("Vislumbre do Possível", "Uma vez por cena, 2 PM permitem rerrolar o d20 e escolher o resultado."),
            power("Presságio", "Uma vez por cena, 2 PM aplicam +2 ou -2 ao primeiro teste de uma criatura."),
            power("Sonho Profético", "Uma vez por sessão, receba uma impressão, símbolo, possibilidade ou pista verdadeira."),
        )),
        SubRaceDefinition("Bestial — Contaminado", listOf(
            power("Sentido Alterado", "+4 em Sentidos por visão, audição ou olfato escolhido."),
            power("Arma Natural", "Ataque desarmado apropriado; uma vez por turno, 1 PE causa +2 físico."),
            power("Locomoção Adaptada", "Escolha escalada, natação ou salto; 1 PE desloca mais 5 m."),
            power("Couraça Parcial", "RD 1 contra corte, impacto ou perfuração escolhido."),
            power("Membranas", "Plane e ignore dano de quedas; não permite voo sustentado."),
            power("Camuflagem Biológica", "Uma vez por cena, 1 PE concede +4 para esconder-se até atacar ou mover mais de 5 m."),
        ), organicOnly = true),
        SubRaceDefinition("Bestial — Completo", listOf(
            power("Predador Dominante", "+5 no sentido escolhido; 1 PE concede +2 no próximo ataque contra alvo localizado."),
            power("Armamento Bestial", "Uma vez por turno, 2 PE causam +1d6 com arma natural."),
            power("Locomoção Especializada", "Escalada, natação ou salto com deslocamento normal; 1 PE move +5 m."),
            power("Couraça Dominante", "RD 2 contra tipo escolhido; uma vez por cena, 2 PE elevam para RD 4."),
            power("Voo Bestial", "Plane; 2 PE permitem voar 10 m terminando apoiado."),
            power("Ecolocalização", "+5 em audição e localização em 10 m sem visão."),
            power("Regeneração Bestial", "Uma vez por cena, 3 PE e uma ação recuperam 1d6 HP."),
            power("Aderência Total", "Escale paredes e tetos sem equipamento ou testes comuns."),
            power("Corpo Constritor", "+4 para manter agarrado; 1 PE causa 1d4 impacto por turno."),
            power("Membros Adicionais", "Manipulam objetos sem conceder ações; +2 quando oferecem vantagem física direta."),
        ), organicOnly = true),
    )

    fun race(name: String) = races.firstOrNull { it.name == name }
    fun subRacesFor(race: RaceDefinition) = subRaces.filter { !it.organicOnly || race.organic }
}
