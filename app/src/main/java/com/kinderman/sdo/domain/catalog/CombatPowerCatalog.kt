package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind

/** Power examples derived from martial training, kept separate from learned Battle Techniques. */
object CombatPowerCatalog {
    const val SOURCE = "50 Exemplos de Poderes Marciais"

    val entries: List<CatalogEntry> = KnowledgeCatalog.entries
        .filter { it.kind == CatalogKind.BATTLE_TECHNIQUE }
        .mapIndexed { index, technique ->
            val identity = martialIdentity(index, technique)
            val passive = index >= 3 && index % 3 == 0
            technique.copy(
                id = "power.martial.${technique.id.substringAfterLast('.')}",
                kind = CatalogKind.POWER,
                name = identity.name,
                group = identity.group,
                summary = identity.summary,
                cost = if (passive) "0 PE" else "1 PE",
                action = if (passive) "Passiva" else if (index % 2 == 0) "Ação" else "Reação",
                range = if (passive) "Pessoal" else "Arma",
                duration = if (passive) "Enquanto empunhar a arma indicada" else "Instantânea",
                source = SOURCE,
                version = BuiltInCatalog.VERSION,
                mechanicalEffect = identity.effect,
                ruleReference = "03 - Regras/Poderes/Exemplos marciais",
                activationCondition = identity.activation,
                keywords = (technique.keywords + identity.keywords).distinct(),
                abilitySource = AbilitySource.NARRATIVE,
                abilityCostType = AbilityCostType.ENERGY,
                abilityCostValue = if (passive) 0 else 1,
                abilityExecution = if (passive) AbilityExecution.PASSIVE else if (index % 2 == 0) AbilityExecution.ACTION else AbilityExecution.REACTION,
                abilityRange = AbilityRange.PERSONAL,
                targetArea = if (passive) "Você e a arma preferida" else "Um alvo ao alcance da arma",
                abilityDuration = if (passive) AbilityDuration.SCENE else AbilityDuration.INSTANT,
                abilityResistance = AbilityResistance.NONE,
            )
        }

    private fun martialIdentity(index: Int, technique: CatalogEntry): MartialIdentity = when (index) {
        0 -> MartialIdentity(
            "Jian // Linha Celeste",
            "Preferência de arma // Espada reta oriental",
            "Disciplina de ponta e linha para jian e outras espadas retas orientais.",
            "Enquanto empunhar uma jian ou espada reta oriental, você pode usar uma Ação para perfurar a guarda: faça o ataque com Vantagem; em acerto, reposicione-se 2 m sem provocar reação.",
            "Empunhar uma jian ou espada reta oriental.",
            listOf("Jian", "Espada reta", "Nova ação"),
        )
        1 -> MartialIdentity(
            "Dao // Lua Cortante",
            "Preferência de arma // Sabre oriental",
            "Movimento circular para dao e outras espadas curvas orientais.",
            "Ao evitar um ataque enquanto empunha dao ou espada curva oriental, use sua Reação para realizar um corte de retorno contra o agressor e mover 1 m.",
            "Empunhar uma dao ou espada curva oriental e evitar um ataque.",
            listOf("Dao", "Espada curva", "Contra-ataque"),
        )
        2 -> MartialIdentity(
            "Canalização Bélica",
            "Ataque mágico // Arma",
            "Conduz energia sobrenatural através de uma arma preparada.",
            "Escolha o tipo Arcano, Elétrico, Fogo, Frio ou Mental ao ativar. O próximo ataque da arma causa esse tipo de dano e pode atingir criaturas resistentes a dano mundano.",
            "Empunhar uma arma e declarar a canalização antes do ataque.",
            listOf("Ataque mágico", "Arma canalizada", "Tipo de dano"),
        )
        else -> MartialIdentity(
            technique.name,
            "Poder marcial // ${technique.group}",
            technique.summary,
            technique.mechanicalEffect.ifBlank { technique.summary },
            technique.activationCondition.ifBlank { "Usar ${technique.name} em uma situação de combate compatível." },
            listOf("Poder marcial", "Ação de combate"),
        )
    }

    private data class MartialIdentity(
        val name: String,
        val group: String,
        val summary: String,
        val effect: String,
        val activation: String,
        val keywords: List<String>,
    )
}
