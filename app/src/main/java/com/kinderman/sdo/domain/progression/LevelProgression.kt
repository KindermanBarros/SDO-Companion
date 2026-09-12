package com.kinderman.sdo.domain.progression

import com.kinderman.sdo.domain.catalog.toSpecialKnowledge
import com.kinderman.sdo.domain.catalog.toStructuredPower
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ProgressionReward
import com.kinderman.sdo.domain.model.ProgressionRewardType
import com.kinderman.sdo.domain.model.basicKnowledgeId
import com.kinderman.sdo.domain.model.AuditableChoice
import com.kinderman.sdo.domain.model.CatalogEntryId
import com.kinderman.sdo.domain.model.CatalogReference
import com.kinderman.sdo.domain.model.ChoiceKind
import com.kinderman.sdo.domain.model.EntityId
import com.kinderman.sdo.domain.model.NarrativeSourceId
import com.kinderman.sdo.domain.model.SourceRef

object LevelProgression {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 20

    fun levelsBetween(current: Int, target: Int): List<Int> {
        require(current in MIN_LEVEL..MAX_LEVEL && target in MIN_LEVEL..MAX_LEVEL)
        require(target > current)
        return ((current + 1)..target).toList()
    }

    fun apply(character: Character, target: Int, rewards: List<ProgressionReward>, catalog: List<CatalogEntry>, now: Long = System.currentTimeMillis()): Character {
        val levels = levelsBetween(character.level, target)
        require(rewards.all { it.level in levels })
        levels.forEach { level ->
            require(rewards.count { it.level == level && it.type == ProgressionRewardType.KNOWLEDGE } == 1) { "Escolha um Conhecimento no nível $level." }
            require(rewards.count { it.level == level && it.type == ProgressionRewardType.RESOURCE } == 1) { "Escolha Arcano ou Energia no nível $level." }
            require(rewards.count { it.level == level && it.type == ProgressionRewardType.ATTRIBUTE } == (if (level % 2 == 0) 1 + if (level % 5 == 0) 1 else 0 else if (level % 5 == 0) 1 else 0))
            if (level % 5 == 0) {
                require(rewards.count { it.level == level && it.type == ProgressionRewardType.NEW_KNOWLEDGE } == 1)
                require(rewards.count { it.level == level && it.type == ProgressionRewardType.PATH_POWER } == 1)
            }
        }
        require(rewards.all { it.stableId.isNotBlank() }) { "Toda recompensa precisa de um vínculo por ID." }
        val alreadyApplied = character.progressionHistory.flatMap { it.rewards }.map { Triple(it.level, it.type, it.stableId) }.toSet()
        require(rewards.none { Triple(it.level, it.type, it.stableId) in alreadyApplied }) { "Esta recompensa já foi aplicada." }

        var result = character
        levels.forEach { level ->
            result = result.copy(
                progressionLifeBonus = result.progressionLifeBonus + 1 + result.vitalityValue(),
                progressionSanityBonus = result.progressionSanityBonus + 1,
            )
            rewards.filter { it.level == level }.forEach { reward -> result = result.applyReward(reward, catalog) }
            if (level % 5 == 0) result = result.copy(progressionEnergyBonus = result.progressionEnergyBonus + 1)
        }
        val automaticRewards = levels.flatMap { level -> buildList {
            add(ProgressionReward(level, ProgressionRewardType.RESOURCE, "LIFE"))
            add(ProgressionReward(level, ProgressionRewardType.RESOURCE, "SANITY"))
            if (level % 5 == 0) add(ProgressionReward(level, ProgressionRewardType.RESOURCE, "ENERGY_MILESTONE"))
        } }
        val record = ProgressionRecord(previousLevel = character.level, newLevel = target, rewards = rewards + automaticRewards, appliedAt = now)
        val auditChoice = AuditableChoice(
            id = EntityId("progression:${record.id}"),
            kind = ChoiceKind.PROGRESSION,
            option = CatalogReference(CatalogEntryId("progression:${record.previousLevel}-${record.newLevel}"), 1),
            source = SourceRef.Narrative(NarrativeSourceId("level-progression")),
            grantedEntityIds = record.rewards.mapIndexed { index, reward ->
                EntityId("progression:${record.id}:$index:${reward.type}:${reward.targetId}:${reward.catalogEntryId}")
            },
            chosenAt = now,
        )
        return result.copy(
            level = target,
            progressionHistory = result.progressionHistory + record,
            progression = result.progression.register(auditChoice),
        )
    }
}

private fun Character.applyReward(reward: ProgressionReward, catalog: List<CatalogEntry>): Character = when (reward.type) {
    ProgressionRewardType.RESOURCE -> when (reward.targetId) {
        "ARCANE" -> copy(progressionArcaneBonus = progressionArcaneBonus + 1)
        "ENERGY" -> copy(progressionEnergyBonus = progressionEnergyBonus + 1)
        else -> error("Recurso inválido.")
    }
    ProgressionRewardType.ATTRIBUTE -> copy(attributes = attributes.increment(reward.targetId))
    ProgressionRewardType.KNOWLEDGE -> incrementKnowledge(reward.targetId)
    ProgressionRewardType.NEW_KNOWLEDGE -> {
        val entry = catalog.single { it.id == reward.catalogEntryId && it.kind in knowledgeKinds }
        require((learnedKnowledges + arcaneKnowledges + battleTechniques).none { it.catalogEntryId == entry.id })
        val knowledge = entry.toSpecialKnowledge().copy(value = 1)
        when (entry.kind) {
            CatalogKind.ACQUIRED_KNOWLEDGE -> copy(learnedKnowledges = learnedKnowledges + knowledge)
            CatalogKind.ARCANE_KNOWLEDGE -> copy(arcaneKnowledges = arcaneKnowledges + knowledge)
            else -> copy(battleTechniques = battleTechniques + knowledge)
        }
    }
    ProgressionRewardType.PATH_POWER -> {
        val entry = catalog.single { it.id == reward.catalogEntryId && it.kind == CatalogKind.POWER }
        require(powers.none { it.catalogEntryId == entry.id })
        copy(powers = powers + entry.toStructuredPower(PowerSourceType.PATH, pathName))
    }
}

private val knowledgeKinds = setOf(CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.ARCANE_KNOWLEDGE, CatalogKind.BATTLE_TECHNIQUE)

private val ProgressionReward.stableId: String
    get() = when (type) {
        ProgressionRewardType.NEW_KNOWLEDGE, ProgressionRewardType.PATH_POWER -> catalogEntryId
        else -> targetId
    }

private fun List<AttributeValue>.increment(acronym: String): List<AttributeValue> = map {
    if (it.acronym == acronym) it.copy(value = (it.value + 1).coerceAtMost(10)) else it
}.also { require(it.any { value -> value.acronym == acronym }) }

private fun Character.incrementKnowledge(id: String): Character {
    fun update(values: List<com.kinderman.sdo.domain.model.SpecialKnowledge>) = values.map {
        if (it.id == id) it.copy(value = (it.value + 1).coerceAtMost(5)) else it
    }
    val basic = attributes.any { attribute -> attribute.skills.any { basicKnowledgeId(attribute.acronym, it.name) == id && it.value < 5 } }
    if (basic) return copy(attributes = attributes.map { attribute ->
        attribute.copy(skills = attribute.skills.map { skill ->
            if (basicKnowledgeId(attribute.acronym, skill.name) == id) skill.copy(value = skill.value + 1) else skill
        })
    })
    require((learnedKnowledges + arcaneKnowledges + battleTechniques).any { it.id == id && it.value < 5 }) { "Conhecimento inválido ou no máximo." }
    return copy(learnedKnowledges = update(learnedKnowledges), arcaneKnowledges = update(arcaneKnowledges), battleTechniques = update(battleTechniques))
}

private fun Character.vitalityValue(): Int = attributes.firstOrNull { it.acronym == "VIG" }
    ?.skills?.firstOrNull { it.name == "Vitalidade" }?.let { it.value + it.modifier }?.coerceAtLeast(0) ?: 0
