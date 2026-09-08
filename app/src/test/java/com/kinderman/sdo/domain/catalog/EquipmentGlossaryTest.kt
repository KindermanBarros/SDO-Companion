package com.kinderman.sdo.domain.catalog

import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentGlossaryTest {
    @Test fun includesEveryRequiredRulesTerm() {
        val terms = EquipmentGlossary.entries.map { it.term }
        listOf(
            "P.G. — Proteção Geral",
            "P.L. — Proteção Local",
            "LA — Limitação de Agilidade",
            "Leve",
            "Pesado",
            "Delicado",
            "Dilaceração",
            "Anti-Pessoal",
            "Anti-Cavalaria",
            "Sobrecarga",
            "Bobina",
            "Passo de dado",
            "Categoria de dado",
            "Qualidade",
            "Durabilidade",
        ).forEach { required -> assertTrue("Termo ausente: $required", required in terms) }
    }

    @Test fun includesAllItemsMaterialsAndModifications() {
        val terms = EquipmentGlossary.entries.map { it.term }.toSet()
        val catalogTerms = listOf(
            ItemCreationRules.weaponMaterials,
            ItemCreationRules.armorMaterials,
            ItemCreationRules.weaponBases,
            ItemCreationRules.armorBases,
            ItemCreationRules.weaponModifications,
            ItemCreationRules.armorModifications,
        ).flatten().map { it.name }

        assertTrue(catalogTerms.all { it in terms })
    }
}
