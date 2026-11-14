package com.kinderman.sdo.domain.model

import com.kinderman.sdo.domain.catalog.withNormalizedInventory

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

const val MAX_AUTOMATIC_CONFLICT_FIELDS = 5

/** Resolve every conflict field by its own edit time, regardless of account or field count.
 * Legacy documents without field metadata retain their document-time fallback.
 * Exact ties keep the committed remote value so clients converge instead of oscillating.
 */
fun automaticRemoteFieldIds(conflict: CharacterSyncConflict): Set<String> {
    fun editTime(character: Character, fieldId: String, fallback: Long): Long {
        val group = if (fieldId == "inventory" || fieldId == "itemStates") listOf("inventory", "itemStates") else listOf(fieldId)
        return group.maxOf { id ->
            character.fieldUpdatedAt[id] ?: if (character.fieldUpdatedAt.isEmpty()) fallback else 0L
        }
    }
    return conflict.fields.filter { field ->
        editTime(conflict.remote, field.id, conflict.remoteUpdatedAt) >=
            editTime(conflict.local, field.id, conflict.local.updatedAt)
    }.mapTo(mutableSetOf(), CharacterConflictField::id)
}

@Suppress("UNUSED_PARAMETER")
fun automaticRemoteFieldIds(conflict: CharacterSyncConflict, sessionUid: String): Set<String> =
    automaticRemoteFieldIds(conflict)

/** Stamp only edited conflict groups; preserve timestamps of untouched fields. */
fun Character.withFieldEditTimes(previous: Character?, at: Long = updatedAt): Character {
    val baseline = previous ?: Character(id = id, updatedAt = 0, dirty = false)
    val inherited = if (baseline.fieldUpdatedAt.isNotEmpty()) baseline.fieldUpdatedAt else
        characterConflictFields(Character(id = id, updatedAt = 0, dirty = false), baseline)
            .associate { it.id to baseline.updatedAt }
    val changed = characterConflictFields(baseline, this)
    // Membership removal has a narrowly-scoped backend contract; retain existing metadata.
    if (previous != null && campaignId.isBlank() && changed.map { it.id }.toSet() == setOf("campaignId")) {
        return copy(fieldUpdatedAt = previous.fieldUpdatedAt)
    }
    val times = inherited + fieldUpdatedAt
    val editAt = if (changed.isEmpty()) at else maxOf(at, (previous?.updatedAt ?: 0L) + 1)
    val updatedTimes = times + changed.associate { field ->
        field.id to maxOf(editAt, (times[field.id] ?: 0L) + 1)
    }
    return copy(fieldUpdatedAt = updatedTimes, updatedAt = maxOf(at, updatedTimes.values.maxOrNull() ?: 0L))
}

fun characterConflictFields(local: Character, remote: Character): List<CharacterConflictField> = buildList {
    fun <T> changed(id: String, label: String, localValue: T, remoteValue: T, summary: (T) -> String = { it.toString() }) {
        if (localValue != remoteValue) {
            add(CharacterConflictField(id, label, summary(localValue), summary(remoteValue)))
        }
    }

    changed("ownerId", "Proprietário", local.ownerId, remote.ownerId, ::textSummary)
    changed("canonicalSchemaVersion", "Versão do contrato", local.canonicalSchemaVersion, remote.canonicalSchemaVersion)
    changed("migrationReviews", "Pendências de migração", local.migrationReviews, remote.migrationReviews) { "${it.size} pendência(s)" }
    changed("campaignId", "Campanha", local.campaignId, remote.campaignId, ::textSummary)
    changed("name", "Nome", local.name, remote.name, ::textSummary)
    changed("race", "Raça", local.race, remote.race, ::textSummary)
    changed("subRace", "Sub-raça", local.subRace, remote.subRace, ::textSummary)
    changed("raceAttribute", "Atributo racial", local.raceAttribute, remote.raceAttribute, ::textSummary)
    changed("occupation", "Ocupação", local.occupation, remote.occupation, ::textSummary)
    changed("height", "Altura", local.height, remote.height, ::textSummary)
    changed("age", "Idade", local.age, remote.age, ::textSummary)
    changed("sex", "Gênero", local.sex, remote.sex, ::textSummary)
    changed("level", "Nível", local.level, remote.level)
    changed("size", "size", local.size, remote.size)
    changed("creationStatus", "creationStatus", local.creationStatus, remote.creationStatus)
    changed("creationStep", "creationStep", local.creationStep, remote.creationStep)
    changed("creationCompletedAt", "creationCompletedAt", local.creationCompletedAt, remote.creationCompletedAt)
    changed("creationRulesVersion", "creationRulesVersion", local.creationRulesVersion, remote.creationRulesVersion)
    changed("itemSchemaVersion", "itemSchemaVersion", local.itemSchemaVersion, remote.itemSchemaVersion)
    changed("progressionLifeBonus", "progressionLifeBonus", local.progressionLifeBonus, remote.progressionLifeBonus)
    changed("progressionSanityBonus", "progressionSanityBonus", local.progressionSanityBonus, remote.progressionSanityBonus)
    changed("progressionArcaneBonus", "progressionArcaneBonus", local.progressionArcaneBonus, remote.progressionArcaneBonus)
    changed("progressionEnergyBonus", "progressionEnergyBonus", local.progressionEnergyBonus, remote.progressionEnergyBonus)
    changed("progressionHistory", "progressionHistory", local.progressionHistory, remote.progressionHistory)
    changed("itemCreationDraft", "itemCreationDraft", local.itemCreationDraft, remote.itemCreationDraft)
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
    if (local.abilities != remote.abilities || local.powers != remote.powers || local.mysticAbilities != remote.mysticAbilities) {
        val localAbilities = local.abilities.ifEmpty { local.allCanonicalAbilitiesSafely() }
        val remoteAbilities = remote.abilities.ifEmpty { remote.allCanonicalAbilitiesSafely() }
        add(CharacterConflictField(
            id = "abilities",
            label = "Habilidades",
            localSummary = localAbilities.takeIf { it.isNotEmpty() }?.let { namedListSummary(it) { ability -> ability.name } }
                ?: "Poderes: ${namedListSummary(local.powers) { it.name }}; místicas: ${namedListSummary(local.mysticAbilities) { it.name }}",
            remoteSummary = remoteAbilities.takeIf { it.isNotEmpty() }?.let { namedListSummary(it) { ability -> ability.name } }
                ?: "Poderes: ${namedListSummary(remote.powers) { it.name }}; místicas: ${namedListSummary(remote.mysticAbilities) { it.name }}",
        ))
    }
    changed("inventory", "Inventário", local.inventory, remote.inventory) { values -> namedListSummary(values) { it.name } }
    changed("itemStates", "Estado canônico do inventário", local.itemStates, remote.itemStates) { "${it.size} item(ns)" }
    changed("customItemCatalog", "Catálogo personalizado", local.customItemCatalog, remote.customItemCatalog) { "${it.size} entrada(s)" }
    changed("progression", "Escolhas auditáveis", local.progression, remote.progression) { "${it.choices.size} escolha(s)" }
    changed("bodyState", "Estado corporal", Triple(local.bodyState, local.bodyRegions, local.organs),
        Triple(remote.bodyState, remote.bodyRegions, remote.organs)) { value ->
        "${value.second.size} regions / ${value.third.size} organs"
    }
    changed("agilityLimit", "Limitação de Agilidade", local.agilityLimit, remote.agilityLimit, ::textSummary)
    changed("conditionInstances", "Condições", local.conditionInstances to local.conditions,
        remote.conditionInstances to remote.conditions) { values -> "${values.first.size} conditions" }
    changed("activeModifiers", "Modificadores ativos", local.activeModifiers, remote.activeModifiers) { "${it.size} modificador(es)" }
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
        fieldUpdatedAt = (local.fieldUpdatedAt.keys + remote.fieldUpdatedAt.keys).associateWith { id ->
            maxOf(local.fieldUpdatedAt[id] ?: 0L, remote.fieldUpdatedAt[id] ?: 0L)
        },
        canonicalSchemaVersion = selected("canonicalSchemaVersion", local.canonicalSchemaVersion, remote.canonicalSchemaVersion),
        migrationReviews = selected("migrationReviews", local.migrationReviews, remote.migrationReviews),
        ownerId = selected("ownerId", local.ownerId, remote.ownerId),
        campaignId = selected("campaignId", local.campaignId, remote.campaignId),
        name = selected("name", local.name, remote.name),
        race = selected("race", local.race, remote.race),
        subRace = selected("subRace", local.subRace, remote.subRace),
        raceAttribute = selected("raceAttribute", local.raceAttribute, remote.raceAttribute),
        occupation = selected("occupation", local.occupation, remote.occupation),
        height = selected("height", local.height, remote.height),
        age = selected("age", local.age, remote.age),
        sex = selected("sex", local.sex, remote.sex),
        size = selected("size", local.size, remote.size),
        creationStatus = selected("creationStatus", local.creationStatus, remote.creationStatus),
        creationStep = selected("creationStep", local.creationStep, remote.creationStep),
        creationCompletedAt = selected("creationCompletedAt", local.creationCompletedAt, remote.creationCompletedAt),
        creationRulesVersion = selected("creationRulesVersion", local.creationRulesVersion, remote.creationRulesVersion),
        itemSchemaVersion = selected("itemSchemaVersion", local.itemSchemaVersion, remote.itemSchemaVersion),
        progressionLifeBonus = selected("progressionLifeBonus", local.progressionLifeBonus, remote.progressionLifeBonus),
        progressionSanityBonus = selected("progressionSanityBonus", local.progressionSanityBonus, remote.progressionSanityBonus),
        progressionArcaneBonus = selected("progressionArcaneBonus", local.progressionArcaneBonus, remote.progressionArcaneBonus),
        progressionEnergyBonus = selected("progressionEnergyBonus", local.progressionEnergyBonus, remote.progressionEnergyBonus),
        progressionHistory = selected("progressionHistory", local.progressionHistory, remote.progressionHistory),
        itemCreationDraft = selected("itemCreationDraft", local.itemCreationDraft, remote.itemCreationDraft),

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
        abilities = selected("abilities", local.abilities, remote.abilities),
        powers = if ("abilities" in remoteFieldIds) remote.powers else local.powers,
        mysticAbilities = if ("abilities" in remoteFieldIds) remote.mysticAbilities else local.mysticAbilities,
        inventory = selected("inventory", local.inventory, remote.inventory),
        itemStates = selected("itemStates", local.itemStates, remote.itemStates),
        customItemCatalog = selected("customItemCatalog", local.customItemCatalog, remote.customItemCatalog),
        progression = selected("progression", local.progression, remote.progression),
        bodyState = selected("bodyState", local.bodyState, remote.bodyState),
        bodyRegions = if ("bodyState" in remoteFieldIds) remote.bodyRegions else local.bodyRegions,
        agilityLimit = selected("agilityLimit", local.agilityLimit, remote.agilityLimit),
        organs = if ("bodyState" in remoteFieldIds) remote.organs else local.organs,
        conditionInstances = selected("conditionInstances", local.conditionInstances, remote.conditionInstances),
        conditions = if ("conditionInstances" in remoteFieldIds) remote.conditions else local.conditions,
        activeModifiers = selected("activeModifiers", local.activeModifiers, remote.activeModifiers),
        story = selected("story", local.story, remote.story),
        notes = remote.notes,
        personalNotes = selected("personalNotes", local.personalNotes, remote.personalNotes),
        lockType = lock.type,
        lockedBy = lock.by,
        lockedAt = lock.at,
        appliedDeliveryIds = (local.appliedDeliveryIds + remote.appliedDeliveryIds).distinct(),
        deleted = false,
    ).withNormalizedInventory().synchronizeItemPowers()
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
