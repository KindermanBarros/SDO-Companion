package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.initialCreationCost

object CharacterCreation {
    const val STEP_COUNT = 13
    const val SPECIAL_KNOWLEDGE_CHOICES = 5
    const val KNOWLEDGE_POINTS = 15

    fun specialKnowledges(character: Character) = character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques

    fun knowledgePointsSpent(character: Character): Int =
        character.attributes.sumOf { attribute -> attribute.skills.sumOf { it.value.coerceAtLeast(0) } } +
            specialKnowledges(character).sumOf { (it.value - 1).coerceAtLeast(0) }

    fun attributePointsSpent(character: Character): Int = character.attributes.sumOf { it.value }

    fun heritageSpent(character: Character): Int = character.inventory.sumOf { it.initialCreationCost() }

    fun stepError(step: Int, character: Character): String? = when (step) {
        1 -> "Informe o nome e selecione a raça.".takeIf { character.name.isBlank() || character.race.isBlank() }
        2 -> "Distribua exatamente 10 pontos entre os Atributos.".takeIf { attributePointsSpent(character) != 10 }
        3 -> "Escolha exatamente 5 Conhecimentos Especiais, todos no nível mínimo 1.".takeIf {
            specialKnowledges(character).size != SPECIAL_KNOWLEDGE_CHOICES || specialKnowledges(character).any { it.value < 1 }
        }
        4 -> "Distribua exatamente 15 pontos entre qualquer Conhecimento.".takeIf { knowledgePointsSpent(character) != KNOWLEDGE_POINTS }
        9 -> "Defina o Caminho, os 3 Pilares e os 2 Poderes iniciais.".takeIf {
            character.pathName.isBlank() || character.pathPillars.count(String::isNotBlank) != 3 || character.powers.count { it.sourceType == PowerSourceType.PATH } < 2
        }
        10 -> "O equipamento inicial ultrapassa os ${ItemCreationRules.HERITAGE_BUDGET} PH.".takeIf { heritageSpent(character) > ItemCreationRules.HERITAGE_BUDGET }
        12 -> "Escolha um Traço Positivo e um Traço Negativo.".takeIf { character.positiveTraits.none(String::isNotBlank) || character.negativeTraits.none(String::isNotBlank) }
        else -> null
    }

    fun finish(character: Character, now: Long = System.currentTimeMillis()): Character {
        val error = (1..12).firstNotNullOfOrNull { stepError(it, character) }
        require(error == null) { error.orEmpty() }
        val startingItems = buildList {
            if (character.inventory.none { it.name.equals("Roupas simples", true) }) add(InventoryItem(name = "Roupas simples", category = "Item", quantity = 1, acquisitionSource = ItemAcquisitionSource.NARRATIVE))
            if (character.inventory.none { it.name.equals("Objeto pessoal", true) }) add(InventoryItem(name = "Objeto pessoal", category = "Item", effect = "Sem efeito mecânico.", quantity = 1, acquisitionSource = ItemAcquisitionSource.NARRATIVE))
        }
        return character.copy(
            creationStatus = CharacterCreationStatus.COMPLETED,
            creationStep = STEP_COUNT,
            creationCompletedAt = now,
            money = character.money.coerceAtLeast(100),
            inventory = character.inventory + startingItems,
            life = character.life.copy(current = character.lifeMaximum, maximum = character.lifeMaximum),
            sanity = character.sanity.copy(current = character.sanityMaximum, maximum = character.sanityMaximum),
            arcane = character.arcane.copy(current = character.arcaneMaximum, maximum = character.arcaneMaximum),
            energy = character.energy.copy(current = character.energyMaximum, maximum = character.energyMaximum),
        )
    }
}
