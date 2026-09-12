package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.initialCreationCost

enum class CharacterCreationErrorCode {
    IDENTITY_INCOMPLETE,
    ATTRIBUTE_BUDGET,
    ATTRIBUTE_VALUE_INVALID,
    SPECIAL_KNOWLEDGE_COUNT,
    SPECIAL_KNOWLEDGE_INCOMPLETE,
    SPECIAL_KNOWLEDGE_DUPLICATE,
    KNOWLEDGE_BUDGET,
    KNOWLEDGE_LIMIT,
    MILESTONE_REWARD_MISSING,
    PATH_INCOMPLETE,
    HERITAGE_BUDGET,
    HERITAGE_ITEM_INVALID,
}

data class CharacterCreationError(
    val step: Int,
    val code: CharacterCreationErrorCode,
    val message: String,
)

object CharacterCreation {
    const val STEP_COUNT = 8
    const val SPECIAL_KNOWLEDGE_CHOICES = 5
    const val KNOWLEDGE_POINTS = 15

    fun specialKnowledges(character: Character) = character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques

    fun initialSpecialKnowledges(character: Character) = specialKnowledges(character).filter { it.specializationParentId.isBlank() }

    fun knowledgePointsSpent(character: Character): Int =
        character.attributes.sumOf { attribute -> attribute.skills.sumOf { it.value.coerceAtLeast(0) } } +
            specialKnowledges(character).sumOf { knowledge ->
                (knowledge.value - if (knowledge.specializationParentId.isNotBlank()) 1 else 0).coerceAtLeast(0)
            }

    fun attributePointsSpent(character: Character): Int =
        character.attributes.sumOf { it.value.coerceAtLeast(0) }

    fun heritageSpent(character: Character): Int = if (!character.isInCreation) 0 else character.inventory
        .filter { it.acquisitionSource == com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE }
        .sumOf { it.initialCreationCost() }

    fun validate(character: Character): List<CharacterCreationError> =
        (1 until STEP_COUNT).flatMap { validateStep(it, character) }

    fun validateStep(step: Int, character: Character): List<CharacterCreationError> = buildList {
        fun error(code: CharacterCreationErrorCode, message: String) = add(CharacterCreationError(step, code, message))
        when (step) {
            1 -> if (character.name.isBlank() || character.race.isBlank()) {
                error(CharacterCreationErrorCode.IDENTITY_INCOMPLETE, "Informe o nome e selecione a raça.")
            }
            2 -> {
                if (character.attributes.any { it.value !in 0..5 }) {
                    error(CharacterCreationErrorCode.ATTRIBUTE_VALUE_INVALID, "Atributos devem permanecer entre 0 e 5 durante a criação.")
                }
                if (attributePointsSpent(character) != 10) {
                    error(CharacterCreationErrorCode.ATTRIBUTE_BUDGET, "Distribua exatamente 10 pontos-base entre os Atributos; bônus raciais não consomem esse orçamento.")
                }
            }
            3 -> {
                val knowledges = initialSpecialKnowledges(character)
                if (knowledges.size != SPECIAL_KNOWLEDGE_CHOICES) {
                    error(CharacterCreationErrorCode.SPECIAL_KNOWLEDGE_COUNT, "Escolha exatamente 5 Conhecimentos Especiais. Eles começam gratuitamente no nível 0.")
                }
                if (knowledges.any { it.name.isBlank() || it.attribute.isBlank() || character.attributes.none { attribute -> attribute.acronym.equals(it.attribute, true) } }) {
                    error(CharacterCreationErrorCode.SPECIAL_KNOWLEDGE_INCOMPLETE, "Todo Conhecimento Especial precisa de nome e Atributo relacionado válidos.")
                }
                val keys = knowledges.map { it.catalogEntryId.ifBlank { it.name.trim().lowercase() } }.filter(String::isNotBlank)
                if (keys.size != keys.distinct().size) {
                    error(CharacterCreationErrorCode.SPECIAL_KNOWLEDGE_DUPLICATE, "Os 5 Conhecimentos Especiais devem ser únicos.")
                }
            }
            4 -> {
                if (knowledgePointsSpent(character) != KNOWLEDGE_POINTS) {
                    error(CharacterCreationErrorCode.KNOWLEDGE_BUDGET, "Distribua exatamente 15 pontos entre qualquer Conhecimento.")
                }
                val basicAboveLimit = character.attributes.any { attribute -> attribute.skills.any { it.value !in 0..5 } }
                val specialAboveLimit = specialKnowledges(character).any { knowledge -> knowledge.value !in 0..5 }
                if (basicAboveLimit || specialAboveLimit) {
                    error(CharacterCreationErrorCode.KNOWLEDGE_LIMIT, "Valores-base de Conhecimento devem permanecer entre 0 e 5; bônus narrativos são aplicados separadamente.")
                }
                val missingReward = specialKnowledges(character).any { knowledge ->
                    listOf(3, 5).any { level ->
                        knowledge.value >= level && knowledge.milestoneRewards.none { it.level == level && it.rewardCatalogId.isNotBlank() && it.grantedEntityId.isNotBlank() }
                    }
                }
                if (missingReward) {
                    error(CharacterCreationErrorCode.MILESTONE_REWARD_MISSING, "Escolha e confirme todas as recompensas obrigatórias dos níveis 3 e 5.")
                }
            }
            5 -> if (character.pathName.isBlank() || character.pathPillars.size != 3 || character.pathPillars.any(String::isBlank)) {
                error(CharacterCreationErrorCode.PATH_INCOMPLETE, "Selecione o Caminho e confirme seus 3 Pilares.")
            }
            6 -> if (
                character.pathName.isBlank() ||
                character.pathPillars.size != 3 ||
                character.pathPillars.any(String::isBlank) ||
                character.powers.count { it.sourceType == PowerSourceType.PATH } != 2
            ) {
                error(CharacterCreationErrorCode.PATH_INCOMPLETE, "Defina o Caminho, os 3 Pilares e exatamente 2 Poderes iniciais.")
            }
            7 -> {
                val heritageItems = character.inventory.filter { it.acquisitionSource == ItemAcquisitionSource.HERITAGE }
                if (heritageItems.any { it.heritageCost == null || it.heritageCost < 0 }) {
                    error(CharacterCreationErrorCode.HERITAGE_ITEM_INVALID, "Todo item de Herança precisa possuir custo de PH válido.")
                }
                if (heritageSpent(character) != ItemCreationRules.HERITAGE_BUDGET) {
                    error(CharacterCreationErrorCode.HERITAGE_BUDGET, "Use exatamente os ${ItemCreationRules.HERITAGE_BUDGET} Pontos de Herança antes de continuar.")
                }
            }
        }
    }

    fun stepError(step: Int, character: Character): String? = validateStep(step, character).firstOrNull()?.message

    fun flowError(step: Int, character: Character): String? =
        (1..step.coerceAtMost(STEP_COUNT - 1)).firstNotNullOfOrNull { validateStep(it, character).firstOrNull()?.message }

    fun finish(character: Character, now: Long = System.currentTimeMillis()): Character {
        val errors = validate(character)
        require(errors.isEmpty()) { errors.first().message }
        val startingItems = buildList {
            if (character.inventory.none { it.name.equals("Roupas simples", true) }) add(InventoryItem(name = "Roupas simples", category = "Item", quantity = 1, acquisitionSource = ItemAcquisitionSource.NARRATIVE))
            if (character.inventory.none { it.name.equals("Objeto pessoal", true) }) add(InventoryItem(name = "Objeto pessoal", category = "Item", effect = "Sem efeito mecânico.", quantity = 1, acquisitionSource = ItemAcquisitionSource.NARRATIVE))
        }
        return character.copy(
            creationStatus = CharacterCreationStatus.COMPLETED,
            creationStep = STEP_COUNT,
            creationCompletedAt = now,
            creationRulesVersion = maxOf(character.creationRulesVersion, 2),
            itemCreationDraft = null,
            money = character.money.coerceAtLeast(100),
            inventory = character.inventory + startingItems,
            life = character.life.copy(current = character.lifeMaximum, maximum = character.lifeMaximum),
            sanity = character.sanity.copy(current = character.sanityMaximum, maximum = character.sanityMaximum),
            arcane = character.arcane.copy(current = character.arcaneMaximum, maximum = character.arcaneMaximum),
            energy = character.energy.copy(current = character.energyMaximum, maximum = character.energyMaximum),
        )
    }
}
