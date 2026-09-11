package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge

private val runicPackages = linkedMapOf(
    1 to listOf("Centelha", "Solo Firme", "Frescor", "Mensagem de Eco"),
    3 to listOf("Armadilha de Geada", "Selo de Estabilização", "Ruptura Harmônica", "Cerca Espinhosa"),
    5 to listOf("Memória da Rocha", "Santuário Regenerativo", "Arquivo Inexistente", "Âncora Onírica"),
)

data class InitialKnowledgeAllocation(
    val selections: List<SpecialKnowledge>,
    val distributedPoints: Int,
) {
    fun validate() {
        require(selections.size == 5) { "A criação exige exatamente 5 escolhas de Conhecimentos e Técnicas." }
        require(selections.all { it.value == 1 }) { "Cada escolha inicial deve começar no nível 1." }
        require(selections.all { it.attribute.isNotBlank() }) { "Cada escolha exige um Atributo base permanente." }
        require(distributedPoints == 15) { "Os 15 Pontos de Conhecimento devem ser gastos integralmente na criação." }
    }
}

fun Character.withKnowledgeLevel(knowledgeId: String, requestedLevel: Int, catalog: List<CatalogEntry>): Character {
    val current = allSpecialKnowledges().firstOrNull { it.id == knowledgeId } ?: return this
    val attributeLimit = attributes.firstOrNull { it.acronym.equals(current.attribute, true) }
        ?.value?.coerceAtLeast(0) ?: 0
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
        val grantedNames = newlyReached.flatMap { runicPackages[it].orEmpty() }
        val granted = catalog.filter { it.kind == CatalogKind.RUNE && it.name in grantedNames }
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

private fun Character.allSpecialKnowledges() = learnedKnowledges + arcaneKnowledges + battleTechniques
private fun SpecialKnowledge.isRunic() = name.equals("Rúnico", true) || name.equals("Runas", true)
private fun List<SpecialKnowledge>.replaceKnowledge(value: SpecialKnowledge) = map { if (it.id == value.id) value else it }
