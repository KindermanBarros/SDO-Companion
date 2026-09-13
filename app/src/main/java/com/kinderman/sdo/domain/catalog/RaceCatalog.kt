package com.kinderman.sdo.domain.catalog

data class RacialPower(
    val name: String,
    val effect: String,
    val cost: String = "Sem custo",
    val action: String = "Passiva",
    val range: String = "Pessoal",
    val duration: String = "Instantânea",
    val limit: String = "Sem limite adicional",
    val activationCondition: String = "Sempre ativo",
    val deactivationCondition: String = "Não aplicável",
    val costType: com.kinderman.sdo.domain.model.AbilityCostType = com.kinderman.sdo.domain.model.AbilityCostType.NONE,
    val costValue: Int = 0,
    val destinyCostEligible: Boolean = false,
    val executionType: com.kinderman.sdo.domain.model.AbilityExecution = com.kinderman.sdo.domain.model.AbilityExecution.PASSIVE,
    val rangeType: com.kinderman.sdo.domain.model.AbilityRange = com.kinderman.sdo.domain.model.AbilityRange.PERSONAL,
    val durationType: com.kinderman.sdo.domain.model.AbilityDuration = com.kinderman.sdo.domain.model.AbilityDuration.INSTANT,
)

data class RaceDefinition(
    val name: String,
    val attribute: String,
    val hp: Int,
    val sanity: Int,
    val arcane: Int,
    val energy: Int,
    val powers: List<RacialPower>,
    val organic: Boolean = true,
    val source: String = RaceCatalog.SOURCE,
    val ruleReference: String = RaceCatalog.RULE_REFERENCE,
    val version: Int = BuiltInCatalog.VERSION,
)

data class SubRaceDefinition(
    val name: String,
    val powers: List<RacialPower>,
    val parentRace: String? = null,
    val organicOnly: Boolean = false,
    val source: String = RaceCatalog.SOURCE,
    val ruleReference: String = RaceCatalog.RULE_REFERENCE,
    val version: Int = BuiltInCatalog.VERSION,
)

object RaceCatalog {
    const val SOURCE = "Raças — Regras de Personagem"
    const val RULE_REFERENCE = "03 - Regras/Raças.md"

    private fun power(
        name: String,
        effect: String,
        costValue: Int = 0,
        destinyCostEligible: Boolean = false,
        action: String = "Passiva",
        range: String = "Pessoal",
        duration: String = "Instantânea",
        limit: String = "Sem limite adicional",
        activationCondition: String = "Sempre ativo",
        deactivationCondition: String = "Não aplicável",
    ) = RacialPower(
        name = name,
        effect = effect,
        cost = if (costValue > 0) "$costValue PE" else "Sem custo",
        action = if (action == "Passiva" && costValue > 0) "1 ação" else action,
        range = range,
        duration = duration,
        limit = limit,
        activationCondition = activationCondition,
        deactivationCondition = deactivationCondition,
        costType = if (costValue > 0) com.kinderman.sdo.domain.model.AbilityCostType.ENERGY else com.kinderman.sdo.domain.model.AbilityCostType.NONE,
        costValue = costValue,
        destinyCostEligible = destinyCostEligible,
        executionType = when {
            action == "Reação" -> com.kinderman.sdo.domain.model.AbilityExecution.REACTION
            action == "Livre" -> com.kinderman.sdo.domain.model.AbilityExecution.FREE
            costValue == 0 && action == "Passiva" -> com.kinderman.sdo.domain.model.AbilityExecution.PASSIVE
            else -> com.kinderman.sdo.domain.model.AbilityExecution.ACTION
        },
        rangeType = when {
            range == "Indefinido" -> com.kinderman.sdo.domain.model.AbilityRange.INDEFINITE
            range == "Pessoal" || range == "Toque" -> com.kinderman.sdo.domain.model.AbilityRange.PERSONAL
            range.substringBefore(" ").toIntOrNull() in 1..9 -> com.kinderman.sdo.domain.model.AbilityRange.SHORT
            range.substringBefore(" ").toIntOrNull() in 10..30 -> com.kinderman.sdo.domain.model.AbilityRange.MEDIUM
            else -> com.kinderman.sdo.domain.model.AbilityRange.LONG
        },
        durationType = when {
            duration.contains("cena", true) -> com.kinderman.sdo.domain.model.AbilityDuration.SCENE
            duration.contains("turno", true) -> com.kinderman.sdo.domain.model.AbilityDuration.TURNS
            else -> com.kinderman.sdo.domain.model.AbilityDuration.INSTANT
        },
    )
    private fun race(name: String, attribute: String, hp: Int, sanity: Int, arcane: Int, energy: Int, organic: Boolean = true, vararg powers: RacialPower) =
        RaceDefinition(name, attribute, hp, sanity, arcane, energy, powers.toList(), organic)

    val races = listOf(
        race("Skayra", "POD", 1, 1, 2, 0, powers = arrayOf(power("Sombra Autônoma", "Interaja com um objeto ou mecanismo simples a até 5 m pela sombra.", costValue = 1, range = "5 metros"), power("Marca do Pacto", "Uma vez por cena, receba +4 em Proteção Mental ou Arcana.", costValue = 2, action = "Reação", limit = "Uma vez por cena"))),
        race("Humanos", "Qualquer", 1, 1, 1, 1, powers = arrayOf(power("Versatilidade", "Escolha 1 Conhecimento Adquirido adicional."), power("Adaptação", "Uma vez por cena, repita um teste falho e mantenha o segundo resultado.", costValue = 2, action = "Reação", limit = "Uma vez por cena"))),
        race("Elfos", "POD", 0, 1, 2, 1, powers = arrayOf(power("Afinidade Arcana", "+2 em testes de uma tradição, escola ou aplicação de magia escolhida.", action = "1 ação"), power("Passo Gracioso", "Uma vez por turno após Movimento, desloque-se mais 5 m; não vale para Corrida.", costValue = 1, limit = "Uma vez por turno"))),
        race("Ascendidos", "VIG", 2, 0, 1, 1, powers = arrayOf(power("Asas Manifestas", "Plane, ignore quedas ou voe 10 m, terminando apoiado.", costValue = 2), power("Sopro Dracônico", "Uma vez por cena, afete uma linha de 6 m com POD + Arcano e cause 1d6 de dano mágico.", costValue = 2, range = "6 metros", limit = "Uma vez por cena"))),
        race("Golms", "VIG", 3, 1, 0, 0, organic = false, powers = arrayOf(power("Corpo Construído", "Imune a sangramento, venenos e doenças comuns; não respira; cura biológica pela metade."), power("Matéria Resistente", "Escolha Pedra, Metal, Cristal ou Cerâmica para obter a resistência correspondente."))),
        race("Ciuvati", "POD", 0, 2, 2, 0, powers = arrayOf(power("Anatomia Impossível", "Uma vez por cena, reduza em um grau uma Falha Corporal, mutilação ou desmembramento.", costValue = 2, action = "Reação", limit = "Uma vez por cena"), power("Presença Anômala", "Uma vez por cena, faça um teste resistido; o alvo sofre -2 contra você até o próximo turno.", costValue = 3, range = "10 metros", duration = "1 turno", limit = "Uma vez por cena"))),
        race("Crias da Neblina", "CAR", 2, 1, 0, 1, powers = arrayOf(power("Pedra Viva", "Membros resistem a +1 Falha Corporal; críticos não desmembram automaticamente."), power("Reparo Dourado", "Imune a sangramento; recebe resistência crescente conforme Falhas de Órgão."))),
        race("Kaltoch", "VIG", 1, 1, 1, 1, organic = false, powers = arrayOf(power("Reparo", "Corpo mecânico: recupera HP por Reparo e começa com Reparo [0]."), power("Imunidade Mecânica", "Imune a venenos e doenças comuns."))),
        race("Anões", "FOR", 2, 0, 0, 2, powers = arrayOf(power("Nascidos da Terra", "+4 em Sentidos no subterrâneo ou em estruturas de pedra/terra."), power("Resistência Anã", "RD 2 contra corte, impacto e perfuração; RD 4 abaixo da metade do HP.", action = "1 ação"))),
        race("Sonaris", "POD", 0, 2, 1, 1, powers = arrayOf(power("Tímpano Vivo", "+5 em Sentidos por audição e localização sonora em 10 m; vulnerável a som extremo.", action = "1 ação"), power("Memória Emocional", "Capte a impressão emocional mais forte de um objeto ou local.", costValue = 2, range = "Toque"))),
        race("Goblins", "AGI", 2, 0, 0, 2, powers = arrayOf(power("Remendo Genial", "Uma vez por dia, improvise um item comum que dura 1 cena.", limit = "Uma vez por dia"), power("Instinto Paranoico", "+4 em Iniciativa e ignora penalidade de PG por surpresa no primeiro turno."))),
        race("Ovaryn", "VIG", 2, 1, 0, 1, powers = arrayOf(power("Passo Montanhês", "+4 para escalar, equilibrar e cruzar terreno íngreme ou instável."), power("Investida de Chifres", "Após mover 5 m em linha reta, cause +1d4 de dano corpo a corpo.", costValue = 1))),
        race("Orcs", "FOR", 2, 0, 0, 2, powers = arrayOf(power("Ímpeto Brutal", "Após mover 5 m, cause +2 de dano corpo a corpo.", costValue = 1), power("Recusar a Queda", "Uma vez por cena, receba +1 Exaustão para ficar com 1 HP em vez de 0.", limit = "Uma vez por cena"))),
        race("Tritões", "VIG", 1, 1, 1, 1, powers = arrayOf(power("Anfíbio", "Respira dentro e fora d'água; nada com deslocamento normal."), power("Sentidos Abissais", "+4 em Sentidos debaixo d'água e visão em profundidade até 10 m.", range = "10 metros"))),
        race("Fadas", "CAR", 0, 2, 2, 0, powers = arrayOf(power("Voo Feérico", "Voe 10 m e termine apoiado.", costValue = 2), power("Glamour", "Uma vez por cena, crie uma ilusão sensorial simples em uma área de 5 m.", costValue = 2, range = "5 metros", limit = "Uma vez por cena"))),
        race("Sangue-Vil", "Qualquer", 1, 0, 2, 1, powers = arrayOf(power("Herança", "Ao acertar, cause +1d6 de dano mágico do tipo definido na criação.", costValue = 1, action = "Livre"), power("Olhar Amaldiçoado", "Uma vez por cena, tente intimidar até 3 criaturas adicionando POD.", costValue = 4, limit = "Uma vez por cena"))),
        race("Avianos", "AGI", 0, 1, 0, 3, powers = arrayOf(power("Asas", "Plane e ignore quedas; voe 10 m terminando apoiado.", costValue = 2), power("Visão de Caça", "+4 em Sentidos visuais além de 10 m; penalidade de distância reduzida em 2."))),
        race("Lúmens", "POD", 0, 2, 2, 0, organic = false, powers = arrayOf(power("Corpo Energético", "Imune a sangramento, venenos e doenças comuns."), power("Desfase", "Uma vez por cena, atravesse até 3 m de matéria sólida não viva.", costValue = 3, limit = "Uma vez por cena", range = "3 metros"))),
    )

    val subRaces = listOf(
        SubRaceDefinition("Elfos do Crepúsculo", listOf(
            power("Interface Arcana", "+2 em um Conhecimento ligado a tecnologia, engenharia, artefatos ou magia."),
            power("Conversão de Energia", "Uma vez por turno, recupere 1 PM sem ultrapassar seu máximo.", costValue = 2, limit = "Uma vez por turno"),
        ), parentRace = "Elfos"),
        SubRaceDefinition("Aumentado", listOf(
            power("Pós-Mortal", "Partes mecânicas são imunes a venenos; começa com 4 modificações ou implantes."),
            power("Tecnologia Aprimorada", "Escolha 1 Conhecimento Adquirido suportado por um implante."),
        ), parentRace = "Kaltoch"),
        SubRaceDefinition("Oráculo", listOf(
            power("Vislumbre do Possível", "Uma vez por cena, rerrole o d20 e escolha o resultado.", costValue = 2, destinyCostEligible = true, action = "Reação", limit = "Uma vez por cena"),
            power("Presságio", "Uma vez por cena, aplique +2 ou -2 ao primeiro teste de uma criatura.", costValue = 2, destinyCostEligible = true, range = "Indefinido", duration = "1 turno", limit = "Uma vez por cena"),
            power("Sonho Profético", "Uma vez por sessão, receba uma impressão, símbolo, possibilidade ou pista verdadeira.", limit = "Uma vez por sessão"),
        )),
        SubRaceDefinition("Bestial — Contaminado", listOf(
            power("Sentido Alterado", "+4 em Sentidos por visão, audição ou olfato escolhido."),
            power("Arma Natural", "Uma vez por turno, ao acertar um ataque desarmado apropriado, cause +2 de dano físico.", costValue = 1, limit = "uma vez por turno"),
            power("Locomoção Adaptada", "Durante a locomoção escolhida, desloque-se mais 5 m.", costValue = 1, action = "1 ação"),
            power("Couraça Parcial", "RD 1 contra corte, impacto ou perfuração escolhido.", action = "1 ação"),
            power("Membranas", "Plane e ignore dano de quedas; não permite voo sustentado."),
            power("Camuflagem Biológica", "Uma vez por cena, receba +4 para esconder-se até atacar ou mover mais de 5 m.", costValue = 1, limit = "Uma vez por cena"),
        ), organicOnly = true),
        SubRaceDefinition("Bestial — Completo", listOf(
            power("Predador Dominante", "Depois de localizar uma criatura com o sentido escolhido, receba +2 no próximo ataque contra ela.", costValue = 1),
            power("Armamento Bestial", "Uma vez por turno, cause +1d6 de dano físico com uma arma natural.", costValue = 2, limit = "Uma vez por turno"),
            power("Locomoção Especializada", "Durante a locomoção escolhida, desloque-se mais 5 m.", costValue = 1, action = "1 ação"),
            power("Couraça Dominante", "Uma vez por cena, aumente para RD 4 a resistência contra o ataque do tipo escolhido.", costValue = 2, action = "Reação", limit = "Uma vez por cena"),
            power("Voo Bestial", "Voe 10 m e termine apoiado.", costValue = 2),
            power("Ecolocalização", "+5 em audição e localização em 10 m sem visão.", action = "1 ação"),
            power("Regeneração Bestial", "Uma vez por cena, recupere 1d6 HP sem remover falhas, mutilações ou membros perdidos.", costValue = 3, limit = "Uma vez por cena", action = "1 ação"),
            power("Aderência Total", "Escale paredes e tetos sem equipamento ou testes comuns."),
            power("Corpo Constritor", "Uma vez por turno, cause 1d4 de dano de impacto ao alvo agarrado.", costValue = 1),
            power("Membros Adicionais", "Manipulam objetos sem conceder ações; +2 quando oferecem vantagem física direta."),
        ), organicOnly = true),
    )

    fun race(name: String): RaceDefinition? {
        val canonicalName = when (name) {
            "Kaltoch — Andarilho", "Kaltoch — Aumentado" -> "Kaltoch"
            "Elfos do Crepúsculo" -> "Elfos"
            else -> name
        }
        return races.firstOrNull { it.name == canonicalName }
    }

    fun legacySubRace(raceName: String): String? = when (raceName) {
        "Kaltoch — Aumentado" -> "Aumentado"
        "Elfos do Crepúsculo" -> "Elfos do Crepúsculo"
        else -> null
    }

    fun subRacesFor(race: RaceDefinition) = subRaces.filter {
        (it.parentRace == null || it.parentRace == race.name) && (!it.organicOnly || race.organic)
    }
}
