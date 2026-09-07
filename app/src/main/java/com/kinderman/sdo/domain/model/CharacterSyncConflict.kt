package com.kinderman.sdo.domain.model

data class CharacterConflictField(
    val id: String,
    val label: String,
    val localSummary: String,
    val remoteSummary: String,
)

data class CharacterSyncConflict(
    val local: Character,
    val remote: Character,
    val remoteUpdatedAt: Long,
    val fields: List<CharacterConflictField> = characterConflictFields(local, remote),
)

fun characterConflictFields(local: Character, remote: Character): List<CharacterConflictField> = buildList {
    fun <T> changed(id: String, label: String, localValue: T, remoteValue: T, summary: (T) -> String = { it.toString() }) {
        if (localValue != remoteValue) {
            add(CharacterConflictField(id, label, summary(localValue), summary(remoteValue)))
        }
    }

    changed("ownerId", "Proprietário", local.ownerId, remote.ownerId, ::textSummary)
    changed("campaignId", "Campanha", local.campaignId, remote.campaignId, ::textSummary)
    changed("name", "Nome", local.name, remote.name, ::textSummary)
    changed("race", "Raça", local.race, remote.race, ::textSummary)
    changed("subRace", "Sub-raça", local.subRace, remote.subRace, ::textSummary)
    changed("occupation", "Ocupação", local.occupation, remote.occupation, ::textSummary)
    changed("height", "Altura", local.height, remote.height, ::textSummary)
    changed("age", "Idade", local.age, remote.age, ::textSummary)
    changed("sex", "Sexo", local.sex, remote.sex, ::textSummary)
    changed("level", "Nível", local.level, remote.level)
    changed("money", "Dinheiro", local.money, remote.money)
    changed("life", "Vida", local.life, remote.life, ::resourceSummary)
    changed("sanity", "Sanidade", local.sanity, remote.sanity, ::resourceSummary)
    changed("arcane", "Arcano", local.arcane, remote.arcane, ::resourceSummary)
    changed("energy", "Energia", local.energy, remote.energy, ::resourceSummary)
    changed("destiny", "Destino", local.destiny, remote.destiny, ::resourceSummary)
    changed("exhaustion", "Exaustão", local.exhaustion, remote.exhaustion, ::resourceSummary)
    changed("corruption", "Corrupção divina", local.corruption, remote.corruption, ::resourceSummary)
    changed("attributes", "Atributos e conhecimentos", local.attributes, remote.attributes, ::attributeSummary)
    changed(
        "protectionAdjustments",
        "Ajustes de proteção",
        local.protectionAdjustments,
        remote.protectionAdjustments,
        ::mapSummary,
    )
    changed("positiveTraits", "Traços positivos", local.positiveTraits, remote.positiveTraits, ::stringListSummary)
    changed("negativeTraits", "Traços negativos", local.negativeTraits, remote.negativeTraits, ::stringListSummary)
    changed("learnedKnowledges", "Conhecimentos aprendidos", local.learnedKnowledges, remote.learnedKnowledges, ::namedListSummary)
    changed("arcaneKnowledges", "Conhecimentos arcanos", local.arcaneKnowledges, remote.arcaneKnowledges, ::namedListSummary)
    changed("battleTechniques", "Técnicas de batalha", local.battleTechniques, remote.battleTechniques, ::namedListSummary)
    changed("pathName", "Nome do Caminho", local.pathName, remote.pathName, ::textSummary)
    changed("pathMotto", "Lema do Caminho", local.pathMotto, remote.pathMotto, ::textSummary)
    changed("pathKeywords", "Palavras-chave do Caminho", local.pathKeywords, remote.pathKeywords, ::stringListSummary)
    changed("pathPillars", "Pilares do Caminho", local.pathPillars, remote.pathPillars, ::stringListSummary)
    changed("powers", "Poderes", local.powers, remote.powers) { values -> namedListSummary(values) { it.name } }
    changed("inventory", "Inventário", local.inventory, remote.inventory) { values -> namedListSummary(values) { it.name } }
    changed("containerCapacity", "Capacidade do recipiente", local.containerCapacity, remote.containerCapacity)
    changed("bodyRegions", "Corpo e armadura", local.bodyRegions, remote.bodyRegions) { values ->
        values.joinToString(" • ") { "${it.name}: ${it.failures} falhas, PL ${it.localProtection}, PG ${it.generalProtection}" }
    }
    changed("agilityLimit", "Limitação de Agilidade", local.agilityLimit, remote.agilityLimit, ::textSummary)
    changed("organs", "Órgãos", local.organs, remote.organs) { values ->
        values.joinToString(" • ") { "${it.name}: ${it.failures} falhas" }
    }
    changed("mysticAbilities", "Magias, runas e cinzas", local.mysticAbilities, remote.mysticAbilities) { values ->
        namedListSummary(values) { it.name }
    }
    changed("conditions", "Condições", local.conditions, remote.conditions) { values -> namedListSummary(values) { it.name } }
    changed("story", "História", local.story, remote.story, ::textSummary)
    changed("personalNotes", "Anotações", local.personalNotes, remote.personalNotes) { values -> namedListSummary(values) { it.title } }
    changed("lock", "Bloqueio da ficha", local.lockSnapshot(), remote.lockSnapshot(), ::lockSummary)
}

fun mergeCharacterConflict(
    local: Character,
    remote: Character,
    remoteFieldIds: Set<String>,
): Character {
    fun <T> selected(id: String, localValue: T, remoteValue: T): T =
        if (id in remoteFieldIds) remoteValue else localValue

    val lock = selected("lock", local.lockSnapshot(), remote.lockSnapshot())
    return local.copy(
        ownerId = selected("ownerId", local.ownerId, remote.ownerId),
        campaignId = selected("campaignId", local.campaignId, remote.campaignId),
        name = selected("name", local.name, remote.name),
        race = selected("race", local.race, remote.race),
        subRace = selected("subRace", local.subRace, remote.subRace),
        occupation = selected("occupation", local.occupation, remote.occupation),
        height = selected("height", local.height, remote.height),
        age = selected("age", local.age, remote.age),
        sex = selected("sex", local.sex, remote.sex),
        size = remote.size,
        level = selected("level", local.level, remote.level),
        money = selected("money", local.money, remote.money),
        life = selected("life", local.life, remote.life),
        sanity = selected("sanity", local.sanity, remote.sanity),
        arcane = selected("arcane", local.arcane, remote.arcane),
        energy = selected("energy", local.energy, remote.energy),
        destiny = selected("destiny", local.destiny, remote.destiny),
        exhaustion = selected("exhaustion", local.exhaustion, remote.exhaustion),
        corruption = selected("corruption", local.corruption, remote.corruption),
        attributes = selected("attributes", local.attributes, remote.attributes),
        protections = remote.protections,
        protectionAdjustments = selected(
            "protectionAdjustments",
            local.protectionAdjustments,
            remote.protectionAdjustments,
        ),
        positiveTraits = selected("positiveTraits", local.positiveTraits, remote.positiveTraits),
        negativeTraits = selected("negativeTraits", local.negativeTraits, remote.negativeTraits),
        learnedKnowledges = selected("learnedKnowledges", local.learnedKnowledges, remote.learnedKnowledges),
        arcaneKnowledges = selected("arcaneKnowledges", local.arcaneKnowledges, remote.arcaneKnowledges),
        battleTechniques = selected("battleTechniques", local.battleTechniques, remote.battleTechniques),
        pathName = selected("pathName", local.pathName, remote.pathName),
        pathMotto = selected("pathMotto", local.pathMotto, remote.pathMotto),
        pathKeywords = selected("pathKeywords", local.pathKeywords, remote.pathKeywords),
        pathPillars = selected("pathPillars", local.pathPillars, remote.pathPillars),
        powers = selected("powers", local.powers, remote.powers),
        inventory = selected("inventory", local.inventory, remote.inventory),
        containerCapacity = selected("containerCapacity", local.containerCapacity, remote.containerCapacity),
        bodyRegions = selected("bodyRegions", local.bodyRegions, remote.bodyRegions),
        agilityLimit = selected("agilityLimit", local.agilityLimit, remote.agilityLimit),
        organs = selected("organs", local.organs, remote.organs),
        mysticAbilities = selected("mysticAbilities", local.mysticAbilities, remote.mysticAbilities),
        conditions = selected("conditions", local.conditions, remote.conditions),
        story = selected("story", local.story, remote.story),
        notes = remote.notes,
        personalNotes = selected("personalNotes", local.personalNotes, remote.personalNotes),
        lockType = lock.type,
        lockedBy = lock.by,
        lockedAt = lock.at,
        deleted = false,
    )
}

private data class LockSnapshot(val type: CharacterLock, val by: String, val at: Long?)

private fun Character.lockSnapshot() = LockSnapshot(lockType, lockedBy, lockedAt)

private fun textSummary(value: String) = value.ifBlank { "—" }

private fun resourceSummary(value: ResourceValue): String {
    val adjustment = if (value.adjustment >= 0) "+${value.adjustment}" else value.adjustment.toString()
    return "${value.current}/${value.maximum} • ajuste $adjustment"
}

private fun attributeSummary(values: List<AttributeValue>) = values.joinToString(" • ") { attribute ->
    val skills = attribute.skills.filter { it.value != 0 || it.modifier != 0 }.joinToString { skill ->
        "${skill.name} ${skill.value}${signedModifier(skill.modifier)}"
    }
    "${attribute.acronym} ${attribute.value}${signedModifier(attribute.modifier)}" +
        skills.takeIf(String::isNotBlank)?.let { " [$it]" }.orEmpty()
}

private fun signedModifier(value: Int) = when {
    value > 0 -> "+$value"
    value < 0 -> value.toString()
    else -> ""
}

private fun mapSummary(values: Map<String, Int>) = values.entries.joinToString(" • ") { (name, value) ->
    "$name ${if (value >= 0) "+$value" else value}"
}

private fun stringListSummary(values: List<String>) = values.filter(String::isNotBlank).ifEmpty { listOf("—") }.joinToString(" • ")

private fun namedListSummary(values: List<SpecialKnowledge>) = namedListSummary(values) { it.name }

private fun <T> namedListSummary(values: List<T>, name: (T) -> String): String {
    if (values.isEmpty()) return "Nenhum"
    val names = values.map(name).filter(String::isNotBlank)
    return if (names.isEmpty()) "${values.size} registro(s)" else names.joinToString(" • ")
}

private fun lockSummary(value: LockSnapshot) = when (value.type) {
    CharacterLock.NONE -> "Sem bloqueio"
    CharacterLock.PLAYER -> "Bloqueada pelo jogador"
    CharacterLock.HISTORIAN -> "Bloqueada pelo historiador"
}
