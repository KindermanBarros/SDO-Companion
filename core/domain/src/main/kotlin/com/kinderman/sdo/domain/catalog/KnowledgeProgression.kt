package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.KnowledgeMilestoneReward
import com.kinderman.sdo.domain.model.KnowledgeMilestoneRewardType
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.withAddedAbility
import com.kinderman.sdo.domain.model.withAddedPower

data class InitialKnowledgeAllocation(
    val selections: List<SpecialKnowledge>,
    val distributedPoints: Int,
) {
    fun validate() {
        require(selections.size == 5) { "A criação exige exatamente 5 escolhas de Conhecimentos e Técnicas." }
        require(selections.all { it.value == 0 }) { "Cada escolha inicial deve começar gratuitamente no nível 0." }
        require(selections.all { it.attribute.isNotBlank() }) { "Cada escolha exige um Atributo base permanente." }
        require(distributedPoints == 15) { "Os 15 Pontos de Conhecimento devem ser gastos integralmente na criação." }
    }
}

fun Character.withKnowledgeLevel(knowledgeId: String, requestedLevel: Int, catalog: List<CatalogEntry>): Character {
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId } ?: return this
    val attributeLimit = permanentAttributeValue(current.attribute)
    val level = requestedLevel.coerceIn(0, minOf(5, attributeLimit))
    val reached = buildSet {
        addAll(current.milestoneLevels)
        if (level >= 3) add(3)
        if (level >= 5) add(5)
        if (current.isRunic() && level >= 1) add(1)
    }
    val updatedKnowledge = current.copy(value = level, milestoneLevels = reached.sorted())
    var result = copy(
        learnedKnowledges = learnedKnowledges.replaceKnowledge(updatedKnowledge),
        arcaneKnowledges = arcaneKnowledges.replaceKnowledge(updatedKnowledge),
        battleTechniques = battleTechniques.replaceKnowledge(updatedKnowledge),
    )
    if (current.isRunic()) {
        val newlyReached = reached - current.milestoneLevels.toSet()
        val granted = catalog.filter { entry ->
            entry.kind == CatalogKind.RUNE && entry.runePackage.isNotBlank() &&
                newlyReached.any { milestone -> entry.sourceLevel == when (milestone) { 1 -> 1; 3 -> 2; 5 -> 3; else -> -1 } }
        }
        granted.forEach { entry ->
            if (result.mysticAbilities.none { it.catalogEntryId == entry.id || it.name.equals(entry.name, true) }) {
                result = result.copy(
                    mysticAbilities = result.mysticAbilities + entry.toMysticAbility().copy(
                        canonicalSource = AbilitySource.KNOWLEDGE,
                        knowledgeId = current.id,
                        knowledgeLevel = level,
                    ),
                )
            }
        }
    }
    return result
}

/** Starts a level transition. Levels 3 and 5 are transactional: the value is only committed
 * after every crossed milestone has a persisted reward. */
fun Character.requestKnowledgeLevel(knowledgeId: String, requestedLevel: Int, catalog: List<CatalogEntry>): Character {
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId } ?: return this
    require(current.pendingMilestoneLevels.isEmpty()) { "Resolva a recompensa pendente antes de alterar novamente o nível." }
    val target = requestedLevel.coerceIn(0, 5)
    val pending = listOf(3, 5).filter { it > current.value && it <= target && current.milestoneRewards.none { reward -> reward.level == it } }
    if (pending.isEmpty()) return withKnowledgeLevel(knowledgeId, target, catalog)
    return replaceKnowledge(current.copy(pendingMilestoneLevels = pending, pendingTargetLevel = target))
}

fun Character.cancelKnowledgeLevelRequest(knowledgeId: String): Character {
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId } ?: return this
    return replaceKnowledge(current.copy(pendingMilestoneLevels = emptyList(), pendingTargetLevel = null))
}

fun Character.resolveKnowledgeMilestone(knowledgeId: String, rewardEntry: CatalogEntry, catalog: List<CatalogEntry>): Character {
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId }
        ?: error("Conhecimento do marco não encontrado.")
    val milestone = current.pendingMilestoneLevels.firstOrNull() ?: error("Não existe marco pendente.")
    require(rewardEntry in eligibleMilestoneRewards(current, catalog)) { "A recompensa não é permitida para este Conhecimento." }
    var result = this
    val record = when (rewardEntry.kind) {
        CatalogKind.POWER -> {
            val granted = rewardEntry.toStructuredPower(PowerSourceType.KNOWLEDGE, current.id)
            result = result.withAddedPower(granted)
            KnowledgeMilestoneReward(milestone, KnowledgeMilestoneRewardType.POWER, rewardEntry.id, granted.id)
        }
        CatalogKind.MAGIC, CatalogKind.RUNE -> {
            val granted = rewardEntry.toMysticAbility().copy(knowledgeId = current.id, knowledgeLevel = milestone)
            result = result.withAddedAbility(granted)
            KnowledgeMilestoneReward(milestone, KnowledgeMilestoneRewardType.MYSTIC_ABILITY, rewardEntry.id, granted.id)
        }
        CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.ARCANE_KNOWLEDGE, CatalogKind.BATTLE_TECHNIQUE -> {
            val parentKind = result.knowledgeKind(current, catalog)
            require(rewardEntry.kind == parentKind && rewardEntry.group.contains("especializa", true)) {
                "A especialização não pertence à categoria deste Conhecimento."
            }
            val granted = rewardEntry.toSpecialKnowledge().copy(
                value = 1, attribute = current.attribute, specializationParentId = current.id,
            )
            require(result.allSpecialKnowledges().none { it.catalogEntryId == granted.catalogEntryId && it.specializationParentId == current.id })
            result = when (parentKind) {
                CatalogKind.ACQUIRED_KNOWLEDGE -> result.copy(learnedKnowledges = result.learnedKnowledges + granted)
                CatalogKind.ARCANE_KNOWLEDGE -> result.copy(arcaneKnowledges = result.arcaneKnowledges + granted)
                else -> result.copy(battleTechniques = result.battleTechniques + granted)
            }
            KnowledgeMilestoneReward(milestone, KnowledgeMilestoneRewardType.SPECIALIZATION, rewardEntry.id, granted.id)
        }
        else -> error("Tipo de recompensa de marco não suportado.")
    }
    val afterGrant = result.allSpecialKnowledges().first { it.id == knowledgeId }
    val remaining = afterGrant.pendingMilestoneLevels.drop(1)
    val target = afterGrant.pendingTargetLevel ?: milestone
    val completed = afterGrant.copy(
        milestoneRewards = afterGrant.milestoneRewards + record,
        milestoneLevels = (afterGrant.milestoneLevels + milestone).distinct().sorted(),
        pendingMilestoneLevels = remaining,
        pendingTargetLevel = target.takeIf { remaining.isNotEmpty() },
    )
    result = result.replaceKnowledge(completed)
    return if (remaining.isEmpty()) result.withKnowledgeLevel(knowledgeId, target, catalog) else result
}

fun Character.resolveKnowledgeSpecialization(knowledgeId: String, name: String, catalog: List<CatalogEntry>): Character {
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId }
        ?: error("Conhecimento do marco não encontrado.")
    val milestone = current.pendingMilestoneLevels.firstOrNull() ?: error("Não existe marco pendente.")
    require(name.isNotBlank()) { "Informe o nome da especialização." }
    val kind = knowledgeKind(current, catalog)
    require(kind in setOf(CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.ARCANE_KNOWLEDGE, CatalogKind.BATTLE_TECHNIQUE))
    require(allSpecialKnowledges().none { it.name.equals(name.trim(), true) }) { "Já existe um Conhecimento com esse nome." }
    val specialization = SpecialKnowledge(
        name = name.trim(), attribute = current.attribute, value = 1,
        category = "Especialização", specializationParentId = current.id,
        source = "Marco de ${current.name} — nível $milestone",
    )
    var result = when (kind) {
        CatalogKind.ACQUIRED_KNOWLEDGE -> copy(learnedKnowledges = learnedKnowledges + specialization)
        CatalogKind.ARCANE_KNOWLEDGE -> copy(arcaneKnowledges = arcaneKnowledges + specialization)
        else -> copy(battleTechniques = battleTechniques + specialization)
    }
    val afterGrant = result.allSpecialKnowledges().first { it.id == knowledgeId }
    val remaining = afterGrant.pendingMilestoneLevels.drop(1)
    val target = afterGrant.pendingTargetLevel ?: milestone
    result = result.replaceKnowledge(afterGrant.copy(
        milestoneRewards = afterGrant.milestoneRewards + KnowledgeMilestoneReward(
            milestone, KnowledgeMilestoneRewardType.SPECIALIZATION, "specialization:${specialization.id}", specialization.id,
        ),
        milestoneLevels = (afterGrant.milestoneLevels + milestone).distinct().sorted(),
        pendingMilestoneLevels = remaining,
        pendingTargetLevel = target.takeIf { remaining.isNotEmpty() },
    ))
    return if (remaining.isEmpty()) result.withKnowledgeLevel(knowledgeId, target, catalog) else result
}

fun eligibleMilestoneRewards(knowledge: SpecialKnowledge, catalog: List<CatalogEntry>): List<CatalogEntry> {
    val canonicalName = catalog.firstOrNull { it.id == knowledge.catalogEntryId }?.name ?: knowledge.name
    val allowedKinds = when (catalogKnowledgeKind(knowledge, catalog)) {
        CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.BATTLE_TECHNIQUE -> setOf(CatalogKind.POWER)
        CatalogKind.ARCANE_KNOWLEDGE -> setOf(CatalogKind.POWER, CatalogKind.MAGIC, CatalogKind.RUNE)
        else -> emptySet()
    }
    return catalog.filter {
        (it.kind in allowedKinds && it.sourceKnowledge.equals(canonicalName, true)) ||
            (it.kind == catalogKnowledgeKind(knowledge, catalog) && it.group.contains("especializa", true))
    }.distinctBy(CatalogEntry::id)
}

private fun catalogKnowledgeKind(knowledge: SpecialKnowledge, catalog: List<CatalogEntry>): CatalogKind? =
    catalog.firstOrNull { it.id == knowledge.catalogEntryId }?.kind
        ?: runCatching { CatalogKind.valueOf(knowledge.category) }.getOrNull()

private fun Character.knowledgeKind(knowledge: SpecialKnowledge, catalog: List<CatalogEntry>): CatalogKind? = when {
    learnedKnowledges.any { it.id == knowledge.id } -> CatalogKind.ACQUIRED_KNOWLEDGE
    arcaneKnowledges.any { it.id == knowledge.id } -> CatalogKind.ARCANE_KNOWLEDGE
    battleTechniques.any { it.id == knowledge.id } -> CatalogKind.BATTLE_TECHNIQUE
    else -> catalogKnowledgeKind(knowledge, catalog)
}

fun Character.withKnowledgeMilestoneReward(
    knowledgeId: String,
    reward: KnowledgeMilestoneReward,
): Character {
    require(reward.level == 3 || reward.level == 5) { "Marcos de Conhecimento válidos existem apenas nos níveis 3 e 5." }
    require(reward.rewardCatalogId.isNotBlank() && reward.grantedEntityId.isNotBlank()) { "A recompensa do marco precisa estar vinculada ao catálogo e ao registro concedido." }
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId }
        ?: error("Conhecimento do marco não encontrado.")
    require(current.value >= reward.level) { "O Conhecimento ainda não atingiu o nível desta recompensa." }
    require(current.milestoneRewards.none { it.level == reward.level }) { "Este marco já possui uma recompensa registrada." }
    val updated = current.copy(milestoneRewards = current.milestoneRewards + reward)
    return copy(
        learnedKnowledges = learnedKnowledges.replaceKnowledge(updated),
        arcaneKnowledges = arcaneKnowledges.replaceKnowledge(updated),
        battleTechniques = battleTechniques.replaceKnowledge(updated),
    )
}

private fun Character.replaceKnowledge(value: SpecialKnowledge) = copy(
    learnedKnowledges = learnedKnowledges.replaceKnowledge(value),
    arcaneKnowledges = arcaneKnowledges.replaceKnowledge(value),
    battleTechniques = battleTechniques.replaceKnowledge(value),
)

private fun Character.allSpecialKnowledges() = learnedKnowledges + arcaneKnowledges + battleTechniques
private fun SpecialKnowledge.isRunic() = name.equals("Rúnico", true) || name.equals("Runas", true)
private fun List<SpecialKnowledge>.replaceKnowledge(value: SpecialKnowledge) = map { if (it.id == value.id) value else it }
